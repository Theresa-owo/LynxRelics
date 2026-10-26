// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.mixin.PlayerBlockInvoker
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.acos
import kotlin.math.ceil

object Combat {
    private data class EchoHit(val level: ServerLevel, val owner: UUID, val target: UUID, val at: Long, val damage: Float)
    private val echoes = mutableListOf<EchoHit>()
    private val guardSlow = Content.id("bulwark_slow")
    fun validHostile(player: ServerPlayer, source: DamageSource): Boolean {
        val attacker = source.entity as? LivingEntity ?: return false
        return attacker !== player && !attacker.isAlliedTo(player) && (attacker is Enemy || attacker is Player && Balance.enabled("skills.pvp") && !attacker.isCreative && !attacker.isSpectator && player.canHarmPlayer(attacker))
    }
    @JvmStatic fun raised(player: ServerPlayer) {
        val stack = player.useItem; val state = player.relics(); val now = Skills.now(player)
        if (!stack.has(DataComponents.BLOCKS_ATTACKS)) return
        val upgrade=net.ciyuan.lynxrelics.content.VanillaMods.upgrade(stack)
        if(upgrade=="deflector"&&state.ready("deflector",now))state.deflectorUntilNanos=System.nanoTime()+200_000_000L
        if(upgrade=="breaching_fitting"&&player.isShiftKeyDown&&state.shieldCharge>=3&&state.ready("shield_push",now)){
            state.shieldCharge=0;state.cooldown("shield_push",now,Balance.ticks("shield.pushCooldown"))
            for(target in Skills.targets(player,2.0,70.0,64)){
                Skills.hit(player,target,Balance.value("shield.pushDamage").toFloat())
                val away=target.entity.position().subtract(player.position()).normalize();target.entity.knockback(.25,-away.x,-away.z,player.level().damageSources().playerAttack(player),3f)
            }
        }
        val capable = stack.`is`(Content.item("echo_shield")) || Skills.has(player, "keeper_gauntlet") || RelicEnchantments.level(stack, "resonant_guard") > 0
        if (capable && state.ready("parry_gate", now)) {
            val seconds = minOf(.35, .25 + (if (Skills.has(player, "keeper_gauntlet")) .05 else 0.0) + .02 * RelicEnchantments.level(stack, "resonant_guard"))
            state.parryUntilNanos = System.nanoTime() + (seconds * 1_000_000_000L).toLong()
            state.cooldown("parry_gate", now, 120)
        }
        if (player.isShiftKeyDown && state.storedEcho > 0 && state.echoUntil > now && state.ready("echo_bash", now) && stack.`is`(Content.item("echo_shield"))) {
            val damage = minOf(10f, (if (RelicEnchantments.spec(stack) == "b") 5f else 4f) + state.storedEcho)
            state.storedEcho = 0f; state.cooldown("echo_bash", now, 160)
            val contacts=Skills.targets(player,3.0,70.0,64)
            for (target in contacts) Skills.hit(player, target, damage)
            SkillFeedback.notice(player,"echo_bashed",contacts.size)
            stack.hurtAndBreak(12, player, player.usedItemHand)
        }
    }
    fun allowDamage(entity: LivingEntity, source: DamageSource, amount: Float): Boolean {
        val player = entity as? ServerPlayer ?: return true
        val state = player.relics(); val stack = player.useItem
        if (!player.isUsingItem || state.parryUntilNanos <= System.nanoTime() || amount <= 0 || !validHostile(player, source) || source.`is`(DamageTypeTags.BYPASSES_SHIELD) || DamageContext.current() != DamageOrigin.DIRECT || (source.directEntity?.entityTags()?.contains("lynx_reflected") == true)) return true
        if ((source.directEntity as? AbstractArrow)?.pierceLevel?.let { it > 0 } == true) return true
        val blocks = stack.get(DataComponents.BLOCKS_ATTACKS) ?: return true
        val position = source.sourcePosition ?: return true
        val direction = position.subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize()
        val look = player.lookAngle.multiply(1.0, 0.0, 1.0).normalize()
        val blocked = blocks.resolveBlockedDamage(source, amount, acos(direction.dot(look).coerceIn(-1.0, 1.0)))
        if (blocked + .0001f < amount) return true
        recordParry(player, stack, blocked)
        blocks.onBlocked(player.level(), player)
        blocks.hurtBlockingItem(player.level(), stack, player, player.usedItemHand, blocked)
        (source.directEntity as? LivingEntity)?.let { (player as PlayerBlockInvoker).lynxBlockReaction(player.level(), it, source, blocked, true) }
        return false
    }
    @JvmStatic fun blocked(player: ServerPlayer, source: DamageSource, amount: Float) {
        if (amount <= 0 || !validHostile(player, source) || DamageContext.current() != DamageOrigin.DIRECT) return
        val s=player.relics();val now=Skills.now(player)
        if(source.directEntity is LivingEntity&&source.directEntity===source.entity&&net.ciyuan.lynxrelics.content.VanillaMods.upgrade(player.useItem)=="breaching_fitting"&&s.lastShieldChargeTick!=now){s.shieldCharge=minOf(3,s.shieldCharge+1);s.lastShieldChargeTick=now}
        if (player.relics().parryUntilNanos > System.nanoTime()) recordParry(player, player.useItem, amount)
    }
    private fun recordParry(player: ServerPlayer, shield: ItemStack, blocked: Float) {
        val state = player.relics(); val now = Skills.now(player)
        state.parryUntilNanos = 0
        state.storedEcho = blocked.coerceIn(0f, 6f)
        val base = if (shield.`is`(Content.item("echo_shield")) && RelicEnchantments.spec(shield) == "a") 240 else 160
        val retention = minOf(280, base + (if (Skills.has(player, "resonant_necklace")) 40 else 0) + 20 * RelicEnchantments.accessory(player, RelicSlot.NECKLACE, "overtone"))
        state.echoUntil = now + retention
        SkillFeedback.notice(player,"parried",state.storedEcho)
        player.level().sendParticles(ParticleTypes.ENCHANTED_HIT, true, true, player.x, player.eyeY, player.z, 8, .4, .3, .4, .02)
        if (Skills.pieces(player, Route.ECHO) == 4 && state.ready("echo_set", now)) {
            val candidates = state.cooldowns.filter { (key, until) -> until > now && (key in setOf("echo_bash", "echo_arrow") || key == "necklace" && Skills.has(player, "resonant_necklace")) }
            candidates.maxByOrNull { it.value }?.let { (key, until) -> state.cooldowns[key] = maxOf(now, until - 40); state.cooldown("echo_set", now, 160) }
        }
    }
    @JvmStatic fun adjustedDamage(entity: LivingEntity, source: DamageSource, damage: Float): Float {
        val player = entity as? ServerPlayer ?: return damage
        if (damage <= 0) return damage
        val adjusted=net.ciyuan.lynxrelics.content.Endforged.resistMagic(player,source,damage)
        val state = player.relics(); val now = Skills.now(player)
        if (source.`is`(DamageTypeTags.IS_FALL)) {
            var reduction=RelicEnchantments.accessory(player,RelicSlot.CLOAK,"cushion")*.05f
            if(state.shelterAuraUntil>now&&state.ready("shelter_fall",now)){reduction+=Balance.value("beacon.shelterReduction").toFloat();state.cooldown("shelter_fall",now,300)}
            return damage*(1f-minOf(.25f,reduction))
        }
        if (!validHostile(player, source) || source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)) return adjusted
        if (Skills.has(player, "undertow_cloak") && state.tide >= Balance.ticks("tide.shieldCost") && state.ready("tide_ward", now)) {
            state.tide -= Balance.ticks("tide.shieldCost")
            val base = if (RelicEnchantments.spec(Skills.equipped(player, RelicSlot.CLOAK)) == "b") 320 else if (Skills.pieces(player, Route.TIDE) == 4) 360 else Balance.ticks("tide.shieldCooldown")
            state.cooldown("tide_ward", now, base); Skills.wear(player, RelicSlot.CLOAK, 1)
            player.level().sendParticles(ParticleTypes.NAUTILUS,true,true,player.x,player.y+1,player.z,12,.5,.5,.5,.01);SkillFeedback.notice(player,"ward")
            return (adjusted - Balance.value("tide.shieldAbsorb").toFloat()).coerceAtLeast(0f)
        }
        return adjusted
    }
    fun afterDamage(entity: LivingEntity, source: DamageSource, damage: Float) {
        if (damage <= 0) return
        if (entity is ServerPlayer) {
            val state = entity.relics(); val now = Skills.now(entity)
            state.lastDamage = now; finishSilence(entity)
            if (validHostile(entity, source) && source.directEntity is LivingEntity && Skills.has(entity, "murmur_cloak") && state.ready("echo_cloak", now)) {
                Investigations.create(entity, entity.position(), 40, 12.0); state.cooldown("echo_cloak", now, 400); Skills.wear(entity, RelicSlot.CLOAK, 1)
            }
        }
        val player = source.entity as? ServerPlayer ?: return
        if (source.directEntity !== player || DamageContext.current() != DamageOrigin.DIRECT || !Skills.enemy(player, entity)) return
        val state = player.relics(); val now = Skills.now(player)
        val conduction = RelicEnchantments.level(player.mainHandItem, "tide_conduction")
        val tideEquipped = Skills.pieces(player, Route.TIDE) > 0 || listOf(player.mainHandItem,player.offhandItem).any{(it.item as? net.ciyuan.lynxrelics.content.RelicItem)?.route == Route.TIDE}
        if ((tideEquipped || conduction > 0) && state.ready("tide_hit", now)) { state.tide = minOf(Skills.tideCapacity(player), state.tide + (if (tideEquipped) 3 else 0) + conduction); state.cooldown("tide_hit", now, 20) }
        val echo = RelicEnchantments.level(player.mainHandItem, "echo")
        if (echo > 0 && state.lastAttackCharged && state.ready("echo_hit", now) && entity.isAlive) {
            echoes.add(EchoHit(player.level(), player.uuid, entity.uuid, now + 10, damage * if (echo == 1) .10f else .15f)); state.cooldown("echo_hit", now, 40)
        }
    }
    @JvmStatic fun extraMelee(player: Player, target: net.minecraft.world.entity.Entity, base: Float, modified: Float): Float {
        if (player.level().isClientSide) return modified
        val n = RelicEnchantments.level(player.mainHandItem, "breach")
        val effective = if (target is net.minecraft.world.entity.boss.enderdragon.EnderDragonPart) target.parentMob else target
        val endHostiles = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Content.id("end_hostiles"))
        if (n == 0 || effective !is Enemy || !effective.`is`(endHostiles)) return modified
        return base + maxOf(modified - base, (n + 1).toFloat())
    }
    @JvmStatic fun claimReflection(arrow: AbstractArrow, target: net.minecraft.world.entity.Entity): Boolean {
        val player = target as? ServerPlayer ?: return false
        if (arrow.pickupItemStackOrigin.`is`(Items.ARROW) != true || arrow.pierceLevel > 0 || "lynx_reflected" in arrow.entityTags()) return false
        val state = player.relics(); val now = Skills.now(player)
        val cost = if (RelicEnchantments.spec(Skills.equipped(player, RelicSlot.NECKLACE)) == "b") 35 else 40
        val shield=player.useItem
        val shieldCanReflect=player.isUsingItem&&shield.has(DataComponents.BLOCKS_ATTACKS)&&net.ciyuan.lynxrelics.content.VanillaMods.upgrade(shield)=="deflector"&&state.deflectorUntilNanos>System.nanoTime()&&state.ready("deflector",now)&&arrow.position().subtract(player.position()).multiply(1.0,0.0,1.0).normalize().dot(player.lookAngle.multiply(1.0,0.0,1.0).normalize())>0
        if(shieldCanReflect){state.deflectorUntilNanos=0;state.cooldown("deflector",now,Balance.ticks("shield.deflectorCooldown"));shield.hurtAndBreak(1,player,player.usedItemHand)}else{
            if (state.mirrorUntilNanos <= System.nanoTime() || state.tide < cost || !Skills.has(player, "watermirror_necklace")) return false
            state.tide -= cost; state.mirrorUntil = 0; state.mirrorUntilNanos = 0; state.cooldown("necklace", now, Balance.ticks("tide.mirrorCooldown"));Skills.wear(player, RelicSlot.NECKLACE, 2)
        }
        arrow.addTag("lynx_reflected"); arrow.setCritArrow(false); arrow.clearFire()
        arrow.setBaseDamage(minOf((arrow as net.ciyuan.lynxrelics.mixin.ArrowDataAccessor).lynxBaseDamage(), 7.999 / arrow.deltaMovement.length().coerceAtLeast(.1)))
        return true
    }
    @JvmStatic fun reflect(arrow: AbstractArrow, target: net.minecraft.world.entity.Entity): Boolean {
        if (!claimReflection(arrow, target)) return false
        arrow.deflect(net.minecraft.world.entity.projectile.ProjectileDeflection.REVERSE, target, net.minecraft.world.entity.EntityReference.of<net.minecraft.world.entity.Entity>(target), false, 1.0)
        return true
    }
    @JvmStatic fun arrowShot(arrow: AbstractArrow) {
        val player = arrow.owner as? ServerPlayer ?: return
        if ("lynx_arrow_initialized" in arrow.entityTags()) return
        arrow.addTag("lynx_arrow_initialized")
        if(net.ciyuan.lynxrelics.content.VanillaMods.upgrade(arrow.weaponItem ?: ItemStack.EMPTY)=="quickdraw_string")arrow.setBaseDamage((arrow as net.ciyuan.lynxrelics.mixin.ArrowDataAccessor).lynxBaseDamage()*.9)
        if (!arrow.pickupItemStackOrigin.`is`(Content.item("echo_arrow")) || !player.isShiftKeyDown) return
        val state = player.relics(); val now = Skills.now(player)
        if (!state.ready("echo_arrow", now)) return
        val weapon = arrow.weaponItem ?: ItemStack.EMPTY
        val spec = RelicEnchantments.spec(weapon)
        val duration = minOf(240, (if (spec == "a") 180 else 120) + (if (Skills.pieces(player, Route.ECHO) >= 2) 20 else 0) + 20 * RelicEnchantments.level(weapon, "long_resonance"))
        arrow.addTag("lynx_echo_lure"); arrow.addTag("lynx_echo_duration_$duration"); arrow.addTag("lynx_echo_radius_${if (spec == "b") 16 else 12}")
        state.cooldown("echo_arrow", now, if (spec == "a") 120 else 80)
    }
    @JvmStatic fun arrowHitEntity(arrow: AbstractArrow, target: net.minecraft.world.entity.Entity): Boolean {
        if ("lynx_echo_lure" in arrow.entityTags()) { arrow.discard(); return true }
        if (arrow.level().isClientSide) return false
        val player = arrow.owner as? ServerPlayer ?: return false
        val effective = if (target is net.minecraft.world.entity.boss.enderdragon.EnderDragonPart) target.parentMob else target
        if (effective !is LivingEntity) return false
        if (arrow.pickupItemStackOrigin.`is`(Content.item("foam_arrow"))) {
            effective.clearFire()
            if (Skills.enemy(player, effective)) effective.addEffect(net.minecraft.world.effect.MobEffectInstance(Content.FOAM_SLOW, 20))
        }
        if (arrow.pickupItemStackOrigin.`is`(Content.item("scouting_arrow")) && effective !is Player && Skills.enemy(player, effective)) effective.addEffect(net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, 80))
        return false
    }
    @JvmStatic fun arrowHitBlock(arrow: AbstractArrow, position: Vec3) {
        if (arrow.level().isClientSide || "lynx_echo_lure" !in arrow.entityTags()) return
        val player = arrow.owner as? ServerPlayer ?: return
        val duration = arrow.entityTags().firstOrNull { it.startsWith("lynx_echo_duration_") }?.removePrefix("lynx_echo_duration_")?.toIntOrNull() ?: 120
        val radius = arrow.entityTags().firstOrNull { it.startsWith("lynx_echo_radius_") }?.removePrefix("lynx_echo_radius_")?.toDoubleOrNull() ?: 12.0
        Investigations.create(player, position, duration, radius); arrow.discard()
    }
    @JvmStatic fun step(player: Player): Boolean {
        if (player !is ServerPlayer) return player.relics().clientQuiet
        val state = player.relics(); val now = Skills.now(player)
        if (state.quietUntil <= now || state.quietSteps <= 0) return false
        state.quietStepTick = now; state.quietSteps--
        if (state.quietSteps == 0) finishSilence(player)
        return true
    }
    @JvmStatic fun quietEvent(player: Player, event: net.minecraft.core.Holder<GameEvent>): Boolean = player is ServerPlayer && event.`is`(GameEvent.STEP) && (player.relics().quietUntil > Skills.now(player) || player.relics().quietStepTick == Skills.now(player))
    private fun finishSilence(player: ServerPlayer) {
        val s = player.relics()
        if (s.quietUntil != 0L) Skills.wear(player, RelicSlot.BELT, 12)
        s.quietUntil = 0; s.quietSteps = 0
    }
    @JvmStatic fun air(entity: LivingEntity, original: Int, incoming: Int): Int {
        val player = entity as? ServerPlayer ?: return original
        val state = player.relics()
        if (incoming <= 0 && Skills.has(player, "undertow_belt") && state.extraAir > 0) { state.extraAir--; return 0 }
        return original
    }
    @JvmStatic fun shieldKnockback(entity: LivingEntity, strength: Double): Double = if (entity.isBlocking) strength * (1 - .05 * RelicEnchantments.level(entity.useItem, "bulwark")) else strength
    fun tick(player: ServerPlayer) {
        val s = player.relics(); val now = Skills.now(player)
        val quietEligible = Skills.has(player, "silent_belt") && player.isShiftKeyDown && player.onGround() && !player.isSprinting
        if (quietEligible) {
            s.sneakTicks++
            if (s.sneakTicks >= 12 && s.quietUntil == 0L && s.ready("silence", now)) {
                val spec = RelicEnchantments.spec(Skills.equipped(player, RelicSlot.BELT))
                s.quietUntil = now + 120; s.quietSteps = if (spec == "a") 8 else 6
                s.cooldown("silence", now, if (spec == "b") 300 else 360)
            }
            if (s.quietUntil in 1..now) finishSilence(player)
        } else { s.sneakTicks = 0; finishSilence(player) }
        val airCap = if (Skills.has(player, "undertow_belt")) if (Skills.has(player, "undertow_cloak") && RelicEnchantments.spec(Skills.equipped(player, RelicSlot.CLOAK)) == "a") 600 else 400 else 0
        s.extraAir = minOf(airCap, s.extraAir)
        if (!player.isUnderWater) s.extraAir = minOf(airCap, s.extraAir + 4)
        val modifier = player.getAttribute(Attributes.MOVEMENT_SPEED)
        val n = if (player.isBlocking) RelicEnchantments.level(player.useItem, "bulwark") else 0
        updateSlow(modifier,guardSlow,-.03*n)
        val rim=Content.id("iron_rim_slow")
        updateSlow(modifier,rim,if(player.isBlocking&&net.ciyuan.lynxrelics.content.VanillaMods.upgrade(player.useItem)=="iron_rim")-.05 else 0.0)
        if (s.stillPosition.distanceToSqr(player.position()) > .0025) { s.stillPosition = player.position(); s.stillSince = now }
        if (s.echoUntil <= now) s.storedEcho = 0f
    }
    fun tickServer(server: MinecraftServer) {
        val now = server.overworld().gameTime
        val due = echoes.filter { it.at <= now }; echoes.removeAll(due.toSet())
        for (hit in due) {
            val player = server.playerList.getPlayer(hit.owner) ?: continue
            val target = hit.level.getEntity(hit.target) as? LivingEntity ?: continue
            if (player.level() === hit.level && Skills.enemy(player, target)) Skills.hit(player, target, hit.damage, DamageOrigin.ECHO)
        }
        Investigations.tick(server)
    }
    fun clear() { echoes.clear(); Investigations.clear() }
    private fun updateSlow(instance:net.minecraft.world.entity.ai.attributes.AttributeInstance?,id:net.minecraft.resources.Identifier,amount:Double){
        if(instance==null)return;val existing=instance.getModifier(id)
        if(amount==0.0){if(existing!=null)instance.removeModifier(id)}else if(existing?.amount()!=amount){instance.removeModifier(id);instance.addTransientModifier(AttributeModifier(id,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))}
    }
}

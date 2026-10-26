// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core

import net.ciyuan.lynxrelics.compat.ElytraCompat
import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.content.RelicItem
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.PowerParticleOption
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.ceil

enum class DamageOrigin { DIRECT, SKILL, ECHO, REFLECTION }
object DamageContext {
    private val origin = ThreadLocal.withInitial { DamageOrigin.DIRECT }
    fun current(): DamageOrigin = origin.get()
    fun <T> with(value: DamageOrigin, block: () -> T): T {
        val before = origin.get(); origin.set(value)
        try { return block() } finally { origin.set(before) }
    }
}

object Skills {
    private val slowId = Content.id("breathing_slow")
    fun now(player: ServerPlayer) = player.level().server.overworld().gameTime
    fun equipped(player: Player, slot: RelicSlot) = player.relics().accessories.getItem(slot.ordinal)
    fun has(player: Player, path: String): Boolean { val item=Content.item(path) as? RelicItem ?: return false; if(item.route!=null&&!Balance.enabled("${item.route.name.lowercase()}.enabled"))return false;val slot=item.slot ?: return false; val stack=equipped(player,slot); return stack.`is`(item) && !stack.isBroken }
    fun pieces(player: Player, route: Route) = player.relics().accessories.items.withIndex().count { (i, stack) ->
        val item = stack.item as? RelicItem
        return@count Balance.enabled("${route.name.lowercase()}.enabled")&&!stack.isEmpty && !stack.isBroken && item?.route == route && item.slot?.ordinal == i
    }
    fun breathCapacity(player: Player) = player.relics().breathCapacity()
    fun tideCapacity(player: Player) = player.relics().tideCapacity()
    fun enemy(player: ServerPlayer, target: LivingEntity): Boolean {
        if (target === player || !target.isAlive || target.isAlliedTo(player)) return false
        if (target is Player) return Balance.enabled("skills.pvp") && player.canHarmPlayer(target) && !target.isCreative && !target.isSpectator
        return target is Enemy || Balance.enabled("skills.hurtPassive")
    }
    data class Contact(val entity: LivingEntity, val part: net.minecraft.world.entity.Entity) { fun position() = part.boundingBox.center }
    private fun clearLine(player: ServerPlayer, point: Vec3) = player.level().clip(net.minecraft.world.level.ClipContext(player.eyePosition, point, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player)).type == net.minecraft.world.phys.HitResult.Type.MISS
    fun targets(player: ServerPlayer, range: Double, angle: Double, max: Int): List<Contact> {
        val living=player.level().getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.inflate(range)) { enemy(player,it) }.toMutableSet()
        living.addAll(player.level().getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EnderDragon::class.java, player.boundingBox.inflate(range+32)) { enemy(player,it) })
        return living.mapNotNull { target ->
            val parts=if(target is net.minecraft.world.entity.boss.enderdragon.EnderDragon) target.subEntities.toList() else listOf(target)
            parts.filter { part -> val v=part.boundingBox.center.subtract(player.eyePosition); v.lengthSqr()<=range*range&&v.normalize().dot(player.lookAngle)>=cos(Math.toRadians(angle/2))&&clearLine(player,part.boundingBox.center) }.minByOrNull { it.distanceToSqr(player) }?.let { Contact(target,it) }
        }.sortedBy { it.part.distanceToSqr(player) }.take(max)
    }
    fun pathTargets(player:ServerPlayer,start:Vec3,end:Vec3,width:Double,max:Int):List<Contact>{
        val bounds=net.minecraft.world.phys.AABB(start,end).inflate(width,.35,width)
        val living=player.level().getEntitiesOfClass(LivingEntity::class.java,bounds){enemy(player,it)}.toMutableSet()
        living.addAll(player.level().getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EnderDragon::class.java,bounds.inflate(32.0)){enemy(player,it)})
        return living.mapNotNull{target->
            val parts=if(target is net.minecraft.world.entity.boss.enderdragon.EnderDragon)target.subEntities.toList()else listOf(target)
            parts.filter{part->val box=part.boundingBox.inflate(width,.35,width);val point=if(box.contains(start))start else box.clip(start,end).orElse(null);point!=null&&clearLine(player,point)}.minByOrNull{it.distanceToSqr(start)}?.let{Contact(target,it)}
        }.sortedBy{it.part.distanceToSqr(start)}.take(max)
    }
    fun loadedRay(player:ServerPlayer,start:Vec3,direction:Vec3,range:Double):Vec3{
        var end=start
        for(step in 1..ceil(range*4).toInt()){val next=start.add(direction.scale(minOf(range,step*.25)));val pos=BlockPos.containing(next);if(!player.level().chunkSource.hasChunk(pos.x shr 4,pos.z shr 4))break;end=next}
        return player.level().clip(net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player)).location
    }
    fun hit(player: ServerPlayer, target: Contact, damage: Float, origin: DamageOrigin = DamageOrigin.SKILL, pulse: Boolean = false): Boolean = DamageContext.with(origin) {
        val source=if(pulse)RelicDamage.pulse(player.level(),player) else player.damageSources().playerAttack(player)
        if(target.entity is net.minecraft.world.entity.boss.enderdragon.EnderDragon && target.part is net.minecraft.world.entity.boss.enderdragon.EnderDragonPart) target.entity.hurt(player.level(),target.part,source,damage) else target.entity.hurtServer(player.level(),source,damage)
    }
    fun hit(player: ServerPlayer, target: LivingEntity, damage: Float, origin: DamageOrigin = DamageOrigin.SKILL) = DamageContext.with(origin) {
        if(target is net.minecraft.world.entity.boss.enderdragon.EnderDragon) target.hurt(player.level(),target.head,RelicDamage.pulse(player.level(),player),damage) else target.hurtServer(player.level(), RelicDamage.pulse(player.level(),player), damage)
    }
    fun wear(player: ServerPlayer, slot: RelicSlot, amount: Int) {
        val stack = equipped(player, slot)
        if (stack.isDamageableItem) stack.hurtAndBreak(amount, player.level(), player) { player.relics().accessories.setItem(slot.ordinal, ItemStack.EMPTY) }
    }
    fun action(player: ServerPlayer, action: Int) {
        if (!player.isAlive || player.isSpectator) return
        val s = player.relics(); val now = now(player)
        if(action !in 0..6)return
        s.lastReceivedAction=action;s.lastReceivedAt=now;s.receivedActions++;SkillFeedback.creativeResources(player)
        if(action!=5&&s.actionTicks[action]==now)return
        s.actionTicks[action]=now
        if(action==2&&bindLookTarget(player))return
        if(!SkillFeedback.beforeAction(player,action))return
        when (action) {
            0 -> player.openMenu(net.ciyuan.lynxrelics.menu.AccessoryMenu)
            1 -> gauntlet(player)
            2 -> necklace(player)
            3 -> flap(player)
            4 -> {val item=player.mainHandItem.item as? RelicItem;if(item==null||item.action.isEmpty())SkillFeedback.noHeldSkill(player)else useItem(player,InteractionHand.MAIN_HAND,item.action,true)}
            5 -> if (s.channel == "breath") finishBreath(player)
            6 -> flap(player)
        }
    }
    internal fun dragonCost(player: ServerPlayer, base: Int, now: Long): Int = RelicRules.breathCost(base, pieces(player, Route.DRAGON) == 4 && player.relics().ready("dragon_set", now))
    private fun bindLookTarget(player:ServerPlayer):Boolean{
        if(!has(player,"homing_dragon_necklace"))return false
        val hit=player.pick(4.5,1f,false) as? net.minecraft.world.phys.BlockHitResult?:return false
        if(hit.type!=net.minecraft.world.phys.HitResult.Type.BLOCK||!player.level().getBlockState(hit.blockPos).`is`(Blocks.LODESTONE))return false
        player.relics().anchorDimension=player.level().dimension().identifier().toString();player.relics().anchorPos=hit.blockPos.asLong()
        player.sendOverlayMessage(Component.translatable("msg.lynxrelics.anchor_bound"));return true
    }
    private fun spendDragon(player: ServerPlayer, cost: Int, pressure: Double, now: Long, discount: Boolean = true): Boolean {
        val s = player.relics()
        if (s.overloadUntil > now || s.breath < cost) return false
        s.breath -= cost; s.pressure = (s.pressure + RelicRules.pressureGain(pressure, pieces(player, Route.DRAGON))).coerceAtMost(100.0)
        s.lastSkill = now
        if (discount && pieces(player, Route.DRAGON) == 4 && s.ready("dragon_set", now)) s.cooldown("dragon_set", now, 400)
        if (s.pressure >= 100) { s.overloadUntil = now + Balance.ticks("breath.overloadTicks"); player.sendOverlayMessage(Component.translatable("msg.lynxrelics.overloaded")) }
        return true
    }
    private fun gauntlet(player: ServerPlayer) {
        val s = player.relics(); val now = now(player)
        when {
            has(player, "dragonthroat_gauntlet") && s.channel.isEmpty() && s.ready("gauntlet", now) && s.overloadUntil <= now -> {
                val cost = dragonCost(player, Balance.ticks("breath.gauntletCost"), now)
                if (s.breath < cost) return
                s.channel = "breath"; s.channelStarted = now; s.channelPulses = 0; s.channelCost = cost; s.channelPressure = 0.0
                SkillFeedback.notice(player,"charging_breath",Balance.ticks("breath.gauntletChargeTicks")/20.0)
            }
            has(player, "tidepull_gauntlet") && s.tide >= Balance.ticks("tide.pullCost") && s.ready("gauntlet", now) -> {
                val targets = targets(player, Balance.value("tide.pullRange"), 70.0, 3)
                s.tide -= Balance.ticks("tide.pullCost"); s.cooldown("gauntlet", now, RelicEnchantments.gauntletCooldown(player,Balance.ticks("tide.pullCooldown")))
                for (target in targets) {
                    hit(player, target, Balance.value("tide.pullDamage").toFloat())
                    if (target.entity !is Player) {
                        val pull=player.position().subtract(target.entity.position()).multiply(1.0,0.0,1.0).normalize()
                        val origin=target.entity.position();var end=origin
                        for(step in 1..10){val next=origin.add(pull.scale(step*.1));if(!player.level().noCollision(target.entity,target.entity.boundingBox.move(next.subtract(origin))))break;end=next}
                        target.entity.setPos(end)
                    }
                }
                wear(player, RelicSlot.GAUNTLET, 1)
                player.level().sendParticles(ParticleTypes.NAUTILUS,true,true,player.x,player.eyeY,player.z,16,1.0,.3,1.0,.04)
                SkillFeedback.notice(player,"undertow_released",targets.size)
            }
        }
    }
    private fun finishBreath(player: ServerPlayer) {
        val s = player.relics()
        if (s.channelPulses > 0) { s.extendCooldown("gauntlet", now(player), RelicEnchantments.gauntletCooldown(player,Balance.ticks("breath.gauntletCooldown"))); wear(player, RelicSlot.GAUNTLET, 1) }
        s.channel = ""; player.getAttribute(Attributes.MOVEMENT_SPEED)?.removeModifier(slowId)
    }
    private fun pulseBreath(player: ServerPlayer, now: Long) {
        val s = player.relics()
        if (!has(player, "dragonthroat_gauntlet") || !player.isAlive || player.isSpectator || s.overloadUntil > now) { finishBreath(player); return }
        val age = now - s.channelStarted - Balance.ticks("breath.gauntletChargeTicks")
        if (age < 0) { if (age % 3 == 0L) player.level().sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1f), player.x, player.eyeY, player.z, 2, 0.1, 0.1, 0.1, 0.01); return }
        val speed = player.getAttribute(Attributes.MOVEMENT_SPEED)
        if (speed != null && !speed.hasModifier(slowId)) speed.addTransientModifier(AttributeModifier(slowId, -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
        val pulses = Balance.ticks("breath.gauntletPulses").coerceIn(1, 24)
        if(age>=Balance.ticks("breath.gauntletPulseTicks").coerceAtLeast(1)*pulses){finishBreath(player);return}
        if(s.channelPulses>=pulses)return
        if (age % Balance.ticks("breath.gauntletPulseTicks").coerceAtLeast(1) != 0L) return
        val next = ceil(s.channelCost.toDouble() * (s.channelPulses + 1) / pulses).toInt()
        val before = ceil(s.channelCost.toDouble() * s.channelPulses / pulses).toInt()
        if (!spendDragon(player, next - before, Balance.value("breath.gauntletPressure") / pulses, now, false)) { finishBreath(player); return }
        if(s.channelPulses==0&&s.channelCost<Balance.ticks("breath.gauntletCost"))s.cooldown("dragon_set",now,400)
        s.extendCooldown("gauntlet",now,RelicEnchantments.gauntletCooldown(player,Balance.ticks("breath.gauntletCooldown")))
        val spec=RelicEnchantments.spec(equipped(player,RelicSlot.GAUNTLET))
        val contacts=targets(player, if(spec=="a")9.0 else Balance.value("breath.gauntletRange"), if(spec=="b")35.0 else 60.0, 64)
        for (target in contacts) hit(player, target, (Balance.value("breath.gauntletDamage")*if(spec=="b")1.2 else 1.0).toFloat(), pulse=true)
        if(s.channelPulses==0)SkillFeedback.notice(player,if(contacts.isEmpty())"breathing_no_target"else"breathing",contacts.size)
        val aim = player.lookAngle
        for (i in 1..7) { val pos = player.eyePosition.add(aim.scale(i.toDouble())); player.level().sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1f), pos.x, pos.y, pos.z, 3, i * 0.05, i * 0.04, i * 0.05, 0.01) }
        s.channelPulses++
    }
    private fun flap(player: ServerPlayer) {
        val s = player.relics(); val now = now(player)
        if (!has(player, "dragonwing_cloak") || player.onGround() || player.isPassenger || !s.ready("flap", now)) return
        val gliding = player.isFallFlying && !ElytraCompat.actualWings(player).isEmpty
        if (!gliding && s.airborneUsed) return
        val cost = dragonCost(player, Balance.ticks(if (gliding) "breath.glideCost" else "breath.flapCost"), now)
        if (!spendDragon(player, cost, if (gliding) 7.5 else 15.0, now)) return
        val aim = player.lookAngle.multiply(1.0, 0.0, 1.0).normalize()
        val v = player.deltaMovement
        val spec=RelicEnchantments.spec(equipped(player,RelicSlot.CLOAK))
        val horizontal=if(spec=="b")1.05 else Balance.value("breath.flapHorizontal")
        player.deltaMovement = if (gliding) v.add(aim.scale(0.18)) else Vec3(aim.x * horizontal, maxOf(v.y, if(spec=="a").55 else Balance.value("breath.flapUp")), aim.z * horizontal)
        s.airborneUsed = true; s.cooldown("flap", now, Balance.ticks("breath.flapCooldown"))
        player.connection.send(ClientboundSetEntityMotionPacket(player)); wear(player, RelicSlot.CLOAK, 1)
        SkillFeedback.notice(player,if(gliding)"glide_corrected"else"flapped")
    }
    private fun necklace(player: ServerPlayer) {
        val s = player.relics(); val now = now(player)
        if (!s.ready("necklace", now)) return
        when {
            has(player, "homing_dragon_necklace") && s.anchorDimension == player.level().dimension().identifier().toString() && s.channel.isEmpty() && s.breath >= dragonCost(player,12,now) -> {
                s.channel = "recall"; s.channelStarted = now; s.channelOrigin = player.position()
                SkillFeedback.notice(player,"recall_started")
            }
            has(player, "resonant_necklace") -> {
                s.cooldown("necklace", now, 320); Investigations.create(player,player.pick(12.0,1f,false).location,40,12.0);wear(player,RelicSlot.NECKLACE,1)
                SkillFeedback.notice(player,"echo_mark")
            }
            has(player, "watermirror_necklace") && s.tide >= (if(RelicEnchantments.spec(equipped(player,RelicSlot.NECKLACE))=="b")35 else 40) -> {
                val seconds=minOf(.35,.25+(if(RelicEnchantments.spec(equipped(player,RelicSlot.NECKLACE))=="a").05 else 0.0)+.025*RelicEnchantments.accessory(player,RelicSlot.NECKLACE,"mirror"))
                s.mirrorUntil=now+ceil(seconds*20).toInt();s.mirrorUntilNanos=System.nanoTime()+(seconds*1_000_000_000L).toLong();s.cooldown("necklace",now,60)
                player.level().sendParticles(ParticleTypes.ENCHANTED_HIT,true,true,player.x,player.eyeY,player.z,12,.4,.4,.4,.01);SkillFeedback.notice(player,"mirror_ready",seconds)
            }
        }
    }
    fun tick(player: ServerPlayer) {
        val s = player.relics(); val now = now(player)
        SkillFeedback.creativeResources(player)
        if(s.presentationMigrationPending){
            for(i in 0 until player.inventory.containerSize)net.ciyuan.lynxrelics.content.RelicLore.refreshOwned(player.inventory.getItem(i))
            for(stack in s.accessories.items)net.ciyuan.lynxrelics.content.RelicLore.refreshOwned(stack)
            for(slot in listOf(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET,net.minecraft.world.entity.EquipmentSlot.OFFHAND))net.ciyuan.lynxrelics.content.RelicLore.refreshOwned(player.getItemBySlot(slot))
            s.presentationMigrationPending=false
        }
        s.breath = s.breath.coerceIn(0, breathCapacity(player)); s.tide = s.tide.coerceIn(0, tideCapacity(player))
        if (player.onGround()) { if (++s.groundTicks >= 10) s.airborneUsed = false } else s.groundTicks = 0
        if (s.channel == "breath") pulseBreath(player, now)
        Conch.tick(player)
        if (s.channel in setOf("rift", "wave", "tide_dash")) tickWeapon(player,now)
        if (s.channel == "recall") {
            val necklace=equipped(player,RelicSlot.NECKLACE)
            var baseTicks=if(RelicEnchantments.spec(necklace)=="a")80 else 120
            if(net.ciyuan.lynxrelics.content.AnchorAddons.stable(player.level(),BlockPos.of(s.anchorPos)))baseTicks=maxOf(80,baseTicks-40)
            val castTicks=(baseTicks-10*RelicEnchantments.level(necklace,"homing")).coerceAtLeast(60)
            if (!has(player, "homing_dragon_necklace") || s.channelOrigin.distanceToSqr(player.position()) > 0.0225 || s.lastDamage >= s.channelStarted || !player.isAlive) { s.channel = ""; s.extendCooldown("necklace", now, 60);SkillFeedback.notice(player,"interrupted") }
            else if (now - s.channelStarted >= castTicks) {
                val anchor = BlockPos.of(s.anchorPos)
                player.level().getChunk(anchor.x shr 4,anchor.z shr 4)
                val dest = Vec3.atBottomCenterOf(anchor.above())
                val cost=dragonCost(player,12,now)
                if (player.level().getBlockState(anchor).`is`(Blocks.LODESTONE) && safe(player, dest) && s.breath>=cost && s.overloadUntil<=now && player.teleportTo(player.level(),dest.x,dest.y,dest.z,emptySet(),player.yRot,player.xRot,false)) {
                    spendDragon(player,cost,0.0,now);s.cooldown("necklace",now,if(RelicEnchantments.spec(necklace)=="b")4800 else 6000);SkillFeedback.notice(player,"returned")
                } else {s.extendCooldown("necklace", now, 60);SkillFeedback.notice(player,"unsafe_destination")}
                s.channel = ""
            }
        }
        if (s.mirrorUntil in 1..now) { s.mirrorUntil = 0; s.mirrorUntilNanos=0; s.tide = (s.tide - 5).coerceAtLeast(0) }
        if (now % 20 == 0L) {
            if (s.overloadUntil in 1..now) { s.overloadUntil = 0; s.pressure = 60.0 }
            if (now - s.lastSkill >= 60 && s.overloadUntil == 0L) s.pressure = (s.pressure - Balance.value("breath.pressureDecay")).coerceAtLeast(0.0)
            if (pieces(player, Route.TIDE) > 0 || listOf(player.mainHandItem,player.offhandItem).any{(it.item as? RelicItem)?.route == Route.TIDE}) {
                when {
                    player.isUnderWater -> { s.tide = minOf(tideCapacity(player), s.tide + if (pieces(player, Route.TIDE) == 4) 6 else 5); s.lastWater = now }
                    player.level().isRainingAt(player.blockPosition()) -> { s.tide = minOf(tideCapacity(player), s.tide + 1); s.lastWater = now }
                    now - s.lastWater >= if (pieces(player, Route.TIDE) >= 2) 160 else 100 -> s.tide = (s.tide - 1).coerceAtLeast(0)
                }
            }
        }
        RelicNetworking.sync(player)
    }
    fun safe(player: ServerPlayer, dest: Vec3) = player.level().worldBorder.isWithinBounds(BlockPos.containing(dest)) && player.level().hasChunkAt(BlockPos.containing(dest)) && player.level().noCollision(player, player.boundingBox.move(dest.subtract(player.position()))) && !player.level().getBlockState(BlockPos.containing(dest)).fluidState.`is`(net.minecraft.tags.FluidTags.LAVA)
    fun interrupt(player: ServerPlayer) { if(player.relics().channel=="breath")finishBreath(player);else if(player.relics().channel=="recall")player.relics().extendCooldown("necklace",now(player),60);else if(player.relics().channel=="conch_return")player.relics().extendCooldown("conch_return",now(player),60);Conch.leave(player);player.relics().channel="" }
    internal fun tickWeapon(player: ServerPlayer, now: Long) {
        val s=player.relics();val mode=s.channel
        val expected=if(mode=="rift")"rift_blade" else "tide_spear"
        val weapon=player.getItemInHand(s.channelHand)
        if(!weapon.`is`(Content.item(expected))||!player.isAlive||player.isSpectator){s.channel="";return}
        if(now-s.channelStarted<12)return
        val spec=RelicEnchantments.spec(weapon)
        if(mode=="rift"){
            val cost=dragonCost(player,10,now);if(!spendDragon(player,cost,25.0,now)){s.channel="";return}
            val start=player.position();val aim=player.lookAngle.normalize();var end=start
            val range=if(spec=="b")4.0 else 6.0
            for(step in 1..(range*4).toInt()){val candidate=start.add(aim.scale(step*.25));if(!safe(player,candidate))break;end=candidate}
            val contacts=pathTargets(player,start.add(0.0,1.0,0.0),end.add(0.0,1.0,0.0),.9,if(spec=="a")5 else 3)
            for(target in contacts)hit(player,target,12f)
            player.stopUsingItem();player.teleportTo(end.x,end.y,end.z);s.cooldown("rift",now,if(spec=="b")180 else 240);s.cooldown("weapon_recovery",now,7)
            SkillFeedback.notice(player,"rift_released",contacts.size)
        }else if(mode=="wave"){
            if(s.tide<25){s.channel="";return};s.tide-=25;s.cooldown("wave",now,160);s.cooldown("tide_lock",now,40)
            val start=player.position().add(0.0,1.0,0.0);val end=loadedRay(player,start,player.lookAngle.normalize(),if(spec=="a")10.0 else 8.0)
            val contacts=pathTargets(player,start,end,.75,3)
            for(target in contacts)if(hit(player,target,10f))target.entity.addEffect(net.minecraft.world.effect.MobEffectInstance(Content.FOAM_SLOW,40,1))
            SkillFeedback.notice(player,"wave_released",contacts.size)
            val distance=start.distanceTo(end);for(i in 0..ceil(distance*2).toInt()){val p=start.lerp(end,if(distance<.01)0.0 else (i/(distance*2)).coerceIn(0.0,1.0));player.level().sendParticles(ParticleTypes.NAUTILUS,true,true,p.x,p.y,p.z,2,.6,.2,.6,.0)}
        }else{
            if(s.tide<40||!player.onGround()&&!player.isInWater){s.channel="";return};s.tide-=40;s.cooldown("tide_dash",now,240);s.cooldown("tide_lock",now,40)
            val start=player.position();val direction=player.lookAngle.multiply(1.0,0.0,1.0).normalize();var end=start
            for(step in 1..(if(spec=="b")32 else 24)){val candidate=start.add(direction.scale(step*.25));if(!safe(player,candidate))break;end=candidate};player.teleportTo(end.x,end.y,end.z)
            SkillFeedback.notice(player,"tide_dashed")
        }
        s.channel=""
    }
    fun useItem(player: ServerPlayer, hand: InteractionHand, action: String, fromKey: Boolean = false): InteractionResult {
        if (action.isEmpty()) return InteractionResult.PASS
        val route=(player.getItemInHand(hand).item as? RelicItem)?.route
        if(route!=null&&!Balance.enabled("${route.name.lowercase()}.enabled")){SkillFeedback.notice(player,"route_disabled");return InteractionResult.FAIL}
        if (!fromKey && action in setOf("rift", "wave", "conch") && hand == InteractionHand.MAIN_HAND && player.offhandItem.item is net.minecraft.world.item.ShieldItem) return InteractionResult.PASS
        player.relics().lastUseAction=action;player.relics().lastUseAt=now(player)
        SkillFeedback.creativeResources(player)
        if(!SkillFeedback.beforeItem(player,hand,action))return InteractionResult.FAIL
        val s = player.relics(); val now = now(player)
        when (action) {
            "firework", "pearl" -> return net.ciyuan.lynxrelics.content.InfiniteItems.use(player, hand, action == "firework")
            "conch" -> return Conch.use(player,hand)
            "embryo" -> return net.ciyuan.lynxrelics.mob.RelicMobs.spawnWisp(player,hand)
            "compass" -> return net.ciyuan.lynxrelics.content.AnchorAddons.compass(player,player.getItemInHand(hand))
            "refill" -> {
                if (s.breath >= breathCapacity(player)) return InteractionResult.FAIL
                val added = minOf(Balance.ticks("breath.refill"), breathCapacity(player) - s.breath)
                s.breath += added; player.getItemInHand(hand).consume(1, player)
                player.sendOverlayMessage(Component.translatable("msg.lynxrelics.breath_refill",added,s.breath,breathCapacity(player)))
            }
            "rift" -> {
                if (!s.ready("rift", now) || s.channel.isNotEmpty() || s.breath<dragonCost(player,10,now) || s.overloadUntil>now) return InteractionResult.FAIL
                s.channel="rift";s.channelStarted=now;s.channelHand=hand;SkillFeedback.notice(player,"weapon_charging",.6)
            }
            "wave" -> {
                val dash=player.isShiftKeyDown
                if (s.tide < (if(dash)40 else 25) || !s.ready(if(dash)"tide_dash" else "wave", now) || !s.ready("tide_lock",now) || s.channel.isNotEmpty()) return InteractionResult.FAIL
                s.channel=if(dash)"tide_dash" else "wave";s.channelStarted=now;s.channelHand=hand;SkillFeedback.notice(player,"weapon_charging",.6)
            }
            else -> return InteractionResult.PASS
        }
        return InteractionResult.SUCCESS
    }
}

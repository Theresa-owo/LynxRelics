// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.raid

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.Balance
import net.ciyuan.lynxrelics.core.RelicRules
import net.ciyuan.lynxrelics.mixin.EndFightAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.PowerParticleOption
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.dimension.end.EnderDragonFight
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import java.util.Locale
import java.util.UUID
import kotlin.math.*

object DragonRaid {
    @JvmField val ENABLED: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(EnderDragon::class.java, EntityDataSerializers.BOOLEAN)
    private const val OWNER = "lynx_raid_owner_"
    private const val EXPIRY = "lynx_raid_expiry_"
    @JvmStatic fun bootstrap() { /* force data accessor allocation during EnderDragon class initialization */ }
    private fun state(dragon: EnderDragon) = (dragon as RaidHolder).lynxRaid()
    @JvmStatic fun define(builder: SynchedEntityData.Builder) { builder.define(ENABLED, false) }
    @JvmStatic fun active(dragon: EnderDragon) = dragon.entityData.get(ENABLED)
    @JvmStatic fun initialize(dragon: EnderDragon) {
        if (!dragon.level().isClientSide && Balance.enabled("raid.enabled")) {
            dragon.entityData.set(ENABLED, true); markAttribute(dragon); dragon.health = 3000f
        }
    }
    @JvmStatic fun markAttribute(dragon: EnderDragon) {
        val attribute = dragon.getAttribute(Attributes.MAX_HEALTH) ?: return
        (attribute as DragonHealthAttribute).lynxDragonHealth(active(dragon))
        if (active(dragon)) attribute.baseValue = 3000.0
    }
    @JvmStatic fun save(dragon: EnderDragon, out: ValueOutput) { if (active(dragon)) state(dragon).save(out.child("LynxRaid")) }
    @JvmStatic fun load(dragon: EnderDragon, input: ValueInput) {
        if (!active(dragon)) return
        val saved = input.child("LynxRaid")
        if (saved.isPresent) {state(dragon).load(saved.get());if(state(dragon).skill.isEmpty())RaidSkills.releaseFlight(dragon,state(dragon))}
        else if (dragon.health in 0.001f..200f) dragon.health = (dragon.health * 15f).coerceAtMost(3000f)
        // A saved LynxRaid never resets health. Vanilla reads Health before this callback.
    }
    @JvmStatic fun title(dragon: EnderDragon): Component {
        val s = state(dragon)
        val health = dragon.health
        val shown = if (health == floor(health)) health.toInt().toString() else String.format(Locale.ROOT, "%.1f", health)
        return Component.translatable("boss.lynxrelics.dragon.status",shown,Component.translatable("boss.lynxrelics.phase.${s.phase}"),s.recalls,Balance.ticks("raid.maxRecalls"))
    }
    @JvmStatic fun updateBar(fight: EnderDragonFight, dragon: EnderDragon) {
        if (!active(dragon)) return
        val bar = (fight as EndFightAccessor).lynxDragonEvent()
        bar.name = title(dragon); bar.progress = (dragon.health / dragon.maxHealth).coerceIn(0f, 1f)
    }
    @JvmStatic fun tick(dragon: EnderDragon) {
        val level = dragon.level() as? ServerLevel ?: return
        if (!active(dragon) || !dragon.isAlive) return
        val s = state(dragon); val now = level.server.overworld().gameTime
        if(dragon.phaseManager.currentPhase.phase==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING){RaidSkills.cancel(dragon,s);return}
        val phase = RelicRules.phase(dragon.health)
        if (phase > s.phase) {
            RaidSkills.cancel(dragon,s);s.phase = phase; s.transitionUntil = now + Balance.ticks("raid.transitionTicks");s.nextSkill = s.transitionUntil
            nearby(dragon).forEach { it.sendOverlayMessage(Component.translatable("boss.lynxrelics.transition",Component.translatable("boss.lynxrelics.phase.$phase"))) }
        }
        val expired = s.crystals.filterValues { it <= now }.keys.toList()
        for (uuid in expired) { level.getEntity(UUID.fromString(uuid))?.discard(); s.crystals.remove(uuid) }
        if (s.nextRecall == 0L) s.nextRecall = now + recallInterval(s.phase)
        if(participants(dragon).isEmpty()){
            if(s.emptySince==0L)s.emptySince=now
            RaidSkills.cancel(dragon,s)
            if(now-s.emptySince>=Balance.ticks("raid.emptyCleanupTicks")){for(uuid in s.crystals.keys+s.embers)level.getEntity(UUID.fromString(uuid))?.discard();s.crystals.clear();s.embers.clear()}
            dragon.dragonFight?.let{updateBar(it,dragon)};return
        }
        s.emptySince=0
        s.embers.removeAll{uuid->val entity=level.getEntity(UUID.fromString(uuid));entity!=null&&(entity.isRemoved||entity is LivingEntity&&!entity.isAlive)}
        if (now % 20 == 0L && s.healed < Balance.value("raid.healBudget")) {
            val connected = s.crystals.keys.any { uuid -> (level.getEntity(UUID.fromString(uuid)) as? EndCrystal)?.let { !it.isRemoved && it.distanceToSqr(dragon) <= 1024 && dragon.hasLineOfSight(it) } == true }
            if (connected) {
                val heal = RelicRules.heal(dragon.health, s.healed, Balance.value("raid.healPerSecond").toFloat(), Balance.value("raid.healBudget").toFloat())
                if (heal > 0) { dragon.health += heal; s.healed += heal }
            }
        }
        if (s.recalls < Balance.ticks("raid.maxRecalls") && s.crystals.size < 3 && now >= s.nextRecall && s.recallAt == 0L) {
            s.recallAt = now + Balance.ticks("raid.recallWarningTicks"); nearby(dragon).forEach { it.sendOverlayMessage(Component.translatable("boss.lynxrelics.recall_warning",Balance.value("raid.recallWarningTicks")/20)) }
        }
        if (s.recallAt > 0 && now >= s.recallAt) {
            summonCrystals(dragon, now); s.recallAt = 0; s.recalls++
            s.nextRecall = now + recallInterval(s.phase)
        }
        if (s.recallAt > now && now % 5 == 0L) crystalPositions(dragon).forEach { p -> level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 4, .6, 1.0, .6, .02) }
        if (s.transitionUntil <= now) RaidSkills.tick(dragon, now)
        dragon.dragonFight?.let { updateBar(it, dragon) }
    }
    private fun nearby(dragon: EnderDragon) = (dragon.level() as ServerLevel).players().filter { it.isAlive && !it.isSpectator && !it.isCreative && it.distanceToSqr(dragon.fightOrigin.centerPosition()) <= 256 * 256 }
    internal fun participants(dragon:EnderDragon)=nearby(dragon)
    private fun recallInterval(phase:Int)=Balance.ticks("raid.recallPhase${phase}Ticks").coerceAtLeast(1)
    private fun BlockPos.centerPosition() = Vec3.atCenterOf(this)
    private fun crystalPositions(dragon: EnderDragon): List<Vec3> {
        val level = dragon.level() as ServerLevel; val s = state(dragon); val origin = dragon.fightOrigin
        return (0..2).mapNotNull { i ->
            val angle = Math.toRadians((i * 120 + s.recalls * 37).toDouble())
            val x = origin.x + (cos(angle) * 36).toInt(); val z = origin.z + (sin(angle) * 36).toInt()
            val probe = BlockPos(x, 64, z)
            if (!level.hasChunkAt(probe)) return@mapNotNull null
            val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
            val pos = BlockPos(x, y, z)
            if (y !in 32..150 || !level.getBlockState(pos).isAir || !level.getBlockState(pos.above()).isAir || level.getBlockState(pos.below()).isAir) return@mapNotNull null
            val p = Vec3.atBottomCenterOf(pos)
            if (level.players().any { it.boundingBox.inflate(2.0).contains(p) }) null else p
        }
    }
    private fun summonCrystals(dragon: EnderDragon, now: Long) {
        val s = state(dragon); val level = dragon.level() as ServerLevel
        val count = minOf(3 - s.crystals.size, Balance.ticks("raid.crystalsPerRecall"))
        for (pos in crystalPositions(dragon).take(count.coerceAtLeast(0))) {
            val crystal = EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.MOB_SUMMONED) ?: continue
            val expiry = now + Balance.ticks("raid.crystalLifetime")
            crystal.setPos(pos); crystal.setShowBottom(false); crystal.addTag(OWNER + dragon.uuid); crystal.addTag(EXPIRY + expiry)
            if (level.addFreshEntity(crystal)) s.crystals[crystal.uuid.toString()] = expiry
        }
    }
    @JvmStatic fun owned(crystal: EndCrystal?) = crystal != null && crystal.entityTags().any { it.startsWith(OWNER) }
    @JvmStatic fun tickOwned(crystal: EndCrystal): Boolean {
        if (!owned(crystal) || crystal.level().isClientSide) return false
        val expiry = crystal.entityTags().firstOrNull { it.startsWith(EXPIRY) }?.removePrefix(EXPIRY)?.toLongOrNull() ?: 0L
        if ((crystal.level().server?.overworld()?.gameTime ?: 0) >= expiry) crystal.discard() else crystal.baseTick()
        return true
    }
    @JvmStatic fun destroyCrystal(crystal: EndCrystal, level: ServerLevel, source: DamageSource): Boolean {
        if (crystal.isRemoved || crystal.isInvulnerable && !source.`is`(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false
        val owner = crystal.entityTags().first { it.startsWith(OWNER) }.removePrefix(OWNER)
        val dragon = level.getEntity(UUID.fromString(owner)) as? EnderDragon
        crystal.discard(); dragon?.let { state(it).crystals.remove(crystal.uuid.toString()); it.onCrystalDestroyed(level, crystal, crystal.blockPosition(), source) }
        level.sendParticles(ParticleTypes.EXPLOSION, crystal.x, crystal.y + .5, crystal.z, 4, .5, .5, .5, .02)
        for (target in level.getEntitiesOfClass(LivingEntity::class.java, crystal.boundingBox.inflate(3.0))) {
            if (target is ServerPlayer && (target.isCreative || target.isSpectator)) continue
            if (target.distanceToSqr(crystal) <= 9) target.hurtServer(level, level.damageSources().explosion(crystal, source.entity), 6f)
        }
        return true
    }
    @JvmStatic fun death(dragon: EnderDragon) {
        if (!active(dragon) || dragon.level().isClientSide) return
        val s = state(dragon); val level = dragon.level() as ServerLevel
        for (uuid in s.crystals.keys + s.embers) level.getEntity(UUID.fromString(uuid))?.discard()
        if (dragon.dragonDeathTime >= 180 && !s.rewarded) {
            s.rewarded = true
            fun drop(id: String, count: Int) { dragon.spawnAtLocation(level, ItemStack(Content.item(id), count)) }
            fun amount(prefix:String):Int{val min=Balance.ticks("raid.${prefix}DropMin");val max=Balance.ticks("raid.${prefix}DropMax").coerceAtLeast(min);return min+level.random.nextInt(max-min+1)}
            drop("dragon_essence", amount("essence")); drop("endmarrow_scrap", amount("scrap")); drop("rift_emblem", 2)
            drop("dragonheart_prism", amount("prism")); drop("raid_cloak_pattern", 1)
        }
    }
}

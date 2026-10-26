// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import net.ciyuan.lynxrelics.compat.ElytraCompat
import net.ciyuan.lynxrelics.core.*
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.phys.Vec3

interface BoundPearlData {
    fun lynxBoundExpiry(): Long
    fun lynxBoundDimension(): String
    fun lynxMarkBound(expiry: Long, dimension: String)
}
object InfiniteItems {
    const val TAG = "lynxrelics_bound"
    fun use(player: ServerPlayer, hand: InteractionHand, firework: Boolean): InteractionResult {
        val s = player.relics(); val now = Skills.now(player)
        val key = if (firework) "firework" else "pearl"
        if (!s.ready(key, now) || !player.isAlive || player.isSpectator) return InteractionResult.FAIL
        if (firework) {
            if (player.isFallFlying && !ElytraCompat.actualWings(player).isEmpty) {
                val rocket = ItemStack(Items.FIREWORK_ROCKET).also { it.set(DataComponents.FIREWORKS, Fireworks(1, emptyList())) }
                if (!player.level().addFreshEntity(FireworkRocketEntity(player.level(), rocket, player))) return InteractionResult.FAIL
            } else player.level().sendParticles(ParticleTypes.END_ROD, player.x, player.eyeY, player.z, 5, 0.1, 0.1, 0.1, 0.02)
        } else {
            if (s.pearlId.isNotEmpty() && s.pearlExpires > now) return InteractionResult.FAIL
            val pearl = ThrownEnderpearl(player.level(), player, ItemStack(Items.ENDER_PEARL))
            pearl.addTag(TAG)
            player.deregisterEnderPearl(pearl)
            (pearl as BoundPearlData).lynxMarkBound(now + Balance.ticks("infinite.pearlLifetime"), player.level().dimension().identifier().toString())
            pearl.shootFromRotation(player, player.xRot, player.yRot, 0f, 1.5f, 1f)
            if (!player.level().addFreshEntity(pearl)) return InteractionResult.FAIL
            s.pearlId = pearl.uuid.toString(); s.pearlExpires = now + Balance.ticks("infinite.pearlLifetime")
        }
        val ticks = Balance.ticks(if (firework) "infinite.fireworkCooldown" else "infinite.pearlCooldown").coerceAtLeast(1)
        s.cooldown(key, now, ticks); player.cooldowns.addCooldown(player.getItemInHand(hand), ticks)
        // The held relic is never shrunk, converted or replenished.
        return InteractionResult.SUCCESS
    }
    @JvmStatic fun bound(pearl: ThrownEnderpearl) = TAG in pearl.entityTags()
    private fun clear(pearl: ThrownEnderpearl) {
        (pearl.owner as? ServerPlayer)?.relics()?.let { if (it.pearlId == pearl.uuid.toString()) { it.pearlId = ""; it.pearlExpires = 0 } }
    }
    @JvmStatic fun tick(pearl: ThrownEnderpearl): Boolean {
        if (!bound(pearl) || pearl.level().isClientSide) return false
        val data = pearl as BoundPearlData
        val owner = pearl.owner as? ServerPlayer
        val now = pearl.level().server?.overworld()?.gameTime ?: 0L
        val next = pearl.position().add(pearl.deltaMovement)
        if (now >= data.lynxBoundExpiry() || data.lynxBoundDimension() != pearl.level().dimension().identifier().toString() || owner != null && (!owner.isAlive || owner.level() !== pearl.level()) || !pearl.level().hasChunkAt(BlockPos.containing(next))) {
            clear(pearl); pearl.discard(); return true
        }
        return false
    }
    @JvmStatic fun beforeHit(pearl: ThrownEnderpearl): Boolean {
        if (!bound(pearl) || pearl.level().isClientSide) return false
        val owner = pearl.owner as? ServerPlayer
        if (owner == null || owner.level() !== pearl.level()) { clear(pearl); pearl.discard(); return true }
        val origin = pearl.position()
        val offsets = listOf(Vec3.ZERO, Vec3(1.0, 0.0, 0.0), Vec3(-1.0, 0.0, 0.0), Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, -1.0), Vec3(0.0, 1.0, 0.0))
        val safe = offsets.map { origin.add(it) }.firstOrNull { Skills.safe(owner, it) }
        clear(pearl)
        if (safe == null) { pearl.discard(); return true }
        pearl.setPos(safe)
        // Continue vanilla onHit: normal 5 damage, portal protection and teleport cancellation remain in its path.
        return false
    }
}

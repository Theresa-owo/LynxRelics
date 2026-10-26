// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core
import net.ciyuan.lynxrelics.content.Content
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity

object RelicDamage {
    fun pulse(level: ServerLevel, attacker: LivingEntity): DamageSource = DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, Content.id("skill_pulse"))), attacker)
}

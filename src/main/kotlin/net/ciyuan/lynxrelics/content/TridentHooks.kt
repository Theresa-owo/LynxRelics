// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import net.ciyuan.lynxrelics.core.Balance
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.EntityHitResult

object TridentHooks {
    @JvmStatic fun riptideWeather(original: Boolean) = original || Balance.enabled("trident.dryRiptide")
    private val channeling=ThreadLocal<Boolean>()
    @JvmStatic fun enter(enchantment:net.minecraft.world.item.enchantment.Enchantment,level:ServerLevel):Boolean{
        val previous=channeling.get()?:false
        channeling.set(Balance.enabled("trident.dryChanneling")&&enchantment===level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.CHANNELING).value())
        return previous
    }
    @JvmStatic fun leave(previous:Boolean){if(previous)channeling.set(true)else channeling.remove()}
    @JvmStatic fun channelingWeather(original:Boolean)=original||channeling.get()==true
}

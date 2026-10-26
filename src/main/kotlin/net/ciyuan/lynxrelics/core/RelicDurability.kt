// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core
import net.ciyuan.lynxrelics.compat.ElytraCompat
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

object RelicDurability {
    @JvmStatic fun afterUnbreaking(stack:ItemStack,player:ServerPlayer?,damage:Int):Int{
        if(damage<=0||player==null)return damage
        val cruise=RelicEnchantments.level(stack,"cruise")
        if(cruise>0&&player.isFallFlying&&ElytraCompat.actualWings(player)===stack&&player.random.nextFloat()<floatArrayOf(.10f,.15f,.20f)[cruise-1])return 0
        if(player.relics().craftsmanshipUntil>Skills.now(player)&&player.random.nextFloat()<Balance.value("beacon.craftsmanshipChance"))return 0
        return damage
    }
}

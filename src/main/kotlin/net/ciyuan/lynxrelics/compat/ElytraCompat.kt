// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.compat

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.component.DataComponents

/** Exact optional API of the supplied equinox 1.2 JAR; no invented slot numbers or Curios dependency. */
object ElytraCompat {
    private val loaded by lazy { FabricLoader.getInstance().isModLoaded("elytra-slot") }
    private val inventoryGetter by lazy { if(loaded)Class.forName("com.equinox.ElytraInventoryHolder").getMethod("equinox\$getElytraInventory") else null }
    fun inventory(player: Player): net.minecraft.world.SimpleContainer? {
        val method=inventoryGetter?:return null
        check(method.declaringClass.isInstance(player)){"Elytra Slot 1.2 is present but its verified Player inventory interface is missing"}
        return method.invoke(player) as net.minecraft.world.SimpleContainer
    }
    fun actualWings(player: Player): ItemStack {
        if (loaded) {
            val stack = inventory(player)!!.getItem(0)
            if (canFly(stack)) return stack
        }
        val chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
        return if (canFly(chest)) chest else ItemStack.EMPTY
    }
    fun canFly(stack: ItemStack) = !stack.isEmpty && net.minecraft.world.entity.LivingEntity.canGlideUsing(stack, net.minecraft.world.entity.EquipmentSlot.CHEST)
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.menu

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.content.RelicItem
import net.ciyuan.lynxrelics.core.relics
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class AccessoryMenu(id: Int, inventory: Inventory) : AbstractContainerMenu(Content.ACCESSORY_MENU, id) {
    init {
        val accessories = inventory.player.relics().accessories
        for (i in 0..3) addSlot(object : Slot(accessories, i, 35 + 36 * i, 44) {
            override fun mayPlace(stack: ItemStack) = (stack.item as? RelicItem)?.slot?.ordinal == i
            override fun getMaxStackSize() = 1
        })
        addStandardInventorySlots(inventory, 8, 88)
    }
    override fun stillValid(player: Player) = player.isAlive
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY
        val slot = slots[index]
        val original = slot.item
        if (original.isEmpty) return ItemStack.EMPTY
        val copy = original.copy()
        val moved = if (index < 4) moveItemStackTo(original, 4, slots.size, true) else {
            val target = (original.item as? RelicItem)?.slot?.ordinal ?: return ItemStack.EMPTY
            moveItemStackTo(original, target, target + 1, false)
        }
        if (!moved) return ItemStack.EMPTY
        if (original.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        slot.onTake(player, original)
        return copy
    }
    companion object : ExtendedMenuProvider<Unit> {
        override fun getScreenOpeningData(player: ServerPlayer) = Unit
        override fun getDisplayName() = Component.translatable("menu.lynxrelics.accessories")
        override fun createMenu(id: Int, inventory: Inventory, player: Player) = AccessoryMenu(id, inventory)
    }
}

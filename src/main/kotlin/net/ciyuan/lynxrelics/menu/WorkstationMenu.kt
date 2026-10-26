// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.menu

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.craft.Workstation
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack

class WorkstationMenu(id: Int, inventory: Inventory, val container: Container, val data: ContainerData) : AbstractContainerMenu(Content.WORKSTATION_MENU, id) {
    constructor(id: Int, inventory: Inventory, pos: BlockPos) : this(id, inventory, inventory.player.level().getBlockEntity(pos) as? Workstation ?: SimpleContainer(16), SimpleContainerData(4))
    init {
        for (i in 0..8) addSlot(Slot(container, i, 8 + (i % 3) * 18, 36 + (i / 3) * 18))
        addSlot(Slot(container, 9, 81, 54))
        for (i in 10..15) addSlot(object : Slot(container, i, 116 + ((i - 10) % 3) * 18, 45 + ((i - 10) / 3) * 18) {
            override fun mayPlace(stack: ItemStack) = false
        })
        addStandardInventorySlots(inventory, 8, 126)
        addDataSlots(data)
    }
    override fun stillValid(player: Player) = container.stillValid(player)
    override fun clickMenuButton(player: Player, button: Int): Boolean {
        val machine = container as? Workstation ?: return false
        if (!stillValid(player)) return false
        if(button>=100){val selected=button-100;if(selected !in machine.recipes.indices)return false;machine.selected=selected;machine.progress=0;machine.setChanged();return true}
        if (button == 0 || button == 1) {
            if (machine.recipes.isEmpty()) return false
            machine.selected = Math.floorMod(machine.selected + if (button == 0) -1 else 1, machine.recipes.size)
            machine.progress = 0; machine.setChanged(); return true
        }
        return false
    }
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        if (index !in slots.indices) return ItemStack.EMPTY
        val slot = slots[index]; val stack = slot.item
        if (stack.isEmpty) return ItemStack.EMPTY
        val original = stack.copy()
        if (index < 16) { if (!moveItemStackTo(stack, 16, slots.size, true)) return ItemStack.EMPTY }
        else {
            val catalyst = (container as? Workstation)?.recipes?.any { it.catalyst == net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item).toString() } == true
            if (!(if (catalyst) moveItemStackTo(stack, 9, 10, false) else moveItemStackTo(stack, 0, 9, false))) return ItemStack.EMPTY
        }
        if (stack.isEmpty) slot.setByPlayer(ItemStack.EMPTY) else slot.setChanged()
        slot.onTake(player, stack); return original
    }
}

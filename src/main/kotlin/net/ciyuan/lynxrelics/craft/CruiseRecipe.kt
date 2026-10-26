// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.craft

import net.ciyuan.lynxrelics.content.Content
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.level.Level

class CruiseRecipe : CustomRecipe() {
    override fun matches(input: CraftingInput, level: Level): Boolean {
        val stacks = input.items().filterNot { it.isEmpty }
        if (stacks.size != 9) return false
        val counts = stacks.groupingBy { BuiltInRegistries.ITEM.getKey(it.item).toString() }.eachCount()
        if (counts != mapOf("lynxrelics:perpetual_core" to 1, "minecraft:firework_rocket" to 1, "lynxrelics:dragon_essence" to 4, "minecraft:phantom_membrane" to 2, "minecraft:blaze_rod" to 1)) return false
        val fireworks = stacks.first { it.`is`(Items.FIREWORK_ROCKET) }.get(DataComponents.FIREWORKS) ?: return false
        return fireworks.flightDuration() == 1 && fireworks.explosions().isEmpty()
    }
    override fun assemble(input: CraftingInput) = ItemStack(Content.item("cruise_firework"))
    override fun getSerializer() = Content.CRUISE_RECIPE
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core

import net.ciyuan.lynxrelics.content.Content
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantment
import kotlin.math.ceil

object RelicEnchantments {
    fun level(stack: ItemStack, id: String): Int {
        if (stack.isEmpty) return 0
        return stack.enchantments.entrySet().firstOrNull { it.key.`is`(Content.id(id)) }?.intValue?.coerceIn(0, if (id in setOf("echo", "long_resonance", "homing", "mirror", "overtone", "smelting", "prospecting")) 2 else 3) ?: 0
    }
    fun accessory(player: Player, slot: RelicSlot, id: String) = level(Skills.equipped(player, slot), id)
    fun spec(stack: ItemStack) = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("LynxRelicsSpecialization", "") ?: ""
    fun setSpec(stack: ItemStack, value: String) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack) { it.putString("LynxRelicsSpecialization", value) }
        val lines=(stack.get(DataComponents.LORE)?.lines()?:emptyList()).filterNot{it.style.insertion=="lynxrelics-specialization"}.toMutableList()
        if(value.isNotEmpty()){
            val id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item).path
            lines.add(net.minecraft.network.chat.Component.translatable("specialization.lynxrelics.$id.$value").withStyle{it.withColor(0xc7a0ec).withItalic(false).withInsertion("lynxrelics-specialization")})
        }
        stack.set(DataComponents.LORE,net.minecraft.world.item.component.ItemLore(lines))
    }
    fun gauntletCooldown(player: Player, base: Int) = ceil(base * (1.0 - minOf(.2, .03 * accessory(player, RelicSlot.GAUNTLET, "rhythm")))).toInt().coerceAtLeast(1)
    fun validAccessory(stack: ItemStack) = stack.enchantments.entrySet().count { it.key.unwrapKey().map { k -> k.identifier().namespace == "lynxrelics" }.orElse(false) } <= 2
    fun key(id: String): ResourceKey<Enchantment> = ResourceKey.create(Registries.ENCHANTMENT, Content.id(id))
    fun bookHas(stack: ItemStack, id: String, level: Int): Boolean {
        val enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack)
        return enchantments.entrySet().size == 1 && enchantments.entrySet().any { it.key.`is`(Content.id(id)) && it.intValue == level }
    }
}

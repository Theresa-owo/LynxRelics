// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content
import net.ciyuan.lynxrelics.core.RelicRules
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.crafting.SmithingRecipeInput
import kotlin.math.ceil

object VanillaMods {
    @JvmStatic fun upgrade(stack: ItemStack) = stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr("LynxRelicsUpgrade", "") ?: ""
    @JvmStatic fun transform(input: SmithingRecipeInput, result: ItemStack): ItemStack {
        if (!input.template().`is`(Content.item("relic_upgrade_template")) || result.isEmpty) return result
        val base = input.base(); val addition=input.addition()
        val modification = when {
            base.`is`(Items.SHIELD) && addition.`is`(Content.item("iron_rim")) -> "iron_rim"
            base.`is`(Items.SHIELD) && addition.`is`(Content.item("deflector")) -> "deflector"
            base.`is`(Items.SHIELD) && addition.`is`(Content.item("breaching_fitting")) -> "breaching_fitting"
            base.`is`(Items.BOW) && addition.`is`(Content.item("quickdraw_string")) -> "quickdraw_string"
            base.`is`(Items.CROSSBOW) && addition.`is`(Content.item("steady_rest")) -> "steady_rest"
            else -> return ItemStack.EMPTY
        }
        val stored=base.get(DataComponents.CUSTOM_DATA)?.copyTag()
        val originalMax=stored?.getIntOr("LynxRelicsBaseDurability",base.maxDamage) ?: base.maxDamage
        CustomData.update(DataComponents.CUSTOM_DATA,result){it.putString("LynxRelicsUpgrade",modification);it.putInt("LynxRelicsBaseDurability",originalMax)}
        if(base.`is`(Items.SHIELD)){
            val newMax=if(modification=="iron_rim")ceil(originalMax*1.25).toInt() else originalMax
            result.set(DataComponents.MAX_DAMAGE,newMax);result.damageValue=RelicRules.damageAfterUpgrade(base.damageValue,base.maxDamage,newMax)
        }
        val lore=(base.get(DataComponents.LORE)?.lines() ?: emptyList()).filterNot { it.style.insertion=="lynxrelics-upgrade" }.toMutableList()
        lore.add(Component.translatable("upgrade.lynxrelics.$modification").withStyle { it.withColor(0x91c6c9).withItalic(false).withInsertion("lynxrelics-upgrade") })
        result.set(DataComponents.LORE,ItemLore(lore));return result
    }
}

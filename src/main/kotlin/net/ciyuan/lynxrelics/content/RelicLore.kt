// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import com.google.gson.JsonParser
import net.ciyuan.lynxrelics.core.Balance
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.component.ItemLore

object RelicLore {
    private val catalog by lazy {
        val stream = RelicLore::class.java.getResourceAsStream("/assets/lynxrelics/lore.json") ?: error("Missing LynxRelics lore catalog")
        stream.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
    }
    fun create(id: String): ItemLore {
        val rows = catalog.getAsJsonArray(id) ?: error("Missing item introduction: $id")
        return ItemLore(rows.map { row ->
            val entry = row.asJsonObject
            val args = entry.getAsJsonArray("args").map { element ->
                val text = element.asString
                when {
                    text.startsWith("key:") -> Component.keybind(text.removePrefix("key:")).withStyle(ChatFormatting.AQUA,ChatFormatting.BOLD)
                    text.startsWith("balance:") -> number(Balance.value(text.removePrefix("balance:")))
                    text.startsWith("seconds:") -> number(Balance.value(text.removePrefix("seconds:"))/20)
                    text.startsWith("percent:") -> number(Balance.value(text.removePrefix("percent:"))*100)
                    text.startsWith("gold:") -> Component.literal(text.removePrefix("gold:")).withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD)
                    else -> Component.literal(text)
                }
            }
            val colorName=entry.get("color")?.asString?:"gray"
            val text=Component.translatable(entry.get("key").asString, *args.toTypedArray())
            if(colorName.startsWith("#"))text.withStyle{it.withColor(colorName.substring(1).toInt(16))}else text.withStyle(ChatFormatting.valueOf(colorName.uppercase(java.util.Locale.ROOT)))
            text.withStyle{it.withBold(entry.get("bold")?.asBoolean?:false).withItalic(entry.get("italic")?.asBoolean?:false).withInsertion("lynxrelics-lore")}
        })
    }
    private fun number(value:Double)=Component.literal(if(value==value.toLong().toDouble())value.toLong().toString()else value.toString()).withStyle(ChatFormatting.GOLD,ChatFormatting.BOLD)
    fun refreshOwned(stack:net.minecraft.world.item.ItemStack){
        if(stack.isEmpty||net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item).namespace!="lynxrelics")return
        val old=stack.get(net.minecraft.core.component.DataComponents.LORE)?:return
        fun legacy(c:Component)=(c.contents as? net.minecraft.network.chat.contents.TranslatableContents)?.key?.startsWith("lore.lynxrelics.")==true
        if(old.lines().none{legacy(it)||it.style.insertion=="lynxrelics-lore"})return
        val extra=old.lines().filterNot{legacy(it)||it.style.insertion=="lynxrelics-lore"||it.style.insertion=="lynxrelics-specialization"}
        val id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.item).path
        stack.set(net.minecraft.core.component.DataComponents.LORE,ItemLore(create(id).lines()+extra))
        val spec=net.ciyuan.lynxrelics.core.RelicEnchantments.spec(stack)
        if(spec.isNotEmpty())net.ciyuan.lynxrelics.core.RelicEnchantments.setSpec(stack,spec)
    }
}

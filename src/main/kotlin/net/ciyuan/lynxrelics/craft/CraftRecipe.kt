// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.craft

import com.google.gson.JsonParser
import net.ciyuan.lynxrelics.core.Balance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.EnchantmentInstance
import net.ciyuan.lynxrelics.core.RelicEnchantments

data class CraftRecipe(val id: String, val route: String, val ticks: Int, val inputs: Map<String, Int>, val outputs: Map<String, Int>, val catalyst: String = "", val mode: String = "", val enchantment: String = "", val bookLevel: Int = 1, val gear: String = "", val specialization: String = "", val encounter: String = "")

object CraftRecipes {
    var all: List<CraftRecipe> = emptyList(); private set
    private var routes:Map<String,List<CraftRecipe>> = emptyMap()
    var clientRecipes:List<CraftRecipe> = emptyList(); private set
    var catalogVersion=0;private set
    fun load() {
        val path=net.fabricmc.loader.api.FabricLoader.getInstance().configDir.resolve("lynxrelics/workstation_recipes.json")
        if(!java.nio.file.Files.exists(path)){
            java.nio.file.Files.createDirectories(path.parent)
            CraftRecipes::class.java.getResourceAsStream("/data/lynxrelics/workstation_recipes.json").use{stream->java.nio.file.Files.copy(stream?:error("Missing workstation recipes"),path)}
        }
        all=java.nio.file.Files.newBufferedReader(path).use{parse(it.readText())}
        routes=listOf("dragon","echo","tide").associateWith{route->all.filter{it.route==route||it.route=="any"}}
        acceptCatalog(all)
    }
    fun parse(json:String):List<CraftRecipe>{
        val parsed=JsonParser.parseString(json).asJsonArray.map { element ->
            val r = element.asJsonObject
            fun counts(key: String) = r.getAsJsonObject(key).entrySet().associate { it.key to it.value.asInt }
            CraftRecipe(r["id"].asString, r["route"].asString, r["ticks"].asInt, counts("inputs"), counts("outputs"), r["catalyst"]?.asString ?: "", r["mode"]?.asString ?: "", r["enchantment"]?.asString ?: "", r["bookLevel"]?.asInt ?: 1, r["gear"]?.asString ?: "", r["specialization"]?.asString ?: "",r["encounter"]?.asString ?: "")
        }
        require(parsed.size<=512&&parsed.map{it.id}.distinct().size==parsed.size){"Recipe IDs must be unique; at most 512 workstation recipes"}
        for(r in parsed){
            require(r.id.length in 1..80&&r.route in setOf("any","dragon","echo","tide")&&r.ticks in 1..72000){"Invalid recipe header: ${r.id}"}
            require(r.inputs.size<=9&&r.outputs.size<=6&&r.inputs.values.all{it in 1..4096}&&r.outputs.values.all{it in 1..4096}){"Invalid recipe quantities: ${r.id}"}
            for(id in r.inputs.keys+r.outputs.keys+listOf(r.catalyst,r.gear).filter{it.isNotEmpty()}){
                val parsed=Identifier.tryParse(id.removePrefix("#"));require(parsed!=null){"Invalid item/tag ID in ${r.id}: $id"}
                require(id.startsWith("#")||id!="minecraft:air"&&BuiltInRegistries.ITEM.containsKey(parsed)){"Unknown item in ${r.id}: $id"}
            }
            require(r.mode in setOf("","book","upgrade_book","specialize","respecialize","ritual")&&r.bookLevel in 1..3&&r.specialization in setOf("","a","b")){"Invalid recipe mode: ${r.id}"}
            if(r.mode in setOf("specialize","respecialize"))require(r.gear.isNotEmpty()&&r.inputs[r.gear]==1&&r.specialization.isNotEmpty()){"Specialization must transform exactly one original item: ${r.id}"}
        }
        return parsed
    }
    fun acceptCatalog(recipes:List<CraftRecipe>){clientRecipes=recipes;catalogVersion++}
    fun forRoute(route: String) = routes[route]?:emptyList()
    fun catalogJson()=com.google.gson.Gson().toJson(all)
}

/** A side-effect-free plan. Inventories are changed only after every output/remainder fits. */
object CraftTransaction {
    fun plan(items: List<ItemStack>, recipe: CraftRecipe, lookup: HolderLookup.Provider? = null): List<ItemStack>? {
        require(items.size == 16)
        if (recipe.catalyst.isNotEmpty() && BuiltInRegistries.ITEM.getKey(items[9].item).toString() != recipe.catalyst) return null
        val result = items.map { it.copy() }.toMutableList()
        var originalGear: ItemStack? = null
        fun eligible(stack: ItemStack, name: String): Boolean {
            if(name.startsWith("#")){if(!stack.`is`(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,Identifier.parse(name.substring(1)))))return false}
            else if (BuiltInRegistries.ITEM.getKey(stack.item).toString() != name) return false
            if (recipe.mode == "upgrade_book" && stack.`is`(Items.ENCHANTED_BOOK)) return RelicEnchantments.bookHas(stack, recipe.enchantment, 1)
            if (name == recipe.gear && recipe.mode in setOf("specialize", "respecialize")) {
                val specialized = RelicEnchantments.spec(stack).isNotEmpty()
                return if (recipe.mode == "specialize") !specialized else specialized
            }
            return true
        }
        for ((name, required) in recipe.inputs) {
            var remaining = required
            for (i in 0..8) {
                val stack = result[i]
                if (!eligible(stack, name)) continue
                if (name == recipe.gear) originalGear = stack.copyWithCount(1)
                val take = minOf(stack.count, remaining)
                stack.shrink(take); remaining -= take
                if (remaining == 0) break
            }
            if (remaining != 0) return null
        }
        val products = recipe.outputs.map { (name, count) ->
            val key=Identifier.parse(name);if(!BuiltInRegistries.ITEM.containsKey(key))return null
            val item = BuiltInRegistries.ITEM.getValue(key)
            ItemStack(item, count)
        }.toMutableList()
        if (recipe.mode == "book" || recipe.mode == "upgrade_book") {
            val enchantment = lookup?.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)?.getOrThrow(RelicEnchantments.key(recipe.enchantment)) ?: return null
            if (recipe.bookLevel !in 1..enchantment.value().maxLevel) return null
            products.add(EnchantmentHelper.createBook(EnchantmentInstance(enchantment, recipe.bookLevel)))
        }
        if (recipe.mode == "specialize" || recipe.mode == "respecialize") {
            val output = originalGear ?: return null
            RelicEnchantments.setSpec(output, recipe.specialization)
            products.add(output)
        }
        for (product in products) {
            val item = product.item
            val count = product.count
            var remaining = count
            for (i in 10..15) {
                val stack = result[i]
                if (!stack.isEmpty && ItemStack.isSameItemSameComponents(stack, product)) {
                    val add = minOf(remaining, stack.maxStackSize - stack.count)
                    stack.grow(add); remaining -= add
                }
            }
            for (i in 10..15) if (result[i].isEmpty && remaining > 0) {
                val add = minOf(remaining, item.defaultInstance.maxStackSize)
                result[i] = product.copyWithCount(add); remaining -= add
            }
            if (remaining > 0) return null
        }
        return result
    }
}

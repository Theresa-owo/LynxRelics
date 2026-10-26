// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import net.ciyuan.lynxrelics.core.Balance
import net.ciyuan.lynxrelics.core.RelicRules
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.item.*
import net.minecraft.world.item.equipment.*

object Endforged {
    private val repairs = TagKey.create(Registries.ITEM, Content.id("repairs_endforged"))
    private val magic=TagKey.create(Registries.DAMAGE_TYPE,Content.id("magic_damage"))
    private val weapons=setOf("endforged_sword","endforged_axe","endforged_pickaxe","endforged_shovel","endforged_hoe")
    @JvmStatic fun attackSource(stack:ItemStack,attacker:net.minecraft.world.entity.LivingEntity,original:net.minecraft.world.damagesource.DamageSource):net.minecraft.world.damagesource.DamageSource{
        val level=attacker.level() as? net.minecraft.server.level.ServerLevel?:return original
        val id=BuiltInRegistries.ITEM.getKey(stack.item)
        if(!Balance.enabled("forging.magicDamage")||id.namespace!="lynxrelics"||id.path !in weapons)return original
        return net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE,Content.id("endforged_magic"))),attacker)
    }
    fun magicResistance(player:net.minecraft.world.entity.player.Player):Double{
        val pieces=listOf(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET).count{slot->
            val stack=player.getItemBySlot(slot);val id=BuiltInRegistries.ITEM.getKey(stack.item)
            !stack.isEmpty&&!stack.isBroken&&id.namespace=="lynxrelics"&&id.path in setOf("endforged_helmet","endforged_chestplate","endforged_leggings","endforged_boots")
        }
        return minOf(Balance.value("forging.magicResistanceCap"),pieces*Balance.value("forging.magicResistancePerPiece"))
    }
    fun resistMagic(player:net.minecraft.world.entity.player.Player,source:net.minecraft.world.damagesource.DamageSource,damage:Float)=if(source.`is`(magic))damage*(1-magicResistance(player)).toFloat()else damage
    fun register() {
        val base = ArmorMaterials.NETHERITE
        val armor = ArmorMaterial(base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(), 3.5f, 0.1f, repairs, ResourceKey.create(EquipmentAssets.ROOT_ID, Content.id("endforged")))
        for ((name, original, type) in listOf(
            Triple("helmet", Items.NETHERITE_HELMET, ArmorType.HELMET), Triple("chestplate", Items.NETHERITE_CHESTPLATE, ArmorType.CHESTPLATE),
            Triple("leggings", Items.NETHERITE_LEGGINGS, ArmorType.LEGGINGS), Triple("boots", Items.NETHERITE_BOOTS, ArmorType.BOOTS))) {
            add("endforged_$name", Item(Content.props("endforged_$name").humanoidArmor(armor, type).durability(Balance.scaledDurability(type.getDurability(base.durability()))).fireResistant()))
        }
        val n = ToolMaterial.NETHERITE
        val material = ToolMaterial(n.incorrectBlocksForDrops(), Balance.scaledDurability(n.durability()), n.speed() * 1.1f, n.attackDamageBonus(), n.enchantmentValue(), repairs)
        add("endforged_pickaxe", Item(Content.props("endforged_pickaxe").pickaxe(material, 1f, -2.8f).fireResistant()))
        add("endforged_axe", Item(Content.props("endforged_axe").axe(material, (Balance.value("forging.axeAttackDamage")-1-n.attackDamageBonus()).toFloat(), (Balance.value("forging.axeAttackSpeed")-4).toFloat()).fireResistant()))
        add("endforged_shovel", Item(Content.props("endforged_shovel").shovel(material, 1.5f, -3f).fireResistant()))
        add("endforged_hoe", Item(Content.props("endforged_hoe").hoe(material, -4f, 0f).fireResistant()))
        add("endforged_sword", Item(Content.props("endforged_sword").sword(material, (Balance.value("forging.swordAttackDamage")-1-n.attackDamageBonus()).toFloat(), (Balance.value("forging.swordAttackSpeed")-4).toFloat()).fireResistant()))
    }
    private fun add(name: String, item: Item) { Registry.register(BuiltInRegistries.ITEM, Content.id(name), item); Content.items[name] = item }
    @JvmStatic fun preserveDamage(base: ItemStack, result: ItemStack): ItemStack {
        if (result.isEmpty || !BuiltInRegistries.ITEM.getKey(result.item).toString().startsWith("lynxrelics:endforged_")) return result
        if (base.isDamageableItem && result.isDamageableItem) {
            result.set(DataComponents.MAX_DAMAGE, result.item.defaultInstance.maxDamage)
            result.damageValue = RelicRules.damageAfterUpgrade(base.damageValue, base.maxDamage, result.maxDamage)
        }
        return result
    }
}

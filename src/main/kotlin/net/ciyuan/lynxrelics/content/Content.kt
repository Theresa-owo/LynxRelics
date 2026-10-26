// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import net.ciyuan.lynxrelics.core.*
import net.ciyuan.lynxrelics.craft.WorkshopBlock
import net.ciyuan.lynxrelics.craft.Workstation
import net.ciyuan.lynxrelics.craft.CruiseRecipe
import net.ciyuan.lynxrelics.menu.AccessoryMenu
import net.ciyuan.lynxrelics.menu.WorkstationMenu
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.*
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour

object Content {
    val items = linkedMapOf<String, Item>()
    val blocks = linkedMapOf<String, Block>()
    lateinit var ACCESSORY_MENU: MenuType<AccessoryMenu>; private set
    lateinit var WORKSTATION_MENU: MenuType<WorkstationMenu>; private set
    lateinit var WORKSTATION_TYPE: BlockEntityType<Workstation>; private set
    lateinit var CRUISE_RECIPE: RecipeSerializer<CruiseRecipe>; private set
    lateinit var FOAM_SLOW: net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>; private set
    lateinit var RESONANCE_SPEED: net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>; private set
    lateinit var RESONATOR_TYPE: BlockEntityType<Resonator>; private set
    fun id(path: String) = Identifier.fromNamespaceAndPath("lynxrelics", path)
    fun item(path: String) = items.getValue(path)
    fun props(path: String) = Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path))).component(net.minecraft.core.component.DataComponents.LORE, RelicLore.create(path))
    private fun add(path: String, item: Item): Item = Registry.register(BuiltInRegistries.ITEM, id(path), item).also { items[path] = it }
    fun block(path: String, factory: (BlockBehaviour.Properties) -> Block): Block {
        val block = factory(BlockBehaviour.Properties.of().strength(3.5f).setId(ResourceKey.create(Registries.BLOCK, id(path))))
        Registry.register(BuiltInRegistries.BLOCK, id(path), block); blocks[path] = block
        val itemProps = props(path).useBlockDescriptionPrefix()
        add(path, BlockItem(block, if (path == "endmarrow_ore") itemProps.fireResistant() else itemProps)); return block
    }
    fun register() {
        FOAM_SLOW = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id("tide_slow"), object : net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.HARMFUL, 0x438f9e) {
            init { addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, id("tide_slow"), -.1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) }
        })
        RESONANCE_SPEED=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,id("resonance_speed"),object:net.minecraft.world.effect.MobEffect(net.minecraft.world.effect.MobEffectCategory.BENEFICIAL,0x58a3b1){init{addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,id("resonance_speed"),.05,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)}})
        listOf("dragon_essence", "dragon_shell_dust", "dragonbound_ingot", "shed_dragon_scale", "embryo_crystal", "empty_embryo_shell",
            "echo_dust", "resonant_bone", "moth_scale", "keeper_shard", "condensed_tide", "tideforged_copper", "jelly_pearl", "reef_shell",
            "endmarrow_scrap", "endmarrow_ingot", "rift_emblem", "endforged_template", "perpetual_core", "dragonheart_prism", "raid_cloak_pattern",
            "relic_upgrade_template", "iron_rim", "deflector", "breaching_fitting", "quickdraw_string", "steady_rest", "magnetic_ring", "expedition_compass", "stabilizing_ring", "abundance_core", "craftsmanship_core", "shelter_core").forEach {
            add(it, RelicItem(if(it=="expedition_compass")props(it).stacksTo(1)else props(it), action = if (it == "dragon_essence") "refill" else if(it=="embryo_crystal")"embryo" else if(it=="expedition_compass")"compass"else ""))
        }
        fun accessory(path: String, slot: RelicSlot, route: Route, durability: Int) {
            var p = props(path).stacksTo(1).enchantable(15)
            if (durability > 0) p = p.durability(durability).enchantable(15)
            add(path, RelicItem(p, slot, route))
        }
        accessory("homing_dragon_necklace", RelicSlot.NECKLACE, Route.DRAGON, 0)
        accessory("dragonbound_belt", RelicSlot.BELT, Route.DRAGON, 384)
        accessory("dragonthroat_gauntlet", RelicSlot.GAUNTLET, Route.DRAGON, 512)
        accessory("dragonwing_cloak", RelicSlot.CLOAK, Route.DRAGON, 432)
        accessory("resonant_necklace", RelicSlot.NECKLACE, Route.ECHO, 384)
        accessory("silent_belt", RelicSlot.BELT, Route.ECHO, 384)
        accessory("keeper_gauntlet", RelicSlot.GAUNTLET, Route.ECHO, 512)
        accessory("murmur_cloak", RelicSlot.CLOAK, Route.ECHO, 432)
        accessory("watermirror_necklace", RelicSlot.NECKLACE, Route.TIDE, 384)
        accessory("undertow_belt", RelicSlot.BELT, Route.TIDE, 384)
        accessory("tidepull_gauntlet", RelicSlot.GAUNTLET, Route.TIDE, 512)
        accessory("undertow_cloak", RelicSlot.CLOAK, Route.TIDE, 528)
        add("rift_blade", RelicItem(props("rift_blade").sword(ToolMaterial.DIAMOND, 4f, -2.8f).durability(768), route = Route.DRAGON, action = "rift"))
        add("tide_spear", RelicItem(props("tide_spear").sword(ToolMaterial.DIAMOND, 4f, -3f).durability(768), route = Route.TIDE, action = "wave"))
        add("echo_shield", ShieldItem(props("echo_shield").durability(672).delayedComponent(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS) { lookup ->
            net.minecraft.world.item.component.BlocksAttacks(.25f, 1f,
                listOf(net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90f, java.util.Optional.empty(), 0f, 1f)),
                net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3f, 1f, 1f),
                java.util.Optional.of(lookup.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
                java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BLOCK),
                java.util.Optional.of(net.minecraft.sounds.SoundEvents.SHIELD_BREAK))
        }))
        add("resonant_bow", BowItem(props("resonant_bow").durability(576).enchantable(1)))
        listOf("echo_arrow", "scouting_arrow", "foam_arrow").forEach { add(it, ArrowItem(props(it))) }
        add("tide_conch", RelicItem(props("tide_conch").durability(384), route = Route.TIDE, action = "conch"))
        add("cruise_firework", RelicItem(props("cruise_firework").stacksTo(1).fireResistant(), action = "firework"))
        add("bound_pearl", RelicItem(props("bound_pearl").stacksTo(1).fireResistant(), action = "pearl"))
        listOf("dragon", "echo", "tide").forEach { route -> block("${route}_workstation") { WorkshopBlock(it, route) } }
        block("resonance_box") { ResonatorBlock(it) }
        block("speed_rail") { SpeedRail(it) }
        block("endmarrow_ore") { Block(it.strength(30f, 1200f).requiresCorrectToolForDrops()) }
        Endforged.register()
        WORKSTATION_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("workstation"), FabricBlockEntityTypeBuilder.create(::Workstation, *blocks.values.filterIsInstance<WorkshopBlock>().toTypedArray()).build())
        RESONATOR_TYPE=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,id("resonance_box"),FabricBlockEntityTypeBuilder.create(::Resonator,blocks.getValue("resonance_box")).build())
        ACCESSORY_MENU = Registry.register(BuiltInRegistries.MENU, id("accessories"), ExtendedMenuType({ n, inv, _: Unit -> AccessoryMenu(n, inv) }, StreamCodec.unit(Unit)))
        WORKSTATION_MENU = Registry.register(BuiltInRegistries.MENU, id("workstation"), ExtendedMenuType({ n, inv, pos: BlockPos -> WorkstationMenu(n, inv, pos) }, BlockPos.STREAM_CODEC))
        CRUISE_RECIPE = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("cruise_firework"), RecipeSerializer(com.mojang.serialization.MapCodec.unit(::CruiseRecipe), StreamCodec.unit(CruiseRecipe())))
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,id("relics"),net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.lynxrelics")).icon{ItemStack(item("dragonthroat_gauntlet"))}.displayItems{_,output->items.values.forEach{output.accept(it)}}.build())
    }
}

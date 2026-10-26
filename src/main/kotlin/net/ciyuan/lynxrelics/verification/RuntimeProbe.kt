// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.verification

import com.google.gson.GsonBuilder
import com.mojang.authlib.GameProfile
import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.*
import net.ciyuan.lynxrelics.craft.*
import net.ciyuan.lynxrelics.menu.AccessoryMenu
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ClientInformation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.util.ProblemReporter
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** Runs only with an explicit JVM flag in the isolated dev server. Never touches a live player. */
object RuntimeProbe {
    fun run(server: MinecraftServer) {
        val results = mutableListOf<Map<String, Any>>()
        fun check(name: String, block: () -> Unit) {
            try { block(); results.add(mapOf("name" to name, "passed" to true)) }
            catch (e: Throwable) { results.add(mapOf("name" to name, "passed" to false, "error" to (e.message ?: e.javaClass.name))) }
        }
        var lastOverlay:net.minecraft.network.chat.Component?=null
        val player:ServerPlayer = object : ServerPlayer(server, server.overworld(), GameProfile(UUID.fromString("732d8911-2526-4425-a2fa-03756abc0011"), "RelicsProbe"), ClientInformation.createDefault()) {
            override fun sendOverlayMessage(message: net.minecraft.network.chat.Component) { lastOverlay=message }
        }
        check("player_mixin_and_elytra_exact_interface") {
            require(player is RelicHolder)
            val wings = net.ciyuan.lynxrelics.compat.ElytraCompat.inventory(player) ?: error("The exact Elytra Slot 1.2 JAR is required for this interoperability probe")
            require(wings.containerSize == 1)
            require(player.relics().accessories.containerSize == 4)
        }
        check("D02_atomic_materials_bottles_catalyst") {
            val items = MutableList(16) { ItemStack.EMPTY }
            items[0] = ItemStack(Items.DRAGON_BREATH, 8); items[9] = ItemStack(Items.DRAGON_EGG)
            val plan = CraftTransaction.plan(items, CraftRecipes.all.first { it.id == "D02" }) ?: error("No transaction plan")
            require(items[0].count == 8 && items[9].count == 1)
            require(plan[0].isEmpty && plan[9].`is`(Items.DRAGON_EGG) && plan[9].count == 1)
            require(plan.subList(10, 16).filter { it.`is`(Items.GLASS_BOTTLE) }.sumOf { it.count } == 8)
            require(plan.subList(10, 16).filter { it.`is`(Content.item("dragon_essence")) }.sumOf { it.count } == 4)
        }
        check("full_outputs_are_noop_even_when_some_outputs_could_fit") {
            val items = MutableList(16) { ItemStack.EMPTY }
            items[0] = ItemStack(Items.DRAGON_BREATH, 8); items[9] = ItemStack(Items.DRAGON_EGG)
            for (i in 10..15) items[i] = ItemStack(Items.STONE, 64)
            items[10] = ItemStack(Content.item("dragon_essence"), 60)
            val before = items.map { it.copy() }
            require(CraftTransaction.plan(items, CraftRecipes.all.first { it.id == "D02" }) == null)
            require(items.indices.all { ItemStack.matches(items[it], before[it]) })
        }
        check("tide_returns_one_empty_bucket") {
            val items = MutableList(16) { ItemStack.EMPTY }
            items[0] = ItemStack(Items.NAUTILUS_SHELL); items[1] = ItemStack(Items.PRISMARINE_SHARD, 4)
            items[2] = ItemStack(Items.DRIED_KELP, 8); items[3] = ItemStack(Items.WATER_BUCKET); items[9] = ItemStack(Items.HEART_OF_THE_SEA)
            val plan = CraftTransaction.plan(items, CraftRecipes.all.first { it.id == "T02" }) ?: error("No tide plan")
            require(plan.subList(10, 16).filter { it.`is`(Items.BUCKET) }.sumOf { it.count } == 1)
            require(plan[9].count == 1 && plan[3].isEmpty)
        }
        check("accessory_slot_types_and_vanilla_inventory_unchanged") {
            val vanillaSize = player.inventoryMenu.slots.size
            val menu = AccessoryMenu(31, player.inventory)
            val g = ItemStack(Content.item("dragonthroat_gauntlet"))
            require(menu.slots.size == 40 && menu.slots[2].mayPlace(g) && !menu.slots[0].mayPlace(g))
            require(!menu.slots[3].mayPlace(ItemStack(Items.ELYTRA)))
            require(!menu.slots[1].mayPlace(ItemStack(Items.NETHERITE_CHESTPLATE)))
            require(player.inventoryMenu.slots.size == vanillaSize)
        }
        check("shift_click_full_inventory_preserves_accessory") {
            player.relics().accessories.setItem(2, ItemStack(Content.item("dragonthroat_gauntlet")))
            for (i in 0..35) player.inventory.setItem(i, ItemStack(Items.STONE, 64))
            val menu = AccessoryMenu(32, player.inventory)
            require(menu.quickMoveStack(player, 2).isEmpty)
            require(player.relics().accessories.getItem(2).count == 1)
            for (i in 0..35) player.inventory.setItem(i, ItemStack.EMPTY)
            require(!menu.quickMoveStack(player, 2).isEmpty)
            require(player.relics().accessories.getItem(2).isEmpty)
            require((0..35).count { player.inventory.getItem(it).`is`(Content.item("dragonthroat_gauntlet")) } == 1)
        }
        check("persistent_resources_cooldowns_and_four_slots_roundtrip") {
            val s = player.relics(); s.breath = 38; s.tide = 70; s.cooldown("firework", 1000, 30); s.cooldown("pearl", 1000, 240)
            s.accessories.setItem(3, ItemStack(Content.item("dragonwing_cloak")))
            val out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess())
            s.save(out)
            val copy = RelicState(); copy.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), out.buildResult()))
            require(copy.breath == 38 && copy.tide == 70 && copy.remaining("pearl", 1000) == 240L && copy.remaining("firework", 1000) == 30L)
            require(copy.accessories.getItem(3).`is`(Content.item("dragonwing_cloak")))
            val dead = RelicState(); dead.copyFrom(copy, false)
            require(dead.accessories.isEmpty && dead.cooldowns == copy.cooldowns)
            val kept = RelicState(); kept.copyFrom(copy, true)
            require(kept.accessories.getItem(3).count == 1 && kept.cooldowns == copy.cooldowns)
        }
        check("dragon_actual_3000_health_and_reload_preserves_encounter") {
            val level = server.overworld()
            val dragon = net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) ?: error("No dragon")
            require(dragon.maxHealth == 3000f && dragon.health == 3000f)
            require(dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) == 3000.0)
            require(player.maxHealth == 20f)
            require(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value().sanitizeValue(3000.0) == 1024.0)
            dragon.health = 1800f
            val raid = (dragon as net.ciyuan.lynxrelics.raid.RaidHolder).lynxRaid()
            raid.phase = 2; raid.recalls = 4; raid.healed = 123f
            val out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess())
            dragon.saveWithoutId(out)
            val restored = net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) ?: error("No restored dragon")
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), out.buildResult()))
            val loaded = (restored as net.ciyuan.lynxrelics.raid.RaidHolder).lynxRaid()
            require(restored.health == 1800f && restored.maxHealth == 3000f)
            require(loaded.phase == 2 && loaded.recalls == 4 && loaded.healed == 123f && !loaded.rewarded)
        }
        check("endforged_actual_durability_and_smithing_component_retention") {
            require(Content.item("endforged_helmet").defaultInstance.maxDamage == 509)
            require(Content.item("endforged_chestplate").defaultInstance.maxDamage == 740)
            require(Content.item("endforged_leggings").defaultInstance.maxDamage == 694)
            require(Content.item("endforged_boots").defaultInstance.maxDamage == 602)
            require(Content.item("endforged_sword").defaultInstance.maxDamage == 2539)
            val base = ItemStack(Items.NETHERITE_SWORD)
            base.damageValue = 1000
            base.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("保留名称"))
            val sharpness = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)
            base.enchant(sharpness, 5)
            val output = net.ciyuan.lynxrelics.content.Endforged.preserveDamage(base, base.transmuteCopy(Content.item("endforged_sword")))
            require(output.damageValue == 1251 && base.damageValue == 1000)
            require(output.customName?.string == "保留名称" && output.enchantments.getLevel(sharpness) == 5)
        }
        check("both_vanilla_minecart_behaviors_read_speed_rail") {
            val level = server.overworld(); val pos = net.minecraft.core.BlockPos(8, 100, 8)
            level.getChunk(0, 0)
            level.setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3)
            val cart = net.minecraft.world.entity.EntityTypes.MINECART.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED) ?: error("No minecart")
            cart.setPos(8.5, 100.1, 8.5)
            for (mode in 0..3) {
                level.setBlock(pos, Content.blocks.getValue("speed_rail").defaultBlockState().setValue(net.ciyuan.lynxrelics.content.SpeedRail.SPEED, mode), 3)
                val expected = listOf(0.2, 0.4, 0.8, 1.2)[mode]
                require(kotlin.math.abs(net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior(cart).getMaxSpeed(level) - expected) < .00001)
                require(kotlin.math.abs(net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior(cart).getMaxSpeed(level) - expected) < .00001)
            }
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3)
        }
        check("every_registered_item_has_native_lore") {
            for ((id, item) in Content.items) {
                val lore = item.defaultInstance.get(net.minecraft.core.component.DataComponents.LORE)
                require(lore != null && lore.lines().isNotEmpty()) { "Missing lore: $id" }
            }
        }
        check("all_seven_mobs_have_actual_design_attributes") {
            for((kind,type)in net.ciyuan.lynxrelics.mob.RelicMobs.types){
                val mob=type.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)?:error("Create ${kind.id}")
                require(mob.maxHealth.toDouble()==kind.hp&&mob.health.toDouble()==kind.hp){"Health ${kind.id}: ${mob.health}/${mob.maxHealth}"}
                require(mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)==kind.armor)
                require(kotlin.math.abs(mob.bbWidth-kind.width)<.001&&kotlin.math.abs(mob.bbHeight-kind.height)<.001)
            }
        }
        check("brood_encounter_health_is_distinct_and_locked_and_saved"){
            val type=net.ciyuan.lynxrelics.mob.RelicMobs.types.getValue(net.ciyuan.lynxrelics.mob.RelicMobKind.BROOD)
            val mob=type.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!;mob.lockEncounter(8)
            require(mob.maxHealth==1746f);mob.lockEncounter(2);require(mob.maxHealth==1746f);mob.health=999f
            val out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,server.registryAccess());mob.saveWithoutId(out)
            val copy=type.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!;copy.load(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),out.buildResult()))
            require(copy.maxHealth==1746f&&copy.health==999f)
        }
        check("one_companion_per_owner_without_loading_other_dimensions"){
            val registry=net.ciyuan.lynxrelics.mob.Companions.get(server);val owner=UUID.fromString("732d8911-2526-4425-a2fa-03756abc0022");val one=UUID.fromString("732d8911-2526-4425-a2fa-03756abc0023");val two=UUID.fromString("732d8911-2526-4425-a2fa-03756abc0024")
            require(registry.available(owner));require(registry.register(owner,one));require(!registry.register(owner,two));registry.died(owner,two);require(!registry.available(owner));registry.died(owner,one);require(registry.available(owner))
        }
        check("six_pulses_are_six_hits_and_do_not_bypass_armor"){
            val level=server.overworld();val target=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)!!.baseValue=40.0;target.health=40f;target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)!!.baseValue=0.0
            val source=RelicDamage.pulse(level,player);require(!source.`is`(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR))
            repeat(6){require(Skills.hit(player,target,4f))};require(kotlin.math.abs(target.health-16f)<.001)
        }
        check("book_craft_and_specialization_keep_atomic_output_and_item_data"){
            val book=CraftRecipes.all.first{it.mode=="book"&&it.enchantment=="breath_capacity"}
            val items=MutableList(16){ItemStack.EMPTY};var index=0;for((id,n)in book.inputs)items[index++]=ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)),n)
            val output=CraftTransaction.plan(items,book,server.registryAccess())?:error("Book plan")
            require(output.subList(10,16).any{RelicEnchantments.bookHas(it,"breath_capacity",1)})
            val recipe=CraftRecipes.all.first{it.mode=="specialize"&&it.gear=="lynxrelics:rift_blade"&&it.specialization=="a"}
            val gear=MutableList(16){ItemStack.EMPTY};index=0;for((id,n)in recipe.inputs)gear[index++]=ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)),n)
            val blade=gear.first{it.`is`(Content.item("rift_blade"))};blade.damageValue=233;blade.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("原刃"))
            val plan=CraftTransaction.plan(gear,recipe,server.registryAccess())?:error("Specialization plan");val result=plan.subList(10,16).first{!it.isEmpty};require(result.damageValue==233&&result.customName?.string=="原刃"&&RelicEnchantments.spec(result)=="a");require(blade.damageValue==233&&RelicEnchantments.spec(blade).isEmpty())
        }
        check("enchantment_targets_and_exclusivity_are_native"){
            val registry=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            val breach=registry.getOrThrow(RelicEnchantments.key("breach"));val sharp=registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS)
            require(!net.minecraft.world.item.enchantment.Enchantment.areCompatible(breach,sharp))
            require(!net.minecraft.world.item.enchantment.Enchantment.areCompatible(registry.getOrThrow(RelicEnchantments.key("resonant_guard")),registry.getOrThrow(RelicEnchantments.key("bulwark"))))
            require(!registry.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION).value().canEnchant(ItemStack(Content.item("dragonthroat_gauntlet"))))
            require(registry.getOrThrow(RelicEnchantments.key("rhythm")).value().canEnchant(ItemStack(Content.item("dragonthroat_gauntlet"))))
        }
        check("quiet_state_packet_roundtrip_consumes_exact_payload"){
            val buffer=net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),server.registryAccess())
            val payload=StatePayload(777,List(4){player.relics().accessories.getItem(it).copy()},31,82,44,"breath",true)
            StatePayload.CODEC.encode(buffer,payload);val decoded=StatePayload.CODEC.decode(buffer);require(decoded.entity==777&&decoded.channel=="breath"&&decoded.quiet&&decoded.breath==31&&decoded.tide==82&&decoded.pressure==44&&buffer.readableBytes()==0);buffer.release()
        }
        check("actual_elytra_provider_survives_cloak_broken_and_repaired_wings"){
            val wings=ItemStack(Items.ELYTRA);val inv=net.ciyuan.lynxrelics.compat.ElytraCompat.inventory(player)!!
            inv.setItem(0,wings);player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,ItemStack(Content.item("endforged_chestplate")))
            player.relics().accessories.setItem(3,ItemStack(Content.item("dragonwing_cloak")))
            require(net.ciyuan.lynxrelics.compat.ElytraCompat.actualWings(player)===wings)
            wings.damageValue=wings.maxDamage-1;require(net.ciyuan.lynxrelics.compat.ElytraCompat.actualWings(player).isEmpty)
            wings.damageValue=0;require(net.ciyuan.lynxrelics.compat.ElytraCompat.actualWings(player)===wings)
            inv.clearContent();player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,ItemStack.EMPTY)
        }
        check("accessories_enter_native_mending_selection_without_armor_effects"){
            player.relics().accessories.clearContent();player.inventory.clearContent()
            val glove=ItemStack(Content.item("dragonthroat_gauntlet"));glove.damageValue=100
            glove.enchant(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING),1)
            player.relics().accessories.setItem(2,glove)
            val selected=net.minecraft.world.item.enchantment.EnchantmentHelper.getRandomItemWith(net.minecraft.world.item.enchantment.EnchantmentEffectComponents.REPAIR_WITH_XP,player){it.isDamaged}.orElseThrow()
            require(selected.itemStack()===glove)
            require(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)==0.0)
        }
        check("timed_workstation_reload_resumes_once_and_keeps_catalyst_and_bottles"){
            val level=server.overworld();val pos=net.minecraft.core.BlockPos(9,120,9);val state=Content.blocks.getValue("dragon_workstation").defaultBlockState()
            val station=Workstation(pos,state);station.setLevel(level);station.selected=station.recipes.indexOfFirst{it.id=="D02"}
            station.setItem(0,ItemStack(Items.DRAGON_BREATH,8));station.setItem(9,ItemStack(Items.DRAGON_EGG))
            repeat(200){station.tick()};require(station.progress==200)
            val restored=Workstation(pos,state);restored.setLevel(level);restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),station.saveCustomOnly(server.registryAccess())))
            repeat(200){restored.tick()};require(restored.progress==0&&restored.getItem(0).isEmpty&&restored.getItem(9).`is`(Items.DRAGON_EGG))
            require((10..15).sumOf{if(restored.getItem(it).`is`(Content.item("dragon_essence")))restored.getItem(it).count else 0}==4)
            require((10..15).sumOf{if(restored.getItem(it).`is`(Items.GLASS_BOTTLE))restored.getItem(it).count else 0}==8)
            repeat(400){restored.tick()};require((10..15).sumOf{if(restored.getItem(it).`is`(Content.item("dragon_essence")))restored.getItem(it).count else 0}==4)
        }
        check("smelting_hook_uses_real_ore_loot_and_exactly_one_extra_wear"){
            val level=server.overworld();val pos=net.minecraft.core.BlockPos(9,120,9);val tool=ItemStack(Items.NETHERITE_PICKAXE)
            tool.enchant(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(RelicEnchantments.key("smelting")),2)
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool)
            val state=net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState();val token=Mining.begin(level,player,pos,state,tool)
            val drops=try{net.minecraft.world.level.block.Block.getDrops(state,level,pos,null,player,tool)}finally{Mining.end(token)}
            require(drops.size==1&&drops[0].`is`(Items.IRON_INGOT)&&tool.damageValue==1)
            val diamond=net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState();val second=Mining.begin(level,player,pos,diamond,tool)
            val unaffected=try{net.minecraft.world.level.block.Block.getDrops(diamond,level,pos,null,player,tool)}finally{Mining.end(second)}
            require(unaffected.any{it.`is`(Items.DIAMOND)}&&tool.damageValue==1);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY)
        }
        check("beacon_core_persists_and_rejects_fuel_without_valid_pyramid"){
            val level=server.overworld();val pos=net.minecraft.core.BlockPos(10,120,10);val beacon=net.minecraft.world.level.block.entity.BeaconBlockEntity(pos,net.minecraft.world.level.block.Blocks.BEACON.defaultBlockState());beacon.setLevel(level)
            player.setShiftKeyDown(false);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack(Content.item("abundance_core")))
            require(net.ciyuan.lynxrelics.content.BeaconCores.use(player,net.minecraft.world.InteractionHand.MAIN_HAND,beacon)==net.minecraft.world.InteractionResult.SUCCESS)
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack(Content.item("condensed_tide")))
            require(net.ciyuan.lynxrelics.content.BeaconCores.use(player,net.minecraft.world.InteractionHand.MAIN_HAND,beacon)==net.minecraft.world.InteractionResult.FAIL&&player.mainHandItem.count==1)
            val copy=net.minecraft.world.level.block.entity.BeaconBlockEntity(pos,beacon.blockState);copy.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),beacon.saveCustomOnly(server.registryAccess())))
            require((copy as net.ciyuan.lynxrelics.content.BeaconCoreHolder).lynxBeaconCore().core.`is`(Content.item("abundance_core")))
            for(i in 0..35)player.inventory.setItem(i,ItemStack(Items.STONE,64));player.setShiftKeyDown(true)
            require(net.ciyuan.lynxrelics.content.BeaconCores.use(player,net.minecraft.world.InteractionHand.OFF_HAND,beacon)==net.minecraft.world.InteractionResult.FAIL)
            require((beacon as net.ciyuan.lynxrelics.content.BeaconCoreHolder).lynxBeaconCore().core.count==1)
            player.inventory.clearContent();player.setShiftKeyDown(false)
        }
        check("resonator_pickup_roundtrip_keeps_disc_fuel_mode_and_specialization"){
            val state=Content.blocks.getValue("resonance_box").defaultBlockState();val pos=net.minecraft.core.BlockPos(11,120,11)
            val box=net.ciyuan.lynxrelics.content.Resonator(pos,state);box.disc=ItemStack(Items.MUSIC_DISC_CAT);box.fuel=621;box.mode=2;box.specialization="b"
            val packed=box.packed();val restored=net.ciyuan.lynxrelics.content.Resonator(pos,state);restored.placed(player,packed)
            require(restored.disc.`is`(Items.MUSIC_DISC_CAT)&&restored.fuel==621&&restored.mode==2&&restored.specialization=="b"&&packed.maxStackSize==1)
        }
        check("server_recipe_catalog_codec_preserves_all_choices_and_indices"){
            val buffer=net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),server.registryAccess())
            RecipeCatalogPayload.CODEC.encode(buffer,RecipeCatalogPayload(CraftRecipes.catalogJson()))
            val decoded=CraftRecipes.parse(RecipeCatalogPayload.CODEC.decode(buffer).json)
            require(decoded==CraftRecipes.all&&buffer.readableBytes()==0)
            for(route in listOf("dragon","echo","tide"))require(decoded.filter{it.route==route||it.route=="any"}.map{it.id}==CraftRecipes.forRoute(route).map{it.id})
            buffer.release()
        }
        check("native_dragon_charge_and_three_part_combo_keep_warn_and_recovery_boundaries"){
            val level=server.overworld();val dragon=net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!
            val s=(dragon as net.ciyuan.lynxrelics.raid.RaidHolder).lynxRaid();val now=level.gameTime
            s.phase=3;s.combo=0;s.skill="swoop";s.warningFrom=now;s.skillAt=now+24;s.skillOrigin=net.minecraft.world.phys.Vec3(0.0,140.0,0.0);s.skillTarget=net.minecraft.world.phys.Vec3(12.0,120.0,12.0)
            net.ciyuan.lynxrelics.raid.RaidSkills.tick(dragon,now);require(!s.started)
            net.ciyuan.lynxrelics.raid.RaidSkills.tick(dragon,now+24);require(s.started&&dragon.phaseManager.currentPhase.phase==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.CHARGING_PLAYER)
            require(net.ciyuan.lynxrelics.raid.RaidSkills.controlsContact(dragon)&&!net.ciyuan.lynxrelics.raid.RaidSkills.allowGrief(dragon))
            net.ciyuan.lynxrelics.raid.RaidSkills.tick(dragon,now+104);require(s.combo==1&&s.skill=="breath"&&!s.started&&s.warningFrom==now+144&&s.skillAt==now+174)
            val secondEnd=s.skillAt+30;net.ciyuan.lynxrelics.raid.RaidSkills.tick(dragon,secondEnd);require(s.combo==2&&s.skill=="leave"&&s.skillAt>=secondEnd+64)
            net.ciyuan.lynxrelics.raid.RaidSkills.tick(dragon,s.skillAt+60);require(s.skill.isEmpty()&&s.combo== -1&&dragon.phaseManager.currentPhase.phase==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN)
        }
        check("dragon_pending_combo_and_reward_flag_roundtrip_without_reset"){
            val s=net.ciyuan.lynxrelics.raid.RaidState();s.phase=3;s.skill="breath";s.skillAt=14323;s.warningFrom=14293;s.started=true;s.combo=1;s.targetPlayer=player.uuid.toString();s.skillDirection=net.minecraft.world.phys.Vec3(1.0,0.0,0.0);s.healed=450f;s.recalls=8;s.rewarded=true;s.hitPlayers.add(player.uuid.toString());s.comboGuards[player.uuid.toString()]=14333;s.comboSegments[player.uuid.toString()]=0
            val out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,server.registryAccess());s.save(out)
            val copy=net.ciyuan.lynxrelics.raid.RaidState();copy.load(TagValueInput.create(ProblemReporter.DISCARDING,server.registryAccess(),out.buildResult()))
            require(copy.started&&copy.restorePhase&&copy.skill=="breath"&&copy.combo==1&&copy.skillAt==14323L&&copy.skillDirection==s.skillDirection&&copy.hitPlayers==s.hitPlayers&&copy.comboGuards==s.comboGuards&&copy.comboSegments==s.comboSegments&&copy.healed==450f&&copy.recalls==8&&copy.rewarded)
        }
        check("raid_ember_ttl_only_affects_tagged_embers"){
            val level=server.overworld();val ember=net.minecraft.world.entity.EntityTypes.ENDERMITE.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!
            val normal=net.minecraft.world.entity.EntityTypes.ENDERMITE.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!
            ember.addTag("lynx_raid_ember_${UUID.randomUUID()}");ember.addTag("lynx_ember_expiry_0")
            net.ciyuan.lynxrelics.raid.RaidSkills.emberTick(ember);net.ciyuan.lynxrelics.raid.RaidSkills.emberTick(normal);require(ember.isRemoved&&!normal.isRemoved)
        }
        check("dry_channeling_uses_native_rod_and_sky_conditions_and_restores_weather_context"){
            val level=server.overworld();val pos=net.minecraft.core.BlockPos(16,140,16);level.getChunk(1,1)
            level.setRainLevel(0f);level.setThunderLevel(0f);require(!level.isThundering)
            val rod=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("minecraft:lightning_rod")).defaultBlockState();level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);level.setBlock(pos,rod,3)
            val trident=net.minecraft.world.entity.EntityTypes.TRIDENT.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!;trident.setPos(net.minecraft.world.phys.Vec3.atCenterOf(pos))
            val enchantment=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.CHANNELING)
            val stack=ItemStack(Items.TRIDENT);stack.enchant(enchantment,1)
            val used=net.minecraft.world.item.enchantment.EnchantedItemInUse(stack,net.minecraft.world.entity.EquipmentSlot.MAINHAND,player){ }
            val box=net.minecraft.world.phys.AABB(pos).inflate(8.0)
            val before=level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt::class.java,box).map{it.uuid}.toSet()
            enchantment.value().onHitBlock(level,1,used,trident,net.minecraft.world.phys.Vec3.atCenterOf(pos),rod)
            val created=level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt::class.java,box).filter{it.uuid !in before}
            require(created.size==1&&!net.ciyuan.lynxrelics.content.TridentHooks.channelingWeather(false))
            created.forEach{it.discard()};level.setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3)
            enchantment.value().onHitBlock(level,1,used,trident,net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState())
            require(level.getEntitiesOfClass(net.minecraft.world.entity.LightningBolt::class.java,box).none{it.uuid !in before})
            level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.below(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3)
        }
        check("native_riptide_use_and_release_work_in_clear_dry_air"){
            val level=server.overworld();player.setPos(16.5,145.0,16.5);require(!player.isInWaterOrRain)
            val stack=ItemStack(Items.TRIDENT);stack.enchant(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.RIPTIDE),3)
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);player.deltaMovement=net.minecraft.world.phys.Vec3.ZERO
            stack.item.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);require(player.isUsingItem)
            (stack.item as net.minecraft.world.item.TridentItem).releaseUsing(stack,level,player,stack.getUseDuration(player)-20)
            require(player.deltaMovement.lengthSqr()>1.0&&player.isAutoSpinAttack)
            player.stopUsingItem();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY)
        }
        check("release_in_the_same_server_tick_cancels_charge_without_spending"){
            val s=player.relics();s.channel="";s.breath=40;s.overloadUntil=0;s.cooldowns.remove("gauntlet");s.actionTicks.fill(Long.MIN_VALUE)
            s.accessories.setItem(2,ItemStack(Content.item("dragonthroat_gauntlet")))
            Skills.action(player,1);require(s.channel=="breath");Skills.action(player,5);require(s.channel.isEmpty()&&s.breath==40)
        }
        check("wave_corridor_stops_at_real_blocks_and_excludes_off_path_targets"){
            val level=server.overworld();player.setPos(.5,150.0,.5)
            val start=net.minecraft.world.phys.Vec3(.5,151.0,.5);val forward=net.minecraft.world.phys.Vec3(0.0,0.0,1.0)
            val one=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!;one.setPos(.5,150.0,5.5);level.addFreshEntity(one)
            val off=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level,net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!;off.setPos(3.0,150.0,3.5);level.addFreshEntity(off)
            try{
                val end=Skills.loadedRay(player,start,forward,8.0);val found=Skills.pathTargets(player,start,end,.75,3)
                require(found.any{it.entity===one}&&found.none{it.entity===off})
                val wall=net.minecraft.core.BlockPos(0,150,3);level.setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);level.setBlock(wall.above(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3)
                try{val blocked=Skills.loadedRay(player,start,forward,8.0);require(blocked.z<=3.01&&Skills.pathTargets(player,start,blocked,.75,3).none{it.entity===one})}
                finally{level.setBlock(wall,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);level.setBlock(wall.above(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3)}
            }finally{one.discard();off.discard()}
        }
        check("magnetic_ring_full_chest_owner_filter_and_four_entity_rate_are_atomic"){
            val level=server.overworld();val pos=net.minecraft.core.BlockPos(13,150,13);val chestPos=pos.east();level.setBlock(pos,net.minecraft.world.level.block.Blocks.LODESTONE.defaultBlockState(),3);level.setBlock(chestPos,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(),3)
            val registry=net.ciyuan.lynxrelics.content.AnchorRegistry.get(server);val key="${level.dimension().identifier()}|${pos.asLong()}"
            val entities=mutableListOf<net.minecraft.world.entity.item.ItemEntity>()
            try{
                player.setShiftKeyDown(false);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack(Content.item("magnetic_ring")));require(net.ciyuan.lynxrelics.content.AnchorAddons.use(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos)==net.minecraft.world.InteractionResult.SUCCESS)
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack(Items.AMETHYST_SHARD));net.ciyuan.lynxrelics.content.AnchorAddons.use(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos)
                val chest=level.getBlockEntity(chestPos) as net.minecraft.world.Container
                repeat(6){i->val entity=net.minecraft.world.entity.item.ItemEntity(level,13.5,150.3,16.0+i*.05,ItemStack(Items.DIRT));entity.setNoPickUpDelay();if(i==5)entity.setTarget(UUID.randomUUID());level.addFreshEntity(entity);entities.add(entity)}
                for(i in 0 until chest.containerSize)chest.setItem(i,ItemStack(Items.STONE,64))
                net.ciyuan.lynxrelics.content.AnchorAddons.pulse(server);require(entities.all{!it.isRemoved&&it.item.count==1})
                chest.clearContent();net.ciyuan.lynxrelics.content.AnchorAddons.pulse(server)
                require((0 until chest.containerSize).sumOf{if(chest.getItem(it).`is`(Items.DIRT))chest.getItem(it).count else 0}==4)
                require(entities.take(5).count{it.isRemoved}==4&&!entities[5].isRemoved&&registry.entries.getValue(key).fuel==Balance.ticks("lodestone.fuelTicks")-40)
            }finally{entities.forEach{it.discard()};registry.entries.remove(key);registry.setDirty();level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);(level.getBlockEntity(chestPos) as? net.minecraft.world.Container)?.clearContent();level.setBlock(chestPos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);player.inventory.clearContent()}
        }
        check("only_defined_specializations_are_offered_and_lore_describes_the_branch"){
            require(CraftRecipes.all.none{it.mode in setOf("specialize","respecialize")&&it.gear in setOf("lynxrelics:dragonbound_belt","lynxrelics:keeper_gauntlet","lynxrelics:undertow_belt")})
            require(CraftRecipes.all.any{it.mode=="specialize"&&it.gear=="lynxrelics:resonance_box"})
            val blade=ItemStack(Content.item("rift_blade"));RelicEnchantments.setSpec(blade,"a");RelicEnchantments.setSpec(blade,"b")
            require(blade.get(net.minecraft.core.component.DataComponents.LORE)!!.lines().count{it.style.insertion=="lynxrelics-specialization"}==1)
            val box=net.ciyuan.lynxrelics.content.Resonator(net.minecraft.core.BlockPos.ZERO,Content.blocks.getValue("resonance_box").defaultBlockState());val original=ItemStack(Content.item("resonance_box"));original.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("留声"));RelicEnchantments.setSpec(original,"b");box.placed(player,original)
            require(box.packed().customName?.string=="留声"&&RelicEnchantments.spec(box.packed())=="b")
        }
        check("real_elytra_death_callback_legacy_abi_keepinventory_and_vanishing"){
            val level=server.overworld();val rule=net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY;val oldRule=level.gameRules.get(rule)
            val wings=net.ciyuan.lynxrelics.compat.ElytraCompat.inventory(player)!!;val accessories=player.relics().accessories
            val bounds=player.boundingBox.inflate(12.0);val before=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,bounds).map{it.uuid}.toSet()
            fun death(){net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.invoker().afterDeath(player,level.damageSources().generic())}
            try{
                player.setShiftKeyDown(false);accessories.clearContent();wings.clearContent();level.gameRules.set(rule,false,server)
                wings.setItem(0,ItemStack(Items.ELYTRA));accessories.setItem(2,ItemStack(Content.item("dragonthroat_gauntlet")));death()
                require(wings.getItem(0).isEmpty&&accessories.getItem(2).isEmpty)
                var dropped=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,bounds).filter{it.uuid !in before}
                require(dropped.count{it.item.`is`(Items.ELYTRA)}==1&&dropped.count{it.item.`is`(Content.item("dragonthroat_gauntlet"))}==1)
                dropped.forEach{it.discard()};level.gameRules.set(rule,true,server)
                wings.setItem(0,ItemStack(Items.ELYTRA));accessories.setItem(2,ItemStack(Content.item("dragonthroat_gauntlet")));death()
                require(wings.getItem(0).count==1&&accessories.getItem(2).count==1&&level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,bounds).none{it.uuid !in before})
                val curse=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE)
                wings.getItem(0).enchant(curse,1);accessories.getItem(2).enchant(curse,1);level.gameRules.set(rule,false,server);death()
                require(wings.getItem(0).isEmpty&&accessories.getItem(2).isEmpty&&level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,bounds).none{it.uuid !in before})
            }finally{level.gameRules.set(rule,oldRule,server);wings.clearContent();accessories.clearContent();level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java,bounds).filter{it.uuid !in before}.forEach{it.discard()}}
        }
        check("endforged_magic_source_armor_resistance_and_boosted_attack_attributes"){
            val sword=ItemStack(Content.item("endforged_sword"));val source=sword.getDamageSource(player)
            require(source.entity===player&&source.`is`(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR))
            require(source.`is`(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,Content.id("magic_damage"))))
            require(!ItemStack(Items.NETHERITE_SWORD).getDamageSource(player).`is`(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR))
            val slots=listOf(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET)
            try{
                for((i,id)in listOf("endforged_helmet","endforged_chestplate","endforged_leggings","endforged_boots").withIndex())player.setItemSlot(slots[i],ItemStack(Content.item(id)))
                require(kotlin.math.abs(net.ciyuan.lynxrelics.content.Endforged.magicResistance(player)-.32)<.00001)
                require(kotlin.math.abs(Combat.adjustedDamage(player,source,10f)-6.8f)<.0001)
                require(Combat.adjustedDamage(player,server.overworld().damageSources().generic(),10f)==10f)
                player.setItemSlot(slots[0],ItemStack(Items.NETHERITE_HELMET));require(kotlin.math.abs(net.ciyuan.lynxrelics.content.Endforged.magicResistance(player)-.24)<.00001)
                val attributes=net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS
                for((id,damage,speed)in listOf(Triple("endforged_axe",11.0,1.2),Triple("endforged_sword",9.0,1.8))){val a=ItemStack(Content.item(id)).get(attributes)!!;require(kotlin.math.abs(a.compute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,1.0,net.minecraft.world.entity.EquipmentSlot.MAINHAND)-damage)<.00001);require(kotlin.math.abs(a.compute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,4.0,net.minecraft.world.entity.EquipmentSlot.MAINHAND)-speed)<.00001)}
            }finally{slots.forEach{player.setItemSlot(it,ItemStack.EMPTY)}}
        }
        check("dragon_phase_two_three_empty_encounter_does_not_leave_hover_or_override_perching"){
            val dragon=net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!
            val s=(dragon as net.ciyuan.lynxrelics.raid.RaidHolder).lynxRaid()
            dragon.health=1800f;net.ciyuan.lynxrelics.raid.DragonRaid.tick(dragon)
            require(s.phase==2&&dragon.phaseManager.currentPhase.phase!=net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOVERING)
            dragon.phaseManager.setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOVERING);dragon.health=800f;net.ciyuan.lynxrelics.raid.DragonRaid.tick(dragon)
            require(s.phase==3&&dragon.phaseManager.currentPhase.phase==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN)
            dragon.phaseManager.setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_SCANNING);net.ciyuan.lynxrelics.raid.DragonRaid.tick(dragon)
            require(dragon.phaseManager.currentPhase.phase==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.SITTING_SCANNING)
            dragon.phaseManager.setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING);net.ciyuan.lynxrelics.raid.DragonRaid.tick(dragon)
            require(dragon.phaseManager.currentPhase.phase==net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.DYING)
        }
        check("skill_resource_failure_is_explained_and_tide_gauntlet_actually_spends_and_cools"){
            val s=player.relics();s.accessories.clearContent();s.accessories.setItem(2,ItemStack(Content.item("tidepull_gauntlet")));s.tide=0;s.channel="";s.cooldowns.remove("gauntlet");s.feedbackAt=Long.MIN_VALUE
            require(!SkillFeedback.beforeAction(player,1))
            require((lastOverlay?.contents as? net.minecraft.network.chat.contents.TranslatableContents)?.key=="skill.lynxrelics.need_tide")
            s.tide=100;s.actionTicks.fill(Long.MIN_VALUE);s.feedbackAt=Long.MIN_VALUE;Skills.action(player,1)
            require(s.tide==75&&s.remaining("gauntlet",Skills.now(player))>0&&s.accessories.getItem(2).damageValue==1)
            require(Balance.ticks("infinite.fireworkCooldown")==15&&Balance.ticks("infinite.pearlCooldown")==100)
        }
        check("offhand_wave_uses_its_own_weapon_and_reaches_a_real_hostile_target"){
            val s=player.relics();val now=Skills.now(player);s.accessories.clearContent();s.channel="";s.tide=100;s.lastWater=now;s.cooldowns.clear();s.feedbackAt=Long.MIN_VALUE
            player.setPos(.5,150.0,.5);player.yRot=0f;player.xRot=0f;player.setShiftKeyDown(false)
            val zombie=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!
            zombie.setPos(.5,150.0,3.5);zombie.isNoAi=true;server.overworld().addFreshEntity(zombie)
            try{
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY)
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack(Content.item("tide_spear")))
                require(Skills.useItem(player,net.minecraft.world.InteractionHand.OFF_HAND,"wave")==net.minecraft.world.InteractionResult.SUCCESS)
                require(s.channel=="wave"&&s.channelHand==net.minecraft.world.InteractionHand.OFF_HAND)
                s.channelStarted=now-12;Skills.tickWeapon(player,now)
                require(s.channel.isEmpty()&&s.tide==75&&s.remaining("wave",now)>0&&zombie.health<zombie.maxHealth)
            }finally{zombie.discard();player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);s.channel=""}
        }
        check("mainhand_skill_yields_to_offhand_shield_and_weapon_key_still_charges"){
            val s=player.relics();s.channel="";s.breath=40;s.overloadUntil=0;s.cooldowns.clear();s.actionTicks.fill(Long.MIN_VALUE)
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack(Content.item("rift_blade")))
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack(Items.SHIELD))
            try{
                require(player.mainHandItem.item.use(player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND)==net.minecraft.world.InteractionResult.PASS&&s.channel.isEmpty())
                Skills.action(player,4);require(s.channel=="rift"&&s.channelHand==net.minecraft.world.InteractionHand.MAIN_HAND)
            }finally{s.channel="";player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY)}
        }
        check("three_necklace_actives_enter_their_real_channel_lure_or_reflection_state"){
            val s=player.relics();val now=Skills.now(player)
            val lodestone=net.minecraft.core.BlockPos(0,151,3);val previousBlock=player.level().getBlockState(lodestone)
            try{
                Investigations.clear();player.setPos(.5,150.0,.5);player.yRot=0f;player.xRot=0f;player.yHeadRot=0f
                // A newly received move/turn must not raycast from the previous tick's pose.
                player.xo=-64.0;player.yo=12.0;player.zo=-64.0;player.yRotO=90f;player.xRotO=45f
                s.channel="";s.cooldowns.clear();s.breath=40;s.overloadUntil=0;s.anchorDimension=player.level().dimension().identifier().toString()
                s.accessories.setItem(0,ItemStack(Content.item("homing_dragon_necklace")));player.level().setBlock(lodestone,net.minecraft.world.level.block.Blocks.LODESTONE.defaultBlockState(),3)
                s.actionTicks.fill(Long.MIN_VALUE);Skills.action(player,2);require(s.channel.isEmpty()&&s.anchorPos==lodestone.asLong()){"Homing necklace did not bind from the current pose"}
                player.level().setBlock(lodestone,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3)
                s.actionTicks.fill(Long.MIN_VALUE);Skills.action(player,2);require(s.channel=="recall"){"Homing necklace did not start recall: ${s.lastFeedbackKey}, channel=${s.channel}"}
                player.level().setBlock(lodestone,net.minecraft.world.level.block.Blocks.LODESTONE.defaultBlockState(),3)
                s.channel="";s.accessories.setItem(0,ItemStack(Content.item("resonant_necklace")));s.actionTicks.fill(Long.MIN_VALUE);Skills.action(player,2)
                require(s.remaining("necklace",now)>0&&s.accessories.getItem(0).damageValue==1&&Investigations.closest(player.level(),player.position(),20.0)!=null){"Echo necklace did not create a lure: ${s.lastFeedbackKey}, cooldown=${s.remaining("necklace",now)}, wear=${s.accessories.getItem(0).damageValue}"}
                val mark=Investigations.closest(player.level(),player.position(),20.0)
                require(mark!=null&&mark.distanceToSqr(net.minecraft.world.phys.Vec3(.5,player.eyeY,3.0))<.01){"Lure missed the aimed lodestone from the current pose: $mark"}
                s.cooldowns.clear();s.tide=100;s.accessories.setItem(0,ItemStack(Content.item("watermirror_necklace")));s.actionTicks.fill(Long.MIN_VALUE);Skills.action(player,2)
                require(s.mirrorUntil>now&&s.mirrorUntilNanos>System.nanoTime()&&s.tide==100&&s.remaining("necklace",now)==60L){"Mirror necklace did not arm: ${s.lastFeedbackKey}, until=${s.mirrorUntil}, tide=${s.tide}, cooldown=${s.remaining("necklace",now)}"}
            }finally{player.level().setBlock(lodestone,previousBlock,3);s.accessories.clearContent();s.channel="";s.mirrorUntil=0;s.mirrorUntilNanos=0;Investigations.clear()}
        }
        check("owned_lore_refresh_keeps_custom_text_name_damage_and_specialization"){
            val stack=ItemStack(Content.item("rift_blade"));val loreKey=net.minecraft.core.component.DataComponents.LORE
            stack.damageValue=37;stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Remembered blade"));RelicEnchantments.setSpec(stack,"b")
            val extra=net.minecraft.network.chat.Component.literal("Personal note").withStyle(net.minecraft.ChatFormatting.YELLOW)
            val outdated=net.minecraft.network.chat.Component.literal("Outdated mod introduction").withStyle{it.withInsertion("lynxrelics-lore")}
            stack.set(loreKey,net.minecraft.world.item.component.ItemLore(listOf(outdated,extra)+stack.get(loreKey)!!.lines().filter{it.style.insertion=="lynxrelics-specialization"}))
            net.ciyuan.lynxrelics.content.RelicLore.refreshOwned(stack)
            val lines=stack.get(loreKey)!!.lines()
            require(lines.any{it==extra}&&lines.none{it.string=="Outdated mod introduction"}&&lines.count{it.style.insertion=="lynxrelics-specialization"}==1)
            require(lines.count{it.style.insertion=="lynxrelics-lore"}==net.ciyuan.lynxrelics.content.RelicLore.create("rift_blade").lines().size)
            require(stack.damageValue==37&&stack.customName?.string=="Remembered blade"&&RelicEnchantments.spec(stack)=="b")
        }
        check("disabled_relic_pvp_does_not_charge_defensive_passives_from_players"){
            val other=ServerPlayer(server,server.overworld(),GameProfile(UUID.fromString("732d8911-2526-4425-a2fa-03756abc0022"),"RelicsOther"),ClientInformation.createDefault())
            require(!Balance.enabled("skills.pvp")&&player.canHarmPlayer(other))
            require(!Combat.validHostile(player,server.overworld().damageSources().playerAttack(other)))
            val zombie=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED)!!
            require(Combat.validHostile(player,server.overworld().damageSources().mobAttack(zombie)))
        }
        val report = mapOf("minecraft" to "26.3", "stage" to "isolated-server-component-probe", "passed" to results.all { it["passed"] == true }, "checks" to results, "realClientAcceptance" to false)
        val output = Path.of("verification.json").toAbsolutePath()
        Files.newBufferedWriter(output).use { GsonBuilder().setPrettyPrinting().create().toJson(report, it) }
        org.slf4j.LoggerFactory.getLogger("LynxRelicsProbe").info("LynxRelics runtime probe: {}/{} passed; {}", results.count { it["passed"] == true }, results.size, output)
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.*
import net.ciyuan.lynxrelics.craft.CraftRecipes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.gamerules.GameRules
import org.slf4j.LoggerFactory

class LynxRelics : ModInitializer {
    override fun onInitialize() {
        Balance.load(); Content.register(); CraftRecipes.load(); RelicNetworking.register()
        net.ciyuan.lynxrelics.content.EndmarrowFeature.register()
        net.ciyuan.lynxrelics.mob.RelicMobs.register()
        net.ciyuan.lynxrelics.verification.DevCommands.register()
        ServerLifecycleEvents.SERVER_STARTED.register { server -> if (java.lang.Boolean.getBoolean("lynxrelics.probe")) net.ciyuan.lynxrelics.verification.RuntimeProbe.run(server) }
        ServerTickEvents.END_SERVER_TICK.register { server -> server.playerList.players.forEach { Skills.tick(it); Combat.tick(it); Mining.tick(it) }; Combat.tickServer(server);net.ciyuan.lynxrelics.content.AnchorAddons.tick(server) }
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Combat::allowDamage)
        ServerLifecycleEvents.SERVER_STOPPED.register { Combat.clear(); OreIndex.clear();Conch.clear() }
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register { level,chunk,_->OreIndex.loaded(level,chunk) }
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_UNLOAD.register(OreIndex::unloaded)
        ServerPlayerEvents.COPY_FROM.register { old, fresh, alive -> fresh.relics().copyFrom(old.relics(), alive || old.level().gameRules.get(GameRules.KEEP_INVENTORY)) }
        ServerPlayerEvents.JOIN.register { player -> RelicNetworking.sendCatalog(player);RelicNetworking.sync(player, true) }
        ServerPlayerEvents.LEAVE.register(Skills::interrupt)
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register { player, _, hand, hit ->
            if(player !is ServerPlayer)return@register net.minecraft.world.InteractionResult.PASS
            val blockEntity=player.level().getBlockEntity(hit.blockPos)
            if(blockEntity is net.minecraft.world.level.block.entity.BeaconBlockEntity){val result=net.ciyuan.lynxrelics.content.BeaconCores.use(player,hand,blockEntity);if(result!=net.minecraft.world.InteractionResult.PASS)return@register result}
            if(player.level().getBlockState(hit.blockPos).`is`(net.minecraft.world.level.block.Blocks.LODESTONE)){
                val result=net.ciyuan.lynxrelics.content.AnchorAddons.use(player,hand,hit.blockPos);if(result!=net.minecraft.world.InteractionResult.PASS)return@register result
                if(player.isShiftKeyDown && (player.getItemInHand(hand).`is`(Content.item("homing_dragon_necklace")) || player.getItemInHand(hand).isEmpty && Skills.has(player,"homing_dragon_necklace"))){player.relics().anchorDimension=player.level().dimension().identifier().toString();player.relics().anchorPos=hit.blockPos.asLong();player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("msg.lynxrelics.anchor_bound"));return@register net.minecraft.world.InteractionResult.SUCCESS}
            }
            net.minecraft.world.InteractionResult.PASS
        }
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            net.ciyuan.lynxrelics.raid.RaidSkills.emberDied(entity)
            if (entity is ServerPlayer && !entity.level().gameRules.get(GameRules.KEEP_INVENTORY) && !entity.isSpectator) {
                val inv = entity.relics().accessories
                for (i in 0..3) { val stack = inv.removeItemNoUpdate(i); if (!stack.isEmpty && !net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack,net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) entity.spawnAtLocation(entity.level(), stack) }
                entity.relics().channel = ""
            }
        }
        ServerLivingEntityEvents.AFTER_DAMAGE.register { entity, source, _, damage, _ -> Combat.afterDamage(entity, source, damage) }
        LoggerFactory.getLogger("LynxRelics").info("LynxRelics v0.2 initialized; four independent accessory slots; Minecraft 26.3 / Java 25")
    }
}

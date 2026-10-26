// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.client

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.*
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

class RelicsClient : ClientModInitializer {
    override fun onInitializeClient() {
        MenuScreens.register(Content.ACCESSORY_MENU, ::AccessoryScreen)
        MenuScreens.register(Content.WORKSTATION_MENU, ::WorkshopScreen)
        for((kind,type)in net.ciyuan.lynxrelics.mob.RelicMobs.types)net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type){context->RelicMobRenderer(context,kind)}
        val category = KeyMapping.Category.register(Content.id("controls"))
        val open = KeyMappingHelper.registerKeyMapping(KeyMapping("key.lynxrelics.accessories", com.mojang.blaze3d.platform.InputConstants.KEY_K, category))
        val gauntlet = KeyMappingHelper.registerKeyMapping(KeyMapping("key.lynxrelics.gauntlet", com.mojang.blaze3d.platform.InputConstants.KEY_G, category))
        val weapon = KeyMappingHelper.registerKeyMapping(KeyMapping("key.lynxrelics.weapon", com.mojang.blaze3d.platform.InputConstants.KEY_V, category))
        val cloak = KeyMappingHelper.registerKeyMapping(KeyMapping("key.lynxrelics.cloak", com.mojang.blaze3d.platform.InputConstants.KEY_H, category))
        var wasG = false; var wasJump = false
        ClientPlayNetworking.registerGlobalReceiver(RecipeCatalogPayload.TYPE){packet,ctx->ctx.client().execute{net.ciyuan.lynxrelics.craft.CraftRecipes.acceptCatalog(net.ciyuan.lynxrelics.craft.CraftRecipes.parse(packet.json))}}
        ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE) { packet, ctx -> ctx.client().execute {
            (ctx.client().level?.getEntity(packet.entity) as? Player)?.let { target ->
                val state = target.relics()
                for (i in 0..3) state.accessories.setItem(i, packet.stacks[i])
                state.breath = packet.breath; state.tide = packet.tide; state.pressure = packet.pressure.toDouble(); state.channel = packet.channel
                state.clientQuiet = packet.quiet
            }
        } }
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player
            if (player != null && ClientPlayNetworking.canSend(ActionPayload.TYPE)) {
                while (open.consumeClick()) ClientPlayNetworking.send(ActionPayload(0))
                if (client.gui.screen() == null) {
                    while (gauntlet.consumeClick()) ClientPlayNetworking.send(ActionPayload(if (player.isShiftKeyDown) 2 else 1))
                    while (weapon.consumeClick()) ClientPlayNetworking.send(ActionPayload(4))
                    while (cloak.consumeClick()) ClientPlayNetworking.send(ActionPayload(6))
                    val jump = client.options.keyJump.isDown
                    if (jump && !wasJump && !player.onGround()&&!(player.isCreative&&player.abilities.flying)) ClientPlayNetworking.send(ActionPayload(3))
                    wasJump = jump
                }
                if (wasG && !gauntlet.isDown) ClientPlayNetworking.send(ActionPayload(5))
                wasG = gauntlet.isDown
            } else { wasG = false; wasJump = false }
        }
        ScreenEvents.AFTER_INIT.register { _, screen, w, h ->
            if (screen is InventoryScreen || screen is CreativeModeInventoryScreen) {
                Screens.getWidgets(screen).add(Button.builder(Component.translatable("ui.lynxrelics.open_accessories")) { if (ClientPlayNetworking.canSend(ActionPayload.TYPE)) ClientPlayNetworking.send(ActionPayload(0)) }.bounds(w / 2 - 109, h / 2 - 80, 20, 20).build())
            }
        }
    }
}

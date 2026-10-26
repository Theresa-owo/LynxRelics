// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.verification

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.*
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** Local SDK development only. This command is absent in a normal installed JAR. */
object DevCommands {
    fun register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment) return
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> dispatcher.register(Commands.literal("relicstest")
            .executes { ctx ->
                val p = ctx.source.playerOrException
                for ((i, id) in listOf("homing_dragon_necklace", "dragonbound_belt", "dragonthroat_gauntlet", "dragonwing_cloak").withIndex())
                    if (p.relics().accessories.getItem(i).isEmpty) p.relics().accessories.setItem(i, ItemStack(Content.item(id)))
                listOf("dragon_workstation", "echo_workstation", "tide_workstation", "rift_blade", "tide_spear", "echo_shield", "resonant_bow", "cruise_firework", "bound_pearl").forEach { p.inventory.add(ItemStack(Content.item(it))) }
                p.inventory.add(ItemStack(Items.DRAGON_EGG)); p.inventory.add(ItemStack(Items.DRAGON_BREATH, 8)); p.inventory.add(ItemStack(Content.item("dragon_essence"), 32))
                net.ciyuan.lynxrelics.compat.ElytraCompat.inventory(p)?.let{if(it.getItem(0).isEmpty)it.setItem(0,ItemStack(Items.ELYTRA))}
                p.relics().breath = 50; RelicNetworking.sync(p, true)
                p.sendSystemMessage(Component.translatable("msg.lynxrelics.dev_ready")); 1
            }.then(Commands.literal("breathe").executes { ctx -> Skills.action(ctx.source.playerOrException, 1); 1 })
            .then(Commands.literal("status").executes { ctx ->
                val p=ctx.source.playerOrException;val s=p.relics();val now=Skills.now(p)
                val actionKey=when(s.lastReceivedAction){0->"key.lynxrelics.accessories";1->"key.lynxrelics.gauntlet";2->"debug.lynxrelics.necklace";3->"key.jump";4->"key.lynxrelics.weapon";5->"debug.lynxrelics.release";6->"key.lynxrelics.cloak";else->"debug.lynxrelics.none"}
                p.sendSystemMessage(Component.translatable("debug.lynxrelics.resources",s.breath,Skills.breathCapacity(p),s.tide,Skills.tideCapacity(p),s.pressure.toInt()))
                p.sendSystemMessage(Component.translatable("debug.lynxrelics.input",s.receivedActions,Component.translatable(actionKey),if(s.receivedActions==0L)0 else now-s.lastReceivedAt))
                p.sendSystemMessage(Component.translatable("debug.lynxrelics.state",if(s.channel.isEmpty())Component.translatable("debug.lynxrelics.idle")else s.channel,s.remaining("gauntlet",now)/20.0,s.remaining("necklace",now)/20.0))
                s.lastFeedback?.let{p.sendSystemMessage(Component.translatable("debug.lynxrelics.feedback",it))}
                p.sendSystemMessage(Component.translatable("debug.lynxrelics.test_hint"));1
            })
            .then(Commands.literal("workshop").executes { ctx ->
                val p = ctx.source.playerOrException
                val pos = p.blockPosition().relative(p.direction, 2)
                p.level().setBlock(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3)
                p.level().setBlock(pos, Content.blocks.getValue("dragon_workstation").defaultBlockState(), 3)
                val machine = p.level().getBlockEntity(pos) as net.ciyuan.lynxrelics.craft.Workstation
                machine.setItem(0, ItemStack(Items.DRAGON_BREATH, 8)); machine.setItem(9, ItemStack(Items.DRAGON_EGG))
                p.openMenu(machine); 1
            })) }
    }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import net.ciyuan.lynxrelics.core.*
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

open class RelicItem(properties: Properties, val slot: RelicSlot? = null, val route: Route? = null, val action: String = "") : Item(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        if(route!=null&&!Balance.enabled("${route.name.lowercase()}.enabled"))return InteractionResult.FAIL
        if (slot != null) {
            if (!level.isClientSide) {
                val container = player.relics().accessories
                if (!container.getItem(slot.ordinal).isEmpty) {
                    if(player is net.minecraft.server.level.ServerPlayer)SkillFeedback.notice(player,"slot_occupied")
                    return InteractionResult.PASS
                }
                container.setItem(slot.ordinal, player.getItemInHand(hand).split(1))
            }
            return InteractionResult.SUCCESS
        }
        // Both sides must pass the main hand so vanilla can reach the offhand shield.
        if(action in setOf("rift","wave","conch")&&hand==InteractionHand.MAIN_HAND&&player.offhandItem.item is net.minecraft.world.item.ShieldItem)return InteractionResult.PASS
        if (player is net.minecraft.server.level.ServerPlayer) return Skills.useItem(player, hand, action)
        return if (action.isNotEmpty()) InteractionResult.SUCCESS else InteractionResult.PASS
    }
}

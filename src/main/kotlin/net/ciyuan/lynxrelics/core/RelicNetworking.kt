// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core

import net.ciyuan.lynxrelics.content.Content
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

data class ActionPayload(val action: Int) : CustomPacketPayload {
    override fun type() = TYPE
    companion object {
        val TYPE = CustomPacketPayload.Type<ActionPayload>(Content.id("action"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, ActionPayload> = StreamCodec.of({ b, p -> b.writeVarInt(p.action) }, { ActionPayload(it.readVarInt()) })
    }
}
data class RecipeCatalogPayload(val json:String):CustomPacketPayload{
    override fun type()=TYPE
    companion object{
        val TYPE=CustomPacketPayload.Type<RecipeCatalogPayload>(Content.id("recipe_catalog"))
        val CODEC:StreamCodec<RegistryFriendlyByteBuf,RecipeCatalogPayload> = StreamCodec.of({b,p->b.writeUtf(p.json,196608)},{b->RecipeCatalogPayload(b.readUtf(196608))})
    }
}
data class StatePayload(val entity: Int, val stacks: List<ItemStack>, val breath: Int, val tide: Int, val pressure: Int, val channel: String, val quiet: Boolean) : CustomPacketPayload {
    override fun type() = TYPE
    companion object {
        val TYPE = CustomPacketPayload.Type<StatePayload>(Content.id("state"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StatePayload> = StreamCodec.of({ b, p ->
            b.writeVarInt(p.entity); for (i in 0..3) ItemStack.OPTIONAL_STREAM_CODEC.encode(b, p.stacks[i])
            b.writeVarInt(p.breath); b.writeVarInt(p.tide); b.writeVarInt(p.pressure); b.writeUtf(p.channel, 32); b.writeBoolean(p.quiet)
        }, { b -> StatePayload(b.readVarInt(), List(4) { ItemStack.OPTIONAL_STREAM_CODEC.decode(b) }, b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(32), b.readBoolean()) })
    }
}
object RelicNetworking {
    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(ActionPayload.TYPE, ActionPayload.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.TYPE, StatePayload.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(RecipeCatalogPayload.TYPE,RecipeCatalogPayload.CODEC)
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE) { payload, context -> context.server().execute { if (payload.action in 0..6) Skills.action(context.player(), payload.action) } }
        net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents.START_TRACKING.register { entity, viewer -> if (entity is ServerPlayer) sendTo(entity, viewer) }
    }
    fun sendCatalog(player:ServerPlayer){if(ServerPlayNetworking.canSend(player,RecipeCatalogPayload.TYPE))ServerPlayNetworking.send(player,RecipeCatalogPayload(net.ciyuan.lynxrelics.craft.CraftRecipes.catalogJson()))}
    private fun payload(player: ServerPlayer): StatePayload { val s=player.relics(); return StatePayload(player.id, s.accessories.items.map { it.copy() }, s.breath, s.tide, s.pressure.toInt(), s.channel, s.quietUntil > Skills.now(player)) }
    fun sendTo(player: ServerPlayer, viewer: ServerPlayer) { if (ServerPlayNetworking.canSend(viewer, StatePayload.TYPE)) ServerPlayNetworking.send(viewer, payload(player)) }
    fun sync(player: ServerPlayer, force: Boolean = false) {
        val s = player.relics()
        val signature = "${ItemStack.hashStackList(s.accessories.items)}:${s.breath}:${s.tide}:${s.pressure.toInt()}:${s.channel}:${s.quietUntil > Skills.now(player)}"
        if (!force && signature == s.synced) return
        s.synced = signature
        val payload = StatePayload(player.id, s.accessories.items.map { it.copy() }, s.breath, s.tide, s.pressure.toInt(), s.channel, s.quietUntil > Skills.now(player))
        val viewers = PlayerLookup.tracking(player).toMutableSet().also { it.add(player) }
        viewers.filter { ServerPlayNetworking.canSend(it, StatePayload.TYPE) }.forEach { ServerPlayNetworking.send(it, payload) }
    }
}

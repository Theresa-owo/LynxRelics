// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.ciyuan.lynxrelics.core.Balance
import net.ciyuan.lynxrelics.mixin.ItemPickupAccessor
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.GlobalPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.LodestoneTracker
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.saveddata.*
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.Optional

data class AnchorAddon(var owner:String,var magnetic:Boolean=false,var stable:Boolean=false,var fuel:Int=0){companion object{
    val CODEC:Codec<AnchorAddon> = RecordCodecBuilder.create{it.group(Codec.STRING.fieldOf("owner").forGetter(AnchorAddon::owner),Codec.BOOL.fieldOf("magnetic").forGetter(AnchorAddon::magnetic),Codec.BOOL.fieldOf("stable").forGetter(AnchorAddon::stable),Codec.INT.fieldOf("fuel").forGetter(AnchorAddon::fuel)).apply(it,::AnchorAddon)}
}}
class AnchorRegistry(val entries:MutableMap<String,AnchorAddon> = mutableMapOf()):SavedData(){companion object{
    val CODEC:Codec<AnchorRegistry> = Codec.unboundedMap(Codec.STRING,AnchorAddon.CODEC).xmap({AnchorRegistry(it.toMutableMap())},{it.entries})
    val TYPE=SavedDataType(Content.id("anchor_addons"),{AnchorRegistry()},CODEC,net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE)
    fun get(server:MinecraftServer):AnchorRegistry=server.overworld().dataStorage.computeIfAbsent(TYPE)
}}
object AnchorAddons {
    private fun key(level:ServerLevel,pos:BlockPos)=level.dimension().identifier().toString()+"|"+pos.asLong()
    fun stable(level:ServerLevel,pos:BlockPos)=AnchorRegistry.get(level.server).entries[key(level,pos)]?.stable==true
    fun use(player:ServerPlayer,hand:InteractionHand,pos:BlockPos):InteractionResult{
        val level=player.level();val held=player.getItemInHand(hand)
        if(held.`is`(Content.item("expedition_compass"))){bindCompass(player,held,pos);return InteractionResult.SUCCESS}
        val registry=AnchorRegistry.get(level.server);val id=key(level,pos);var entry=registry.entries[id]
        if(held.`is`(Content.item("magnetic_ring"))||held.`is`(Content.item("stabilizing_ring"))){
            if(entry==null){entry=AnchorAddon(player.uuid.toString());registry.entries[id]=entry}
            val magnetic=held.`is`(Content.item("magnetic_ring"));if(if(magnetic)entry.magnetic else entry.stable)return InteractionResult.FAIL
            if(magnetic)entry.magnetic=true else entry.stable=true
            held.consume(1,player);registry.setDirty();player.sendOverlayMessage(Component.translatable("msg.lynxrelics.anchor_installed"));return InteractionResult.SUCCESS
        }
        if(entry==null)return InteractionResult.PASS
        if(held.`is`(Items.AMETHYST_SHARD)&&entry.magnetic){if(entry.fuel>0)return InteractionResult.FAIL;held.consume(1,player);entry.fuel=Balance.ticks("lodestone.fuelTicks");registry.setDirty();return InteractionResult.SUCCESS}
        if(player.isShiftKeyDown&&held.`is`(Items.SHEARS)){
            val item=if(entry.stable)"stabilizing_ring"else"magnetic_ring"
            if(!net.ciyuan.lynxrelics.mob.InventoryInsertion.insertAll(player,ItemStack(Content.item(item))))return InteractionResult.FAIL
            if(entry.stable)entry.stable=false else{entry.magnetic=false;entry.fuel=0}
            if(!entry.magnetic&&!entry.stable)registry.entries.remove(id);registry.setDirty();return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }
    fun tick(server:MinecraftServer){
        val now=server.overworld().gameTime;if(now%20!=0L)return
        pulse(server)
    }
    internal fun pulse(server:MinecraftServer){
        val registry=AnchorRegistry.get(server)
        for((id,addon)in registry.entries.toMap()){
            val parts=id.split('|',limit=2);val level=server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(parts[0])))?:continue;val pos=BlockPos.of(parts[1].toLong())
            if(!level.hasChunkAt(pos))continue
            if(!level.getBlockState(pos).`is`(Blocks.LODESTONE)){
                if(addon.magnetic)Block.popResource(level,pos,ItemStack(Content.item("magnetic_ring")));if(addon.stable)Block.popResource(level,pos,ItemStack(Content.item("stabilizing_ring")));registry.entries.remove(id);registry.setDirty();continue
            }
            if(!addon.magnetic||addon.fuel<=0)continue
            addon.fuel=(addon.fuel-20).coerceAtLeast(0);registry.setDirty()
            var movedEntities=0;val center=Vec3.atCenterOf(pos);val radius=Balance.value("lodestone.range")
            for(entity in level.getEntitiesOfClass(ItemEntity::class.java,AABB.ofSize(center,radius*2,radius*2,radius*2))){
                if(movedEntities>=Balance.ticks("lodestone.itemsPerSecond"))break
                val owner=(entity as ItemPickupAccessor).lynxPickupOwner();if(entity.hasPickUpDelay()||owner!=null&&owner.toString()!=addon.owner||entity.position().distanceToSqr(center)>radius*radius)continue
                val hit=level.clip(net.minecraft.world.level.ClipContext(entity.position(),center,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,entity));if(hit.type!=net.minecraft.world.phys.HitResult.Type.MISS&&hit.blockPos!=pos)continue
                var transferred=false
                for(direction in Direction.values()){
                    val adjacent=pos.relative(direction);if(!level.hasChunkAt(adjacent))continue
                    val container=level.getBlockEntity(adjacent) as? Container?:continue
                    val stack=entity.item;val staged=MutableList(container.containerSize){container.getItem(it).copy()};val slots=if(container is WorldlyContainer)container.getSlotsForFace(direction.opposite).toList()else staged.indices.toList();var left=stack.count
                    for(index in slots){if(!container.canPlaceItem(index,stack)||container is WorldlyContainer&&!container.canPlaceItemThroughFace(index,stack,direction.opposite))continue
                        val dest=staged[index];if(!dest.isEmpty&&!ItemStack.isSameItemSameComponents(dest,stack))continue
                        val count=minOf(left,minOf(stack.maxStackSize,container.maxStackSize)-dest.count);if(count<=0)continue
                        if(dest.isEmpty)staged[index]=stack.copyWithCount(count)else dest.grow(count);left-=count;if(left==0)break
                    }
                    if(left<stack.count){for(i in staged.indices)container.setItem(i,staged[i]);container.setChanged();stack.shrink(stack.count-left);if(stack.isEmpty)entity.discard()else entity.item=stack;transferred=true;break}
                }
                if(transferred)movedEntities++
            }
        }
        for(player in server.playerList.players)if(player.mainHandItem.`is`(Content.item("expedition_compass")))showCompass(player,player.mainHandItem)
    }
    private fun bindCompass(player:ServerPlayer,stack:ItemStack,pos:BlockPos){
        CustomData.update(DataComponents.CUSTOM_DATA,stack){tag->val count=tag.getIntOr("LynxAnchorCount",0).coerceIn(0,3);val chosen=if(count<3)count else tag.getIntOr("LynxAnchorSelected",0).coerceIn(0,2);tag.putInt("LynxAnchorCount",minOf(3,count+1));tag.putInt("LynxAnchorSelected",chosen);tag.putString("LynxAnchor${chosen}Dim",player.level().dimension().identifier().toString());tag.putLong("LynxAnchor${chosen}Pos",pos.asLong());tag.putString("LynxAnchor${chosen}Name",stack.customName?.string?.take(32)?:"")}
        updateTracker(stack);showCompass(player,stack)
    }
    private fun updateTracker(stack:ItemStack){val tag=stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?:return;val n=tag.getIntOr("LynxAnchorSelected",0);val dim=tag.getStringOr("LynxAnchor${n}Dim","");if(dim.isEmpty())return;stack.set(DataComponents.LODESTONE_TRACKER,LodestoneTracker(Optional.of(GlobalPos.of(ResourceKey.create(Registries.DIMENSION,Identifier.parse(dim)),BlockPos.of(tag.getLongOr("LynxAnchor${n}Pos",0)))),true))}
    fun compass(player:ServerPlayer,stack:ItemStack):InteractionResult{
        if(player.isShiftKeyDown)CustomData.update(DataComponents.CUSTOM_DATA,stack){tag->val count=tag.getIntOr("LynxAnchorCount",0);if(count>0)tag.putInt("LynxAnchorSelected",(tag.getIntOr("LynxAnchorSelected",0)+1)%count)}
        updateTracker(stack);showCompass(player,stack);return InteractionResult.SUCCESS
    }
    private fun showCompass(player:ServerPlayer,stack:ItemStack){
        val tag=stack.get(DataComponents.CUSTOM_DATA)?.copyTag()
        if(tag==null||tag.getIntOr("LynxAnchorCount",0)==0){player.sendOverlayMessage(Component.translatable("msg.lynxrelics.compass_empty"));return}
        val selected=tag.getIntOr("LynxAnchorSelected",0);val dim=tag.getStringOr("LynxAnchor${selected}Dim","");if(dim.isEmpty())return
        val pos=BlockPos.of(tag.getLongOr("LynxAnchor${selected}Pos",0));val target=player.level().server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(dim)))?:return
        val invalid=target.hasChunkAt(pos)&&!target.getBlockState(pos).`is`(Blocks.LODESTONE)
        if(invalid)stack.set(DataComponents.LODESTONE_TRACKER,LodestoneTracker(Optional.empty(),false))
        val validity=if(invalid)Component.translatable("msg.lynxrelics.compass_invalid")else Component.empty()
        val distance=if(player.level()===target)Component.translatable("msg.lynxrelics.compass_distance",player.position().distanceTo(Vec3.atCenterOf(pos)).toInt())else Component.translatable("msg.lynxrelics.compass_other_dimension")
        val rawName=tag.getStringOr("LynxAnchor${selected}Name","");val name=if(rawName.isEmpty())Component.translatable("msg.lynxrelics.waypoint_default",selected+1)else Component.literal(rawName)
        player.sendOverlayMessage(Component.translatable("msg.lynxrelics.compass",name,dim,distance,validity))
    }
}

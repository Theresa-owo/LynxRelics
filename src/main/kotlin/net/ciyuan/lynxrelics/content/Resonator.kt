// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content
import net.ciyuan.lynxrelics.core.*
import net.ciyuan.lynxrelics.mob.*
import net.minecraft.core.BlockPos
import net.minecraft.core.component.*
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.JukeboxSong
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.*
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.*

class ResonatorBlock(properties:Properties):Block(properties),EntityBlock{
    override fun newBlockEntity(pos:BlockPos,state:BlockState)=Resonator(pos,state)
    override fun useWithoutItem(state:BlockState,level:Level,pos:BlockPos,player:Player,hit:BlockHitResult):InteractionResult{
        if(player is ServerPlayer)(level.getBlockEntity(pos) as? Resonator)?.let{return it.use(player,InteractionHand.MAIN_HAND)}
        return InteractionResult.SUCCESS
    }
    override fun useItemOn(stack:ItemStack,state:BlockState,level:Level,pos:BlockPos,player:Player,hand:InteractionHand,hit:BlockHitResult):InteractionResult=if(player is ServerPlayer)(level.getBlockEntity(pos) as? Resonator)?.use(player,hand)?:InteractionResult.PASS else InteractionResult.SUCCESS
    override fun <T:BlockEntity> getTicker(level:Level,state:BlockState,type:BlockEntityType<T>):BlockEntityTicker<T>?=if(level.isClientSide||type!=Content.RESONATOR_TYPE)null else BlockEntityTicker{_,_,_,be->(be as Resonator).tick()}
    override fun setPlacedBy(level:Level,pos:BlockPos,state:BlockState,entity:LivingEntity?,stack:ItemStack){super.setPlacedBy(level,pos,state,entity,stack);(level.getBlockEntity(pos)as?Resonator)?.placed(entity as? ServerPlayer,stack)}
    override fun getDrops(state:BlockState,params:LootParams.Builder):List<ItemStack>{val drops=super.getDrops(state,params);val be=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)as?Resonator?:return drops;return drops.map{if(it.`is`(Content.item("resonance_box")))be.packed()else it}}
    override fun playerWillDestroy(level:Level,pos:BlockPos,state:BlockState,player:Player):BlockState{if(player.isCreative&&level is ServerLevel){val be=level.getBlockEntity(pos)as?Resonator;if(be!=null&&!be.disc.isEmpty){Block.popResource(level,pos,be.packed());be.disc=ItemStack.EMPTY}};return super.playerWillDestroy(level,pos,state,player)}
}
class Resonator(pos:BlockPos,state:BlockState):BlockEntity(Content.RESONATOR_TYPE,pos,state){
    private var originalItem=ItemStack.EMPTY
    var disc=ItemStack.EMPTY
    var mode=0
    var fuel=0
    var specialization=""
    var ownerName=""
    private var playing=false
    private var songStarted=0L
    fun placed(player:ServerPlayer?,stack:ItemStack){
        originalItem=stack.copyWithCount(1);originalItem.remove(DataComponents.CONTAINER)
        disc=stack.get(DataComponents.CONTAINER)?.copyOne()?:ItemStack.EMPTY
        val tag=stack.get(DataComponents.CUSTOM_DATA)?.copyTag();fuel=tag?.getIntOr("LynxResonatorFuel",0)?:0;mode=tag?.getIntOr("LynxResonatorMode",0)?:0;specialization=RelicEnchantments.spec(stack);ownerName=player?.scoreboardName?:"";setChanged()
    }
    fun packed():ItemStack{
        val stack=if(originalItem.isEmpty)ItemStack(Content.item("resonance_box"))else originalItem.copy();stack.set(DataComponents.MAX_STACK_SIZE,1);stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(listOf(disc.copy())))
        CustomData.update(DataComponents.CUSTOM_DATA,stack){it.putInt("LynxResonatorFuel",fuel);it.putInt("LynxResonatorMode",mode)};RelicEnchantments.setSpec(stack,specialization);return stack
    }
    fun use(player:ServerPlayer,hand:InteractionHand):InteractionResult{
        val held=player.getItemInHand(hand)
        if(held.has(DataComponents.JUKEBOX_PLAYABLE)){if(!disc.isEmpty)return InteractionResult.FAIL;disc=held.split(1);ownerName=player.scoreboardName;setChanged();return InteractionResult.SUCCESS}
        if(held.`is`(Content.item("echo_dust"))){if(fuel>0||disc.isEmpty)return InteractionResult.FAIL;held.consume(1,player);fuel=Balance.ticks(when(specialization){"a"->"resonator.areaFuelTicks";"b"->"resonator.efficientFuelTicks";else->"resonator.fuelTicks"});setChanged();return InteractionResult.SUCCESS}
        if(held.isEmpty){if(player.isShiftKeyDown&&!disc.isEmpty){if(!InventoryInsertion.insertAll(player,disc.copy()))return InteractionResult.FAIL;disc=ItemStack.EMPTY;stopMusic()}else{mode=(mode+1)%3;player.sendOverlayMessage(Component.translatable("msg.lynxrelics.resonator",Component.translatable("msg.lynxrelics.resonator.${listOf("calm","inspire","distract")[mode]}"),fuel/20))};setChanged();return InteractionResult.SUCCESS}
        return InteractionResult.PASS
    }
    fun tick(){
        val world=level as? ServerLevel?:return;val now=world.server.overworld().gameTime
        if(disc.isEmpty||fuel<=0||!Balance.enabled("echo.enabled")){if(playing)stopMusic();return}
        val song=JukeboxSong.fromStack(disc).orElse(null)?:return
        if(!playing||now-songStarted>=song.value().lengthInTicks()){
            world.levelEvent(null,LevelEvent.SOUND_PLAY_JUKEBOX_SONG,worldPosition,world.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(song.value()));playing=true;songStarted=now
        }
        fuel--;if(fuel%20==0)setChanged();if(now%20!=0L)return
        val radius=Balance.value(if(specialization=="a")"resonator.areaRadius"else"resonator.radius");val box=AABB.ofSize(Vec3.atCenterOf(worldPosition),radius*2,radius*2,radius*2)
        when(mode){
            0->for(mob in world.getEntitiesOfClass(RelicMob::class.java,box))if(!mob.kind.hostile&&mob.calmInterruptedUntil<=now)mob.calmedUntil=now+40
            1->{val team=world.server.scoreboard.getPlayersTeam(ownerName);for(player in world.getEntitiesOfClass(ServerPlayer::class.java,box))if(player.scoreboardName==ownerName||team!=null&&player.team!=null&&team.isAlliedTo(player.team))player.addEffect(net.minecraft.world.effect.MobEffectInstance(Content.RESONANCE_SPEED,40))}
            2->if(now%100==0L)Investigations.ambient(world,Vec3.atCenterOf(worldPosition),40,radius)
        }
    }
    private fun stopMusic(){level?.levelEvent(null,LevelEvent.SOUND_STOP_JUKEBOX_SONG,worldPosition,0);playing=false}
    override fun preRemoveSideEffects(pos:BlockPos,state:BlockState){stopMusic()}
    override fun saveAdditional(out:ValueOutput){super.saveAdditional(out);out.store("Disc",ItemStack.OPTIONAL_CODEC,disc);out.store("OriginalItem",ItemStack.OPTIONAL_CODEC,originalItem);out.putInt("Mode",mode);out.putInt("Fuel",fuel);out.putString("Specialization",specialization);out.putString("OwnerName",ownerName)}
    override fun loadAdditional(input:ValueInput){super.loadAdditional(input);disc=input.read("Disc",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);originalItem=input.read("OriginalItem",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);mode=input.getIntOr("Mode",0).coerceIn(0,2);fuel=input.getIntOr("Fuel",0).coerceAtLeast(0);specialization=input.getStringOr("Specialization","");ownerName=input.getStringOr("OwnerName","");playing=false}
}

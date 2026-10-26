// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core
import net.ciyuan.lynxrelics.content.Content
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.Vec3

object Conch {
    private data class Survey(val dimension:String,val until:Long,val targets:List<java.util.UUID>)
    private val surveys=mutableMapOf<java.util.UUID,Survey>()
    fun clear(){surveys.clear()}
    fun leave(player:ServerPlayer){surveys.remove(player.uuid)}
    fun use(player: ServerPlayer,hand:net.minecraft.world.InteractionHand=net.minecraft.world.InteractionHand.MAIN_HAND): InteractionResult {
        val state=player.relics();val now=Skills.now(player)
        val held=player.getItemInHand(hand)
        if(player.isShiftKeyDown){
            if(state.channel.isNotEmpty()||!state.ready("conch_return",now)||state.tide<25||state.waterReturnDimension!=player.level().dimension().identifier().toString()||state.waterReturnPosition.distanceToSqr(player.position())>96*96||kotlin.math.abs(state.waterReturnPosition.y-player.y)>64)return InteractionResult.FAIL
            state.channel="conch_return";state.channelOrigin=player.position();state.channelStarted=now;state.channelHand=hand;SkillFeedback.notice(player,"recall_started");return InteractionResult.SUCCESS
        }
        if(!player.isUnderWater||state.tide<15||!state.ready("conch",now))return InteractionResult.FAIL
        val radius=if(RelicEnchantments.spec(held)=="a")32.0 else 24.0
        state.tide-=15;state.cooldown("conch",now,240)
        val marked=mutableListOf<java.util.UUID>()
        for(item in player.level().getEntitiesOfClass(ItemEntity::class.java,player.boundingBox.inflate(radius))){
            if(!item.isInWater||item.hasPickUpDelay()||item.distanceToSqr(player)>radius*radius)continue
            val owner=(item as net.ciyuan.lynxrelics.mixin.ItemPickupAccessor).lynxPickupOwner();if(owner!=null&&owner!=player.uuid)continue
            if(marked.size<64)marked.add(item.uuid)
            val p=item.position();player.level().sendParticles(ParticleTypes.NAUTILUS,true,true,p.x,p.y,p.z,8,.3,.3,.3,.02)
        }
        for(jelly in player.level().getEntitiesOfClass(net.ciyuan.lynxrelics.mob.RelicMob::class.java,player.boundingBox.inflate(radius)))if(jelly.kind==net.ciyuan.lynxrelics.mob.RelicMobKind.JELLY&&jelly.distanceToSqr(player)<=radius*radius){jelly.addEffect(net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,120));if(marked.size<64)marked.add(jelly.uuid)}
        surveys[player.uuid]=Survey(player.level().dimension().identifier().toString(),now+120,marked)
        SkillFeedback.notice(player,"survey",marked.size);held.hurtAndBreak(2,player,hand)
        return InteractionResult.SUCCESS
    }
    fun tick(player: ServerPlayer){
        val s=player.relics();val now=Skills.now(player)
        surveys[player.uuid]?.let{survey->
            if(survey.until<=now||survey.dimension!=player.level().dimension().identifier().toString())surveys.remove(player.uuid)
            else if(now%10==0L)for(id in survey.targets){val target=player.level().getEntity(id)?:continue;if(target.isRemoved)continue;val p=target.position();player.level().sendParticles(ParticleTypes.NAUTILUS,true,true,p.x,p.y+.2,p.z,3,.25,.25,.25,.01)}
        }
        val deep=player.isUnderWater&&player.level().getFluidState(player.blockPosition().below(2)).`is`(FluidTags.WATER)
        if(deep&&!s.wasDeepWater){
            s.waterReturnDimension="";var pos=player.blockPosition()
            for(i in 0..64){
                if(!player.level().getFluidState(pos).`is`(FluidTags.WATER)){
                    val surface=Vec3.atBottomCenterOf(pos)
                    if(player.level().getBlockState(pos).isAir&&Skills.safe(player,surface)){s.waterReturnPosition=surface;s.waterReturnDimension=player.level().dimension().identifier().toString()};break
                };pos=pos.above()
            }
        }
        s.wasDeepWater=deep
        if(s.channel!="conch_return")return
        val held=player.getItemInHand(s.channelHand)
        if(!held.`is`(Content.item("tide_conch"))||s.channelOrigin.distanceToSqr(player.position())>.0225||s.lastDamage>=s.channelStarted||!player.isAlive){s.channel="";s.extendCooldown("conch_return",now,60);SkillFeedback.notice(player,"interrupted");return}
        if(now-s.channelStarted<80)return
        val p=s.waterReturnPosition
        if(s.tide>=25&&s.waterReturnDimension==player.level().dimension().identifier().toString()&&p.distanceToSqr(player.position())<=96*96&&kotlin.math.abs(p.y-player.y)<=64&&Skills.safe(player,p)&&player.teleportTo(player.level(),p.x,p.y,p.z,emptySet(),player.yRot,player.xRot,false)){
            s.tide-=25;s.cooldown("conch_return",now,if(RelicEnchantments.spec(held)=="b")1200 else 1800);held.hurtAndBreak(2,player,s.channelHand);SkillFeedback.notice(player,"returned")
        }else {s.extendCooldown("conch_return",now,60);SkillFeedback.notice(player,"unsafe_destination")}
        s.channel=""
    }
}

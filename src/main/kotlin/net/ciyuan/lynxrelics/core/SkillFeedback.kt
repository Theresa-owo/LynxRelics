// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.content.RelicItem
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand

/** Explains actual server-side release conditions; client input never supplies resources or cooldowns. */
object SkillFeedback {
    fun creativeResources(player:ServerPlayer){
        if(player.isCreative&&Balance.enabled("skills.creativeFreeResources")){
            val s=player.relics();s.breath=Skills.breathCapacity(player);s.tide=Skills.tideCapacity(player);s.pressure=0.0;s.overloadUntil=0
        }
    }
    fun notice(player:ServerPlayer,key:String,vararg args:Any):Boolean{
        val s=player.relics();val now=Skills.now(player)
        s.lastFeedbackKey="skill.lynxrelics.$key"
        val message=Component.translatable(s.lastFeedbackKey,*args).withStyle(ChatFormatting.AQUA);s.lastFeedback=message
        if(s.feedbackAt==Long.MIN_VALUE||now-s.feedbackAt>=8){player.sendOverlayMessage(message);s.feedbackAt=now}
        return false
    }
    private fun cooldown(player:ServerPlayer,key:String):Boolean{
        val left=player.relics().remaining(key,Skills.now(player))
        return left==0L||notice(player,"cooldown",kotlin.math.ceil(left/2.0)/10)
    }
    private fun resource(player:ServerPlayer,dragon:Boolean,cost:Int):Boolean{
        val current=if(dragon)player.relics().breath else player.relics().tide
        return current>=cost||notice(player,if(dragon)"need_breath"else"need_tide",cost,current)
    }
    private fun idle(player:ServerPlayer)=player.relics().channel.isEmpty()||notice(player,"channel_busy")
    fun beforeAction(player:ServerPlayer,action:Int):Boolean{
        if(action in setOf(0,4,5))return true
        val s=player.relics();val now=Skills.now(player)
        if(action==1){
            val stack=Skills.equipped(player,RelicSlot.GAUNTLET)
            if(stack.isEmpty||stack.isBroken)return notice(player,"equip_gauntlet")
            if(Skills.has(player,"keeper_gauntlet"))return notice(player,"passive_guard")
            if(!idle(player)||!cooldown(player,"gauntlet"))return false
            return when{
                Skills.has(player,"dragonthroat_gauntlet")->(s.overloadUntil<=now||notice(player,"overload"))&&resource(player,true,Skills.dragonCost(player,Balance.ticks("breath.gauntletCost"),now))
                Skills.has(player,"tidepull_gauntlet")->resource(player,false,Balance.ticks("tide.pullCost"))
                else->notice(player,"route_disabled")
            }
        }
        if(action==2){
            val stack=Skills.equipped(player,RelicSlot.NECKLACE)
            if(stack.isEmpty||stack.isBroken)return notice(player,"equip_necklace")
            if(!idle(player)||!cooldown(player,"necklace"))return false
            if(Skills.has(player,"homing_dragon_necklace")){
                if(s.anchorDimension.isEmpty())return notice(player,"bind_lodestone")
                if(s.anchorDimension!=player.level().dimension().identifier().toString())return notice(player,"same_dimension")
                return (s.overloadUntil<=now||notice(player,"overload"))&&resource(player,true,Skills.dragonCost(player,12,now))
            }
            if(Skills.has(player,"watermirror_necklace"))return resource(player,false,if(RelicEnchantments.spec(stack)=="b")35 else 40)
            return Skills.has(player,"resonant_necklace")||notice(player,"route_disabled")
        }
        if(action==3||action==6){
            if(!Skills.has(player,"dragonwing_cloak"))return if(action==3)false else notice(player,"passive_cloak")
            if(player.onGround())return notice(player,"airborne_only")
            if(player.isPassenger)return notice(player,"not_riding")
            if(!cooldown(player,"flap"))return false
            val gliding=player.isFallFlying&&!net.ciyuan.lynxrelics.compat.ElytraCompat.actualWings(player).isEmpty
            if(!gliding&&s.airborneUsed)return notice(player,"land_to_reset")
            return (s.overloadUntil<=now||notice(player,"overload"))&&resource(player,true,Skills.dragonCost(player,Balance.ticks(if(gliding)"breath.glideCost"else"breath.flapCost"),now))
        }
        return true
    }
    fun beforeItem(player:ServerPlayer,hand:InteractionHand,action:String):Boolean{
        val s=player.relics();val now=Skills.now(player)
        if(action in setOf("rift","wave","conch")&&player.getItemInHand(hand).isBroken)return notice(player,"broken_item")
        return when(action){
            "rift"->idle(player)&&cooldown(player,"rift")&&(s.overloadUntil<=now||notice(player,"overload"))&&resource(player,true,Skills.dragonCost(player,10,now))
            "wave"->{val dash=player.isShiftKeyDown;idle(player)&&cooldown(player,"tide_lock")&&cooldown(player,if(dash)"tide_dash"else"wave")&&(!dash||player.onGround()||player.isInWater||notice(player,"dash_ground_water"))&&resource(player,false,if(dash)40 else 25)}
            "conch"->{
                if(player.isShiftKeyDown){
                    if(s.waterReturnDimension.isEmpty())return notice(player,"no_surface")
                    if(s.waterReturnDimension!=player.level().dimension().identifier().toString())return notice(player,"same_dimension")
                    if(s.waterReturnPosition.distanceToSqr(player.position())>96*96||kotlin.math.abs(s.waterReturnPosition.y-player.y)>64)return notice(player,"surface_too_far")
                    idle(player)&&cooldown(player,"conch_return")&&resource(player,false,25)
                }else (player.isUnderWater||notice(player,"underwater_only"))&&cooldown(player,"conch")&&resource(player,false,15)
            }
            "firework"->cooldown(player,"firework")
            "pearl"->cooldown(player,"pearl")&&(s.pearlId.isEmpty()||s.pearlExpires<=now||notice(player,"pearl_in_flight"))
            "refill"->s.breath<Skills.breathCapacity(player)||notice(player,"breath_full")
            "embryo"->net.ciyuan.lynxrelics.mob.Companions.get(player.level().server).available(player.uuid)||notice(player,"one_companion")
            else->true
        }
    }
    fun noHeldSkill(player:ServerPlayer){
        val id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.mainHandItem.item)
        val key=when{ id.namespace=="lynxrelics"&&id.path.startsWith("endforged_")->"endforged_passive";player.mainHandItem.item is net.minecraft.world.item.BowItem->"bow_use";player.mainHandItem.has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS)->"shield_use";else->"no_held_skill" }
        notice(player,key)
    }
}

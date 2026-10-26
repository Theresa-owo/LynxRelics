// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core
import net.ciyuan.lynxrelics.content.Content
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemInstance
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object Mining {
    private class Break(val level:ServerLevel,val player:ServerPlayer,val pos:BlockPos,val state:BlockState,val tool:ItemStack,val previous:Break?){var converted=false;var dropped=false}
    private val current=ThreadLocal<Break?>()
    @JvmStatic fun begin(level:ServerLevel,player:ServerPlayer,pos:BlockPos,state:BlockState,tool:ItemStack):Any{val ctx=Break(level,player,pos.immutable(),state,tool,current.get());current.set(ctx);return ctx}
    @JvmStatic fun drops(original:List<ItemStack>,state:BlockState,level:ServerLevel,pos:BlockPos,actor:Entity?,tool:ItemInstance):List<ItemStack>{
        val ctx=current.get()?:return original
        if(ctx.level!==level||ctx.player!==actor||ctx.pos!=pos)return original
        ctx.dropped=original.any{!it.isEmpty}
        val n=RelicEnchantments.level(ctx.tool,"smelting")
        if(n==0||ctx.player.isCreative||!ctx.dropped||n==1&&level.random.nextBoolean())return original
        if(state.block !in setOf(Blocks.IRON_ORE,Blocks.DEEPSLATE_IRON_ORE,Blocks.GOLD_ORE,Blocks.DEEPSLATE_GOLD_ORE,Blocks.COPPER_ORE,Blocks.DEEPSLATE_COPPER_ORE))return original
        return original.map{stack->val result=when(stack.item){Items.RAW_IRON->Items.IRON_INGOT;Items.RAW_GOLD->Items.GOLD_INGOT;Items.RAW_COPPER->Items.COPPER_INGOT;else->null};if(result==null)stack else{ctx.converted=true;stack.transmuteCopy(result)}}
    }
    @JvmStatic fun end(token:Any){
        val ctx=token as Break
        try{
            if(ctx.converted)ctx.player.mainHandItem.hurtAndBreak(1,ctx.player,net.minecraft.world.InteractionHand.MAIN_HAND)
            if(ctx.state.`is`(Content.blocks.getValue("endmarrow_ore"))&&ctx.dropped){
                OreIndex.remove(ctx.level,ctx.pos)
                val n=RelicEnchantments.level(ctx.tool,"prospecting");val s=ctx.player.relics();val now=Skills.now(ctx.player)
                if(n>0&&s.ready("prospecting",now)){
                    val nearest=OreIndex.nearest(ctx.level,ctx.pos,if(n==1)8 else 12)
                    if(nearest!=null){
                        val dx=nearest.x-ctx.pos.x;val dz=nearest.z-ctx.pos.z;val dy=nearest.y-ctx.pos.y
                        val direction=net.minecraft.network.chat.Component.empty()
                        if(dz!=0)direction.append(net.minecraft.network.chat.Component.translatable("direction.lynxrelics.${if(dz<0)"north"else"south"}"))
                        if(dx!=0)direction.append(net.minecraft.network.chat.Component.translatable("direction.lynxrelics.${if(dx<0)"west"else"east"}"))
                        if(kotlin.math.abs(dy)>2)direction.append(net.minecraft.network.chat.Component.translatable("direction.lynxrelics.${if(dy>0)"up"else"down"}"))
                        s.prospectHint=net.minecraft.network.chat.Component.translatable("msg.lynxrelics.prospect",direction,kotlin.math.sqrt(nearest.distSqr(ctx.pos)).toInt());s.prospectUntil=now+if(n==1)60 else 100
                    }
                    s.cooldown("prospecting",now,160)
                }
            }
        }finally{current.set(ctx.previous)}
    }
    fun tick(player:ServerPlayer){val s=player.relics();val now=Skills.now(player);val hint=s.prospectHint;if(s.prospectUntil>now&&now%20==0L&&hint!=null)player.sendOverlayMessage(hint)}
}

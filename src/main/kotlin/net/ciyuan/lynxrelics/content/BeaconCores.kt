// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content
import net.ciyuan.lynxrelics.core.*
import net.ciyuan.lynxrelics.mixin.BeaconLevelsAccessor
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BeaconBlockEntity
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB

interface BeaconCoreHolder { fun lynxBeaconCore(): BeaconCoreState }
class BeaconCoreState {
    var core=ItemStack.EMPTY
    var fuelTicks=0
    fun save(out:ValueOutput){out.store("Core",ItemStack.OPTIONAL_CODEC,core);out.putInt("FuelTicks",fuelTicks)}
    fun load(input:ValueInput){core=input.read("Core",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);fuelTicks=input.getIntOr("FuelTicks",0).coerceAtLeast(0)}
}
object BeaconCores {
    private fun state(beacon:BeaconBlockEntity)=(beacon as BeaconCoreHolder).lynxBeaconCore()
    fun use(player:ServerPlayer,hand:InteractionHand,beacon:BeaconBlockEntity):InteractionResult{
        val held=player.getItemInHand(hand);val s=state(beacon)
        if(listOf("abundance_core","craftsmanship_core","shelter_core").any{held.`is`(Content.item(it))}){
            if(!s.core.isEmpty)return InteractionResult.FAIL
            s.core=held.split(1);beacon.setChanged();player.sendOverlayMessage(Component.translatable("msg.lynxrelics.beacon_installed"));return InteractionResult.SUCCESS
        }
        if(s.core.isEmpty)return InteractionResult.PASS
        if(player.isShiftKeyDown&&held.isEmpty){if(!net.ciyuan.lynxrelics.mob.InventoryInsertion.insertAll(player,s.core.copy()))return InteractionResult.FAIL;s.core=ItemStack.EMPTY;s.fuelTicks=0;beacon.setChanged();return InteractionResult.SUCCESS}
        val fuel=when{ s.core.`is`(Content.item("abundance_core"))->"condensed_tide";s.core.`is`(Content.item("craftsmanship_core"))->"echo_dust";else->"dragon_essence" }
        if(held.`is`(Content.item(fuel))){
            if(s.fuelTicks>0||!active(beacon)){player.sendOverlayMessage(Component.translatable("msg.lynxrelics.beacon_needs_base"));return InteractionResult.FAIL}
            held.consume(1,player);s.fuelTicks=Balance.ticks("beacon.fuelTicks");beacon.setChanged();return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }
    fun active(beacon:BeaconBlockEntity)=((beacon as BeaconLevelsAccessor).lynxLevels()==4)&&beacon.beamSections.isNotEmpty()
    @JvmStatic fun tick(beacon:BeaconBlockEntity){
        val level=beacon.level as? ServerLevel?:return;val s=state(beacon)
        if(s.core.isEmpty||s.fuelTicks<=0||!active(beacon))return
        s.fuelTicks--;if(s.fuelTicks%20==0)beacon.setChanged()
        val now=level.server.overworld().gameTime;if(now%20!=0L)return
        val box=AABB(beacon.blockPos).inflate(50.0).expandTowards(0.0,level.height.toDouble(),0.0)
        for(player in level.getEntitiesOfClass(ServerPlayer::class.java,box)){
            val p=player.relics()
            when{
                s.core.`is`(Content.item("craftsmanship_core"))->p.craftsmanshipUntil=maxOf(p.craftsmanshipUntil,now+40)
                s.core.`is`(Content.item("shelter_core"))->p.shelterAuraUntil=maxOf(p.shelterAuraUntil,now+40)
                now%Balance.ticks("beacon.foodTicks").coerceAtLeast(20)==0L&&p.lastAbundanceTick!=now->{p.lastAbundanceTick=now;val food=player.foodData;food.foodLevel=minOf(20,food.foodLevel+Balance.ticks("beacon.foodAmount"));food.setSaturation(minOf(food.foodLevel.toFloat(),food.saturationLevel+Balance.value("beacon.saturationAmount").toFloat()))}
            }
        }
    }
    @JvmStatic fun removed(beacon:BeaconBlockEntity){val s=state(beacon);val level=beacon.level as? ServerLevel?:return;if(!s.core.isEmpty){Block.popResource(level,beacon.blockPos,s.core.copy());s.core=ItemStack.EMPTY;s.fuelTicks=0}}
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.craft
import net.ciyuan.lynxrelics.mob.*
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object Rituals {
    fun spawn(machine:Workstation,kindId:String):Boolean {
        val level=machine.level as? ServerLevel ?: return false
        val kind=RelicMobKind.entries.firstOrNull{it.id==kindId}?:return false
        if(kind !in setOf(RelicMobKind.KEEPER,RelicMobKind.BROOD))return false
        val now=level.server.overworld().gameTime
        if(now<machine.ritualUntil)return false
        if(kind==RelicMobKind.BROOD&&level.dimension()!=Level.END)return false
        val center=Vec3.atCenterOf(machine.blockPos)
        val players=level.players().filter{it.isAlive&&!it.isCreative&&!it.isSpectator&&it.distanceToSqr(center)<=32*32}
        if(players.isEmpty()||level.getEntitiesOfClass(RelicMob::class.java,AABB.ofSize(center,192.0,96.0,192.0)).any{it.kind==kind&&it.isAlive})return false
        val pos=machine.blockPos.relative(net.minecraft.core.Direction.NORTH,3)
        var floor=0
        val size=if(kind==RelicMobKind.BROOD)6 else 4
        for(x in -size until size)for(z in -size until size){val p=pos.offset(x,0,z);if(!level.hasChunkAt(p))return false;if(!level.getBlockState(p.below()).isAir)floor++;if(kotlin.math.abs(x)<=2&&kotlin.math.abs(z)<=2)for(y in 0..3)if(!level.getBlockState(p.above(y)).isAir)return false}
        if(floor<size*size*3)return false
        val mob=RelicMobs.types.getValue(kind).create(level,EntitySpawnReason.MOB_SUMMONED)?:return false
        mob.setPos(Vec3.atBottomCenterOf(pos));if(kind==RelicMobKind.BROOD)mob.lockEncounter(players.size)
        if(!level.noCollision(mob)||!level.addFreshEntity(mob))return false
        machine.ritualUntil=now+net.ciyuan.lynxrelics.core.Balance.ticks("mob.ritualCooldownTicks");return true
    }
}

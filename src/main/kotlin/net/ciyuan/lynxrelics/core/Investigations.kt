// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Mob
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID

object Investigations {
    data class Mark(val level: ServerLevel, val pos: Vec3, val until: Long, val radius: Double)
    private val marks = mutableMapOf<UUID, Mark>()
    private val mobGate = mutableMapOf<UUID, Long>()
    fun create(player: ServerPlayer, pos: Vec3, ticks: Int, radius: Double) { marks[player.uuid] = Mark(player.level(), pos, Skills.now(player) + ticks, radius);player.level().sendParticles(ParticleTypes.NOTE,true,true,pos.x,pos.y+.3,pos.z,8,.5,.2,.5,.01) }
    fun ambient(level:ServerLevel,pos:Vec3,ticks:Int,radius:Double){val key=UUID.nameUUIDFromBytes((level.dimension().identifier().toString()+pos.toString()).toByteArray());marks[key]=Mark(level,pos,level.server.overworld().gameTime+ticks,radius)}
    fun closest(level: ServerLevel, pos: Vec3, radius: Double) = marks.values.filter { it.level === level && it.pos.distanceToSqr(pos) <= radius * radius }.minByOrNull { it.pos.distanceToSqr(pos) }?.pos
    fun tick(server: MinecraftServer) {
        val now = server.overworld().gameTime
        marks.entries.removeIf { it.value.until <= now }
        mobGate.entries.removeIf { it.value <= now }
        if (now % 20 != 0L) return
        for (mark in marks.values) {
            mark.level.sendParticles(ParticleTypes.NOTE, true, true, mark.pos.x, mark.pos.y + .3, mark.pos.z, 3, .5, .2, .5, .01)
            for (mob in mark.level.getEntitiesOfClass(Mob::class.java, AABB.ofSize(mark.pos, mark.radius * 2, 8.0, mark.radius * 2))) {
                if (BuiltInRegistries.ENTITY_TYPE.getKey(mob.type).path !in setOf("zombie", "skeleton", "creeper", "rift_hunter") || mob.uuid in mobGate) continue
                if (mob.target?.let { it.distanceToSqr(mob) <= 36 } == true) continue
                mob.navigation.moveTo(mark.pos.x, mark.pos.y, mark.pos.z, 1.0)
                mob.lookControl.setLookAt(mark.pos.x, mark.pos.y + .5, mark.pos.z)
                mobGate[mob.uuid] = now + 40
            }
        }
    }
    fun clear() { marks.clear(); mobGate.clear() }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core
import net.ciyuan.lynxrelics.content.Content
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.chunk.LevelChunk
import java.util.concurrent.ConcurrentHashMap

object OreIndex {
    private val worlds=ConcurrentHashMap<ServerLevel,ConcurrentHashMap<Long,MutableSet<BlockPos>>>()
    private fun chunkKey(pos:BlockPos)=net.minecraft.world.level.ChunkPos.pack(pos.x shr 4,pos.z shr 4)
    fun record(level:ServerLevel,pos:BlockPos){worlds.computeIfAbsent(level){ConcurrentHashMap()}.computeIfAbsent(chunkKey(pos)){ConcurrentHashMap.newKeySet()}.add(pos.immutable())}
    fun loaded(level:ServerLevel,chunk:LevelChunk){
        val world=worlds.computeIfAbsent(level){ConcurrentHashMap()};val key=chunk.pos.pack();if(world.containsKey(key))return
        val found=ConcurrentHashMap.newKeySet<BlockPos>();world[key]=found
        val ore=Content.blocks.getValue("endmarrow_ore")
        val low=Balance.ticks("ore.minY");val high=Balance.ticks("ore.maxY")
        for(sectionY in (low shr 4)..(high shr 4)){val index=chunk.getSectionIndex(sectionY*16);if(index !in chunk.sections.indices)continue;val section=chunk.getSection(index);if(!section.maybeHas{it.`is`(ore)})continue
            for(y in maxOf(low,sectionY*16)..minOf(high,sectionY*16+15))for(x in 0..15)for(z in 0..15){val pos=BlockPos(chunk.pos.minBlockX+x,y,chunk.pos.minBlockZ+z);if(chunk.getBlockState(pos).`is`(ore))found.add(pos)}
        }
    }
    fun unloaded(level:ServerLevel,chunk:LevelChunk){worlds[level]?.remove(chunk.pos.pack())}
    fun remove(level:ServerLevel,pos:BlockPos){worlds[level]?.get(chunkKey(pos))?.remove(pos)}
    fun nearest(level:ServerLevel,origin:BlockPos,radius:Int):BlockPos?{
        val world=worlds[level]?:return null;val choices=mutableListOf<BlockPos>()
        for(x in ((origin.x-radius) shr 4)..((origin.x+radius) shr 4))for(z in ((origin.z-radius) shr 4)..((origin.z+radius) shr 4)){
            if(!level.chunkSource.hasChunk(x,z))continue
            world[net.minecraft.world.level.ChunkPos.pack(x,z)]?.filterTo(choices){it.distSqr(origin)<=radius*radius&&level.getBlockState(it).`is`(Content.blocks.getValue("endmarrow_ore"))}
        }
        return choices.minByOrNull{it.distSqr(origin)}
    }
    fun clear(){worlds.clear()}
}

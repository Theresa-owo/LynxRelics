// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mob
import com.mojang.serialization.Codec
import net.ciyuan.lynxrelics.content.Content
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.UUID

class Companions(val active: MutableMap<String,String> = mutableMapOf()) : SavedData() {
    fun available(owner: UUID) = owner.toString() !in active
    fun register(owner: UUID, mob: UUID): Boolean {
        val existing=active[owner.toString()]
        if(existing==mob.toString())return true
        if(existing!=null&&existing!=mob.toString())return false
        active[owner.toString()]=mob.toString();setDirty();return true
    }
    fun died(owner: UUID, mob: UUID) { if(active[owner.toString()]==mob.toString()){active.remove(owner.toString());setDirty()} }
    companion object {
        val CODEC: Codec<Companions> = Codec.unboundedMap(Codec.STRING,Codec.STRING).xmap({Companions(it.toMutableMap())},{it.active})
        val TYPE=SavedDataType(Content.id("companions"),{Companions()},CODEC,net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE)
        fun get(server: MinecraftServer): Companions = server.overworld().dataStorage.computeIfAbsent(TYPE)
    }
}

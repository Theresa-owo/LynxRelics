// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import com.mojang.serialization.MapCodec
import net.ciyuan.lynxrelics.core.Balance
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.util.RandomSource
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.ChunkGenerator
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.feature.Feature

/** 26.3 Feature is a codec-backed interface; legacy configured-feature classes no longer apply. */
class EndmarrowFeature : Feature {
    override fun codec() = CODEC
    override fun place(level: WorldGenLevel, generator: ChunkGenerator, random: RandomSource, origin: BlockPos): Boolean {
        if (!Balance.enabled("ore.enabled")) return false
        val cx = origin.x and -16; val cz = origin.z and -16
        val outer=Balance.value("ore.outerRadius");val minY=Balance.ticks("ore.minY");val maxY=Balance.ticks("ore.maxY")
        if (cx.toLong() * cx + cz.toLong() * cz < outer*outer) return false
        var stone = 0
        search@ for (x in 0..15) for (z in 0..15) for (y in minY..maxY) {
            if (level.getBlockState(BlockPos(cx + x, y, cz + z)).`is`(Blocks.END_STONE) && ++stone >= 128) break@search
        }
        if (stone < 128 || random.nextDouble() >= Balance.value("ore.chance")) return false
        val favoredMin=48.coerceIn(minY,maxY);val favoredMax=64.coerceIn(minY,maxY)
        val y = if (random.nextFloat() < .65f) favoredMin + random.nextInt(favoredMax-favoredMin+1) else minY + random.nextInt(maxY-minY+1)
        val center = BlockPos(cx + 1 + random.nextInt(14), y, cz + 1 + random.nextInt(14))
        var placed = 0
        val size = Balance.ticks("ore.veinMin") + random.nextInt(Balance.ticks("ore.veinMax")-Balance.ticks("ore.veinMin")+1)
        for (i in 0 until size) {
            val pos = if (i == 0) center else center.relative(Direction.values()[random.nextInt(6)])
            if (pos.y !in minY..maxY || pos.x.toLong() * pos.x + pos.z.toLong() * pos.z < outer*outer || !level.getBlockState(pos).`is`(Blocks.END_STONE)) continue
            if (Direction.values().any { level.getBlockState(pos.relative(it)).isAir }) continue
            if (level.setBlock(pos, Content.blocks.getValue("endmarrow_ore").defaultBlockState(), 2))placed++
        }
        return placed > 0
    }
    companion object {
        @JvmField val CODEC: MapCodec<EndmarrowFeature> = MapCodec.unit(::EndmarrowFeature)
        fun register() {
            Registry.register(BuiltInRegistries.FEATURE_TYPE, Content.id("endmarrow_ore"), CODEC)
            BiomeModifications.addFeature(BiomeSelectors.foundInTheEnd(), GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registries.PLACED_FEATURE, Content.id("endmarrow_ore")))
        }
    }
}

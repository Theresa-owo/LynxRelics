// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mob
import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.Balance
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.biome.Biomes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.phys.Vec3

object RelicMobs {
    val types=linkedMapOf<RelicMobKind,EntityType<RelicMob>>()
    fun register(){
        for(kind in RelicMobKind.entries){
            val factory=EntityType.EntityFactory<RelicMob>{type,level->if(kind.hostile)HostileRelicMob(type,level)else RelicMob(type,level)}
            val builder=EntityType.Builder.of(factory,kind.category).sized(kind.width,kind.height).clientTrackingRange(if(kind==RelicMobKind.BROOD)12 else 8).updateInterval(3)
            builder.noLootTable()
            val type=builder.build(ResourceKey.create(Registries.ENTITY_TYPE,Content.id(kind.id)))
            Registry.register(BuiltInRegistries.ENTITY_TYPE,Content.id(kind.id),type);types[kind]=type
            FabricDefaultAttributeRegistry.register(type,RelicMob.attributes(kind))
            if(kind !in setOf(RelicMobKind.WISP,RelicMobKind.KEEPER,RelicMobKind.BROOD))net.minecraft.world.entity.SpawnPlacements.register(type,if(kind==RelicMobKind.JELLY||kind==RelicMobKind.REEF)net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER else net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES){_,world,_,pos,_->
                val water=world.getFluidState(pos).`is`(net.minecraft.tags.FluidTags.WATER)
                val groupOk=when(kind){RelicMobKind.MOTH->world.level.getEntitiesOfClass(RelicMob::class.java,net.minecraft.world.phys.AABB.ofSize(Vec3.atCenterOf(pos),64.0,32.0,64.0)).count{it.kind==kind}<Balance.ticks("mob.mothLocalCap");RelicMobKind.REEF->world.level.getEntitiesOfClass(RelicMob::class.java,net.minecraft.world.phys.AABB.ofSize(Vec3.atCenterOf(pos),96.0,48.0,96.0)).none{it.kind==kind};RelicMobKind.HUNTER->pos.x.toLong()*pos.x+pos.z.toLong()*pos.z>=Balance.value("mob.hunterOuterRadius")*Balance.value("mob.hunterOuterRadius");else->true}
                Balance.enabled("${kind.routeKey}.enabled")&&groupOk&&if(kind==RelicMobKind.JELLY||kind==RelicMobKind.REEF)water else !water&&!world.getBlockState(pos.below()).isAir
            }
        }
        if(Balance.ticks("mob.echo_moth.spawnWeight")>0)BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DEEP_DARK),RelicMobKind.MOTH.category,types.getValue(RelicMobKind.MOTH),Balance.ticks("mob.echo_moth.spawnWeight"),1,1)
        val ocean=BiomeSelectors.includeByKey(Biomes.DEEP_OCEAN,Biomes.DEEP_COLD_OCEAN,Biomes.DEEP_FROZEN_OCEAN,Biomes.DEEP_LUKEWARM_OCEAN)
        if(Balance.ticks("mob.tide_jelly.spawnWeight")>0)BiomeModifications.addSpawn(ocean,RelicMobKind.JELLY.category,types.getValue(RelicMobKind.JELLY),Balance.ticks("mob.tide_jelly.spawnWeight"),2,4)
        if(Balance.ticks("mob.reef_lurker.spawnWeight")>0)BiomeModifications.addSpawn(ocean.or(BiomeSelectors.includeByKey(Biomes.WARM_OCEAN)),RelicMobKind.REEF.category,types.getValue(RelicMobKind.REEF),Balance.ticks("mob.reef_lurker.spawnWeight"),1,1)
        if(Balance.ticks("mob.rift_hunter.spawnWeight")>0)BiomeModifications.addSpawn(BiomeSelectors.foundInTheEnd(),RelicMobKind.HUNTER.category,types.getValue(RelicMobKind.HUNTER),Balance.ticks("mob.rift_hunter.spawnWeight"),1,2)
    }
    fun spawnWisp(player:ServerPlayer,hand:InteractionHand):InteractionResult{
        val registry=Companions.get(player.level().server)
        if(!registry.available(player.uuid))return InteractionResult.FAIL
        val mob=types.getValue(RelicMobKind.WISP).create(player.level(),EntitySpawnReason.MOB_SUMMONED)?:return InteractionResult.FAIL
        val point=player.position().add(player.lookAngle.multiply(1.0,0.0,1.0).normalize().scale(1.5)).add(0.0,1.0,0.0)
        mob.setPos(point);mob.ownerId=player.uuid
        if(!player.level().worldBorder.isWithinBounds(mob.blockPosition())||!player.level().noCollision(mob)||!player.level().addFreshEntity(mob))return InteractionResult.FAIL
        registry.register(player.uuid,mob.uuid);player.getItemInHand(hand).consume(1,player);return InteractionResult.SUCCESS
    }
}

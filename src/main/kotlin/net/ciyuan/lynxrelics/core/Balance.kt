// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import kotlin.math.ceil

/** All gameplay tuning is resolved on the logical server, never accepted from a packet. */
object Balance {
    private val defaults = linkedMapOf(
        "dragon.enabled" to 1.0, "echo.enabled" to 1.0, "tide.enabled" to 1.0,
        "skills.pvp" to 0.0, "skills.hurtPassive" to 0.0,"skills.creativeFreeResources" to 1.0,
        "breath.capacity" to 40.0, "breath.beltCapacity" to 50.0,
        "breath.refill" to 20.0, "breath.gauntletCost" to 8.0,
        "breath.gauntletDamage" to 4.0, "breath.gauntletRange" to 7.0,
        "breath.gauntletChargeTicks" to 15.0, "breath.gauntletPulseTicks" to 8.0,
        "breath.gauntletPulses" to 6.0, "breath.gauntletCooldown" to 200.0,
        "breath.gauntletPressure" to 18.0, "breath.pressureDecay" to 8.0,
        "breath.overloadTicks" to 120.0, "breath.flapCost" to 8.0,
        "breath.flapCooldown" to 160.0, "breath.flapUp" to 0.42,
        "breath.flapHorizontal" to 0.8, "breath.glideCost" to 4.0,
        "tide.capacity" to 100.0, "tide.beltCapacity" to 120.0,
        "tide.pullCost" to 25.0, "tide.pullDamage" to 6.0,
        "tide.pullRange" to 5.0, "tide.pullCooldown" to 200.0,
        "tide.shieldCost" to 40.0, "tide.shieldAbsorb" to 4.0,
        "tide.shieldCooldown" to 400.0, "tide.mirrorCooldown" to 240.0,
        "craft.timeMultiplier" to 1.0, "craft.enabled" to 1.0,
        "infinite.fireworkCooldown" to 15.0, "infinite.pearlCooldown" to 100.0,
        "infinite.pearlLifetime" to 300.0, "forging.durabilityMultiplier" to 1.25,
        "rail.slow" to 4.0, "rail.normal" to 8.0, "rail.fast" to 16.0,
        "rail.express" to 24.0, "rail.acceleration" to 0.06,
        "trident.dryRiptide" to 1.0, "trident.dryChanneling" to 1.0,
        "raid.enabled" to 1.0, "raid.healBudget" to 450.0,
        "raid.maxRecalls" to 8.0, "raid.crystalsPerRecall" to 3.0,
        "raid.crystalLifetime" to 600.0, "raid.healPerSecond" to 2.0,
        "raid.skillDamageMultiplier" to 1.0, "raid.vanillaGriefing" to 1.0,
        "ore.enabled" to 1.0, "ore.chance" to 0.45,
        "lodestone.range" to 6.0, "lodestone.itemsPerSecond" to 4.0,
        "beacon.fuelTicks" to 24000.0, "render.hideCloakGliding" to 1.0,
        "shield.deflectorCooldown" to 160.0,"shield.pushCooldown" to 200.0,"shield.pushDamage" to 3.0,
        "raid.recallPhase1Ticks" to 900.0,"raid.recallPhase2Ticks" to 700.0,"raid.recallPhase3Ticks" to 1200.0,
        "raid.recallWarningTicks" to 80.0,"raid.transitionTicks" to 40.0,"raid.emptyCleanupTicks" to 1200.0,
        "raid.swoopWarningTicks" to 24.0,"raid.swoopActiveTicks" to 80.0,"raid.swoopDamage" to 16.0,
        "raid.wingWarningTicks" to 20.0,"raid.wingDamage" to 12.0,"raid.wingRange" to 8.0,
        "raid.breathWarningTicks" to 30.0,"raid.breathActiveTicks" to 60.0,"raid.breathPulseTicks" to 10.0,
        "raid.breathDamage" to 6.0,"raid.breathRange" to 40.0,"raid.breathRadius" to 1.2,"raid.breathTurnDegrees" to 2.0,
        "raid.markWarningTicks" to 30.0,"raid.markDamage" to 12.0,"raid.markRadius" to 2.5,
        "raid.recoveryTicks" to 40.0,"raid.phase12SkillGapTicks" to 100.0,"raid.phase3SkillGapTicks" to 40.0,
        "raid.emberCooldownTicks" to 1800.0,"raid.emberLifetimeTicks" to 1800.0,"raid.emberHealth" to 24.0,"raid.emberDamage" to 4.0,
        "raid.essenceDropMin" to 24.0,"raid.essenceDropMax" to 32.0,"raid.scrapDropMin" to 8.0,"raid.scrapDropMax" to 12.0,
        "raid.prismDropMin" to 2.0,"raid.prismDropMax" to 4.0,
        "mob.feedTicks" to 36000.0,"mob.feedHeal" to 4.0,"mob.stockLimit" to 2.0,
        "mob.wispProductionTicks" to 18000.0,"mob.mothProductionTicks" to 9600.0,"mob.jellyProductionTicks" to 12000.0,
        "mob.wispPickupPerMinute" to 8.0,"mob.mothLocalCap" to 3.0,"mob.hunterOuterRadius" to 1000.0,
        "mob.bossEmptyTicks" to 1200.0,"mob.broodPartyCap" to 8.0,"mob.broodExtraHealthPerPlayer" to .55,
        "mob.hunterScaleChance" to .35,"mob.ritualCooldownTicks" to 6000.0,
        "lodestone.fuelTicks" to 19200.0,"beacon.craftsmanshipChance" to .15,"beacon.foodTicks" to 200.0,
        "beacon.foodAmount" to 1.0,"beacon.saturationAmount" to 2.0,"beacon.shelterReduction" to .2,
        "resonator.radius" to 8.0,"resonator.fuelTicks" to 1200.0,"resonator.areaRadius" to 10.0,
        "resonator.areaFuelTicks" to 900.0,"resonator.efficientFuelTicks" to 1800.0,
        "ore.outerRadius" to 1000.0,"ore.minY" to 32.0,"ore.maxY" to 72.0,"ore.veinMin" to 1.0,"ore.veinMax" to 3.0,
        "forging.axeAttackDamage" to 11.0,"forging.axeAttackSpeed" to 1.2,
        "forging.swordAttackDamage" to 9.0,"forging.swordAttackSpeed" to 1.8,
        "forging.magicDamage" to 1.0,"forging.magicResistancePerPiece" to .08,"forging.magicResistanceCap" to .32
    ).apply{
        for(kind in net.ciyuan.lynxrelics.mob.RelicMobKind.entries){
            put("mob.${kind.id}.health",kind.hp);put("mob.${kind.id}.armor",kind.armor);put("mob.${kind.id}.damage",kind.damage)
            put("mob.${kind.id}.speed",if(kind.id=="rift_hunter").3 else .22)
            put("mob.${kind.id}.spawnWeight",when(kind.id){"echo_moth"->2.0;"tide_jelly"->3.0;"reef_lurker"->1.0;"rift_hunter"->8.0;else->0.0})
        }
    }
    private val values = defaults.toMutableMap()
    fun load() {
        val path = FabricLoader.getInstance().configDir.resolve("lynxrelics/balance.json")
        Files.createDirectories(path.parent)
        values.clear();values.putAll(defaults)
        var missing=false
        if (Files.exists(path)) {
            val json = Files.newBufferedReader(path).use { com.google.gson.JsonParser.parseReader(it).asJsonObject }
            for ((key, element) in json.entrySet()) {
                require(key in defaults) { "Unknown LynxRelics balance key: $key" }
                val n = element.asDouble
                require(n.isFinite() && n >= 0 && n <= 1_000_000) { "Invalid balance value: $key" }
                values[key] = n
            }
            missing=defaults.keys.any{!json.has(it)}
        }else missing=true
        for((key,n)in values){
            val probability=key in setOf("ore.chance","mob.hunterScaleChance","beacon.craftsmanshipChance","beacon.shelterReduction")
            require(!probability||n<=1){"$key must be between 0 and 1"}
            require(!key.endsWith(".health")||n in 1.0..1024.0){"$key must be between 1 and 1024"}
            require(!key.endsWith(".speed")||n<=2){"$key exceeds the supported mob speed"}
            require(!key.endsWith(".spawnWeight")||n<=100){"$key must be between 0 and 100"}
        }
        require(value("ore.minY")<=value("ore.maxY")&&value("ore.maxY")<=255){"Invalid ore height interval"}
        require(value("beacon.foodTicks")>=20&&value("beacon.foodTicks")%20.0==0.0){"beacon.foodTicks must be a multiple of 20 ticks"}
        require(value("forging.axeAttackDamage") in 1.0..64.0&&value("forging.swordAttackDamage") in 1.0..64.0){"Invalid endforged attack damage"}
        require(value("forging.axeAttackSpeed") in .1..4.0&&value("forging.swordAttackSpeed") in .1..4.0){"Invalid endforged attack speed"}
        require(value("forging.magicResistancePerPiece") in 0.0..0.25&&value("forging.magicResistanceCap") in 0.0..0.8){"Invalid endforged magic resistance"}
        require(value("ore.veinMin") in 1.0..16.0&&value("ore.veinMax") in value("ore.veinMin")..16.0){"Invalid ore vein size"}
        require(value("mob.broodPartyCap") in 1.0..8.0&&value("mob.stockLimit") in 1.0..2.0){"Invalid bounded party/stock setting"}
        require(value("lodestone.range")<=32&&value("raid.breathRange")<=128&&value("raid.wingRange")<=32){"Configured range exceeds the bounded loaded-area implementation"}
        if(missing)Files.newBufferedWriter(path).use { GsonBuilder().setPrettyPrinting().create().toJson(values, it) }
    }
    fun value(key: String) = values.getValue(key)
    fun ticks(key: String) = value(key).toInt()
    fun enabled(key: String) = value(key) != 0.0
    fun scaledDurability(base: Int) = ceil(base * value("forging.durabilityMultiplier")).toInt()
}

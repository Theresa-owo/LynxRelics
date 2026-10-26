// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.raid

import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3

interface DragonHealthAttribute { fun lynxDragonHealth(enabled: Boolean) }
interface EncounterHealthAttribute { fun lynxEncounterHealth(value: Double) }
interface RaidHolder { fun lynxRaid(): RaidState }
class RaidState {
    var phase = 1
    var recalls = 0
    var healed = 0f
    var nextRecall = 0L
    var recallAt = 0L
    var nextSkill = 0L
    var transitionUntil = 0L
    var rewarded = false
    var skill = ""
    var skillAt = 0L
    var skillNumber = 0
    var skillOrigin = Vec3.ZERO
    var skillTarget = Vec3.ZERO
    var skillDirection=Vec3.ZERO
    var previousHead=Vec3.ZERO
    var targetPlayer=""
    var warningFrom=0L
    var started=false
    var combo=-1
    var emptySince=0L
    var restorePhase=false
    var flightControlled=false
    val comboGuards=mutableMapOf<String,Long>()
    val comboSegments=mutableMapOf<String,Int>()
    var lastEmbers = 0L
    val crystals = mutableMapOf<String, Long>()
    val embers = mutableSetOf<String>()
    val hitPlayers = mutableSetOf<String>()
    fun save(out: ValueOutput) {
        out.putInt("Phase", phase); out.putInt("Recalls", recalls); out.putFloat("Healed", healed)
        out.putLong("NextRecall", nextRecall); out.putLong("RecallAt", recallAt)
        out.putLong("NextSkill", nextSkill); out.putLong("TransitionUntil", transitionUntil)
        out.putBoolean("Rewarded", rewarded); out.putString("Skill", skill); out.putLong("SkillAt", skillAt)
        out.putInt("SkillNumber", skillNumber); out.putLong("LastEmbers", lastEmbers)
        out.store("Origin", Vec3.CODEC, skillOrigin); out.store("Target", Vec3.CODEC, skillTarget)
        out.store("Direction",Vec3.CODEC,skillDirection);out.store("PreviousHead",Vec3.CODEC,previousHead)
        out.putString("TargetPlayer",targetPlayer);out.putLong("WarningFrom",warningFrom);out.putBoolean("Started",started);out.putInt("Combo",combo);out.putLong("EmptySince",emptySince)
        out.putBoolean("FlightControlled",flightControlled)
        val guards=out.childrenList("ComboGuards");comboGuards.forEach{(id,until)->guards.addChild().also{it.putString("UUID",id);it.putLong("Until",until);it.putInt("Segment",comboSegments[id]?:-1)}}
        val list = out.childrenList("Crystals")
        crystals.forEach { (uuid, expiry) -> list.addChild().also { it.putString("UUID", uuid); it.putLong("Expiry", expiry) } }
        val e = out.childrenList("Embers"); embers.forEach { uuid -> e.addChild().putString("UUID", uuid) }
        val h = out.childrenList("HitPlayers"); hitPlayers.forEach { uuid -> h.addChild().putString("UUID", uuid) }
    }
    fun load(input: ValueInput) {
        phase = input.getIntOr("Phase", 1).coerceIn(1, 3); recalls = input.getIntOr("Recalls", 0).coerceAtLeast(0)
        healed = input.getFloatOr("Healed", 0f).coerceAtLeast(0f)
        nextRecall = input.getLongOr("NextRecall", 0); recallAt = input.getLongOr("RecallAt", 0)
        nextSkill = input.getLongOr("NextSkill", 0); transitionUntil = input.getLongOr("TransitionUntil", 0)
        rewarded = input.getBooleanOr("Rewarded", false); skill = input.getStringOr("Skill", "")
        skillAt = input.getLongOr("SkillAt", 0); skillNumber = input.getIntOr("SkillNumber", 0)
        lastEmbers = input.getLongOr("LastEmbers", 0)
        skillOrigin = input.read("Origin", Vec3.CODEC).orElse(Vec3.ZERO); skillTarget = input.read("Target", Vec3.CODEC).orElse(Vec3.ZERO)
        skillDirection=input.read("Direction",Vec3.CODEC).orElse(Vec3.ZERO);previousHead=input.read("PreviousHead",Vec3.CODEC).orElse(Vec3.ZERO)
        targetPlayer=input.getStringOr("TargetPlayer","");warningFrom=input.getLongOr("WarningFrom",0);started=input.getBooleanOr("Started",false);combo=input.getIntOr("Combo",-1).coerceIn(-1,2);emptySince=input.getLongOr("EmptySince",0);restorePhase=started
        flightControlled=input.getBooleanOr("FlightControlled",false)
        comboGuards.clear();comboSegments.clear();input.childrenListOrEmpty("ComboGuards").forEach{val id=it.getStringOr("UUID","");comboGuards[id]=it.getLongOr("Until",0);comboSegments[id]=it.getIntOr("Segment",-1)}
        crystals.clear(); input.childrenListOrEmpty("Crystals").forEach { crystals[it.getStringOr("UUID", "")] = it.getLongOr("Expiry", 0) }
        embers.clear(); input.childrenListOrEmpty("Embers").forEach { embers.add(it.getStringOr("UUID", "")) }
        hitPlayers.clear(); input.childrenListOrEmpty("HitPlayers").forEach { hitPlayers.add(it.getStringOr("UUID", "")) }
    }
}

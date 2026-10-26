// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.core

import net.minecraft.world.SimpleContainer
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import kotlin.math.min

interface RelicHolder { fun lynxRelics(): RelicState }
fun Player.relics() = (this as RelicHolder).lynxRelics()
enum class RelicSlot { NECKLACE, BELT, GAUNTLET, CLOAK }
enum class Route { DRAGON, ECHO, TIDE }

class RelicState {
    val accessories = object : SimpleContainer(4) {
        override fun setChanged() { super.setChanged(); clampResources() }
        override fun getMaxStackSize() = 1
    }
    var breath = 0
    var tide = 0
    var pressure = 0.0
    val cooldowns = mutableMapOf<String, Long>()
    var lastSkill = 0L
    var overloadUntil = 0L
    var lastWater = 0L
    var airborneUsed = false
    var groundTicks = 0
    var sneakTicks = 0
    var quietUntil = 0L
    var quietSteps = 0
    var quietLastPos = Vec3.ZERO
    var storedEcho = 0f
    var echoUntil = 0L
    var channel = ""
    var channelHand=net.minecraft.world.InteractionHand.MAIN_HAND
    var channelStarted = 0L
    var channelPulses = 0
    var channelCost = 0
    var channelPressure = 0.0
    var channelOrigin = Vec3.ZERO
    var mirrorUntil = 0L
    var mirrorUntilNanos = 0L
    var parryUntilNanos = 0L
    var deflectorUntilNanos = 0L
    var quietStepTick = -1L
    var lastAttackCharged = false
    var shieldCharge = 0
    var lastShieldChargeTick = -1L
    var stillSince = 0L
    var stillPosition = Vec3.ZERO
    var anchorDimension = ""
    var anchorPos = 0L
    var pearlId = ""
    var pearlExpires = 0L
    val actionTicks=LongArray(7){Long.MIN_VALUE}
    var feedbackAt=Long.MIN_VALUE
    var lastReceivedAction= -1
    var lastReceivedAt=0L
    var receivedActions=0L
    var lastUseAction=""
    var lastUseAt=0L
    var lastFeedbackKey=""
    var lastFeedback:net.minecraft.network.chat.Component?=null
    var lastDamage = -1L
    var extraAir = 0
    var synced = ""
    var clientQuiet = false
    var waterReturnDimension = ""
    var waterReturnPosition = Vec3.ZERO
    var wasDeepWater = false
    var prospectHint:net.minecraft.network.chat.Component?=null
    var prospectUntil=0L
    var craftsmanshipUntil=0L
    var shelterAuraUntil=0L
    var lastAbundanceTick=-1L
    var presentationMigrationPending=true

    fun ready(key: String, now: Long) = (cooldowns[key] ?: 0L) <= now
    fun cooldown(key: String, now: Long, ticks: Int) { cooldowns[key] = now + ticks }
    fun extendCooldown(key: String, now: Long, ticks: Int) { cooldowns[key] = maxOf(cooldowns[key] ?: 0, now + ticks) }
    fun remaining(key: String, now: Long) = ((cooldowns[key] ?: 0L) - now).coerceAtLeast(0L)
    fun breathCapacity(): Int {
        val belt = accessories.getItem(RelicSlot.BELT.ordinal)
        return (if (Balance.enabled("dragon.enabled")&&!belt.isEmpty && !belt.isBroken && belt.`is`(net.ciyuan.lynxrelics.content.Content.item("dragonbound_belt"))) Balance.ticks("breath.beltCapacity") + 4 * RelicEnchantments.level(belt, "breath_capacity") else Balance.ticks("breath.capacity")).coerceAtMost(64)
    }
    fun tideCapacity(): Int {
        val belt = accessories.getItem(RelicSlot.BELT.ordinal)
        return (if (Balance.enabled("tide.enabled")&&!belt.isEmpty && !belt.isBroken && belt.`is`(net.ciyuan.lynxrelics.content.Content.item("undertow_belt"))) Balance.ticks("tide.beltCapacity") + 5 * RelicEnchantments.level(belt, "tide_capacity") else Balance.ticks("tide.capacity")).coerceAtMost(140)
    }
    fun clampResources() {
        breath = breath.coerceIn(0, breathCapacity()); tide = tide.coerceIn(0, tideCapacity())
        val belt=accessories.getItem(RelicSlot.BELT.ordinal)
        if(belt.isEmpty||belt.isBroken||!belt.`is`(net.ciyuan.lynxrelics.content.Content.item("undertow_belt")))extraAir=0
    }
    fun save(out: ValueOutput) {
        ContainerHelper.saveAllItems(out.child("Accessories"), accessories.items)
        out.putInt("Breath", breath); out.putInt("Tide", tide); out.putDouble("Pressure", pressure)
        out.putLong("LastSkill", lastSkill); out.putLong("OverloadUntil", overloadUntil)
        out.putLong("LastWater", lastWater); out.putBoolean("AirborneUsed", airborneUsed)
        out.putString("AnchorDimension", anchorDimension); out.putLong("AnchorPos", anchorPos)
        out.putString("PearlId", pearlId); out.putLong("PearlExpires", pearlExpires)
        out.putInt("ExtraAir", extraAir)
        out.putFloat("StoredEcho", storedEcho); out.putLong("EchoUntil", echoUntil)
        out.putString("WaterReturnDimension",waterReturnDimension);out.store("WaterReturnPosition",Vec3.CODEC,waterReturnPosition)
        val list = out.childrenList("Cooldowns")
        cooldowns.forEach { (key, until) -> list.addChild().also { it.putString("Key", key); it.putLong("Until", until) } }
    }
    fun load(input: ValueInput) {
        accessories.clearContent()
        ContainerHelper.loadAllItems(input.childOrEmpty("Accessories"), accessories.items)
        breath = input.getIntOr("Breath", 0).coerceIn(0, 64)
        tide = input.getIntOr("Tide", 0).coerceIn(0, 140)
        pressure = input.getDoubleOr("Pressure", 0.0).coerceIn(0.0, 100.0)
        lastSkill = input.getLongOr("LastSkill", 0); overloadUntil = input.getLongOr("OverloadUntil", 0)
        lastWater = input.getLongOr("LastWater", 0); airborneUsed = input.getBooleanOr("AirborneUsed", false)
        anchorDimension = input.getStringOr("AnchorDimension", ""); anchorPos = input.getLongOr("AnchorPos", 0)
        pearlId = input.getStringOr("PearlId", ""); pearlExpires = input.getLongOr("PearlExpires", 0)
        extraAir = input.getIntOr("ExtraAir", 400).coerceIn(0, 1200)
        storedEcho = input.getFloatOr("StoredEcho", 0f).coerceIn(0f, 6f); echoUntil = input.getLongOr("EchoUntil", 0)
        waterReturnDimension=input.getStringOr("WaterReturnDimension","");waterReturnPosition=input.read("WaterReturnPosition",Vec3.CODEC).orElse(Vec3.ZERO)
        cooldowns.clear()
        input.childrenListOrEmpty("Cooldowns").forEach { cooldowns[it.getStringOr("Key", "")] = it.getLongOr("Until", 0) }
        channel = ""; synced = ""; clampResources()
    }
    fun copyFrom(old: RelicState, keepItems: Boolean) {
        for (i in 0..3) accessories.setItem(i, if (keepItems) old.accessories.getItem(i).copy() else net.minecraft.world.item.ItemStack.EMPTY)
        breath = old.breath; tide = old.tide; pressure = old.pressure
        cooldowns.clear(); cooldowns.putAll(old.cooldowns)
        lastSkill = old.lastSkill; overloadUntil = old.overloadUntil; lastWater = old.lastWater
        airborneUsed = old.airborneUsed; anchorDimension = old.anchorDimension; anchorPos = old.anchorPos
        pearlId = old.pearlId; pearlExpires = old.pearlExpires; extraAir = old.extraAir
        if (keepItems) { storedEcho = old.storedEcho; echoUntil = old.echoUntil }
        waterReturnDimension=old.waterReturnDimension;waterReturnPosition=old.waterReturnPosition
        clampResources()
    }
}

/** Pure rules shared by gameplay and regression tests. */
object RelicRules {
    fun pressureGain(base: Double, dragonPieces: Int) = base * if (dragonPieces >= 2) 0.9 else 1.0
    fun breathCost(base: Int, fourPieceReady: Boolean) = if (fourPieceReady) (base - 2).coerceAtLeast(2) else base
    fun damageAfterUpgrade(damage: Int, oldMax: Int, newMax: Int) = kotlin.math.ceil(damage.toDouble() / oldMax * newMax).toInt().coerceIn(0, newMax - 1)
    fun phase(hp: Float) = if (hp > 2100) 1 else if (hp > 900) 2 else 3
    fun heal(health: Float, budgetSpent: Float, amount: Float, budget: Float) = min(min(3000f - health, budget - budgetSpent), amount).coerceAtLeast(0f)
}

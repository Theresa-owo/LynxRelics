// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.content

import net.ciyuan.lynxrelics.core.Balance
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RailBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.RailShape
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

class SpeedRail(properties: Properties) : RailBlock(properties.noCollision()) {
    init { registerDefaultState(defaultBlockState().setValue(SPEED, 1)) }
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { super.createBlockStateDefinition(builder); builder.add(SPEED) }
    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (player is ServerPlayer) {
            val mode = Math.floorMod(state.getValue(SPEED) + if (player.isShiftKeyDown) -1 else 1, 4)
            level.setBlock(pos, state.setValue(SPEED, mode), 3)
            player.sendOverlayMessage(Component.translatable("msg.lynxrelics.speed_rail",RailHooks.speed(mode)))
        }
        return InteractionResult.SUCCESS
    }
    companion object { @JvmField val SPEED: IntegerProperty = IntegerProperty.create("speed", 0, 3) }
}
object RailHooks {
    fun speed(mode: Int) = Balance.value(listOf("rail.slow", "rail.normal", "rail.fast", "rail.express")[mode.coerceIn(0, 3)])
    private fun state(cart: AbstractMinecart): BlockState? = cart.level().getBlockState(cart.currentBlockPosOrRailBelow).takeIf { it.block is SpeedRail }
    @JvmStatic fun maxSpeed(cart: AbstractMinecart, original: Double): Double {
        val state = state(cart) ?: return original
        val shape = state.getValue(RailBlock.SHAPE)
        val chosen = speed(state.getValue(SpeedRail.SPEED)) / 20.0
        return if (shape == RailShape.NORTH_EAST || shape == RailShape.NORTH_WEST || shape == RailShape.SOUTH_EAST || shape == RailShape.SOUTH_WEST) minOf(chosen, 0.4) else chosen
    }
    @JvmStatic fun tick(cart: AbstractMinecart) {
        if (cart.level().isClientSide) return
        val state = state(cart) ?: return
        val desired = maxSpeed(cart, 0.4)
        val v = cart.deltaMovement
        val horizontal = Vec3(v.x, 0.0, v.z)
        val length = horizontal.length()
        val shape = state.getValue(RailBlock.SHAPE)
        val direction = if (length > 0.005) horizontal.scale(1 / length) else when (shape) {
            RailShape.EAST_WEST, RailShape.ASCENDING_EAST, RailShape.ASCENDING_WEST -> Vec3(if (cart.motionDirection.stepX < 0) -1.0 else 1.0, 0.0, 0.0)
            else -> Vec3(0.0, 0.0, if (cart.motionDirection.stepZ < 0) -1.0 else 1.0)
        }
        val next = minOf(desired, length + Balance.value("rail.acceleration"))
        cart.deltaMovement = Vec3(direction.x * next, v.y, direction.z * next)
    }
}

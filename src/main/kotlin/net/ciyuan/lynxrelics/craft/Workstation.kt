// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.craft

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.Balance
import net.ciyuan.lynxrelics.menu.WorkstationMenu
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.BlockHitResult

class WorkshopBlock(properties: Properties, val route: String) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState) = Workstation(pos, state)
    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        if (player is ServerPlayer) (level.getBlockEntity(pos) as? Workstation)?.let { player.openMenu(it) }
        return InteractionResult.SUCCESS
    }
    override fun <T : BlockEntity> getTicker(level: Level, state: BlockState, type: BlockEntityType<T>): BlockEntityTicker<T>? {
        if (level.isClientSide || type != Content.WORKSTATION_TYPE) return null
        return BlockEntityTicker { _, _, _, be -> (be as Workstation).tick() }
    }
}

class Workstation(pos: BlockPos, state: BlockState) : BlockEntity(Content.WORKSTATION_TYPE, pos, state), net.minecraft.world.WorldlyContainer, ExtendedMenuProvider<BlockPos> {
    val items: NonNullList<ItemStack> = NonNullList.withSize(16, ItemStack.EMPTY)
    var selected = 0
    var progress = 0
    var fingerprint = 0
    var ritualUntil=0L
    val route get() = (blockState.block as WorkshopBlock).route
    val recipes get() = CraftRecipes.forRoute(route)
    val data = object : ContainerData {
        override fun get(i: Int) = when (i) { 0 -> selected; 1 -> progress; 2 -> duration(); 3 -> listOf("dragon", "echo", "tide").indexOf(route); else -> 0 }
        override fun set(i: Int, n: Int) { when (i) { 0 -> selected = n; 1 -> progress = n } }
        override fun getCount() = 4
    }
    private fun duration() = recipes.getOrNull(selected)?.let { (it.ticks * Balance.value("craft.timeMultiplier")).toInt().coerceAtLeast(1) } ?: 1
    fun tick() {
        if (!Balance.enabled("craft.enabled") || !Balance.enabled("$route.enabled")) return
        val recipe = recipes.getOrNull(selected) ?: return
        if(recipe.mode=="ritual"&&(level?.server?.overworld()?.gameTime ?: 0)<ritualUntil)return
        val hash = ItemStack.hashStackList(items.subList(0, 10)) * 31 + selected
        if (hash != fingerprint) { progress = 0; fingerprint = hash }
        val plan = CraftTransaction.plan(items, recipe, level?.registryAccess()) ?: return
        progress++
        if (progress >= duration()) {
            if(recipe.mode=="ritual"&&!Rituals.spawn(this,recipe.encounter)){progress=0;setChanged();return}
            // Single logical server thread; no observers see a half applied recipe.
            for (i in items.indices) items[i] = plan[i]
            progress = 0; fingerprint = 0
        }
        setChanged()
    }
    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input); ContainerHelper.loadAllItems(input, items)
        val recipeId=input.getStringOr("RecipeID","")
        selected = recipes.indexOfFirst{it.id==recipeId}.coerceAtLeast(0)
        val recipe=recipes.getOrNull(selected)
        progress = if(recipe!=null&&input.getStringOr("RecipeDefinition","")==com.google.gson.Gson().toJson(recipe))input.getIntOr("Progress",0).coerceIn(0,duration()-1)else 0
        // Item/component hash codes are process-local. Rebuild from the exact saved inventory.
        fingerprint = ItemStack.hashStackList(items.subList(0,10))*31+selected
        ritualUntil=input.getLongOr("RitualUntil",0)
    }
    override fun saveAdditional(out: ValueOutput) {
        super.saveAdditional(out); ContainerHelper.saveAllItems(out, items)
        out.putInt("Recipe", selected); out.putInt("Progress", progress); out.putInt("Fingerprint", fingerprint)
        recipes.getOrNull(selected)?.let{out.putString("RecipeID",it.id);out.putString("RecipeDefinition",com.google.gson.Gson().toJson(it))}
        out.putLong("RitualUntil",ritualUntil)
    }
    override fun preRemoveSideEffects(pos: BlockPos, state: BlockState) {
        level?.let { Containers.dropContents(it, worldPosition, this) }
        super.preRemoveSideEffects(pos, state)
    }
    override fun getContainerSize() = 16
    override fun isEmpty() = items.all { it.isEmpty }
    override fun getItem(i: Int) = items[i]
    override fun removeItem(i: Int, n: Int): ItemStack = ContainerHelper.removeItem(items, i, n).also { if (!it.isEmpty) setChanged() }
    override fun removeItemNoUpdate(i: Int): ItemStack = ContainerHelper.takeItem(items, i)
    override fun setItem(i: Int, stack: ItemStack) { items[i] = stack; setChanged() }
    override fun setChanged(){
        val hash=ItemStack.hashStackList(items.subList(0,10))*31+selected
        if(hash!=fingerprint){progress=0;fingerprint=hash}
        super.setChanged()
    }
    override fun stillValid(player: Player) = level?.getBlockEntity(worldPosition) === this && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) <= 64
    override fun clearContent() { items.clear(); setChanged() }
    override fun getSlotsForFace(face:net.minecraft.core.Direction)=if(face==net.minecraft.core.Direction.DOWN)(10..15).toList().toIntArray()else(0..8).toList().toIntArray()
    override fun canPlaceItemThroughFace(index:Int,stack:ItemStack,face:net.minecraft.core.Direction?)=index in 0..8
    override fun canTakeItemThroughFace(index:Int,stack:ItemStack,face:net.minecraft.core.Direction)=index in 10..15
    override fun getDisplayName() = Component.translatable("block.lynxrelics.${route}_workstation")
    override fun getScreenOpeningData(player: ServerPlayer) = worldPosition
    override fun createMenu(id: Int, inventory: Inventory, player: Player) = WorkstationMenu(id, inventory, this, data)
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.ciyuan.lynxrelics.core.Mining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;
@Mixin(Block.class)
public abstract class MiningRelicsMixin {
    @WrapMethod(method="playerDestroy")
    private void lynxrelics$break(ServerLevel level,ServerPlayer player,BlockPos pos,BlockState state,BlockEntity be,ItemStack tool,Operation<Void> original){Object token=Mining.begin(level,player,pos,state,tool);try{original.call(level,player,pos,state,be,tool);}finally{Mining.end(token);}}
    @ModifyReturnValue(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",at=@At("RETURN"))
    private static List<ItemStack> lynxrelics$smelt(List<ItemStack> original,BlockState state,ServerLevel level,BlockPos pos,BlockEntity be,Entity actor,ItemInstance tool){return Mining.drops(original,state,level,pos,actor,tool);}
}

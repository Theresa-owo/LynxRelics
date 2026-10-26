// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.BeaconCores;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(BlockEntity.class)
public abstract class BeaconRemovalMixin {
    @Inject(method="preRemoveSideEffects",at=@At("HEAD"))private void lynxrelics$core(BlockPos pos,BlockState state,CallbackInfo ci){if((Object)this instanceof BeaconBlockEntity beacon)BeaconCores.removed(beacon);}
}

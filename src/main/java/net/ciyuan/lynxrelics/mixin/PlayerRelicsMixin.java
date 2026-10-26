// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;

import net.ciyuan.lynxrelics.core.RelicHolder;
import net.ciyuan.lynxrelics.core.RelicState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerRelicsMixin implements RelicHolder {
    @Unique private final RelicState lynxrelics$state = new RelicState();
    @Override public RelicState lynxRelics() { return lynxrelics$state; }
    /** Exact legacy ABI used by the supplied Equinox Elytra Slot 1.2 death callback. */
    public net.minecraft.world.entity.item.ItemEntity drop(net.minecraft.world.item.ItemStack stack, boolean retainOwnership) {
        return ((Player)(Object)this).drop(stack, retainOwnership, net.minecraft.util.Prediction.SERVER_ONLY);
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void lynxrelics$save(ValueOutput output, CallbackInfo ci) { lynxrelics$state.save(output.child("LynxRelics")); }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void lynxrelics$load(ValueInput input, CallbackInfo ci) { lynxrelics$state.load(input.childOrEmpty("LynxRelics")); }
}

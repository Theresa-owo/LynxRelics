// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.RailHooks;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractMinecart.class)
public abstract class MinecartTickMixin {
    @Inject(method="tick", at=@At("TAIL"))
    private void lynxrelics$accelerate(CallbackInfo ci) { RailHooks.tick((AbstractMinecart)(Object)this); }
}

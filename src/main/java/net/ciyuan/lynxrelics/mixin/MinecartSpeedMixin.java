// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.RailHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({OldMinecartBehavior.class, NewMinecartBehavior.class})
public abstract class MinecartSpeedMixin extends MinecartBehavior {
    protected MinecartSpeedMixin(AbstractMinecart minecart) { super(minecart); }
    @Inject(method="getMaxSpeed", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$speed(ServerLevel level, CallbackInfoReturnable<Double> cir) { double speed=RailHooks.maxSpeed(minecart, Double.NaN); if (!Double.isNaN(speed)) cir.setReturnValue(speed); }
}

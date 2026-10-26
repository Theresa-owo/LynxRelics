// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.BoundPearlData;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Projectile.class)
public abstract class ProjectileRelicsMixin {
    @Inject(method="shoot", at=@At("TAIL"))
    private void lynxrelics$arrow(double x, double y, double z, float speed, float spread, CallbackInfo ci) { if ((Object)this instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow) net.ciyuan.lynxrelics.core.Combat.arrowShot(arrow); }
    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void lynxrelics$save(ValueOutput out, CallbackInfo ci) { if ((Object)this instanceof BoundPearlData p) { out.putLong("LynxBoundExpiry", p.lynxBoundExpiry()); out.putString("LynxBoundDimension", p.lynxBoundDimension()); } }
    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void lynxrelics$load(ValueInput in, CallbackInfo ci) { if ((Object)this instanceof BoundPearlData p) p.lynxMarkBound(in.getLongOr("LynxBoundExpiry", 0), in.getStringOr("LynxBoundDimension", "")); }
}

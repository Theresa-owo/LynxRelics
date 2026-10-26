// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.core.Combat;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractArrow.class)
public abstract class ArrowCombatMixin {
    @Inject(method="onHitEntity", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$mirror(EntityHitResult hit, CallbackInfo ci) { AbstractArrow a=(AbstractArrow)(Object)this; if (Combat.arrowHitEntity(a,hit.getEntity()) || Combat.reflect(a,hit.getEntity())) ci.cancel(); }
    @Inject(method="onHitBlock", at=@At("TAIL"))
    private void lynxrelics$sound(BlockHitResult hit, CallbackInfo ci) { Combat.arrowHitBlock((AbstractArrow)(Object)this, hit.getLocation()); }
}

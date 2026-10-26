// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.BoundPearlData;
import net.ciyuan.lynxrelics.content.InfiniteItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ThrownEnderpearl.class)
public abstract class BoundPearlMixin implements BoundPearlData {
    @Unique private long lynxrelics$expiry;
    @Unique private String lynxrelics$dimension = "";
    @Override public long lynxBoundExpiry() { return lynxrelics$expiry; }
    @Override public String lynxBoundDimension() { return lynxrelics$dimension; }
    @Override public void lynxMarkBound(long expiry, String dimension) { lynxrelics$expiry=expiry; lynxrelics$dimension=dimension; }
    @Inject(method="tick", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$limit(CallbackInfo ci) { if (InfiniteItems.tick((ThrownEnderpearl)(Object)this)) ci.cancel(); }
    @Inject(method="onHit", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$land(HitResult hit, CallbackInfo ci) { if (InfiniteItems.beforeHit((ThrownEnderpearl)(Object)this)) ci.cancel(); }
    @Redirect(method="tick", at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerPlayer;registerAndUpdateEnderPearlTicket(Lnet/minecraft/world/entity/projectile/throwableitemprojectile/ThrownEnderpearl;)J"))
    private long lynxrelics$noForceLoad(ServerPlayer owner, ThrownEnderpearl pearl) { return InfiniteItems.bound(pearl) ? 40L : owner.registerAndUpdateEnderPearlTicket(pearl); }
    @Inject(method="canTeleport", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$dimension(Level from, Level to, CallbackInfoReturnable<Boolean> cir) { if (InfiniteItems.bound((ThrownEnderpearl)(Object)this) && from.dimension()!=to.dimension()) cir.setReturnValue(false); }
    @Inject(method="registerToCurrentOwner", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$noPersistentOwnerTicket(CallbackInfo ci) { if (InfiniteItems.bound((ThrownEnderpearl)(Object)this)) ci.cancel(); }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.core.Combat;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class QuietStepMixin {
    @Inject(method="gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/Entity;)V", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$step(Holder<GameEvent> event, Entity source, CallbackInfo ci) { if ((Object)this instanceof Player p && Combat.quietEvent(p,event)) ci.cancel(); }
    @Inject(method="deflection", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$mirror(net.minecraft.world.entity.projectile.Projectile projectile, CallbackInfoReturnable<net.minecraft.world.entity.projectile.ProjectileDeflection> cir) { if ((Object)this instanceof net.minecraft.server.level.ServerPlayer p && projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow && Combat.claimReflection(arrow,p)) cir.setReturnValue(net.minecraft.world.entity.projectile.ProjectileDeflection.REVERSE); }
}

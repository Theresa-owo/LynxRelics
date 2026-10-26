// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.raid.DragonRaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(EndCrystal.class)
public abstract class RaidCrystalMixin {
    @Inject(method="tick", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$expire(CallbackInfo ci) { if (DragonRaid.tickOwned((EndCrystal)(Object)this)) ci.cancel(); }
    @Inject(method="hurtServer", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$safeExplosion(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) { EndCrystal crystal=(EndCrystal)(Object)this; if (DragonRaid.owned(crystal)) cir.setReturnValue(DragonRaid.destroyCrystal(crystal, level, source)); }
}

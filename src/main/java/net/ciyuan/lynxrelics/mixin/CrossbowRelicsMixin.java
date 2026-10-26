// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.VanillaMods;
import net.ciyuan.lynxrelics.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(CrossbowItem.class)
public abstract class CrossbowRelicsMixin {
    @Inject(method="getChargeDuration", at=@At("RETURN"), cancellable=true)
    private static void lynxrelics$load(ItemStack stack, LivingEntity owner, CallbackInfoReturnable<Integer> cir) { if (VanillaMods.upgrade(stack).equals("steady_rest")) cir.setReturnValue((int)Math.ceil(cir.getReturnValue()*1.15)); }
    @Inject(method="shootProjectile", at=@At("TAIL"))
    private void lynxrelics$main(LivingEntity owner, Projectile projectile, int index, float velocity, float inaccuracy, float angle, LivingEntity target, CallbackInfo ci) {
        if (index==0 && owner instanceof ServerPlayer p && projectile instanceof AbstractArrow arrow && VanillaMods.upgrade(arrow.getWeaponItem()).equals("steady_rest") && Skills.INSTANCE.now(p)-((RelicHolder)p).lynxRelics().getStillSince()>=16) arrow.setBaseDamage(((ArrowDataAccessor)arrow).lynxBaseDamage()*1.1);
    }
}

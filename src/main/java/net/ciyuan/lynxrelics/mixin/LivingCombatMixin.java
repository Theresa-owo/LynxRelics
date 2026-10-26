// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.ciyuan.lynxrelics.core.Combat;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class LivingCombatMixin {
    @WrapMethod(method="startUsingItem")
    private void lynxrelics$raise(InteractionHand hand, Operation<Void> original) {
        LivingEntity entity=(LivingEntity)(Object)this;
        if (entity instanceof ServerPlayer p && !((net.ciyuan.lynxrelics.core.RelicHolder)p).lynxRelics().ready("weapon_recovery", net.ciyuan.lynxrelics.core.Skills.INSTANCE.now(p)) && p.getItemInHand(hand).has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS)) return;
        boolean used=entity.isUsingItem(); original.call(hand);
        if (!used && entity.isUsingItem() && entity instanceof ServerPlayer p) Combat.raised(p);
    }
    @ModifyExpressionValue(method="actuallyHurt", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float lynxrelics$ward(float adjusted, ServerLevel level, DamageSource source, float amount) { return Combat.adjustedDamage((LivingEntity)(Object)this, source, adjusted); }
    @Inject(method="decreaseAirSupply", at=@At("RETURN"), cancellable=true)
    private void lynxrelics$air(int air, CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(Combat.air((LivingEntity)(Object)this, cir.getReturnValue(), air)); }
    @ModifyVariable(method="knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at=@At("HEAD"), argsOnly=true, ordinal=0)
    private double lynxrelics$bulwark(double strength) { return Combat.shieldKnockback((LivingEntity)(Object)this, strength); }
}

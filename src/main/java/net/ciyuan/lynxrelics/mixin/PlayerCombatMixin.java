// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
@Mixin(Player.class)
public abstract class PlayerCombatMixin {
    @ModifyExpressionValue(method="actuallyHurt", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/player/Player;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float lynxrelics$ward(float adjusted, ServerLevel level, DamageSource source, float amount) { return Combat.adjustedDamage((Player)(Object)this,source,adjusted); }
    @Inject(method="attack", at=@At("HEAD"))
    private void lynxrelics$charge(Entity target, CallbackInfo ci) { ((RelicHolder)this).lynxRelics().setLastAttackCharged(((Player)(Object)this).getAttackStrengthScale(0)>=.9f); }
    @Inject(method="getEnchantedDamage", at=@At("RETURN"), cancellable=true)
    private void lynxrelics$breach(Entity target, float base, DamageSource source, CallbackInfoReturnable<Float> cir) { cir.setReturnValue(Combat.extraMelee((Player)(Object)this,target,base,cir.getReturnValue())); }
    @Inject(method="blockUsingItem", at=@At("TAIL"))
    private void lynxrelics$parry(ServerLevel level, LivingEntity attacker, DamageSource source, float amount, boolean blocked, CallbackInfo ci) { if ((Object)this instanceof ServerPlayer p) Combat.blocked(p,source,amount); }
    @Inject(method="playStepSound", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$step(BlockPos pos, BlockState state, CallbackInfo ci) { if (Combat.step((Player)(Object)this)) ci.cancel(); }
}

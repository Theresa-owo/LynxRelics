// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ciyuan.lynxrelics.content.Endforged;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ItemStack.class)
public abstract class EndforgedDamageMixin {
    @ModifyReturnValue(method="getDamageSource",at=@At("RETURN"))
    private DamageSource lynxrelics$magic(DamageSource original,LivingEntity attacker){return Endforged.attackSource((ItemStack)(Object)this,attacker,original);}
}

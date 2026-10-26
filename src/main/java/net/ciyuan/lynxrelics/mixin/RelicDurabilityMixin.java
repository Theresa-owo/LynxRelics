// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ciyuan.lynxrelics.core.RelicDurability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ItemStack.class)
public abstract class RelicDurabilityMixin {
    @ModifyExpressionValue(method="processDurabilityChange",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int lynxrelics$sequential(int adjusted,int original,ServerLevel level,ServerPlayer player){return RelicDurability.afterUnbreaking((ItemStack)(Object)this,player,adjusted);}
}

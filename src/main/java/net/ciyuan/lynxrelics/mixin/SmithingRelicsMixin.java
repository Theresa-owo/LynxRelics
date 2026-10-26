// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.Endforged;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingRelicsMixin {
    @Inject(method="assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at=@At("RETURN"), cancellable=true)
    private void lynxrelics$ratio(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(net.ciyuan.lynxrelics.content.VanillaMods.transform(input, Endforged.preserveDamage(input.base(), cir.getReturnValue())));
    }
}

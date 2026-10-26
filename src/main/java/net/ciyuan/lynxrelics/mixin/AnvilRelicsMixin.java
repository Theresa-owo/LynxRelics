// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.RelicItem;
import net.ciyuan.lynxrelics.core.RelicEnchantments;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AnvilMenu.class)
public abstract class AnvilRelicsMixin {
    @Inject(method="createResult", at=@At("TAIL"))
    private void lynxrelics$twoCustom(CallbackInfo ci) { ResultContainer result=((ItemCombinerAccessor)this).lynxResultSlots(); ItemStack stack=result.getItem(0); if (stack.getItem() instanceof RelicItem relic && relic.getSlot()!=null && !RelicEnchantments.INSTANCE.validAccessory(stack)) result.setItem(0,ItemStack.EMPTY); }
}

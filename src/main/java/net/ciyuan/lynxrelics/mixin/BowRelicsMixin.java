// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.sugar.Local;
import net.ciyuan.lynxrelics.content.VanillaMods;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(BowItem.class)
public abstract class BowRelicsMixin {
    @ModifyArg(method="releaseUsing", at=@At(value="INVOKE", target="Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"), index=0)
    private int lynxrelics$draw(int ticks, @Local(argsOnly=true) ItemStack stack) { return VanillaMods.upgrade(stack).equals("quickdraw_string") ? (int)Math.floor(ticks/.9) : ticks; }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.Content;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemEntity.class)
public abstract class OreItemMixin {
    @Inject(method="hurtServer", at=@At("HEAD"), cancellable=true)
    private void lynxrelics$resistantDrop(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity item=(ItemEntity)(Object)this;
        if (source.is(DamageTypeTags.IS_EXPLOSION) && item.getItem().is(Content.INSTANCE.item("endmarrow_ore"))) cir.setReturnValue(false);
    }
}

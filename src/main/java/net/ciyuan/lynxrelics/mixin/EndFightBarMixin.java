// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ciyuan.lynxrelics.raid.DragonRaid;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(EnderDragonFight.class)
public abstract class EndFightBarMixin {
    @Inject(method="updateDragon", at=@At("TAIL"))
    private void lynxrelics$bar(EnderDragon dragon, CallbackInfo ci) { DragonRaid.updateBar((EnderDragonFight)(Object)this, dragon); }
    @ModifyExpressionValue(method="updateDragon", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;getDisplayName()Lnet/minecraft/network/chat/Component;"))
    private Component lynxrelics$title(Component original, @Local(argsOnly=true) EnderDragon dragon) { return DragonRaid.active(dragon) ? DragonRaid.title(dragon) : original; }
}

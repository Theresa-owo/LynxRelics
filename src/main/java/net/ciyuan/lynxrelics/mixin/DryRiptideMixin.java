// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ciyuan.lynxrelics.content.TridentHooks;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(TridentItem.class)
public abstract class DryRiptideMixin {
    @ModifyExpressionValue(method={"use", "releaseUsing"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean lynxrelics$dry(boolean original) { return TridentHooks.riptideWeather(original); }
}

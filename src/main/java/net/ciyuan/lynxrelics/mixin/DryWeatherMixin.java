// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ciyuan.lynxrelics.content.TridentHooks;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(WeatherCheck.class)
public abstract class DryWeatherMixin {
    @ModifyExpressionValue(method="test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;isThundering()Z"))
    private boolean lynxrelics$channelingOnly(boolean original){return TridentHooks.channelingWeather(original);}
}

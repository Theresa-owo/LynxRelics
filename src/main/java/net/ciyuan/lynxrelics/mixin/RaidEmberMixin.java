// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.raid.RaidSkills;
import net.minecraft.world.entity.monster.Endermite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Endermite.class)
public abstract class RaidEmberMixin {
    @Inject(method="tick",at=@At("TAIL")) private void lynxrelics$expiry(CallbackInfo ci){RaidSkills.emberTick((Endermite)(Object)this);}
}

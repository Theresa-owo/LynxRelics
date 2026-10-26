// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.raid.DragonHealthAttribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AttributeInstance.class)
public abstract class DragonHealthAttributeMixin implements DragonHealthAttribute, net.ciyuan.lynxrelics.raid.EncounterHealthAttribute {
    @Unique private double lynxrelics$healthOverride=Double.NaN;
    @Override public void lynxDragonHealth(boolean enabled) { lynxrelics$healthOverride=enabled?3000.0:Double.NaN; }
    @Override public void lynxEncounterHealth(double value) { lynxrelics$healthOverride=value; }
    @Inject(method="getValue", at=@At("RETURN"), cancellable=true)
    private void lynxrelics$fixedHealth(CallbackInfoReturnable<Double> cir) { if (!Double.isNaN(lynxrelics$healthOverride)) cir.setReturnValue(lynxrelics$healthOverride); }
}

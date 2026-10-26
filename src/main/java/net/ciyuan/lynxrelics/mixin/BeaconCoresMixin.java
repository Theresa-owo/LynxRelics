// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconCoresMixin implements BeaconCoreHolder {
    @Unique private final BeaconCoreState lynxrelics$core=new BeaconCoreState();
    public BeaconCoreState lynxBeaconCore(){return lynxrelics$core;}
    @Inject(method="saveAdditional",at=@At("TAIL"))private void lynxrelics$save(ValueOutput out,CallbackInfo ci){lynxrelics$core.save(out.child("LynxRelicsBeacon"));}
    @Inject(method="loadAdditional",at=@At("TAIL"))private void lynxrelics$load(ValueInput in,CallbackInfo ci){lynxrelics$core.load(in.childOrEmpty("LynxRelicsBeacon"));}
    @Inject(method="tick",at=@At("TAIL"))private static void lynxrelics$tick(Level level,BlockPos pos,BlockState state,BeaconBlockEntity beacon,CallbackInfo ci){BeaconCores.tick(beacon);}
}

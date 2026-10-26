// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.raid.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.boss.enderdragon.*;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(EnderDragon.class)
public abstract class DragonRaidMixin implements RaidHolder {
    @Unique private final RaidState lynxrelics$raid = new RaidState();
    @Override public RaidState lynxRaid() { return lynxrelics$raid; }
    @Inject(method="<clinit>", at=@At("TAIL"))
    private static void lynxrelics$bootstrap(CallbackInfo ci) { DragonRaid.bootstrap(); }
    @Inject(method="defineSynchedData", at=@At("TAIL"))
    private void lynxrelics$data(SynchedEntityData.Builder builder, CallbackInfo ci) { DragonRaid.define(builder); }
    @Inject(method="<init>", at=@At("TAIL"))
    private void lynxrelics$initialize(CallbackInfo ci) { DragonRaid.initialize((EnderDragon)(Object)this); }
    @Inject(method="onSyncedDataUpdated", at=@At("TAIL"))
    private void lynxrelics$sync(EntityDataAccessor<?> key, CallbackInfo ci) { if (key.equals(DragonRaid.ENABLED)) DragonRaid.markAttribute((EnderDragon)(Object)this); }
    @Inject(method="aiStep", at=@At("TAIL"))
    private void lynxrelics$tick(CallbackInfo ci) { DragonRaid.tick((EnderDragon)(Object)this); }
    @Inject(method={"knockBack(Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;)V","hurt(Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;)V"},at=@At("HEAD"),cancellable=true)
    private void lynxrelics$controlledContact(net.minecraft.server.level.ServerLevel level,java.util.List<net.minecraft.world.entity.Entity> entities,CallbackInfo ci){if(RaidSkills.controlsContact((EnderDragon)(Object)this))ci.cancel();}
    @Inject(method="checkWalls",at=@At("HEAD"),cancellable=true)
    private void lynxrelics$grief(net.minecraft.server.level.ServerLevel level,net.minecraft.world.phys.AABB box,CallbackInfoReturnable<Boolean> cir){if(!RaidSkills.allowGrief((EnderDragon)(Object)this))cir.setReturnValue(false);}
    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void lynxrelics$save(ValueOutput out, CallbackInfo ci) { DragonRaid.save((EnderDragon)(Object)this, out); }
    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void lynxrelics$load(ValueInput in, CallbackInfo ci) { DragonRaid.load((EnderDragon)(Object)this, in); }
    @Inject(method="tickDeath", at=@At("HEAD"))
    private void lynxrelics$loot(CallbackInfo ci) { DragonRaid.death((EnderDragon)(Object)this); }
    @Redirect(method="checkCrystals", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;setHealth(F)V"))
    private void lynxrelics$budget(EnderDragon dragon, float health) { if (!DragonRaid.owned(dragon.nearestCrystal)) dragon.setHealth(health); }
}

// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.content.TridentHooks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Enchantment.class)
public abstract class DryChannelingMixin {
    @WrapMethod(method="doPostAttack(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;Lnet/minecraft/world/item/enchantment/EnchantmentTarget;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V")
    private void lynxrelics$entity(ServerLevel level,int rank,EnchantedItemInUse item,EnchantmentTarget target,Entity victim,DamageSource source,Operation<Void> original){
        boolean previous=TridentHooks.enter((Enchantment)(Object)this,level);
        try{original.call(level,rank,item,target,victim,source);}finally{TridentHooks.leave(previous);}
    }
    @WrapMethod(method="onHitBlock")
    private void lynxrelics$block(ServerLevel level,int rank,EnchantedItemInUse item,Entity entity,Vec3 position,BlockState state,Operation<Void> original){
        boolean previous=TridentHooks.enter((Enchantment)(Object)this,level);
        try{original.call(level,rank,item,entity,position,state);}finally{TridentHooks.leave(previous);}
    }
}

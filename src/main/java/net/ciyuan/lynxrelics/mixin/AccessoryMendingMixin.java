// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.ciyuan.lynxrelics.core.RelicHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.*;
import java.util.function.Predicate;
@Mixin(EnchantmentHelper.class)
public abstract class AccessoryMendingMixin {
    @WrapOperation(method="getRandomItemWith",at=@At(value="INVOKE",target="Lnet/minecraft/util/Util;getRandomSafe(Ljava/util/List;Lnet/minecraft/util/RandomSource;)Ljava/util/Optional;"))
    private static Optional<EnchantedItemInUse> lynxrelics$mending(List<EnchantedItemInUse> items,RandomSource random,Operation<Optional<EnchantedItemInUse>> original,DataComponentType<?> effect,LivingEntity entity,Predicate<ItemStack> predicate){
        if(effect==EnchantmentEffectComponents.REPAIR_WITH_XP&&entity instanceof Player){var inventory=((RelicHolder)entity).lynxRelics().getAccessories();for(int i=0;i<4;i++){ItemStack stack=inventory.getItem(i);final int slot=i;if(predicate.test(stack)&&EnchantmentHelper.has(stack,effect))items.add(new EnchantedItemInUse(stack,EquipmentSlot.CHEST,entity,broken->inventory.setItem(slot,ItemStack.EMPTY)));}}
        // XP selects a single candidate once. These entries are only used for repair, never armor effects.
        return original.call(items,random);
    }
}

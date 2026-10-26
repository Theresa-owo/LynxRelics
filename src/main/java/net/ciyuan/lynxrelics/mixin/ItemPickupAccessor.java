// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.UUID;
@Mixin(ItemEntity.class)
public interface ItemPickupAccessor { @Accessor("target") UUID lynxPickupOwner(); }

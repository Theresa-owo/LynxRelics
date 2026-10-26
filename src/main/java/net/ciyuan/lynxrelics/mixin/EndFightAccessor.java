// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(EnderDragonFight.class)
public interface EndFightAccessor { @Accessor("dragonEvent") ServerBossEvent lynxDragonEvent(); }

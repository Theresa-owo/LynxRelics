// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Player.class)
public interface PlayerBlockInvoker {
    @Invoker("blockUsingItem") void lynxBlockReaction(ServerLevel level, LivingEntity attacker, DamageSource source, float amount, boolean blocked);
}

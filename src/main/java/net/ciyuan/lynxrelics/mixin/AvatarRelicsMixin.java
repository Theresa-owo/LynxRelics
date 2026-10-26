// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.client.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AvatarRenderer.class)
public abstract class AvatarRelicsMixin extends LivingEntityRenderer<Avatar,AvatarRenderState,PlayerModel> {
    @Unique private AccessoryLayer lynxrelics$layer;
    protected AvatarRelicsMixin(EntityRendererProvider.Context context,PlayerModel model,float shadow){super(context,model,shadow);}
    @Inject(method="<init>",at=@At("TAIL"))
    private void lynxrelics$layer(EntityRendererProvider.Context context,boolean slim,CallbackInfo ci){lynxrelics$layer=new AccessoryLayer(this);addLayer(lynxrelics$layer);}
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
    private void lynxrelics$snapshot(Avatar player,AvatarRenderState state,float tick,CallbackInfo ci){AccessorySnapshots.copy(player,state);}
    @Inject(method="renderRightHand",at=@At("TAIL"))
    private void lynxrelics$right(PoseStack poses,SubmitNodeCollector collector,int light,Identifier skin,boolean sleeves,CallbackInfo ci){if(lynxrelics$layer!=null)lynxrelics$layer.hand(poses,collector,light,false);}
    @Inject(method="renderLeftHand",at=@At("TAIL"))
    private void lynxrelics$left(PoseStack poses,SubmitNodeCollector collector,int light,Identifier skin,boolean sleeves,CallbackInfo ci){if(lynxrelics$layer!=null)lynxrelics$layer.hand(poses,collector,light,true);}
}

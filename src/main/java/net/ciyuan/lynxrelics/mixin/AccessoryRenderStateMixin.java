// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mixin;
import net.ciyuan.lynxrelics.client.AccessoryRenderData;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.*;
@Mixin(AvatarRenderState.class)
public abstract class AccessoryRenderStateMixin implements AccessoryRenderData {
    @Unique private final int[] lynxrelics$routes={-1,-1,-1,-1};
    @Unique private String lynxrelics$channel="";
    @Unique private boolean lynxrelics$gliding;
    public int[] lynxRoutes(){return lynxrelics$routes;}
    public String lynxChannel(){return lynxrelics$channel;}
    public void lynxSetChannel(String v){lynxrelics$channel=v;}
    public boolean lynxGliding(){return lynxrelics$gliding;}
    public void lynxSetGliding(boolean v){lynxrelics$gliding=v;}
}

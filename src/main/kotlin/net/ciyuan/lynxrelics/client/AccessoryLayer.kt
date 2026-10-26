// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.client
import com.google.gson.JsonParser
import com.mojang.blaze3d.vertex.PoseStack
import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.content.RelicItem
import net.ciyuan.lynxrelics.core.*
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.*
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.Avatar
import net.minecraft.world.entity.player.Player
import kotlin.math.sin

interface AccessoryRenderData {
    fun lynxRoutes():IntArray
    fun lynxChannel():String
    fun lynxSetChannel(value:String)
    fun lynxGliding():Boolean
    fun lynxSetGliding(value:Boolean)
}
object AccessorySnapshots {
    @JvmStatic fun copy(entity:Avatar,state:AvatarRenderState){
        val target=state as AccessoryRenderData;val routes=target.lynxRoutes();routes.fill(-1);target.lynxSetChannel("");target.lynxSetGliding(false)
        if(entity !is Player)return
        for(i in 0..3){val stack=entity.relics().accessories.getItem(i);val item=stack.item as? RelicItem;if(item?.slot?.ordinal==i&&!stack.isBroken)routes[i]=item.route?.ordinal?:-1}
        target.lynxSetChannel(entity.relics().channel);target.lynxSetGliding(entity.isFallFlying)
    }
}
class AccessoryLayer(parent:RenderLayerParent<AvatarRenderState,PlayerModel>):RenderLayer<AvatarRenderState,PlayerModel>(parent){
    private val models=listOf("necklace","belt","gauntlet","cloak").map(AccessoryModel::load)
    private val handState=AvatarRenderState()
    override fun submit(poses:PoseStack,collector:SubmitNodeCollector,light:Int,state:AvatarRenderState,yaw:Float,pitch:Float){
        if(state.isInvisible)return
        val data=state as AccessoryRenderData
        for(slot in 0..3){val route=data.lynxRoutes()[slot];if(route<0)continue;if(slot==3&&data.lynxGliding()&&Balance.enabled("render.hideCloakGliding"))continue
            poses.pushPose();if(slot==2)parentModel.rightArm.translateAndRotate(poses)else parentModel.body.translateAndRotate(poses)
            val model=models[slot];model.setupAnim(state);collector.order(0).submitModel(model,state,poses,Content.id("textures/entity/accessories/${listOf("dragon","echo","tide")[route]}.png"),light,OverlayTexture.NO_OVERLAY,-1);poses.popPose()
            if(slot==2){poses.pushPose();parentModel.leftArm.translateAndRotate(poses);poses.scale(-1f,1f,1f);collector.order(0).submitModel(model,state,poses,Content.id("textures/entity/accessories/${listOf("dragon","echo","tide")[route]}.png"),light,OverlayTexture.NO_OVERLAY,-1);poses.popPose()}
        }
    }
    fun hand(poses:PoseStack,collector:SubmitNodeCollector,light:Int,left:Boolean){
        val player=Minecraft.getInstance().player?:return
        AccessorySnapshots.copy(player,handState)
        val route=(handState as AccessoryRenderData).lynxRoutes()[2];if(route<0||player.isInvisible)return
        handState.ageInTicks=player.tickCount.toFloat()
        poses.pushPose();if(left){parentModel.leftArm.translateAndRotate(poses);poses.scale(-1f,1f,1f)}else parentModel.rightArm.translateAndRotate(poses)
        models[2].setupAnim(handState);collector.order(0).submitModel(models[2],handState,poses,Content.id("textures/entity/accessories/${listOf("dragon","echo","tide")[route]}.png"),light,OverlayTexture.NO_OVERLAY,-1);poses.popPose()
    }
}
class AccessoryModel(root:ModelPart,private val bones:Map<String,ModelPart>):EntityModel<AvatarRenderState>(root){
    override fun setupAnim(state:AvatarRenderState){super.setupAnim(state);root().allParts.forEach{it.resetPose()};val data=state as AccessoryRenderData
        bones["jaw"]?.xRot=if(data.lynxChannel()=="breath")-.3f else 0f
        val wave=sin(state.ageInTicks*.1f)*.035f
        bones["wing_left"]?.visible=true;bones["wing_right"]?.visible=true
        bones["wing_left"]?.yRot=.15f+wave;bones["wing_right"]?.yRot=-.15f-wave
        if(state.showCape&&state.skin.cape()!=null){bones["wing_left"]?.visible=false;bones["wing_right"]?.visible=false}
    }
    companion object{
        fun load(family:String):AccessoryModel{
            val resource=Minecraft.getInstance().resourceManager.getResourceOrThrow(Content.id("geometry/accessory_$family.json"));val json=resource.openAsReader().use{JsonParser.parseReader(it).asJsonObject};val mesh=MeshDefinition();val defs=mutableMapOf("root" to mesh.root);val pivots=mutableMapOf("root" to floatArrayOf(0f,0f,0f))
            for(v in json.getAsJsonArray("bones")){val b=v.asJsonObject;val name=b["name"].asString;val parent=b["parent"].asString;val p=b.getAsJsonArray("pivot").map{it.asFloat}.toFloatArray();val pp=pivots.getValue(parent);val cubes=CubeListBuilder.create();for(cv in b.getAsJsonArray("cubes")){val c=cv.asJsonObject;val a=c.getAsJsonArray("from").map{it.asFloat};val z=c.getAsJsonArray("to").map{it.asFloat};val uv=c.getAsJsonArray("uv");cubes.texOffs(uv[0].asInt,uv[1].asInt).addBox(a[0]-p[0],a[1]-p[1],a[2]-p[2],z[0]-a[0],z[1]-a[1],z[2]-a[2])};defs[name]=defs.getValue(parent).addOrReplaceChild(name,cubes,PartPose.offset(p[0]-pp[0],p[1]-pp[1],p[2]-pp[2]));pivots[name]=p}
            val root=LayerDefinition.create(mesh,64,64).bakeRoot();val parts=mutableMapOf("root" to root);for(v in json.getAsJsonArray("bones")){val b=v.asJsonObject;parts[b["name"].asString]=parts.getValue(b["parent"].asString).getChild(b["name"].asString)};return AccessoryModel(root,parts)
        }
    }
}

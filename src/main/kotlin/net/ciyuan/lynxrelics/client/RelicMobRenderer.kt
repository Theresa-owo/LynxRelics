// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.client
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.mob.*
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.*
import net.minecraft.client.model.geom.builders.*
import net.minecraft.client.renderer.entity.*
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.Identifier
import kotlin.math.*

class RelicMobRenderState:LivingEntityRenderState(){var action=0;var phase=1;var kind=RelicMobKind.WISP;var actionTime=0f}
class RelicMobRenderer(context:EntityRendererProvider.Context,private val kind:RelicMobKind):MobRenderer<RelicMob,RelicMobRenderState,RelicMobModel>(context,RelicMobModel.load(kind),kind.width*.4f){
    override fun createRenderState()=RelicMobRenderState()
    override fun getTextureLocation(state:RelicMobRenderState):Identifier=Content.id("textures/entity/${kind.id}.png")
    override fun extractRenderState(entity:RelicMob,state:RelicMobRenderState,partialTick:Float){super.extractRenderState(entity,state,partialTick);state.action=entity.entityData.get(RelicMob.ACTION);state.phase=entity.entityData.get(RelicMob.PHASE);state.kind=kind;state.actionTime=entity.animationAge+partialTick}
    override fun getBlockLightLevel(entity:RelicMob,pos:net.minecraft.core.BlockPos)=if(kind==RelicMobKind.JELLY)12 else super.getBlockLightLevel(entity,pos)
}
class RelicMobModel(root:ModelPart,private val bones:Map<String,ModelPart>,private val document:JsonObject):EntityModel<RelicMobRenderState>(root){
    override fun setupAnim(state:RelicMobRenderState){
        super.setupAnim(state)
        root().allParts.forEach{it.resetPose()}
        val time=state.ageInTicks/20.0
        val name=if(state.action==1)"windup"else if(state.action==2)"strike"else"idle"
        val animation=document.getAsJsonArray("animations").first{it.asJsonObject["name"].asString==name}.asJsonObject
        val length=animation["length"].asDouble
        val local=if(animation["loop"].asBoolean)time%length else minOf(length,state.actionTime/20.0)
        for(trackValue in animation.getAsJsonArray("tracks")){
            val track=trackValue.asJsonObject;val part=bones[track["bone"].asString]?:continue;val values=track.getAsJsonArray("values");val t=(local/length*4).coerceIn(0.0,3.99999);val index=floor(t).toInt();val v=(values[index].asDouble*(1-(t-index))+values[index+1].asDouble*(t-index)).toFloat()
            if(track["channel"].asString=="position")when(track["axis"].asString){"x"->part.x+=v;"y"->part.y+=v;else->part.z+=v}
            else when(track["axis"].asString){"x"->part.xRot+=Math.toRadians(v.toDouble()).toFloat();"y"->part.yRot+=Math.toRadians(v.toDouble()).toFloat();else->part.zRot+=Math.toRadians(v.toDouble()).toFloat()}
        }
        val walk=state.walkAnimationPos;val speed=state.walkAnimationSpeed.coerceAtMost(1f)
        bones.filterKeys{it.startsWith("leg_")}.entries.forEachIndexed{i,(_,part)->part.xRot+=cos(walk*.7f+i%2*PI).toFloat()*.6f*speed}
        if(state.kind==RelicMobKind.JELLY)bones["body"]?.let{val pulse=sin(state.ageInTicks*.12f)*.06f;it.xScale=1+pulse;it.zScale=1+pulse;it.yScale=1-pulse}
    }
    companion object{
        fun load(kind:RelicMobKind):RelicMobModel{
            val resource=Minecraft.getInstance().resourceManager.getResourceOrThrow(Content.id("geometry/${kind.id}.json"))
            val json=resource.openAsReader().use{JsonParser.parseReader(it).asJsonObject}
            val mesh=MeshDefinition();val parts=mutableMapOf<String,PartDefinition>("root" to mesh.root);val pivots=mutableMapOf("root" to floatArrayOf(0f,0f,0f))
            for(value in json.getAsJsonArray("bones")){
                val b=value.asJsonObject;val name=b["name"].asString;val parent=b["parent"].asString;val pivot=b.getAsJsonArray("pivot").map{it.asFloat}.toFloatArray();val parentPivot=pivots.getValue(parent);val cubes=CubeListBuilder.create()
                for(cubeValue in b.getAsJsonArray("cubes")){val c=cubeValue.asJsonObject;val from=c.getAsJsonArray("from").map{it.asFloat};val to=c.getAsJsonArray("to").map{it.asFloat};val uv=c.getAsJsonArray("uv");cubes.texOffs(uv[0].asInt,uv[1].asInt).addBox(from[0]-pivot[0],from[1]-pivot[1],from[2]-pivot[2],to[0]-from[0],to[1]-from[1],to[2]-from[2])}
                parts[name]=parts.getValue(parent).addOrReplaceChild(name,cubes,PartPose.offset(pivot[0]-parentPivot[0],pivot[1]-parentPivot[1],pivot[2]-parentPivot[2]));pivots[name]=pivot
            }
            val size=json["textureSize"].asInt;val root=LayerDefinition.create(mesh,size,size).bakeRoot();val baked=mutableMapOf<String,ModelPart>("root" to root)
            for(value in json.getAsJsonArray("bones")){val b=value.asJsonObject;baked[b["name"].asString]=baked.getValue(b["parent"].asString).getChild(b["name"].asString)}
            return RelicMobModel(root,baked,json)
        }
    }
}

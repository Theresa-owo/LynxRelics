// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.raid

import net.ciyuan.lynxrelics.core.*
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.PowerParticleOption
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import kotlin.math.*

/** Drives the original dragon's native flight phases. No replacement entity or teleport charge. */
object RaidSkills {
    private fun state(dragon:EnderDragon)=(dragon as RaidHolder).lynxRaid()
    private fun ticks(key:String)=Balance.ticks(key).coerceAtLeast(1)
    fun releaseFlight(dragon:EnderDragon,s:RaidState){
        val phase=dragon.phaseManager.currentPhase.phase
        val legacyHover=phase==EnderDragonPhase.HOVERING&&(s.phase>=2||s.skill.isNotEmpty())
        if(dragon.isAlive&&(s.flightControlled&&phase==EnderDragonPhase.CHARGING_PLAYER||legacyHover))dragon.phaseManager.setPhase(EnderDragonPhase.HOLDING_PATTERN)
        s.flightControlled=false
    }
    fun cancel(dragon:EnderDragon,s:RaidState){
        releaseFlight(dragon,s)
        s.skill="";s.started=false;s.combo=-1;s.hitPlayers.clear();s.comboGuards.clear();s.comboSegments.clear()
    }
    @JvmStatic fun controlsContact(dragon:EnderDragon)=DragonRaid.active(dragon)&&(state(dragon).skill.isNotEmpty()||state(dragon).transitionUntil>(dragon.level().server?.overworld()?.gameTime?:0))
    @JvmStatic fun allowGrief(dragon:EnderDragon)=!DragonRaid.active(dragon)||Balance.enabled("raid.vanillaGriefing")&&!controlsContact(dragon)
    fun tick(dragon:EnderDragon,now:Long){
        val s=state(dragon);val level=dragon.level() as ServerLevel
        if(s.skill.isEmpty()){
            val nativePhase=dragon.phaseManager.currentPhase.phase
            if(now<s.nextSkill||nativePhase !in setOf(EnderDragonPhase.HOLDING_PATTERN,EnderDragonPhase.STRAFE_PLAYER,EnderDragonPhase.HOVERING))return
            val target=DragonRaid.participants(dragon).minByOrNull{it.distanceToSqr(dragon)}?:return
            val choices=when(s.phase){1->listOf("swoop","wing","breath");2->listOf("mark","swoop","breath","rifts","wing");else->listOf("combo","mark","breath","wing")}
            s.skill=if(now-s.lastEmbers>=ticks("raid.emberCooldownTicks")&&s.embers.size<4)"embers"else choices[Math.floorMod(s.skillNumber,choices.size)]
            s.skillNumber++;s.targetPlayer=target.uuid.toString();s.combo=if(s.skill=="combo")0 else -1
            if(s.combo==0)s.skill="swoop"
            s.comboGuards.clear();s.comboSegments.clear();prepare(dragon,s,target.position(),now)
        }
        if(now<s.warningFrom)return
        if(now<s.skillAt){warn(dragon,s,now);return}
        val duration=when(s.skill){"swoop"->ticks("raid.swoopActiveTicks");"breath"->if(s.combo==1)30 else ticks("raid.breathActiveTicks");"leave"->60;else->1}
        if(now>=s.skillAt+duration){finish(dragon,s,now);return}
        if(!s.started||s.restorePhase){
            s.started=true;s.restorePhase=false;s.previousHead=dragon.head.position()
            if(s.skill=="swoop"||s.skill=="leave"){
                s.flightControlled=true
                dragon.phaseManager.setPhase(EnderDragonPhase.CHARGING_PLAYER)
                dragon.phaseManager.getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(s.skillTarget)
            }else releaseFlight(dragon,s)
        }
        when(s.skill){
            "swoop"->{
                val head=dragon.head.position()
                for(p in DragonRaid.participants(dragon))if(intersects(p,s.previousHead,head,2.25)&&visible(level,head,p.eyePosition,dragon))hit(dragon,s,p,Balance.value("raid.swoopDamage").toFloat(),head,now,true)
                s.previousHead=head
                if(now-s.skillAt>10&&dragon.phaseManager.currentPhase.phase!=EnderDragonPhase.CHARGING_PLAYER)finish(dragon,s,now)
            }
            "wing"->{
                for(p in DragonRaid.participants(dragon))if(p.distanceToSqr(dragon)<=Balance.value("raid.wingRange").pow(2)&&dragon.hasLineOfSight(p))hit(dragon,s,p,Balance.value("raid.wingDamage").toFloat(),dragon.position(),now,true)
                finish(dragon,s,now)
            }
            "mark"->{
                for(p in DragonRaid.participants(dragon))if(p.distanceToSqr(s.skillTarget)<=Balance.value("raid.markRadius").pow(2)&&visible(level,s.skillTarget.add(0.0,.5,0.0),p.eyePosition,dragon))hit(dragon,s,p,Balance.value("raid.markDamage").toFloat(),s.skillTarget,now)
                level.sendParticles(ParticleTypes.EXPLOSION,s.skillTarget.x,s.skillTarget.y+.2,s.skillTarget.z,2,.4,.2,.4,.0);finish(dragon,s,now)
            }
            "rifts"->{
                for((a,b)in riftLines(dragon,s))for(p in DragonRaid.participants(dragon))if(intersects(p,a,b,.8)&&abs(p.y-a.y)<3)hit(dragon,s,p,Balance.value("raid.markDamage").toFloat(),a,now)
                finish(dragon,s,now)
            }
            "breath"->{
                val from=dragon.head.boundingBox.center
                val target=level.server.playerList.getPlayer(runCatching{java.util.UUID.fromString(s.targetPlayer)}.getOrNull()?:java.util.UUID(0,0))
                if(s.phase>=2&&target!=null&&target.level()===level&&target.isAlive){
                    val desired=target.eyePosition.subtract(from).normalize();val angle=acos(s.skillDirection.dot(desired).coerceIn(-1.0,1.0))
                    val step=Math.toRadians(Balance.value("raid.breathTurnDegrees"));s.skillDirection=s.skillDirection.lerp(desired,if(angle<.0001)1.0 else min(1.0,step/angle)).normalize()
                }
                val end=clipped(level,from,from.add(s.skillDirection.scale(Balance.value("raid.breathRange"))),dragon)
                if(now%2==0L)line(level,from,end,true)
                if((now-s.skillAt)%ticks("raid.breathPulseTicks")==0L){
                    s.hitPlayers.clear()
                    for(p in DragonRaid.participants(dragon))if(intersects(p,from,end,Balance.value("raid.breathRadius")))hit(dragon,s,p,Balance.value("raid.breathDamage").toFloat(),from,now,pulse=true)
                }
            }
            "embers"->{spawnEmbers(dragon,s,now);finish(dragon,s,now)}
        }
    }
    private fun prepare(dragon:EnderDragon,s:RaidState,target:Vec3,now:Long,recovery:Int=0){
        s.skillOrigin=dragon.head.position();s.skillTarget=target;s.skillDirection=target.add(0.0,1.0,0.0).subtract(dragon.head.boundingBox.center).normalize()
        s.started=false;s.restorePhase=false;s.hitPlayers.clear();s.warningFrom=now+recovery
        val delay=when(s.skill){"swoop"->ticks("raid.swoopWarningTicks");"wing"->ticks("raid.wingWarningTicks");"breath"->ticks("raid.breathWarningTicks");"mark","rifts"->ticks("raid.markWarningTicks");"leave"->24;else->40}
        s.skillAt=s.warningFrom+delay;releaseFlight(dragon,s)
        val label=Component.translatable("boss.lynxrelics.skill.${s.skill}")
        val display=if(s.combo>=0)Component.translatable("boss.lynxrelics.combo_warning",s.combo+1,label)else label
        for(p in DragonRaid.participants(dragon))p.sendOverlayMessage(Component.translatable("boss.lynxrelics.skill_warning",display,delay/20.0))
    }
    private fun warn(dragon:EnderDragon,s:RaidState,now:Long){
        if(now%4!=0L)return
        val level=dragon.level() as ServerLevel
        when(s.skill){
            "swoop","leave"->line(level,dragon.head.position(),s.skillTarget)
            "breath"->{val from=dragon.head.boundingBox.center;line(level,from,clipped(level,from,from.add(s.skillDirection.scale(Balance.value("raid.breathRange"))),dragon))}
            "rifts"->for((a,b)in riftLines(dragon,s))line(level,a,b)
            else->{val center=if(s.skill=="wing")dragon.position()else s.skillTarget;val radius=if(s.skill=="wing")Balance.value("raid.wingRange")else Balance.value("raid.markRadius");for(i in 0..24){val angle=i*PI/12;level.sendParticles(ParticleTypes.END_ROD,true,true,center.x+cos(angle)*radius,center.y+.2,center.z+sin(angle)*radius,1,0.0,0.0,0.0,0.0)}}
        }
    }
    private fun finish(dragon:EnderDragon,s:RaidState,now:Long){
        val recovery=ticks("raid.recoveryTicks")
        if(s.combo==0){s.combo=1;s.skill="breath";prepare(dragon,s,s.skillTarget,now,recovery);return}
        if(s.combo==1){s.combo=2;s.skill="leave";val away=dragon.position().subtract(s.skillTarget).multiply(1.0,0.0,1.0).normalize();prepare(dragon,s,dragon.position().add(away.scale(32.0)).add(0.0,20.0,0.0),now,recovery);return}
        cancel(dragon,s);s.nextSkill=now+recovery+ticks(if(s.phase==3)"raid.phase3SkillGapTicks"else"raid.phase12SkillGapTicks")
    }
    private fun hit(dragon:EnderDragon,s:RaidState,p:ServerPlayer,damage:Float,center:Vec3,now:Long,push:Boolean=false,pulse:Boolean=false){
        val id=p.uuid.toString()
        if(s.combo>=0&&(s.comboGuards[id]?:0)>now&&s.comboSegments[id]!=s.combo)return
        if(!s.hitPlayers.add(id))return
        val level=dragon.level() as ServerLevel
        val hurt=DamageContext.with(DamageOrigin.SKILL){p.hurtServer(level,if(pulse)RelicDamage.pulse(level,dragon)else level.damageSources().mobAttack(dragon),damage*Balance.value("raid.skillDamageMultiplier").toFloat())}
        if(hurt&&s.combo>=0){s.comboGuards[id]=now+20;s.comboSegments[id]=s.combo}
        if(hurt&&push){val away=p.position().subtract(center).multiply(1.0,0.0,1.0).normalize().scale(.35);p.push(away.x,.12,away.z)}
    }
    private fun riftLines(dragon:EnderDragon,s:RaidState):List<Pair<Vec3,Vec3>>{
        val direction=s.skillTarget.subtract(s.skillOrigin).multiply(1.0,0.0,1.0).normalize();val from=s.skillTarget.add(0.0,.25,0.0)
        return listOf(-.45,0.0,.45).map{angle->val d=Vec3(direction.x*cos(angle)-direction.z*sin(angle),0.0,direction.x*sin(angle)+direction.z*cos(angle));from to clipped(dragon.level() as ServerLevel,from,from.add(d.scale(12.0)),dragon)}
    }
    private fun intersects(player:ServerPlayer,a:Vec3,b:Vec3,radius:Double)=player.boundingBox.inflate(radius).let{it.contains(a)||it.clip(a,b).isPresent}
    private fun visible(level:ServerLevel,a:Vec3,b:Vec3,dragon:EnderDragon)=clipped(level,a,b,dragon).distanceToSqr(b)<.01
    private fun clipped(level:ServerLevel,a:Vec3,b:Vec3,dragon:EnderDragon)=level.clip(ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,dragon)).location
    private fun line(level:ServerLevel,a:Vec3,b:Vec3,breath:Boolean=false){
        val count=ceil(a.distanceTo(b)/2).toInt().coerceIn(1,32)
        for(i in 0..count){val p=a.lerp(b,i.toDouble()/count);if(breath)level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1f),true,true,p.x,p.y,p.z,2,.15,.15,.15,.01)else level.sendParticles(ParticleTypes.END_ROD,true,true,p.x,p.y,p.z,1,0.0,0.0,0.0,0.0)}
    }
    private fun spawnEmbers(dragon:EnderDragon,s:RaidState,now:Long){
        val level=dragon.level() as ServerLevel
        for(i in s.embers.size until 4){
            val mob=EntityTypes.ENDERMITE.create(level,EntitySpawnReason.MOB_SUMMONED)?:continue
            mob.setPos(s.skillTarget.add(cos(i*PI/2)*3,0.0,sin(i*PI/2)*3));mob.getAttribute(Attributes.MAX_HEALTH)?.baseValue=Balance.value("raid.emberHealth");mob.health=mob.maxHealth;mob.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue=Balance.value("raid.emberDamage")
            mob.customName=Component.translatable("entity.lynxrelics.raid_ember");mob.addTag("lynx_raid_ember_${dragon.uuid}");mob.addTag("lynx_ember_expiry_${now+ticks("raid.emberLifetimeTicks")}")
            if(level.noCollision(mob)&&level.addFreshEntity(mob))s.embers.add(mob.uuid.toString())
        }
        s.lastEmbers=now
    }
    @JvmStatic fun emberTick(mob:net.minecraft.world.entity.monster.Endermite){
        val level=mob.level() as? ServerLevel?:return
        val owner=mob.entityTags().firstOrNull{it.startsWith("lynx_raid_ember_")}?.removePrefix("lynx_raid_ember_")?:return
        val dragon=runCatching{level.getEntity(java.util.UUID.fromString(owner)) as? EnderDragon}.getOrNull()
        val expiry=mob.entityTags().firstOrNull{it.startsWith("lynx_ember_expiry_")}?.removePrefix("lynx_ember_expiry_")?.toLongOrNull()?:0
        if(level.server.overworld().gameTime>=expiry||dragon!=null&&!dragon.isAlive){dragon?.let{state(it).embers.remove(mob.uuid.toString())};mob.discard()}
    }
    fun emberDied(mob:net.minecraft.world.entity.LivingEntity){
        if(mob !is net.minecraft.world.entity.monster.Endermite)return
        val level=mob.level() as? ServerLevel?:return
        val owner=mob.entityTags().firstOrNull{it.startsWith("lynx_raid_ember_")}?.removePrefix("lynx_raid_ember_")?:return
        val dragon=runCatching{level.getEntity(java.util.UUID.fromString(owner)) as? EnderDragon}.getOrNull()?:return
        state(dragon).embers.remove(mob.uuid.toString())
    }
}

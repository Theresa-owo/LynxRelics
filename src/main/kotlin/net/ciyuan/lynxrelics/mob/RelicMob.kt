// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mob

import net.ciyuan.lynxrelics.content.Content
import net.ciyuan.lynxrelics.core.*
import net.ciyuan.lynxrelics.raid.EncounterHealthAttribute
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.PowerParticleOption
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.*
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.BossEvent
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.*

open class RelicMob(type:EntityType<out RelicMob>,level:Level):PathfinderMob(type,level){
    val kind get()=RelicMobKind.entries.first{it.id==BuiltInRegistries.ENTITY_TYPE.getKey(type).path}
    var ownerId:UUID?=null
    var productStock=0;private set
    var fedUntil=0L
    var productionTicks=0
    var behavior=0
    var calmedUntil=0L
    var calmInterruptedUntil=0L
    private var pickupReset=0L
    private var pickupCount=0
    private val picked=mutableSetOf<UUID>()
    private var home=BlockPos.ZERO
    private var hasHome=false
    private var nextAttack=0L
    private var actAt=0L
    private var recoverUntil=0L
    private var attack=""
    private var aim=Vec3.ZERO
    private var lastParticipant=0L
    private var openUntil=0L
    private var sameHits=0
    private var lastCategory=""
    private var resistUntil=0L
    private var rewarded=false
    private var encounterLocked=false
    private var stingUntil=0L
    private var firedPulses=0
    private var attackNumber=0
    private val attackVictims=mutableSetOf<UUID>()
    private var registeredOwner=false
    var animationAge=0f;private set
    private val bossBar by lazy{ServerBossEvent(uuid,Component.translatable("entity.lynxrelics.brood_drake"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS)}
    init{if(!level.isClientSide){entityData.set(MAX_HP,maxHealth);if(kind==RelicMobKind.BROOD)overrideHealth(maxHealth)}}
    override fun defineSynchedData(builder:SynchedEntityData.Builder){super.defineSynchedData(builder);builder.define(ACTION,0);builder.define(PHASE,1);builder.define(MAX_HP,0f);builder.define(STOCK,0)}
    override fun onSyncedDataUpdated(key:EntityDataAccessor<*>){super.onSyncedDataUpdated(key);if(key==ACTION)animationAge=0f;if(key==MAX_HP&&kind==RelicMobKind.BROOD&&entityData.get(MAX_HP)>0)overrideHealth(entityData.get(MAX_HP))}
    override fun tick(){super.tick();if(level().isClientSide)animationAge++}
    private fun overrideHealth(value:Float){getAttribute(Attributes.MAX_HEALTH)?.let{(it as EncounterHealthAttribute).lynxEncounterHealth(value.toDouble());it.baseValue=value.toDouble()}}
    fun lockEncounter(players:Int){if(kind!=RelicMobKind.BROOD||encounterLocked)return;val hp=(Balance.value("mob.brood_drake.health")*(1+Balance.value("mob.broodExtraHealthPerPlayer")*(players.coerceIn(1,Balance.ticks("mob.broodPartyCap"))-1))).toFloat();entityData.set(MAX_HP,hp);overrideHealth(hp);health=hp;encounterLocked=true}
    override fun registerGoals(){goalSelector.addGoal(0,FloatGoal(this));goalSelector.addGoal(7,LookAtPlayerGoal(this,Player::class.java,8f));goalSelector.addGoal(8,RandomLookAroundGoal(this))}
    override fun customServerAiStep(level:ServerLevel){
        super.customServerAiStep(level);if(isNoAi||!Balance.enabled("${kind.routeKey}.enabled"))return
        val now=level.server.overworld().gameTime
        if(!hasHome){home=blockPosition();hasHome=true;lastParticipant=now}
        setNoGravity(kind.flying||kind==RelicMobKind.JELLY&&isInWater)
        if(kind==RelicMobKind.JELLY)airSupply=maxAirSupply
        if(kind.hostile)hostile(level,now)else friendly(level,now)
        if(kind==RelicMobKind.BROOD){bossBar.progress=(health/maxHealth).coerceIn(0f,1f);bossBar.name=Component.translatable("boss.lynxrelics.brood.status",ceil(health).toInt(),ceil(maxHealth).toInt(),entityData.get(PHASE))}
    }
    private fun friendly(level:ServerLevel,now:Long){
        val calm=calmedUntil>now
        if(kind==RelicMobKind.WISP&&!registeredOwner&&ownerId!=null){if(!Companions.get(level.server).register(ownerId!!,uuid)){discard();return};registeredOwner=true}
        if(fedUntil>now&&productStock<Balance.ticks("mob.stockLimit")){productionTicks++;val required=Balance.ticks(when(kind){RelicMobKind.WISP->"mob.wispProductionTicks";RelicMobKind.MOTH->"mob.mothProductionTicks";else->"mob.jellyProductionTicks"}).coerceAtLeast(1);if(productionTicks>=required){productStock++;productionTicks=0;entityData.set(STOCK,productStock);entityData.set(ACTION,6);level.sendParticles(ParticleTypes.HAPPY_VILLAGER,x,y+bbHeight,z,6,.2,.2,.2,.02)}}
        if(kind==RelicMobKind.WISP){
            val owner=ownerId?.let{level.server.playerList.getPlayer(it)}
            if(owner!=null&&owner.level()===level&&behavior==0&&distanceToSqr(owner)>(if(calm)4 else 9)&&distanceToSqr(owner)<32*32)flyToward(owner.position().add(0.0,1.0,0.0),if(calm).10 else .12)
            if(owner!=null&&owner.level()===level&&now%20==0L){
                if(now>=pickupReset){pickupCount=0;picked.clear();pickupReset=now+1200}
                if(pickupCount<Balance.ticks("mob.wispPickupPerMinute"))for(item in level.getEntitiesOfClass(ItemEntity::class.java,boundingBox.inflate(4.0))){
                    if(item.uuid in picked||item.hasPickUpDelay()||!hasLineOfSight(item))continue
                    val count=item.item.count;item.playerTouch(owner)
                    if(item.isRemoved||item.item.count<count){picked.add(item.uuid);pickupCount++;if(pickupCount>=Balance.ticks("mob.wispPickupPerMinute"))break}
                }
            }
        }else if(kind==RelicMobKind.MOTH){
            val mark=if(fedUntil>now)Investigations.closest(level,position(),32.0)else null
            if(mark!=null)flyToward(mark.add(0.0,.5,0.0),.08)
            else if(now%(if(calm)80 else 40)==0L)flyToward(Vec3.atCenterOf(home).add(random.nextDouble()*(if(calm)2 else 6)-(if(calm)1 else 3),random.nextDouble()*2,random.nextDouble()*(if(calm)2 else 6)-(if(calm)1 else 3)),if(calm).025 else .04)
        }else if(kind==RelicMobKind.JELLY&&isInWater){if(now%40==0L)deltaMovement=Vec3((random.nextDouble()-.5)*(if(calm).025 else .06),.025,(random.nextDouble()-.5)*(if(calm).025 else .06))}
        if(now%80==0L&&entityData.get(ACTION)==6)entityData.set(ACTION,0)
    }
    private fun flyToward(point:Vec3,speed:Double){val desired=point.subtract(position()).normalize().scale(speed);val next=position().add(desired);if(level().noCollision(this,boundingBox.move(desired)))deltaMovement=deltaMovement.scale(.7).add(desired.scale(.3))}
    private fun hostile(level:ServerLevel,now:Long){
        val players=level.players().filter{it.isAlive&&!it.isCreative&&!it.isSpectator&&!it.isAlliedTo(this)&&distanceToSqr(it)<48*48}
        if(players.isNotEmpty())lastParticipant=now
        if(kind in setOf(RelicMobKind.KEEPER,RelicMobKind.BROOD)&&now-lastParticipant>Balance.ticks("mob.bossEmptyTicks")){discard();return}
        if(kind==RelicMobKind.REEF&&(position().distanceToSqr(Vec3.atCenterOf(home))>24*24||abs(y-home.y)>8)){target=null;navigation.moveTo(home.x+.5,home.y.toDouble(),home.z+.5,1.0);return}
        val victim=players.filter{hasLineOfSight(it)}.minByOrNull{distanceToSqr(it)}
        if(victim==null){target=null;if(kind==RelicMobKind.HUNTER)setInvisible(false);return}
        target=victim;lookControl.setLookAt(victim,30f,30f)
        val phase=if(kind==RelicMobKind.BROOD){if(health/maxHealth>.65)1 else if(health/maxHealth>.30)2 else 3}else if(kind==RelicMobKind.KEEPER&&health/maxHealth<=.5)2 else 1
        if(phase>entityData.get(PHASE)){entityData.set(PHASE,phase);recoverUntil=now+40;attack="";entityData.set(ACTION,3)}
        if(now<recoverUntil){navigation.stop();return}
        if(attack.isEmpty()){
            if(distanceToSqr(victim)>if(kind==RelicMobKind.BROOD)100 else 25){navigation.moveTo(victim,if(kind==RelicMobKind.HUNTER)1.2 else .9);if(kind==RelicMobKind.HUNTER)setInvisible(distanceToSqr(victim)>36);return}
            navigation.stop();if(now<nextAttack)return
            setInvisible(false);aim=victim.position();firedPulses=0;attackVictims.clear()
            attack=when(kind){RelicMobKind.HUNTER->if(distanceToSqr(victim)>9)"pounce" else "claw";RelicMobKind.REEF->if(attackNumber%2==0)"claw" else "wave";RelicMobKind.KEEPER->if(attackNumber%2==0)"claw" else "sweep";RelicMobKind.BROOD->when(phase){1->if(attackNumber%2==0)"claw"else"charge";2->if(attackNumber%2==0)"rift"else"sweep";else->if(attackNumber%2==0)"breath" else "claw"};else->"claw"};attackNumber++
            val warning=when(attack){"pounce"->14;"sweep","rift"->20;"wave","breath"->24;else->when(kind){RelicMobKind.KEEPER->12;RelicMobKind.REEF->20;else->16}}
            actAt=now+warning;entityData.set(ACTION,1)
        }
        if(now<actAt){if(now%4==0L)warn(level);return}
        entityData.set(ACTION,2)
        when(attack){
            "charge"->{moveSafe(aim.subtract(position()).multiply(1.0,0.0,1.0).normalize(),.5);for(p in level.players())if(validVictim(p)&&distanceToSqr(p)<6.25&&attackVictims.add(p.uuid))hurtVictim(level,p,getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat());if(now-actAt>=8)finish(now,20,80)}
            "pounce"->{moveSafe(aim.subtract(position()).multiply(1.0,0.0,1.0).normalize(),4.0);strike(level,9f,2.5,false);finish(now,20,80)}
            "sweep"->{strike(level,12f,4.0,false);finish(now,20,if(phase==2)48 else 60)}
            "wave"->{strike(level,10f,5.0,false);finish(now,20,80);openUntil=now+30}
            "rift"->{for(i in -1..1){val angle=yRot+i*25.0;val end=position().add(-sin(Math.toRadians(angle))*8,0.0,cos(Math.toRadians(angle))*8);for(p in level.players())if(validVictim(p)&&distanceSegment(p.position(),position(),end)<1.2)hurtVictim(level,p,12f)};finish(now,24,80)}
            "breath"->{if((now-actAt)%8==0L&&firedPulses<5){for(p in level.players())if(validVictim(p)&&distanceToSqr(p)<81&&p.position().subtract(position()).normalize().dot(aim.subtract(position()).normalize())>.75)DamageContext.with(DamageOrigin.SKILL){p.hurtServer(level,RelicDamage.pulse(level,this),4f)};level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH,1f),true,true,x,eyeY,z,12,.5,.4,.5,.02);firedPulses++};if(now-actAt>=40)finish(now,24,100)}
            else->{strike(level,getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat(),if(kind==RelicMobKind.BROOD)4.0 else 2.6,true);if(kind==RelicMobKind.REEF)openUntil=now+30;finish(now,20,when(kind){RelicMobKind.HUNTER->44;RelicMobKind.REEF->60;else->if(phase==2)40 else 60})}
        }
    }
    private fun validVictim(player:ServerPlayer)=player.isAlive&&!player.isCreative&&!player.isSpectator&&!isAlliedTo(player)&&hasLineOfSight(player)
    private fun hurtVictim(level:ServerLevel,p:ServerPlayer,amount:Float){DamageContext.with(DamageOrigin.SKILL){p.hurtServer(level,level.damageSources().mobAttack(this),amount)}}
    private fun strike(level:ServerLevel,amount:Float,range:Double,front:Boolean){for(p in level.players())if(validVictim(p)&&distanceToSqr(p)<=range*range&&(!front||p.position().subtract(position()).multiply(1.0,0.0,1.0).normalize().dot(lookAngle.multiply(1.0,0.0,1.0).normalize())>.3))hurtVictim(level,p,amount)}
    private fun finish(now:Long,recovery:Int,cooldown:Int){attack="";recoverUntil=now+recovery;nextAttack=now+cooldown;entityData.set(ACTION,3)}
    private fun warn(level:ServerLevel){level.sendParticles(ParticleTypes.END_ROD,true,true,x,eyeY,z,3,.2,.1,.2,.01);for(i in 0..11){val a=i*PI/6;level.sendParticles(ParticleTypes.END_ROD,true,true,aim.x+cos(a)*2,aim.y+.1,aim.z+sin(a)*2,1,0.0,0.0,0.0,0.0)}}
    private fun moveSafe(direction:Vec3,range:Double){val start=position();var dest=start;for(i in 1..(range*4).toInt()){val p=start.add(direction.scale(i*.25));if(!level().noCollision(this,boundingBox.move(p.subtract(start))))break;dest=p};setPos(dest)}
    override fun hurtServer(level:ServerLevel,source:DamageSource,amount:Float):Boolean{
        val now=level.server.overworld().gameTime;var actual=amount
        if(kind==RelicMobKind.REEF&&now>=openUntil&&source.directEntity!=null&&!source.`is`(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)){
            val direction=source.sourcePosition?.subtract(position())?.multiply(1.0,0.0,1.0)?.normalize()
            if(direction!=null&&direction.dot(lookAngle.multiply(1.0,0.0,1.0).normalize())>.4)actual*=.4f
        }
        val category=when{DamageContext.current()!=DamageOrigin.DIRECT->"skill";source.directEntity is net.minecraft.world.entity.projectile.Projectile->"ranged";else->"melee"}
        if(kind==RelicMobKind.KEEPER&&now<resistUntil&&category==lastCategory)actual*=.8f
        val success=super.hurtServer(level,source,actual)
        if(success){calmedUntil=0;calmInterruptedUntil=now+100}
        if(success&&kind==RelicMobKind.KEEPER){sameHits=if(category==lastCategory)sameHits+1 else 1;lastCategory=category;if(sameHits>=3){resistUntil=now+100;sameHits=0;level.sendParticles(ParticleTypes.ENCHANT,true,true,x,y+1,z,12,.4,.6,.4,.02)}}
        if(success&&kind==RelicMobKind.JELLY&&source.entity is ServerPlayer&&distanceToSqr(source.entity!!)<9&&now>=stingUntil&&DamageContext.current()==DamageOrigin.DIRECT){stingUntil=now+60;DamageContext.with(DamageOrigin.SKILL){(source.entity as ServerPlayer).hurtServer(level,level.damageSources().mobAttack(this),2f)}}
        return success
    }
    override fun mobInteract(player:Player,hand:InteractionHand):InteractionResult{
        if(kind.hostile)return super.mobInteract(player,hand)
        val food=when(kind){RelicMobKind.WISP->Items.CHORUS_FRUIT;RelicMobKind.MOTH->Items.GLOW_BERRIES;else->Items.KELP}
        val stack=player.getItemInHand(hand)
        if(stack.`is`(food)){
            if(player is ServerPlayer){heal(Balance.value("mob.feedHeal").toFloat());fedUntil=player.level().server.overworld().gameTime+Balance.ticks("mob.feedTicks");stack.consume(1,player);entityData.set(ACTION,5);player.level().sendParticles(ParticleTypes.HEART,x,y+bbHeight,z,3,.2,.1,.2,.01)}
            return InteractionResult.SUCCESS
        }
        if(stack.isEmpty&&player is ServerPlayer){
            if(kind==RelicMobKind.WISP&&ownerId!=null&&ownerId!=player.uuid)return InteractionResult.PASS
            if(productStock>0){val product=ItemStack(Content.item(when(kind){RelicMobKind.WISP->"shed_dragon_scale";RelicMobKind.MOTH->"moth_scale";else->"jelly_pearl"}),productStock);if(InventoryInsertion.insertAll(player,product)){productStock=0;entityData.set(STOCK,0)}}
            else if(kind==RelicMobKind.WISP){if(ownerId==null){if(!Companions.get(player.level().server).register(player.uuid,uuid))return InteractionResult.FAIL;ownerId=player.uuid};behavior=1-behavior;player.sendOverlayMessage(Component.translatable(if(behavior==0)"msg.lynxrelics.wisp_follow"else"msg.lynxrelics.wisp_stay"))}
            return InteractionResult.SUCCESS
        }
        return super.mobInteract(player,hand)
    }
    override fun dropCustomDeathLoot(level:ServerLevel,source:DamageSource,recentlyHit:Boolean){
        if(rewarded)return;rewarded=true;super.dropCustomDeathLoot(level,source,recentlyHit)
        if(kind==RelicMobKind.WISP)ownerId?.let{Companions.get(level.server).died(it,uuid)}
        if(!level.gameRules.get(GameRules.MOB_DROPS))return
        fun drop(id:String,n:Int){spawnAtLocation(level,ItemStack(Content.item(id),n))}
        when(kind){RelicMobKind.WISP->drop("empty_embryo_shell",1);RelicMobKind.HUNTER->if(random.nextFloat()<.35f)drop("shed_dragon_scale",1);RelicMobKind.REEF->{drop("reef_shell",1);spawnAtLocation(level,ItemStack(Items.PRISMARINE_SHARD,2+random.nextInt(3)))};RelicMobKind.KEEPER->{drop("keeper_shard",1);drop("echo_dust",4+random.nextInt(3))};RelicMobKind.BROOD->{drop("rift_emblem",1);drop("endmarrow_scrap",2+random.nextInt(3));drop("dragon_essence",8+random.nextInt(5))};else->{}}
    }
    override fun startSeenByPlayer(player:ServerPlayer){super.startSeenByPlayer(player);if(kind==RelicMobKind.BROOD)bossBar.addPlayer(player)}
    override fun stopSeenByPlayer(player:ServerPlayer){super.stopSeenByPlayer(player);if(kind==RelicMobKind.BROOD)bossBar.removePlayer(player)}
    override fun removeWhenFarAway(distance:Double)=kind!=RelicMobKind.BROOD&&kind!=RelicMobKind.KEEPER&&ownerId==null
    override fun getBaseExperienceReward(level:ServerLevel)=when(kind){RelicMobKind.HUNTER->5;RelicMobKind.REEF->10;RelicMobKind.KEEPER->20;RelicMobKind.BROOD->100;else->0}
    override fun addAdditionalSaveData(out:ValueOutput){
        super.addAdditionalSaveData(out);out.putString("LynxOwner",ownerId?.toString()?:"");out.putInt("LynxStock",productStock);out.putLong("LynxFedUntil",fedUntil);out.putInt("LynxProductionTicks",productionTicks);out.putInt("LynxBehavior",behavior);out.putLong("LynxPickupReset",pickupReset);out.putInt("LynxPickupCount",pickupCount)
        out.putLong("LynxHome",home.asLong());out.putBoolean("LynxHasHome",hasHome);out.putLong("LynxAttackReady",nextAttack);out.putLong("LynxActAt",actAt);out.putLong("LynxRecover",recoverUntil);out.putString("LynxAttack",attack);out.store("LynxAim",Vec3.CODEC,aim);out.putLong("LynxLastParticipant",lastParticipant);out.putBoolean("LynxRewarded",rewarded);out.putBoolean("LynxLocked",encounterLocked);out.putFloat("LynxMaxHealth",maxHealth);out.putInt("LynxPhase",entityData.get(PHASE))
        out.putInt("LynxAttackNumber",attackNumber);out.putInt("LynxPulses",firedPulses);out.putLong("LynxOpenUntil",openUntil);out.putLong("LynxResistUntil",resistUntil);out.putLong("LynxStingUntil",stingUntil);out.putLong("LynxCalmInterruptedUntil",calmInterruptedUntil);out.putInt("LynxSameHits",sameHits);out.putString("LynxDamageCategory",lastCategory)
        val victims=out.childrenList("LynxAttackVictims");attackVictims.forEach{victims.addChild().putString("UUID",it.toString())}
    }
    override fun readAdditionalSaveData(input:ValueInput){
        val lockedMax=input.getFloatOr("LynxMaxHealth",kind.hp.toFloat());if(kind==RelicMobKind.BROOD)overrideHealth(lockedMax)
        super.readAdditionalSaveData(input);ownerId=runCatching{UUID.fromString(input.getStringOr("LynxOwner",""))}.getOrNull();productStock=input.getIntOr("LynxStock",0).coerceIn(0,2);entityData.set(STOCK,productStock);fedUntil=input.getLongOr("LynxFedUntil",0);productionTicks=input.getIntOr("LynxProductionTicks",0);behavior=input.getIntOr("LynxBehavior",0);pickupReset=input.getLongOr("LynxPickupReset",0);pickupCount=input.getIntOr("LynxPickupCount",0)
        home=BlockPos.of(input.getLongOr("LynxHome",0));hasHome=input.getBooleanOr("LynxHasHome",false);nextAttack=input.getLongOr("LynxAttackReady",0);actAt=input.getLongOr("LynxActAt",0);recoverUntil=input.getLongOr("LynxRecover",0);attack=input.getStringOr("LynxAttack","");aim=input.read("LynxAim",Vec3.CODEC).orElse(Vec3.ZERO);lastParticipant=input.getLongOr("LynxLastParticipant",0);rewarded=input.getBooleanOr("LynxRewarded",false);encounterLocked=input.getBooleanOr("LynxLocked",false);entityData.set(PHASE,input.getIntOr("LynxPhase",1).coerceIn(1,3))
        attackNumber=input.getIntOr("LynxAttackNumber",0).coerceAtLeast(0);firedPulses=input.getIntOr("LynxPulses",0).coerceIn(0,5);openUntil=input.getLongOr("LynxOpenUntil",0);resistUntil=input.getLongOr("LynxResistUntil",0);stingUntil=input.getLongOr("LynxStingUntil",0);calmInterruptedUntil=input.getLongOr("LynxCalmInterruptedUntil",0);sameHits=input.getIntOr("LynxSameHits",0).coerceAtLeast(0);lastCategory=input.getStringOr("LynxDamageCategory","")
        attackVictims.clear();input.childrenListOrEmpty("LynxAttackVictims").forEach{runCatching{UUID.fromString(it.getStringOr("UUID",""))}.getOrNull()?.let(attackVictims::add)}
        entityData.set(MAX_HP,lockedMax);if(kind==RelicMobKind.BROOD){overrideHealth(lockedMax);health=input.getFloatOr("Health",health).coerceAtMost(lockedMax)}
    }
    companion object{
        @JvmField val ACTION=SynchedEntityData.defineId(RelicMob::class.java,EntityDataSerializers.INT)
        @JvmField val PHASE=SynchedEntityData.defineId(RelicMob::class.java,EntityDataSerializers.INT)
        @JvmField val MAX_HP=SynchedEntityData.defineId(RelicMob::class.java,EntityDataSerializers.FLOAT)
        @JvmField val STOCK=SynchedEntityData.defineId(RelicMob::class.java,EntityDataSerializers.INT)
        fun attributes(kind:RelicMobKind)=Mob.createMobAttributes().add(Attributes.MAX_HEALTH,Balance.value("mob.${kind.id}.health")).add(Attributes.ARMOR,Balance.value("mob.${kind.id}.armor")).add(Attributes.MOVEMENT_SPEED,Balance.value("mob.${kind.id}.speed")).add(Attributes.ATTACK_DAMAGE,Balance.value("mob.${kind.id}.damage"))
        private fun distanceSegment(p:Vec3,a:Vec3,b:Vec3):Double{val d=b.subtract(a);val t=p.subtract(a).dot(d)/d.lengthSqr().coerceAtLeast(.001);return p.distanceTo(a.add(d.scale(t.coerceIn(0.0,1.0))))}
    }
}
class HostileRelicMob(type:EntityType<out RelicMob>,level:Level):RelicMob(type,level),Enemy

object InventoryInsertion{
    fun insertAll(player:ServerPlayer,product:ItemStack):Boolean{
        val items=(0..35).map{player.inventory.getItem(it).copy()}.toMutableList();var left=product.count
        for(stack in items)if(!stack.isEmpty&&ItemStack.isSameItemSameComponents(stack,product)){val add=minOf(left,stack.maxStackSize-stack.count);stack.grow(add);left-=add}
        for(i in items.indices)if(left>0&&items[i].isEmpty){val n=minOf(left,product.maxStackSize);items[i]=product.copyWithCount(n);left-=n}
        if(left>0){player.sendOverlayMessage(Component.translatable("msg.lynxrelics.inventory_full"));return false}
        for(i in items.indices)player.inventory.setItem(i,items[i]);return true
    }
}

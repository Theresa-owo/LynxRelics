// SPDX-License-Identifier: GPL-3.0-or-later
package net.ciyuan.lynxrelics.mob
import net.minecraft.world.entity.MobCategory

enum class RelicMobKind(val id: String,val hp:Double,val armor:Double,val width:Float,val height:Float,val damage:Double,val category:MobCategory) {
    WISP("dragon_wisp",16.0,0.0,.5f,.45f,0.0,MobCategory.CREATURE),
    MOTH("echo_moth",6.0,0.0,.6f,.25f,0.0,MobCategory.AMBIENT),
    JELLY("tide_jelly",20.0,0.0,.8f,1.2f,0.0,MobCategory.WATER_AMBIENT),
    HUNTER("rift_hunter",44.0,4.0,1.1f,.8f,7.0,MobCategory.MONSTER),
    REEF("reef_lurker",84.0,10.0,1.5f,1.1f,8.0,MobCategory.MONSTER),
    KEEPER("silent_keeper",120.0,6.0,.9f,2.2f,8.0,MobCategory.MONSTER),
    BROOD("brood_drake",360.0,8.0,3.2f,2.4f,10.0,MobCategory.MONSTER);
    val hostile get()=category==MobCategory.MONSTER
    val flying get()=this==WISP||this==MOTH
    val routeKey get()=when(this){WISP,HUNTER,BROOD->"dragon";MOTH,KEEPER->"echo";else->"tide"}
}

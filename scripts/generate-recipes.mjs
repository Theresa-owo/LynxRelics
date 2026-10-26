// SPDX-License-Identifier: GPL-3.0-or-later
import fs from 'node:fs'; import path from 'node:path'; import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const dir=path.join(root,'src/main/resources/data/lynxrelics');
const index=[];
function write(p,v){fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');}
const m=id=>'minecraft:'+id, l=id=>'lynxrelics:'+id;
function recipe(code,result,inputs,count=1){
  const ingredients=Object.entries(inputs).flatMap(([id,n])=>Array(n).fill(id));
  if(ingredients.length>9)throw new Error('Crafting grid overflow '+code);
  const id=code.toLowerCase()+'_'+result;
  write(path.join(dir,'recipe',id+'.json'),{type:'minecraft:crafting_shapeless',category:'equipment',ingredients,result:{id:l(result),count}});
  index.push({code,location:'crafting',inputs,output:{[l(result)]:count},resource:id});
}
function shaped(code,result,pattern,key,count=1){
  const id=code.toLowerCase()+'_'+result;
  write(path.join(dir,'recipe',id+'.json'),{type:'minecraft:crafting_shaped',category:'equipment',pattern,key,result:{id:l(result),count}});
  index.push({code,location:'crafting',resource:id});
}
shaped('D01','dragon_workstation',['OEO','BAB','OOO'],{O:m('obsidian'),E:m('end_stone'),B:m('blaze_rod'),A:m('amethyst_block')});
recipe('D03','dragon_shell_dust',{[m('dragon_egg')]:1},8);
recipe('D04','dragon_shell_dust',{[m('popped_chorus_fruit')]:8,[m('obsidian')]:1},4);
recipe('D07','dragonthroat_gauntlet',{[l('dragonbound_ingot')]:3,[m('leather')]:2,[m('iron_ingot')]:1});
recipe('D08','rift_blade',{[l('dragonbound_ingot')]:4,[m('diamond')]:2,[m('stick')]:1});
recipe('D09','dragonwing_cloak',{[l('dragonbound_ingot')]:3,[m('phantom_membrane')]:4,[m('leather')]:2});
recipe('D10','homing_dragon_necklace',{[l('dragonbound_ingot')]:2,[m('ender_eye')]:1,[m('amethyst_shard')]:4});
recipe('E01','echo_workstation',{[m('deepslate')]:4,[m('amethyst_block')]:2,[m('copper_ingot')]:2,[m('note_block')]:1});
recipe('E04','echo_shield',{[l('resonant_bone')]:3,[m('iron_ingot')]:2,'#minecraft:planks':3,[m('leather')]:1});
recipe('E05','resonant_bow',{[l('resonant_bone')]:2,[m('string')]:3,[m('amethyst_shard')]:2});
recipe('E06','echo_arrow',{[m('arrow')]:8,[l('echo_dust')]:1},8);
recipe('E07','silent_belt',{[l('resonant_bone')]:2,[m('leather')]:4,'#minecraft:wool':2});
recipe('E08','resonance_box',{[l('resonant_bone')]:2,[m('jukebox')]:1,[l('echo_dust')]:4,[m('redstone')]:2});
recipe('T01','tide_workstation',{[m('copper_ingot')]:4,[m('prismarine')]:2,[m('glass')]:2,[m('nautilus_shell')]:1});
recipe('T05','tide_spear',{[l('tideforged_copper')]:4,[m('iron_ingot')]:2,[m('stick')]:1});
recipe('T06','undertow_cloak',{[l('tideforged_copper')]:4,[m('copper_ingot')]:4,[m('prismarine_crystals')]:1});
recipe('T07','tide_conch',{[l('tideforged_copper')]:2,[m('nautilus_shell')]:1,[l('condensed_tide')]:2,[m('amethyst_shard')]:2});
recipe('T08','watermirror_necklace',{[l('tideforged_copper')]:2,[m('glass')]:2,[m('iron_ingot')]:2,[l('condensed_tide')]:1});
recipe('A01','dragonbound_belt',{[l('dragonbound_ingot')]:2,[m('leather')]:4,[l('dragon_essence')]:1});
recipe('A02','resonant_necklace',{[l('resonant_bone')]:2,[m('amethyst_shard')]:4,[m('string')]:2});
recipe('A03','keeper_gauntlet',{[l('resonant_bone')]:2,[m('iron_ingot')]:2,[m('leather')]:2});
recipe('A04','murmur_cloak',{[l('resonant_bone')]:2,[m('phantom_membrane')]:2,'#minecraft:wool':4});
recipe('A05','undertow_belt',{[l('tideforged_copper')]:2,[m('leather')]:4,[m('nautilus_shell')]:1});
recipe('A06','tidepull_gauntlet',{[l('tideforged_copper')]:2,[m('iron_ingot')]:2,[m('prismarine_shard')]:2});
recipe('X02','endmarrow_ingot',{[l('endmarrow_scrap')]:4,[m('amethyst_shard')]:4,[l('dragon_essence')]:1});
recipe('X03','endforged_template',{[l('rift_emblem')]:1,[l('endmarrow_ingot')]:1,[m('diamond')]:4,[m('end_stone')]:3});
shaped('X04','endforged_template',['DTD','DED','DDD'],{D:m('diamond'),T:l('endforged_template'),E:m('end_stone')},2);
recipe('X06','perpetual_core',{[l('rift_emblem')]:1,[l('endmarrow_ingot')]:2,[l('dragon_essence')]:2,[l('echo_dust')]:2,[l('condensed_tide')]:2});
// X07 uses a custom recipe that rejects rockets with stars or a flight duration other than 1.
write(path.join(dir,'recipe/x07_cruise_firework.json'),{type:'lynxrelics:cruise_firework',category:'misc'});
index.push({code:'X07',location:'custom_crafting',resource:'x07_cruise_firework'});
recipe('X08','bound_pearl',{[l('perpetual_core')]:1,[m('ender_pearl')]:4,[l('echo_dust')]:2,[l('condensed_tide')]:2});
recipe('V01','relic_upgrade_template',{[m('amethyst_shard')]:1,[m('paper')]:4,[m('copper_ingot')]:4});
recipe('V02','iron_rim',{[m('iron_ingot')]:4,[m('leather')]:2});
recipe('V03','deflector',{[l('resonant_bone')]:1,[m('iron_ingot')]:2,[m('amethyst_shard')]:2});
recipe('V04','breaching_fitting',{[l('dragonbound_ingot')]:1,[m('copper_ingot')]:2,[m('leather')]:2});
recipe('V06','quickdraw_string',{[m('string')]:3,[l('echo_dust')]:1,[m('copper_ingot')]:1});
recipe('V08','steady_rest',{[l('tideforged_copper')]:1,[m('iron_ingot')]:2,[m('leather')]:1});
for(const[code,base,fitting]of [['V05','shield','iron_rim'],['V05','shield','deflector'],['V05','shield','breaching_fitting'],['V07','bow','quickdraw_string'],['V09','crossbow','steady_rest']]){
 write(path.join(dir,`recipe/${code.toLowerCase()}_${fitting}.json`),{type:'minecraft:smithing_transform',template:l('relic_upgrade_template'),base:m(base),addition:l(fitting),result:{id:m(base)}});
 if(!index.some(r=>r.code===code))index.push({code,location:'smithing',base});
}
recipe('V10','scouting_arrow',{[m('spectral_arrow')]:4,[l('echo_dust')]:1},4);
recipe('V11','foam_arrow',{[m('arrow')]:4,[l('condensed_tide')]:1,[m('glass_bottle')]:1},4);
recipe('L01','magnetic_ring',{[m('iron_ingot')]:4,[m('copper_ingot')]:2,[l('echo_dust')]:2,[m('amethyst_shard')]:1});
recipe('L02','expedition_compass',{[m('compass')]:1,[l('echo_dust')]:2,[l('condensed_tide')]:2,[m('copper_ingot')]:4});
recipe('L03','stabilizing_ring',{[l('dragonbound_ingot')]:1,[l('resonant_bone')]:1,[l('tideforged_copper')]:1,[m('amethyst_shard')]:4});
recipe('B01','abundance_core',{[m('heart_of_the_sea')]:1,[m('golden_carrot')]:4,[m('gold_block')]:2,[l('condensed_tide')]:2});
recipe('B02','craftsmanship_core',{[m('echo_shard')]:1,[l('echo_dust')]:4,[m('iron_block')]:4});
recipe('B03','shelter_core',{[l('dragon_essence')]:4,[l('dragonbound_ingot')]:2,[m('golden_apple')]:2,[m('phantom_membrane')]:1});
recipe('R01','speed_rail',{[m('powered_rail')]:1,[m('amethyst_shard')]:2,[m('redstone')]:1});
for(const gear of ['helmet','chestplate','leggings','boots','pickaxe','axe','shovel','hoe','sword'])write(path.join(dir,`recipe/x05_endforged_${gear}.json`),{type:'minecraft:smithing_transform',template:l('endforged_template'),base:m('netherite_'+gear),addition:l('endmarrow_ingot'),result:{id:l('endforged_'+gear)}});
index.push({code:'X05',location:'smithing',variants:9});
for(const [type,time] of [['smelting',200],['blasting',100]])write(path.join(dir,`recipe/x01_endmarrow_${type}.json`),{type:'minecraft:'+type,category:'misc',ingredient:l('endmarrow_ore'),result:{id:l('endmarrow_scrap')},experience:0.5,cookingtime:time});
index.push({code:'X01',location:'furnace_and_blast_furnace'});
const tag=(namespace,type,id,values)=>write(path.join(root,`src/main/resources/data/${namespace}/tags/${type}/${id}.json`),{replace:false,values});
tag('lynxrelics','item','repairs_endforged',[l('endmarrow_ingot')]);
tag('minecraft','block','rails',[l('speed_rail')]);
tag('minecraft','block','mineable/pickaxe',[l('endmarrow_ore'),l('dragon_workstation'),l('echo_workstation'),l('tide_workstation')]);
for(const tier of ['wooden','stone','copper','iron','gold','diamond'])tag('minecraft','block',`incorrect_for_${tier}_tool`,[l('endmarrow_ore')]);
for(const gear of ['helmet','chestplate','leggings','boots']){tag('minecraft','item',`enchantable/${gear==='helmet'?'head_armor':gear==='chestplate'?'chest_armor':gear==='leggings'?'leg_armor':'foot_armor'}`,[l('endforged_'+gear)]);}
for(const [name,gears] of Object.entries({armor:['helmet','chestplate','leggings','boots'],equippable:['helmet','chestplate','leggings','boots'],sword:['sword'],sharp_weapon:['sword','axe'],mining:['pickaxe','axe','shovel','hoe'],mining_loot:['pickaxe','axe','shovel','hoe'],durability:['helmet','chestplate','leggings','boots','sword','pickaxe','axe','shovel','hoe']}))tag('minecraft','item',`enchantable/${name}`,gears.map(g=>l('endforged_'+g)));
tag('minecraft','item','pickaxes',[l('endforged_pickaxe')]);tag('minecraft','item','axes',[l('endforged_axe')]);tag('minecraft','item','swords',[l('endforged_sword')]);tag('minecraft','item','shovels',[l('endforged_shovel')]);tag('minecraft','item','hoes',[l('endforged_hoe')]);
for(const recipe of JSON.parse(fs.readFileSync(path.join(dir,'workstation_recipes.json'),'utf8')))index.push({code:recipe.id,location:'workstation',...recipe});
write(path.join(root,'docs/recipes-index.json'),index.sort((a,b)=>a.code.localeCompare(b.code)));
console.log(`${index.length} current recipe entries generated; specialization/ritual/enchantment/smithing stages tracked separately`);

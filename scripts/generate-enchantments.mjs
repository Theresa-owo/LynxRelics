// SPDX-License-Identifier: GPL-3.0-or-later
import fs from 'node:fs';import path from 'node:path';import{fileURLToPath}from'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),res=path.join(root,'src/main/resources');
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n');};
const tag=(ns,type,id,values)=>write(path.join(res,`data/${ns}/tags/${type}/${id}.json`),{replace:false,values});
const l=id=>'lynxrelics:'+id,m=id=>'minecraft:'+id;
const accessories=['homing_dragon_necklace','dragonbound_belt','dragonthroat_gauntlet','dragonwing_cloak','resonant_necklace','silent_belt','keeper_gauntlet','murmur_cloak','watermirror_necklace','undertow_belt','tidepull_gauntlet','undertow_cloak'];
const weapons=['rift_blade','tide_spear'];
tag('minecraft','item','arrows',['echo_arrow','scouting_arrow','foam_arrow'].map(l));
const defs=[
 ['breach','破界','Breach',3,'melee','dragon'],['echo','回声','Echo',2,'melee','echo'],['tide_conduction','导潮','Tide Conduction',3,'melee','tide'],
 ['long_resonance','长鸣','Long Resonance',2,'ranged','echo'],['resonant_guard','镇响','Resonant Guard',3,'shield','echo'],['bulwark','固守','Bulwark',3,'shield','echo'],
 ['breath_capacity','容息','Breath Capacity',3,'dragon_belt','dragon'],['tide_capacity','蓄潮','Tide Capacity',3,'tide_belt','tide'],['rhythm','节律','Rhythm',3,'gauntlet','any'],
 ['homing','归巢','Homing',2,'homing_necklace','dragon'],['mirror','明镜','Clear Mirror',2,'mirror_necklace','tide'],['overtone','余韵','Overtone',2,'echo_necklace','echo'],
 ['cushion','缓冲','Cushion',3,'cloak','any'],['cruise','巡航','Cruise',3,'elytra','dragon'],['smelting','精炼','Smelting',2,'tool','echo'],['prospecting','寻脉','Prospecting',2,'end_pickaxe','tide']
];
const targets={
 melee:['#minecraft:enchantable/sharp_weapon',m('trident'),...weapons.map(l)],ranged:[m('bow'),m('crossbow'),l('resonant_bow')],shield:[m('shield'),l('echo_shield')],
 dragon_belt:[l('dragonbound_belt')],tide_belt:[l('undertow_belt')],gauntlet:accessories.filter(x=>x.endsWith('_gauntlet')).map(l),
 homing_necklace:[l('homing_dragon_necklace')],mirror_necklace:[l('watermirror_necklace')],echo_necklace:[l('resonant_necklace')],cloak:accessories.filter(x=>x.endsWith('_cloak')).map(l),
 elytra:[m('elytra')],tool:['#minecraft:pickaxes','#minecraft:axes','#minecraft:shovels','#minecraft:hoes'],end_pickaxe:[l('endforged_pickaxe')]
};
for(const[id,values]of Object.entries(targets))tag('lynxrelics','item','enchantable/'+id,values);
tag('minecraft','item','enchantable/sharp_weapon',weapons.map(l));tag('minecraft','item','enchantable/melee_weapon',weapons.map(l));tag('minecraft','item','enchantable/sword',weapons.map(l));
tag('minecraft','item','enchantable/bow',[l('resonant_bow')]);tag('minecraft','item','enchantable/durability',[...accessories.filter(x=>x!=='homing_dragon_necklace'),...weapons,'resonant_bow','echo_shield','tide_conch'].map(l));
tag('minecraft','enchantment','exclusive_set/damage',[l('breach')]);tag('minecraft','enchantment','exclusive_set/mining',[l('smelting')]);tag('lynxrelics','enchantment','exclusive/guard',[l('resonant_guard'),l('bulwark')]);
for(const[id,zh,en,max,target,route]of defs){
 const data={description:{translate:'enchantment.lynxrelics.'+id},supported_items:'#lynxrelics:enchantable/'+target,primary_items:'#lynxrelics:enchantable/'+target,weight:5,max_level:max,min_cost:{base:5,per_level_above_first:10},max_cost:{base:45,per_level_above_first:10},anvil_cost:2,slots:['any'],effects:{}};
 if(id==='breach')data.exclusive_set='#minecraft:exclusive_set/damage';if(id==='smelting')data.exclusive_set='#minecraft:exclusive_set/mining';if(['resonant_guard','bulwark'].includes(id))data.exclusive_set='#lynxrelics:exclusive/guard';
 write(path.join(res,`data/lynxrelics/enchantment/${id}.json`),data);
 for(const lang of ['zh_cn','en_us']){const p=path.join(res,`assets/lynxrelics/lang/${lang}.json`),strings=JSON.parse(fs.readFileSync(p,'utf8'));strings['enchantment.lynxrelics.'+id]=lang==='zh_cn'?zh:en;strings['tag.item.lynxrelics.enchantable.'+target]=lang==='zh_cn'?'可附魔：'+zh:'Enchantable: '+en;write(p,strings);}
}
const common=defs.filter(x=>!['cruise','smelting','prospecting'].includes(x[0])).map(x=>l(x[0]));
for(const name of ['in_enchanting_table','tradeable','on_random_loot'])tag('minecraft','enchantment',name,common);
write(path.join(root,'docs/enchantments-index.json'),defs.map(([id,zh,en,max,target,route])=>({id,zh,en,max,target,route})));
const recipeFile=path.join(res,'data/lynxrelics/workstation_recipes.json');
const recipes=JSON.parse(fs.readFileSync(recipeFile,'utf8')).filter(r=>!r.generated);
if(!recipes.some(r=>r.id==='E11'))recipes.push({id:'E11',route:'echo',ticks:160,catalyst:m('echo_shard'),inputs:{[l('echo_dust')]:4,'#minecraft:decorated_pot_sherds':2,[m('amethyst_block')]:1},outputs:{},mode:'ritual',encounter:'silent_keeper'});
if(!recipes.some(r=>r.id==='R02'))recipes.push({id:'R02',route:'dragon',ticks:160,catalyst:m('dragon_egg'),inputs:{[l('dragon_essence')]:4,[l('echo_dust')]:2,[l('condensed_tide')]:2},outputs:{},mode:'ritual',encounter:'brood_drake'});
if(!recipes.some(r=>r.id==='D11.reconstitution'))recipes.push({id:'D11.reconstitution',route:'dragon',ticks:600,inputs:{[l('dragon_essence')]:2,[m('glass_bottle')]:1,[l('empty_embryo_shell')]:1},outputs:{[l('embryo_crystal')]:1}});
const essence={dragon:'dragon_essence',echo:'echo_dust',tide:'condensed_tide'},metal={dragon:'dragonbound_ingot',echo:'resonant_bone',tide:'tideforged_copper'};
for(const[id,zh,en,max,target,route]of defs){
 const routes=route==='any'?['dragon','echo','tide']:[route];
 for(const r of routes){
  const rare=['cruise','smelting','prospecting'].includes(id),general=['rhythm','cushion'].includes(id);
  const code=rare?'N06':general?'N04':r==='dragon'?'N01':r==='echo'?'N02':'N03';
  const inputs=rare?{[m('book')]:1,[l('endmarrow_ingot')]:1,[l('dragonheart_prism')]:1,[m('lapis_lazuli')]:4}:general?{[m('book')]:1,[l(metal[r])]:1,[m('lapis_lazuli')]:4}:{[m('book')]:1,[l(essence[r])]:4,[m('lapis_lazuli')]:4};
  recipes.push({id:`${code}.${id}.${r}`,route:r,ticks:200,inputs,outputs:{},mode:'book',enchantment:id,bookLevel:1,generated:'enchantments-v1'});
  if(max>=2)recipes.push({id:`N05.${id}.${r}`,route:r,ticks:400,inputs:{[m('enchanted_book')]:1,[l('dragonheart_prism')]:1,[l(essence[r])]:8},outputs:{},mode:'upgrade_book',enchantment:id,bookLevel:2,generated:'enchantments-v1'});
 }
}
// Chapter 06 keeps newly added accessories at their base version; don't charge for undefined branches.
const equipment={dragon:['dragonthroat_gauntlet','rift_blade','dragonwing_cloak','homing_dragon_necklace'],echo:['echo_shield','resonant_bow','silent_belt','resonance_box'],tide:['tide_spear','undertow_cloak','tide_conch','watermirror_necklace']};
for(const route of ['dragon','echo','tide'])for(const gear of equipment[route])for(const spec of ['a','b'])for(const replace of [false,true]){
 const code=route==='dragon'?(replace?'D13':'D12'):route==='echo'?(replace?'E10':'E09'):(replace?'T10':'T09');
 const material=route==='dragon'?'shed_dragon_scale':route==='echo'?'moth_scale':'jelly_pearl';
 recipes.push({id:`${code}.${gear}.${spec}`,route,ticks:400,inputs:{[l(gear)]:1,[l(essence[route])]:replace?2:4,[l(material)]:replace?1:2},outputs:{},mode:replace?'respecialize':'specialize',gear:l(gear),specialization:spec,catalyst:!replace&&route!=='dragon'?l(route==='echo'?'keeper_shard':'reef_shell'):'',generated:'specializations-v1'});
}
write(recipeFile,recipes);
console.log('Generated 16 enchantments; 13 common pool entries; 3 deterministic rare book crafts');

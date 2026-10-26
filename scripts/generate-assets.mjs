// SPDX-License-Identifier: GPL-3.0-or-later
// Deterministic source-to-runtime model/data generator. No external image/model service.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { icon32 } from './pixel-icons.mjs';
import { loreFor,richLine,specializations } from './item-lore.mjs';
import { translations } from './translations.mjs';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const gray = process.argv.includes('--gray');
const polishedPath=path.join(root,'assets/icons/polished.json');
const polished=fs.existsSync(polishedPath)?JSON.parse(fs.readFileSync(polishedPath,'utf8')):{};
const resource = path.join(root, 'src/main/resources');
const source = path.join(root, 'assets/models');
const mkdir = p => fs.mkdirSync(p, { recursive: true });
const write = (p, value) => { mkdir(path.dirname(p)); fs.writeFileSync(p, typeof value === 'string' || Buffer.isBuffer(value) ? value : JSON.stringify(value, null, 2) + '\n'); };
const uuid = s => { const h = crypto.createHash('sha256').update(s).digest('hex'); return `${h.slice(0, 8)}-${h.slice(8, 12)}-4${h.slice(13, 16)}-8${h.slice(17, 20)}-${h.slice(20, 32)}`; };
function crc32(b) { let crc = 0xffffffff; for (const n of b) { crc ^= n; for (let i = 0; i < 8; i++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); } return (crc ^ 0xffffffff) >>> 0; }
function png(w, h, pixel) {
  const raw = Buffer.alloc(h * (w * 4 + 1));
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const c = pixel(x, y); for (let k = 0; k < 4; k++) raw[y * (w * 4 + 1) + 1 + x * 4 + k] = c[k] ?? 255; }
  function chunk(type, data) { const t = Buffer.from(type); const n = Buffer.alloc(4); n.writeUInt32BE(data.length); const c = Buffer.alloc(4); c.writeUInt32BE(crc32(Buffer.concat([t, data]))); return Buffer.concat([n, t, data, c]); }
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const names = {
  dragon_essence:'龙精粹',dragon_shell_dust:'龙壳粉',dragonbound_ingot:'缚龙锭',shed_dragon_scale:'蜕龙鳞',embryo_crystal:'幼灵胚晶',empty_embryo_shell:'空胚壳',
  echo_dust:'回响尘',resonant_bone:'鸣骨',moth_scale:'薄鸣鳞',keeper_shard:'守藏纹片',condensed_tide:'凝潮珠',tideforged_copper:'潮铸铜',jelly_pearl:'水母潮珠',reef_shell:'潮纹壳',
  endmarrow_scrap:'界髓碎屑',endmarrow_ingot:'界髓锭',rift_emblem:'裂界纹章',endforged_template:'界铸纹板',perpetual_core:'永续核心',dragonheart_prism:'龙心棱片',raid_cloak_pattern:'龙战披风纹样',
  relic_upgrade_template:'遗珍改造纹板',iron_rim:'铁箍件',deflector:'偏转件',breaching_fitting:'破阵件',quickdraw_string:'快取弦',steady_rest:'驻射架',magnetic_ring:'磁引环',expedition_compass:'远征罗盘',stabilizing_ring:'稳定环',abundance_core:'丰饶核心',craftsmanship_core:'匠心核心',shelter_core:'庇护核心',
  homing_dragon_necklace:'归巢龙印项链',dragonbound_belt:'缚龙腰带',dragonthroat_gauntlet:'龙喉手甲',dragonwing_cloak:'龙翼披风',
  resonant_necklace:'鸣纹项链',silent_belt:'寂静束带',keeper_gauntlet:'守藏护手',murmur_cloak:'薄鸣披风',watermirror_necklace:'水镜项链',undertow_belt:'潜潮腰带',tidepull_gauntlet:'潮引护手',undertow_cloak:'潜潮披风',
  rift_blade:'裂界之刃',tide_spear:'潮锋戟',echo_shield:'回声盾',resonant_bow:'余响长弓',echo_arrow:'余响箭',scouting_arrow:'侦照箭',foam_arrow:'潮沫箭',tide_conch:'潮音海螺',cruise_firework:'巡航焰匣',bound_pearl:'缚界珍珠',
  dragon_workstation:'龙息萃取台',echo_workstation:'回声调谐台',tide_workstation:'潮汐调压台',resonance_box:'共鸣匣',endmarrow_ore:'界髓矿',speed_rail:'变速铁轨',
  endforged_helmet:'界铸头盔',endforged_chestplate:'界铸胸甲',endforged_leggings:'界铸护腿',endforged_boots:'界铸靴子',endforged_pickaxe:'界铸镐',endforged_axe:'界铸斧',endforged_shovel:'界铸锹',endforged_hoe:'界铸锄',endforged_sword:'界铸剑'
};
const palettes = {
  dragon:[[36,20,51],[69,38,91],[128,69,164],[215,175,238]],
  echo:[[18,35,43],[31,74,83],[58,138,153],[191,226,215]],
  tide:[[16,44,49],[40,103,104],[75,168,146],[217,239,225]],
  end:[[33,17,49],[66,29,98],[102,46,153],[187,125,230]],
  gray:[[46,46,50],[85,85,90],[135,135,140],[201,201,205]]
};
const theme = id => /echo|resonan|silent|keeper|murmur|moth|craftsmanship/.test(id) ? 'echo' : /tide|undertow|watermirror|jelly|reef|foam|abundance/.test(id) ? 'tide' : /end|rift|bound_pearl|perpetual/.test(id) ? 'end' : 'dragon';
const templates = {};
const box = (name, from, to, group='root') => ({ name, from, to, group });
function workstation() {
  const b=[box('base',[0,0,0],[16,3,16]),box('top',[0,11,0],[16,13,16]),box('core',[5,3,5],[11,11,11],'core')];
  for (const x of [1,12]) for (const z of [1,12]) { b.push(box(`leg_${x}_${z}`,[x,3,z],[x+3,11,z+3])); b.push(box(`cap_${x}_${z}`,[x,13,z],[x+3,14,z+3])); }
  const positions=[[3,3],[7,2],[11,3],[12,7],[11,11],[7,12],[3,11],[2,7]];
  for (let i=0;i<8;i++) { const [x,z]=positions[i]; b.push(box(`bottle_${i}`,[x,13,z],[x+2,15,z+2], 'bottles')); b.push(box(`stopper_${i}`,[x+.5,15,z+.5],[x+1.5,16,z+1.5], 'bottles')); }
  for(let i=0;i<8;i++) b.push(box(`rune_${i}`,[i*2+.2,10.5,0],[i*2+1.7,11.5,.5]));
  return b;
}
function gauntlet() {
  const b=[box('forearm',[4,2,5],[12,11,11],'forearm'),box('wrist',[3.5,10,4.5],[12.5,13,11.5],'forearm'),box('palm',[4,12,5],[12,17,10],'hand')];
  for(let i=0;i<4;i++) for(let j=0;j<3;j++) b.push(box(`finger_${i}_${j}`,[4+i*2,16+j*1.5,5],[5.5+i*2,17.5+j*1.5,8],'hand'));
  for(let i=0;i<5;i++) b.push(box(`scale_${i}`,[3.5,3+i*1.7,10],[12.5,4+i*1.7,12],'forearm'));
  for(let i=0;i<4;i++) b.push(box(`jaw_${i}`,[5+i*1.5,8,11.5],[6+i*1.5,10,13],'jaw'));
  return b;
}
templates.dragon_workstation = workstation(); templates.echo_workstation = workstation(); templates.tide_workstation = workstation();
templates.resonance_box = [box('base',[1,0,1],[15,2,15]),box('cabinet',[2,2,2],[14,13,14]),box('speaker',[4,4,1],[12,11,2]),box('disc_slot',[4,13,6],[12,14,9]),box('top',[1,12,1],[15,14,15])];
templates.endmarrow_ore=[box('ore',[0,0,0],[16,16,16])];
templates.endforged_helmet=[box('helmet',[-4.5,23.5,-4.5],[4.5,32.5,4.5],'head')];
templates.endforged_chestplate=[box('torso',[-4.5,11.5,-2.5],[4.5,24.5,2.5],'body'),box('left_arm',[4,11.5,-2.5],[8.5,24.5,2.5],'left_arm'),box('right_arm',[-8.5,11.5,-2.5],[-4,24.5,2.5],'right_arm')];
templates.endforged_leggings=[box('waist',[-4.3,11,-2.3],[4.3,17,2.3],'body'),box('left_leg',[0,-.3,-2.3],[4.3,12,2.3],'left_leg'),box('right_leg',[-4.3,-.3,-2.3],[0,12,2.3],'right_leg')];
templates.endforged_boots=[box('left_boot',[-.5,-.5,-2.5],[4.5,6,2.5],'left_leg'),box('right_boot',[-4.5,-.5,-2.5],[.5,6,2.5],'right_leg')];
templates.dragonthroat_gauntlet = gauntlet(); templates.keeper_gauntlet = gauntlet(); templates.tidepull_gauntlet = gauntlet();
templates.rift_blade = [box('grip',[7,0,7],[9,7,9]),box('pommel',[6,0,6],[10,2,10]),box('guard',[3,7,6],[13,9,10]),box('blade',[6,9,7],[10,25,9]),box('edge_l',[5,9,7.5],[6,23,8.5]),box('edge_r',[10,9,7.5],[11,23,8.5]),box('tip',[7,25,7],[9,28,9])];
templates.echo_shield = [box('body',[2,0,7],[14,19,9]),box('left_rim',[1,1,6.5],[3,18,9.5]),box('right_rim',[13,1,6.5],[15,18,9.5]),box('top_rim',[2,18,6.5],[14,20,9.5]),box('bottom_rim',[3,-1,6.5],[13,1,9.5]),box('boss',[6,7,8.5],[10,12,11]),box('grip',[6,7,4],[10,12,6])];
templates.tide_spear = [box('shaft',[7,0,7],[9,24,9]),box('grip',[6.5,7,6.5],[9.5,14,9.5]),box('cross',[2,23,7],[14,25,9]),box('left_prong',[2,24,7],[4,30,9]),box('right_prong',[12,24,7],[14,30,9]),box('central_prong',[7,24,7],[9,32,9])];
for(const id of Object.keys(names)) {
  if (id.endsWith('_necklace')) templates[id]=[box('chain_left',[3,8,7],[4,16,8],'neck'),box('chain_right',[12,8,7],[13,16,8],'neck'),box('chain',[4,7,7],[12,8,8],'neck'),box('pendant',[6,3,6],[10,8,9],'pendant')];
  if(id.endsWith('_belt')) templates[id]=[box('front',[1,6,4],[15,10,6],'waist'),box('left',[0,6,5],[2,10,11],'waist'),box('right',[14,6,5],[16,10,11],'waist'),box('back',[1,6,10],[15,10,12],'waist'),box('buckle',[6,5,2.5],[10,11,5],'buckle')];
  if(id.endsWith('_cloak')) templates[id]=[box('collar',[2,18,9],[14,20,12],'back'),box('left',[1,1,10],[8,19,11],'wing_left'),box('right',[8,1,10],[15,19,11],'wing_right'),box('trim_left',[0,1,10],[2,18,12],'wing_left'),box('trim_right',[14,1,10],[16,18,12],'wing_right')];
}
const display = {
  gui:{rotation:[25,225,0],translation:[0,0,0],scale:[.55,.55,.55]},
  ground:{rotation:[0,0,0],translation:[0,2,0],scale:[.25,.25,.25]},
  fixed:{rotation:[0,180,0],translation:[0,0,0],scale:[.5,.5,.5]},
  thirdperson_righthand:{rotation:[0,90,0],translation:[0,1,0],scale:[.55,.55,.55]},
  thirdperson_lefthand:{rotation:[0,-90,0],translation:[0,1,0],scale:[.55,.55,.55]},
  firstperson_righthand:{rotation:[0,-90,15],translation:[1,1,0],scale:[.5,.5,.5]},
  firstperson_lefthand:{rotation:[0,90,-15],translation:[1,1,0],scale:[.5,.5,.5]}
};
const manifest = { phase:gray?'gray-prototype':'textured', generator:'scripts/generate-assets.mjs', coordinateUnits:'16 units = 1 block', assets:[] };
for(const [id,label] of Object.entries(names)) {
  const palette=palettes[gray?'gray':theme(id)];
  const geometry=templates[id];
  const texture=png(32,32,(x,y)=>{
    if(!geometry && (Math.abs(x-15.5)+Math.abs(y-15.5)>13 || (x<8||x>23)&&(y<8||y>23))) return [0,0,0,0];
    const n=((x*13+y*7+(x*y)%11)%17); return [...palette[n<2?3:n<6?2:n<12?1:0],255];
  });
  const isBlock=id.endsWith('_workstation')||id==='resonance_box'||id==='endmarrow_ore'||id==='speed_rail';
  write(path.join(resource,`assets/lynxrelics/textures/${isBlock?'block':'item'}/${id}.png`),texture);
  const tex=`lynxrelics:${isBlock?'block':'item'}/${id}`;
  if(geometry) {
    const elements=geometry.map((b,i)=>({name:b.name,from:b.from,to:b.to,faces:Object.fromEntries(['north','south','east','west','up','down'].map((f,j)=>[f,{uv:[(i*3+j*2)%24,(i*7+j*3)%24,(i*3+j*2)%24+8,(i*7+j*3)%24+8],texture:'#0'}]))}));
    const isArmor=/endforged_(helmet|chestplate|leggings|boots)/.test(id);
    const model={credit:'LynxRelics original editable model',textures:{'0':tex,particle:tex},elements:elements.map(e=>isArmor?{...e,from:e.from.map((n,i)=>i===0?n+8:n),to:e.to.map((n,i)=>i===0?n+8:Math.min(32,n))}:e),display};
    const modelType=isBlock?'block':'item';
    write(path.join(resource,`assets/lynxrelics/models/${modelType}/${id}.json`),model);
    const groups=[...new Set(geometry.map(b=>b.group))].map(g=>({name:g,origin:[8,12,8],rotation:[0,0,0],uuid:uuid(id+':'+g),children:geometry.map((b,i)=>b.group===g?uuid(id+':element:'+i):null).filter(Boolean)}));
    const editable=elements.map((b,i)=>({...b,type:'cube',uuid:uuid(id+':element:'+i),origin:[8,12,8],rotation:[0,0,0],box_uv:false,faces:Object.fromEntries(Object.entries(b.faces).map(([f,v])=>[f,{...v,texture:0}]))}));
    const animations=['idle','charge','active','recovery'].map((n,i)=>({uuid:uuid(id+':animation:'+n),name:n,loop:i===0?'loop':'once',length:[1,0.75,2.4,0.5][i],snapping:20,animators:{}}));
    write(path.join(source,`${id}.bbmodel`),{meta:{format_version:'4.10',model_format:'free',box_uv:false},name:id,model_identifier:id,visible_box:[2,3,1],resolution:{width:32,height:32},elements:editable,outliner:groups,textures:[{path:`./textures/${id}.png`,name:`${id}.png`,width:32,height:32,uv_width:32,uv_height:32,uuid:uuid(id+':texture'),id:'0',source:'data:image/png;base64,'+texture.toString('base64')}],animations,display});
    write(path.join(source,`textures/${id}.png`),texture);
    manifest.assets.push({id,cuboids:geometry.length,bones:groups.map(g=>g.name),phase:manifest.phase,source:`${id}.bbmodel`,runtime:`models/${modelType}/${id}.json`,animations:'Timeline stubs; runtime animation acceptance pending'});
    write(path.join(resource,`assets/lynxrelics/items/${id}.json`),{model:{type:'minecraft:model',model:`lynxrelics:${modelType}/${id}`}});
    if(modelType==='block') write(path.join(resource,`assets/lynxrelics/blockstates/${id}.json`),{variants:{'':{model:`lynxrelics:block/${id}`}}});
  } else {
    write(path.join(resource,`assets/lynxrelics/models/item/${id}.json`),{parent:'minecraft:item/generated',textures:{layer0:tex}});
    write(path.join(resource,`assets/lynxrelics/items/${id}.json`),{model:{type:'minecraft:model',model:`lynxrelics:item/${id}`}});
  }
  for(const lang of ['zh_cn','en_us']) {
    const langPath=path.join(resource,`assets/lynxrelics/lang/${lang}.json`);
    const content=fs.existsSync(langPath)?JSON.parse(fs.readFileSync(langPath,'utf8')):{};
    content[`${isBlock?'block':'item'}.lynxrelics.${id}`]=lang==='zh_cn'?label:id.split('_').map(x=>x[0].toUpperCase()+x.slice(1)).join(' ');
    write(langPath,content);
  }
}
// Native equipment geometry is retained. Only the original, editable pixel material changes.
const armorPalette=palettes[gray?'gray':'end'];
const armorTexture=png(64,32,(x,y)=>{
  const faceWindow=(x>=10&&x<=13&&y>=11&&y<=14);
  if(faceWindow)return [0,0,0,0];
  const n=(x*7+y*13+(x*y)%11)%100;
  return [...armorPalette[n<5?3:n<75?(n%2?1:2):0],255];
});
for(const layer of ['humanoid','humanoid_leggings','humanoid_baby'])write(path.join(resource,`assets/lynxrelics/textures/entity/equipment/${layer}/endforged.png`),armorTexture);
write(path.join(source,'textures/endforged_armor.png'),armorTexture);
write(path.join(resource,'assets/lynxrelics/equipment/endforged.json'),{layers:Object.fromEntries(['humanoid','humanoid_leggings','humanoid_baby'].map(k=>[k,[{texture:'lynxrelics:endforged'}]]))});
write(path.join(resource,'assets/lynxrelics/models/block/speed_rail.json'),{parent:'minecraft:block/rail_flat',textures:{rail:'lynxrelics:block/speed_rail'}});
for(const model of ['rail_curved','rail_raised_ne','rail_raised_sw'])write(path.join(resource,`assets/lynxrelics/models/block/speed_${model}.json`),{parent:'minecraft:block/'+(model==='rail_curved'?model:'template_'+model),textures:{rail:model==='rail_curved'?'minecraft:block/rail_corner':'lynxrelics:block/speed_rail'}});
const railVariants={};
for(const shape of ['north_south','east_west','ascending_east','ascending_west','ascending_north','ascending_south','south_east','south_west','north_west','north_east'])for(let mode=0;mode<4;mode++){
  const rotation={east_west:90,ascending_east:90,ascending_west:90,ascending_north:0,ascending_south:0,south_east:0,south_west:90,north_west:180,north_east:270}[shape]??0;
  const railModel=shape.startsWith('ascending_')?'lynxrelics:block/speed_rail_raised_'+(['ascending_west','ascending_south'].includes(shape)?'sw':'ne'):shape.includes('_')&&!['north_south','east_west'].includes(shape)?'lynxrelics:block/speed_rail_curved':'lynxrelics:block/speed_rail';
  railVariants[`shape=${shape},speed=${mode}`]={model:railModel,...(rotation?{y:rotation}:{})};
}
write(path.join(resource,'assets/lynxrelics/blockstates/speed_rail.json'),{variants:railVariants});
write(path.join(resource,'assets/lynxrelics/items/speed_rail.json'),{model:{type:'minecraft:model',model:'lynxrelics:block/speed_rail'}});
const railTex=png(16,16,(x,y)=>{if(x===3||x===4||x===11||x===12)return [...armorPalette[(x+y)%3+1],255];if(y%4<2&&x>=1&&x<=14)return [86,62,48,255];return [0,0,0,0];});
write(path.join(resource,'assets/lynxrelics/textures/block/speed_rail.png'),railTex);
// GUI art is an independent 32x32 sprite; inventory rendering never shrinks the editable cuboid mesh.
const icons=[],lore={},languages=Object.fromEntries(['zh_cn','en_us'].map(lang=>[lang,JSON.parse(fs.readFileSync(path.join(resource,`assets/lynxrelics/lang/${lang}.json`),'utf8'))]));
for(const id of Object.keys(names)) {
  const pixels=icon32(id,palettes[theme(id)]);icons.push({id,pixels});
  if(!polished[id])write(path.join(resource,`assets/lynxrelics/textures/item/${id}_icon.png`),png(32,32,(x,y)=>pixels[y][x]));
  const handheld=/endforged_(sword|pickaxe|axe|hoe|shovel)|rift_blade|tide_spear|resonant_bow/.test(id);
  write(path.join(resource,`assets/lynxrelics/models/item/${id}_icon.json`),{parent:`minecraft:item/${handheld?'handheld':'generated'}`,textures:{layer0:`lynxrelics:item/${id}_icon`}});
  const spriteModel={type:'minecraft:model',model:`lynxrelics:item/${id}_icon`};
  const block=id.endsWith('_workstation')||['resonance_box','endmarrow_ore','speed_rail'].includes(id);
  const useGeometry=!!templates[id]&&!/endforged_(helmet|chestplate|leggings|boots)/.test(id);
  const heldModel=useGeometry?{type:'minecraft:model',model:`lynxrelics:${block?'block':'item'}/${id}`}:spriteModel;
  write(path.join(resource,`assets/lynxrelics/items/${id}.json`),{model:useGeometry?{type:'minecraft:select',property:'minecraft:display_context',cases:[{when:['gui','ground','fixed','on_shelf'],model:spriteModel}],fallback:heldModel}:spriteModel});
  lore[id]=loreFor(id).map(richLine).map((row,i)=>{
    const key=`lore.v3.lynxrelics.${id}.${i}`;languages.zh_cn[key]=row.zh;languages.en_us[key]=row.en;
    return {key,args:row.args,color:row.color??'gray',bold:row.bold??false,italic:row.italic??false};
  });
}
for(const[id,branches]of Object.entries(specializations))for(let i=0;i<2;i++){const key=`specialization.lynxrelics.${id}.${i===0?'a':'b'}`;languages.zh_cn[key]=branches[i][0];languages.en_us[key]=branches[i][1];}
languages.zh_cn['itemGroup.lynxrelics']='遗珍复苏';languages.en_us['itemGroup.lynxrelics']='LynxRelics';
for(const[key,[zh,en]]of Object.entries(translations)){languages.zh_cn[key]=zh;languages.en_us[key]=en;}
for(const [lang,data]of Object.entries(languages))write(path.join(resource,`assets/lynxrelics/lang/${lang}.json`),data);
write(path.join(resource,'assets/lynxrelics/lore.json'),lore);
write(path.join(root,'assets/icons/catalog.json'),{resolution:[32,32],columns:10,items:icons.map((v,i)=>({id:v.id,column:i%10,row:Math.floor(i/10)}))});
write(path.join(root,'assets/icons/preview.png'),png(720,Math.ceil(icons.length/10)*72,(x,y)=>{
  const col=Math.floor(x/72),row=Math.floor(y/72),i=row*10+col,lx=x%72,ly=y%72;
  if(i>=icons.length)return [24,22,29,255];
  if(lx>=4&&lx<68&&ly>=4&&ly<68){const c=icons[i].pixels[(ly-4)>>1][(lx-4)>>1];if(c[3])return c;}
  return ((col+row)%2)?[42,38,47,255]:[33,30,38,255];
}));
for(const id of ['dragon_workstation','echo_workstation','tide_workstation','resonance_box','endmarrow_ore','speed_rail'])write(path.join(resource,`data/lynxrelics/loot_table/blocks/${id}.json`),{type:'minecraft:block',pools:[{rolls:1,entries:[{type:'minecraft:item',name:`lynxrelics:${id}`}],condition:{type:'minecraft:survives_explosion'}}]});
write(path.join(source,'manifest.json'),manifest);
if(fs.existsSync(path.join(root,'assets/materials/catalog.json'))){await import('./bake-model-materials.mjs');await import('./generate-bow-rig.mjs');}
// Editable, original gray models are kept separate from concept art.
console.log(`${manifest.phase}: ${Object.keys(names).length} item assets, ${manifest.assets.length} editable cuboid models; ${icons.length} original 32px GUI sprites and lore entries`);

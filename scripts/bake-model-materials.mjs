// SPDX-License-Identifier: GPL-3.0-or-later
// Bake original material tiles into Minecraft UV layouts. Block/item UVs use 0..16; ModelPart UVs use pixels.
import fs from'node:fs';import path from'node:path';import crypto from'node:crypto';import{createRequire}from'node:module';import{fileURLToPath}from'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),res=path.join(root,'src/main/resources/assets/lynxrelics'),models=path.join(root,'assets/models');
const arg=process.argv.indexOf('--modules'),require=createRequire(arg>=0?path.join(path.resolve(process.argv[arg+1]),'..','package.json'):import.meta.url),sharp=require('sharp');
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,Buffer.isBuffer(v)?v:JSON.stringify(v,null,2)+'\n')};
const uid=s=>{const h=crypto.createHash('sha256').update(s).digest('hex');return`${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-8${h.slice(17,20)}-${h.slice(20,32)}`};
const catalog=JSON.parse(fs.readFileSync(path.join(root,'assets/materials/catalog.json'),'utf8')),tiles={};
for(const[id,file]of Object.entries(catalog))tiles[id]=await sharp(path.join(root,'assets/materials',file)).resize(32,32,{kernel:'nearest'}).ensureAlpha().raw().toBuffer();
function pixel(tile,x,y){const p=((y%32)*32+x%32)*4;return tile.subarray(p,p+4)}
function canvas(w,h,paint){const raw=Buffer.alloc(w*h*4);for(let y=0;y<h;y++)for(let x=0;x<w;x++){const c=paint(x,y);for(let k=0;k<4;k++)raw[(y*w+x)*4+k]=c[k]??255}return sharp(raw,{raw:{width:w,height:h,channels:4}}).png().toBuffer()}
const glow={dragon:[198,135,230,255],echo:[91,212,222,255],tide:[97,218,199,255],end:[192,134,237,255]};
const routeFor=id=>/echo|resonant|silent|keeper|murmur|moth/.test(id)?'echo':/tide|undertow|jelly|reef|foam|watermirror/.test(id)?'tide':/endforged|endmarrow/.test(id)?'end':'dragon';
function atlas(route){return canvas(64,64,(x,y)=>{
 if(y<16){if(x<16)return pixel(tiles['metal_'+(route==='end'?'endforged':route)],x,y);if(x<32)return pixel(tiles.bone,x-16,y);if(x<48)return pixel(tiles.wood,x-32,y);return pixel(tiles.stone,x-48,y)}
 if(y<32){if(x<16)return pixel(tiles['cloth_'+(route==='end'?'dragon':route)],x,y-16);if(x<32){const edge=x===16||x===31||y===16||y===31;return edge?[182,221,226,255]:[0,0,0,0]}if(x<48)return glow[route];return pixel(tiles.metal_tide,x-48,y-16)}
 return pixel(tiles['metal_'+(route==='end'?'endforged':route)],x,y)
})}
const atlases={};for(const route of['dragon','echo','tide','end']){atlases[route]=await atlas(route);write(path.join(res,`textures/material/${route}.png`),atlases[route])}
const materialTile={metal:[0,0],bone:[16,0],wood:[32,0],stone:[48,0],cloth:[0,16],glass:[16,16],glow:[32,16],copper:[48,16]};
const faces=['north','south','east','west','up','down'];
function materialFor(id,name){
 if(/bottle/.test(name)&&!/stopper/.test(name))return'glass';if(/stopper|grip|shaft/.test(name))return'wood';
 if(/rune|core|gem|boss|liquid|lens/.test(name))return'glow';if(/cloth|membrane|wing/.test(name))return'cloth';
 if(/rim|jaw|tooth|prong|knuckle|collar|fork|shell/.test(name))return'bone';
 if(/band|clasp|copper/.test(name))return'copper';if(/base|slab/.test(name))return id==='echo_workstation'?'metal':'stone';return'metal'
}
function element(id,b){
 const material=b.material??materialFor(id,b.name),d=b.to.map((n,i)=>n-b.from[i]),[u,v]=materialTile[material];
 return{name:b.name,from:b.from,to:b.to,faces:Object.fromEntries(faces.map(face=>{
  const size=face==='up'||face==='down'?[d[0],d[2]]:face==='east'||face==='west'?[d[2],d[1]]:[d[0],d[1]];
  return[face,{uv:[u/4,v/4,(u+Math.min(16,Math.max(.5,size[0])))/4,(v+Math.min(16,Math.max(.5,size[1])))/4],texture:material==='glass'?'#glass':'#0'}]
 }))}
}
const cube=(name,from,to,material)=>({name,from,to,material});
function station(route){
 const a=[cube('base',[0,0,0],[16,3,16],route==='echo'?'metal':'stone')];
 if(route==='dragon'){
  a.push(cube('slab',[1,10,1],[15,12,15],'stone'),cube('liquid',[4,12,4],[12,12.35,12],'glow'));
  for(const x of[0,13])for(const z of[0,13])a.push(cube('leg_'+x+'_'+z,[x,3,z],[x+3,13,z+3]),cube('band_'+x+'_'+z,[x,8,z],[x+3,9,z+3],'copper'));
  for(const[x,z,w,d]of[[3,3,10,1],[3,12,10,1],[3,4,1,8],[12,4,1,8]])a.push(cube('basin_rim_'+x+'_'+z,[x,12,z],[x+w,13,z+d],'copper'));
  for(const[i,[x,z]]of[[2,2],[7,1],[12,2],[13,7],[12,12],[7,13],[2,12],[1,7]].entries()){a.push(cube('bottle_'+i,[x,12,z],[x+2,15,z+2],'glass'),cube('stopper_'+i,[x+.4,15,z+.4],[x+1.6,15.7,z+1.6],'wood'))}
 }else if(route==='echo'){
  a.push(cube('case',[1,3,1],[15,12,15]),cube('top',[1,12,1],[15,13,15]));
  for(const x of[0,14])for(const z of[0,14])a.push(cube('bone_corner_'+x+'_'+z,[x,3,z],[x+2,13,z+2],'bone'));
  for(const r of[2,4,6])for(const[x,z,w,d]of[[r,r,16-2*r,.5],[r,15.5-r,16-2*r,.5],[r,r,.5,16-2*r],[15.5-r,r,.5,16-2*r]])a.push(cube('ring_'+a.length,[x,13,z],[x+w,13.25,z+d],r===6?'glow':'copper'));
  for(let i=0;i<3;i++){const x=3+i*4;a.push(cube('fork_l_'+i,[x,13,12],[x+.6,15.8,12.6],'bone'),cube('fork_r_'+i,[x+1.4,13,12],[x+2,15.8,12.6],'bone'),cube('fork_base_'+i,[x,13,12],[x+2,13.6,12.6],'copper'))}
 }else{
  for(const x of[0,13])for(const z of[0,13])a.push(cube('frame_'+x+'_'+z,[x,3,z],[x+3,15,z+3]));
  a.push(cube('water_core',[3,3,3],[13,10,13],'glow'));
  for(const[x,z,w,d]of[[2,2,12,1],[2,13,12,1],[2,3,1,10],[13,3,1,10]])a.push(cube('rim_'+a.length,[x,13,z],[x+w,15,z+d]));
  a.push(cube('glass_front',[3,3,1],[13,13,1.2],'glass'),cube('glass_back',[3,3,14.8],[13,13,15],'glass'),cube('glass_left',[1,3,3],[1.2,13,13],'glass'),cube('glass_right',[14.8,3,3],[15,13,13],'glass'),cube('shell',[6,5,.4],[10,8,1.1],'bone'));
 }
 a.push(cube('rune_front',[6,5,0],[10,8,.35],'glow'));return a
}
const displayBlock={gui:{rotation:[30,225,0],scale:[.625,.625,.625]},ground:{translation:[0,3,0],scale:[.25,.25,.25]},fixed:{scale:[.5,.5,.5]},thirdperson_righthand:{rotation:[75,45,0],translation:[0,2.5,0],scale:[.375,.375,.375]},firstperson_righthand:{rotation:[0,45,0],scale:[.4,.4,.4]},firstperson_lefthand:{rotation:[0,225,0],scale:[.4,.4,.4]}};
const manifest=JSON.parse(fs.readFileSync(path.join(models,'manifest.json'),'utf8'));
for(const asset of manifest.assets){
 const id=asset.id,route=routeFor(id),modelPath=path.join(res,asset.runtime);let model=JSON.parse(fs.readFileSync(modelPath,'utf8'));
 const raw=id.endsWith('_workstation')?station(route):(model.elements??[]).map(e=>({name:e.name,from:e.from,to:e.to}));
 model={...model,textures:{'0':`lynxrelics:material/${route}`,glass:{sprite:`lynxrelics:material/${route}`,force_translucent:true},particle:`lynxrelics:material/${route}`},elements:raw.map(b=>element(id,b))};
 if(asset.runtime.startsWith('models/block/'))model.display=displayBlock;
 if(id==='endmarrow_ore'){
  model={parent:'minecraft:block/cube_all',textures:{all:'lynxrelics:block/endmarrow_ore'}};
  write(path.join(res,'textures/block/endmarrow_ore.png'),await sharp(path.join(root,'assets/materials',catalog.ore_endmarrow)).resize(32,32,{kernel:'nearest'}).png().toBuffer());
 }
 write(modelPath,model);
 const sourcePath=path.join(models,asset.source),bb=JSON.parse(fs.readFileSync(sourcePath,'utf8'));
 if(id!=='endmarrow_ore'){
  const els=model.elements;bb.elements=els.map((e,i)=>({...e,type:'cube',uuid:uid(id+':textured:'+i),box_uv:false,faces:Object.fromEntries(Object.entries(e.faces).map(([f,s])=>[f,{...s,uv:s.uv.map(n=>n*4),texture:0}]))}));
  bb.outliner=[{name:'root',uuid:uid(id+':textured:root'),origin:[8,8,8],children:bb.elements.map(e=>e.uuid)}];
  bb.resolution={width:64,height:64};bb.textures=[{name:route+'.png',id:'0',width:64,height:64,uv_width:64,uv_height:64,source:'data:image/png;base64,'+atlases[route].toString('base64')}];
  if(asset.runtime.startsWith('models/block/'))bb.display=displayBlock;
  write(sourcePath,bb);write(path.join(models,'textures',id+'.png'),atlases[route]);asset.cuboids=els.length;
 }
 asset.phase='textured';asset.texture=`textures/material/${route}.png`;
}
manifest.phase='textured';manifest.materialCompiler='scripts/bake-model-materials.mjs';write(path.join(models,'manifest.json'),manifest);

// Reconstruct the native armor UV islands and openings; no Mojang color texture is redistributed.
function armorMask(leggings){
 const mask=Array.from({length:32},()=>Array(64).fill(false)),rect=(x0,y0,x1,y1,value=true)=>{for(let y=y0;y<y1;y++)for(let x=x0;x<x1;x++)mask[y][x]=value};
 if(leggings){rect(4,16,8,20);rect(0,20,16,29);rect(16,28,40,32);rect(16,27,18,28);rect(22,27,26,28);rect(30,27,40,28)}else{
  rect(8,0,16,8);rect(0,8,32,16);
  const rows=['#########..##..#################','####..###..##..###..############','.......##......##.......########','.......###....###.........####..','.......###....###...............'];
  rows.forEach((row,i)=>{for(let x=0;x<32;x++)mask[11+i][x]=row[x]==='#'});
  rect(8,16,12,20);rect(44,16,48,20);rect(16,20,56,27);rect(22,20,26,22,false);rect(23,22,25,23,false);
  rect(3,26,9,27);rect(0,27,40,29);rect(0,29,16,32);rect(20,29,28,30);rect(32,29,40,30);rect(21,30,27,31);rect(33,30,39,31);rect(22,31,26,32);rect(34,31,38,32);
 }
 return mask
}
for(const layer of['humanoid','humanoid_leggings','humanoid_baby']){
 const mask=armorMask(layer==='humanoid_leggings');const texture=await canvas(64,32,(x,y)=>mask[y][x]?pixel(tiles.metal_endforged,x,y):[0,0,0,0]);
 write(path.join(res,`textures/entity/equipment/${layer}/endforged.png`),texture);write(path.join(models,`textures/endforged_${layer}.png`),texture);
}

// ModelPart uses pixel UVs. Preserve each family's net and give cloth an independent material region.
for(const route of['dragon','echo','tide']){
 const image=await canvas(64,64,(x,y)=>y>=48?glow[route]:y>=32?pixel(tiles.bone,x,y-32):x>=32?pixel(tiles['cloth_'+route],x-32,y):pixel(tiles['metal_'+route],x,y));
 write(path.join(res,`textures/entity/accessories/${route}.png`),image);
 for(const family of['necklace','belt','gauntlet','cloak'])if(route==='dragon'){
  const source=path.join(models,`accessories/${family}.bbmodel`),bb=JSON.parse(fs.readFileSync(source,'utf8'));
  bb.textures[0].source='data:image/png;base64,'+image.toString('base64');
  if(family==='cloak')for(const e of bb.elements)if(e.name.startsWith('cloth_'))e.uv_offset=[32,Math.max(0,Math.floor(24-e.to[1]-1))];
  write(source,bb);
 }
}
const cloakPath=path.join(res,'geometry/accessory_cloak.json'),cloak=JSON.parse(fs.readFileSync(cloakPath,'utf8'));
for(const b of cloak.bones)for(const c of b.cubes)if(c.name.startsWith('cloth_')){c.material='cloth';c.uv=[32,Math.max(0,Math.floor(c.from[1]-1))]};write(cloakPath,cloak);

// Seven entity skins retain their pixel UV bands while receiving the original material texture.
const mobManifestPath=path.join(models,'mobs/manifest.json');
if(fs.existsSync(mobManifestPath)){
 const mobs=JSON.parse(fs.readFileSync(mobManifestPath,'utf8'));
 for(const mob of mobs){const id=mob.id,size=mob.texture[0],route=routeFor(id),quarter=size/4;
  const skin=await canvas(size,size,(x,y)=>{const band=Math.floor(y/quarter);return band===3?glow[route]:pixel(tiles[band===2?'bone':band===1?'cloth_'+route:'metal_'+route],x,y%quarter)});
  write(path.join(res,`textures/entity/${id}.png`),skin);write(path.join(models,`mobs/textures/${id}.png`),skin);
  const file=path.join(models,`mobs/${id}.bbmodel`),bb=JSON.parse(fs.readFileSync(file,'utf8'));bb.textures[0].source='data:image/png;base64,'+skin.toString('base64');write(file,bb);mob.phase='textured';
 }
 write(mobManifestPath,mobs);
}

const receipt={phase:'textured',blockUvRange:[0,16],modelPartUvUnit:'pixels',armorTexture:[64,32],materialSources:catalog,outputs:manifest.assets.map(a=>({id:a.id,model:a.runtime}))};
write(path.join(root,'assets/materials/compiled.json'),receipt);
console.log(`Baked ${manifest.assets.length} model materials, three accessory atlases and native armor UV masks; all block UVs bounded to 0..16.`);

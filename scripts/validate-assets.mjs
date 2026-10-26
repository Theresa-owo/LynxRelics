// SPDX-License-Identifier: GPL-3.0-or-later
// Structural checks for the demonstrated UV and localization regressions; not a client visual acceptance test.
import fs from'node:fs';import path from'node:path';import{fileURLToPath}from'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),res=path.join(root,'src/main/resources/assets/lynxrelics');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8')),errors=[],check=(ok,label)=>{if(!ok)errors.push(label)};
const languages=Object.fromEntries(['zh_cn','en_us'].map(k=>[k,read(path.join(res,`lang/${k}.json`))]));
const lore=read(path.join(res,'lore.json')),catalog=read(path.join(root,'assets/icons/catalog.json'));
const atlasSprites=Object.fromEntries(['blocks','items'].map(name=>[name,new Set(read(path.join(root,`src/main/resources/assets/minecraft/atlases/${name}.json`)).sources.filter(s=>s.type==='minecraft:single').map(s=>s.sprite??s.resource))]));
function key(k){for(const[locale,table]of Object.entries(languages))check(typeof table[k]==='string',`${locale} missing ${k}`)}
for(const{id}of catalog.items){
 const file=path.join(res,`textures/item/${id}_icon.png`),png=fs.readFileSync(file);
 check(png.readUInt32BE(16)===64&&png.readUInt32BE(20)===64,`Icon size: ${id}`);
 check(Array.isArray(lore[id])&&lore[id].length>0,`Missing lore: ${id}`);
 for(const row of lore[id]??[]){key(row.key);for(const locale of Object.keys(languages)){
  const text=languages[locale][row.key],positions=[...text.matchAll(/%(\d+)\$s/g)].map(v=>Number(v[1]));
  check(positions.every(i=>i>=1&&i<=row.args.length),`Lore argument range: ${locale}/${row.key}`);
 }}
}
const files=[];function walk(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())walk(p);else if(e.name.endsWith('.json'))files.push(p)}}walk(path.join(res,'models'));
for(const file of files){const model=read(file);for(const element of model.elements??[]){
 check(element.from.every(Number.isFinite)&&element.to.every(Number.isFinite),`Invalid cube: ${file}`);
 check(element.from.every((n,i)=>n<=element.to[i]),`Inverted cube: ${file}/${element.name}`);
 for(const[face,data]of Object.entries(element.faces??{}))if(data.uv)check(data.uv.length===4&&data.uv.every(v=>Number.isFinite(v)&&v>=0&&v<=16),`UV outside 0..16: ${file}/${element.name}/${face}`);
 }
 for(const value of Object.values(model.textures??{})){
  const id=typeof value==='object'?value.sprite:value;if(typeof id==='string'&&id.startsWith('lynxrelics:')){
   check(fs.existsSync(path.join(res,'textures',id.slice(11)+'.png')),`Missing texture: ${file}/${id}`);
   if(id.startsWith('lynxrelics:material/'))for(const[name,sprites]of Object.entries(atlasSprites))check(sprites.has(id),`Unstitched model material: ${name}/${id}`);
  }
 }
}
key('key.category.lynxrelics.controls');key('key.lynxrelics.cloak');
const kotlin=[];function kt(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())kt(p);else if(e.name.endsWith('.kt'))kotlin.push(p)}}kt(path.join(root,'src/main/kotlin'));
for(const file of kotlin){if(file.includes(path.sep+'verification'+path.sep))continue;const source=fs.readFileSync(file,'utf8');
 for(const m of source.matchAll(/(?:Component\.translatable|tr)\("([^"$]+)"/g)){
  const k=m[1].includes('.')?m[1]:'ui.lynxrelics.'+m[1];if(k.includes('lynxrelics'))key(k);
 }
 check(!/Component\.literal\("[^"\n]*[\p{Script=Han}]/u.test(source),`Hardcoded user-facing Chinese: ${file}`);
}
const bow=read(path.join(root,'assets/models/bow-rig-validation.json'));check(bow.states===11&&bow.symmetricLimbs&&bow.equalFixedStringHalves,'Bow symmetry audit');
const rail=read(path.join(res,'blockstates/speed_rail.json'));check(Object.keys(rail.variants).length===40,'Missing rail shapes/speed variants');
if(errors.length){console.error([...new Set(errors)].join('\n'));process.exitCode=1}else console.log(`Validated ${catalog.items.length} icons/lore entries, ${files.length} models, bilingual keys, bounded UVs, material sprites in blocks/items atlases and symmetric bow rig. Client visual acceptance remains separate.`);

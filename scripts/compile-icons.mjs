// SPDX-License-Identifier: GPL-3.0-or-later
// Lossless source preservation + deterministic runtime texture compilation. Artistic changes use imagegen.
import fs from 'node:fs';import path from 'node:path';import crypto from 'node:crypto';import{createRequire}from'node:module';import{fileURLToPath}from'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const moduleArg=process.argv.indexOf('--modules');
const require=createRequire(moduleArg>=0?path.join(path.resolve(process.argv[moduleArg+1]),'..','package.json'):import.meta.url);
const sharp=require('sharp');
const sources=JSON.parse(fs.readFileSync(path.join(root,'assets/icons/polished.json'),'utf8'));
const receipts=[],previews=[];
async function compile(id,relative){
 const source=path.join(root,'assets/icons',relative),dest=path.join(root,`src/main/resources/assets/lynxrelics/textures/item/${id}_icon.png`);
 const stats=await sharp(source).stats();
 if(stats.channels.length!==4||stats.channels[3].min!==0||stats.channels[3].max===0)throw new Error('Source must have visible pixels and genuine transparent alpha: '+id);
 const trimmed=await sharp(source).trim({background:'#00000000',threshold:1}).png().toBuffer();
 const fitted=await sharp(trimmed).resize(58,58,{fit:'inside',kernel:'nearest'}).png().toBuffer();
 const meta=await sharp(fitted).metadata(),left=Math.floor((64-meta.width)/2),top=Math.floor((64-meta.height)/2);
 const output=await sharp({create:{width:64,height:64,channels:4,background:{r:0,g:0,b:0,alpha:0}}}).composite([{input:fitted,left,top}]).png().toBuffer();
 fs.writeFileSync(dest,output);
 return {output,receipt:{id,source:'assets/icons/'+relative,resolution:[64,64],sourceSha256:crypto.createHash('sha256').update(fs.readFileSync(source)).digest('hex'),runtimeSha256:crypto.createHash('sha256').update(output).digest('hex')}};
}
const catalog=JSON.parse(fs.readFileSync(path.join(root,'assets/icons/catalog.json'),'utf8'));
for(const{id}of catalog.items)if(sources[id]){const result=await compile(id,sources[id]);receipts.push(result.receipt);previews.push(await sharp(result.output).resize(128,128,{kernel:'nearest'}).png().toBuffer());}
const columns=Math.max(1,Math.min(10,previews.length)),width=columns*144,height=Math.ceil(previews.length/columns)*144;
await sharp({create:{width,height,channels:4,background:'#24212aff'}}).composite(previews.map((input,i)=>({input,left:(i%columns)*144+8,top:Math.floor(i/columns)*144+8}))).png().toFile(path.join(root,'assets/icons/polished-preview.png'));
fs.writeFileSync(path.join(root,'assets/icons/compiled.json'),JSON.stringify(receipts,null,2)+'\n');
console.log(`Compiled ${receipts.length} textured transparent source icons to 64x64; originals retained.`);
const posesPath=path.join(root,'assets/icons/poses.json');
if(fs.existsSync(posesPath)){
 const poses=JSON.parse(fs.readFileSync(posesPath,'utf8')),poseReceipts=[];
 for(const[id,relative]of Object.entries(poses))poseReceipts.push((await compile(id,relative)).receipt);
 fs.writeFileSync(path.join(root,'assets/icons/poses-compiled.json'),JSON.stringify(poseReceipts,null,2)+'\n');
 console.log(`Compiled ${poseReceipts.length} separate native item poses.`);
}

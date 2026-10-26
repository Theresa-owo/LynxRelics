// SPDX-License-Identifier: GPL-3.0-or-later
// Both limbs and both string halves are calculated from one symmetric, fixed-length rig.
import fs from'node:fs';import path from'node:path';import crypto from'node:crypto';import{fileURLToPath}from'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),res=path.join(root,'src/main/resources/assets/lynxrelics');
const write=(p,v)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n')};
const uid=s=>{const h=crypto.createHash('sha256').update(s).digest('hex');return`${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-8${h.slice(17,20)}-${h.slice(20,32)}`};
const rad=n=>n*Math.PI/180,mid=(a,b)=>a.map((n,i)=>(n+b[i])/2),faces=['north','south','east','west','up','down'];
const uv={wood:[8,0,12,4],bone:[4,0,8,4],metal:[0,0,4,4],rune:[8,4,12,8]};
const display={thirdperson_righthand:{rotation:[-80,260,-70],translation:[-1,-2,2.5],scale:[.9,.9,.9]},thirdperson_lefthand:{rotation:[-80,-280,70],translation:[-1,-2,2.5],scale:[.9,.9,.9]},firstperson_righthand:{rotation:[0,-90,-5],translation:[1.13,3.2,1.13],scale:[.68,.68,.68]},firstperson_lefthand:{rotation:[0,90,5],translation:[1.13,3.2,1.13],scale:[.68,.68,.68]},ground:{translation:[0,3,0],scale:[.4,.4,.4]},fixed:{scale:[.6,.6,.6]}};
function upper(t){
 const points=[[5.6,9.2,8]],lengths=[2.7,2.7,1.7],angles=[-10+12*t,28+12*t,58+12*t];
 for(let i=0;i<3;i++){const a=points.at(-1);points.push([a[0]+Math.sin(rad(angles[i]))*lengths[i],a[1]+Math.cos(rad(angles[i]))*lengths[i],8])}
 return points
}
const mirror=a=>[a[0],16-a[1],a[2]],halfString=upper(0).at(-1)[1]-8;
function beam(name,a,b,width,depth,material){
 const center=mid(a,b),dx=b[0]-a[0],dy=b[1]-a[1],length=Math.hypot(dx,dy),vertical=Math.abs(dy)>=Math.abs(dx);
 let angle=vertical?-Math.atan2(dx*Math.sign(dy||1),Math.abs(dy))*180/Math.PI:Math.atan2(dy*Math.sign(dx||1),Math.abs(dx))*180/Math.PI;
 if(Math.abs(angle)>45.00001)throw new Error('Invalid block element rotation: '+name);
 const from=[center[0]-(vertical?width:length)/2,center[1]-(vertical?length:width)/2,center[2]-depth/2];
 const to=[center[0]+(vertical?width:length)/2,center[1]+(vertical?length:width)/2,center[2]+depth/2];
 return{name,from,to,rotation:{origin:center,axis:'z',angle},faces:Object.fromEntries(faces.map(f=>[f,{uv:uv[material],texture:'#0'}])),rig:{center,angle:Math.atan2(dy,dx)*180/Math.PI}}
}
function pose(t){
 const top=upper(t),bottom=top.map(mirror),tip=top.at(-1),height=tip[1]-8,draw=[tip[0]+Math.sqrt(Math.max(0,halfString*halfString-height*height)),8,8];
 const parts=[];
 for(const[side,points]of[['upper',top],['lower',bottom]])for(let i=0;i<3;i++){
  parts.push(beam(`${side}_limb_${i}`,points[i],points[i+1],.65,.65,'wood'));
  if(i>0){const a=points[i],b=points[i+1],lerp=f=>a.map((n,k)=>n+(b[k]-n)*f);const p=lerp(.18),q=lerp(.82);p[2]=q[2]=7.58;parts.push(beam(`${side}_bone_${i}`,p,q,.77,.2,'bone'))}
 }
 parts.push(beam('upper_string',top.at(-1),draw,.16,.16,'bone'),beam('lower_string',bottom.at(-1),draw,.16,.16,'bone'));
 parts.push(beam('grip',[5.6,6.8,8],[5.6,9.2,8],.9,.85,'metal'),beam('rune',[5.6,7.6,7.5],[5.6,8.4,7.5],.55,.12,'rune'));
 for(let i=0;i<top.length;i++)if(Math.abs(top[i][0]-bottom[i][0])>1e-8||Math.abs(top[i][1]+bottom[i][1]-16)>1e-8)throw new Error('Asymmetric bow');
 if(Math.abs(Math.hypot(tip[0]-draw[0],tip[1]-8)-halfString)>1e-8)throw new Error('String length changed');
 return{parts,top,bottom,draw}
}
const poses=Array.from({length:11},(_,i)=>pose(i/10));
for(let i=0;i<poses.length;i++)write(path.join(res,`models/item/resonant_bow_draw_${i}.json`),{credit:'LynxRelics symmetric bow rig',textures:{'0':'lynxrelics:material/echo',particle:'lynxrelics:material/echo'},display,elements:poses[i].parts.map(({rig,...e})=>e)});
const model=i=>({type:'minecraft:model',model:`lynxrelics:item/resonant_bow_draw_${i}`});
write(path.join(res,'items/resonant_bow.json'),{model:{type:'minecraft:select',property:'minecraft:display_context',cases:[{when:['gui','ground','fixed','on_shelf'],model:{type:'minecraft:model',model:'lynxrelics:item/resonant_bow_icon'}}],fallback:{type:'minecraft:condition',property:'minecraft:using_item',on_false:model(0),on_true:{type:'minecraft:range_dispatch',property:'minecraft:use_duration',scale:.05,fallback:model(0),entries:Array.from({length:10},(_,i)=>({threshold:(i+1)/10,model:model(i+1)}))}}}});
const elements=[],groups=[],animators={};
for(const p of poses[0].parts){
 const gid=uid('bow:bone:'+p.name),eid=uid('bow:cube:'+p.name),{rotation,rig,...e}=p;
 elements.push({...e,type:'cube',uuid:eid,origin:rotation.origin,rotation:[0,0,rotation.angle],box_uv:false,faces:Object.fromEntries(Object.entries(e.faces).map(([f,s])=>[f,{...s,uv:s.uv.map(n=>n*4),texture:0}]))});
 groups.push({name:p.name,uuid:gid,origin:rig.center,rotation:[0,0,0],children:[eid]});
 const keyframes=[];
 for(let i=0;i<poses.length;i++){
  const next=poses[i].parts.find(e=>e.name===p.name).rig,time=i/10;let angle=next.angle-rig.angle;while(angle>180)angle-=360;while(angle< -180)angle+=360;
  keyframes.push({channel:'rotation',time,interpolation:'linear',data_points:[{x:'0',y:'0',z:String(angle)}]},{channel:'position',time,interpolation:'linear',data_points:[{x:String(next.center[0]-rig.center[0]),y:String(next.center[1]-rig.center[1]),z:String(next.center[2]-rig.center[2])}]});
 }
 animators[gid]={name:p.name,type:'bone',keyframes};
}
const texture=fs.readFileSync(path.join(res,'textures/material/echo.png'));
write(path.join(root,'assets/models/resonant_bow.bbmodel'),{meta:{format_version:'4.10',model_format:'free',box_uv:false},name:'resonant_bow',resolution:{width:64,height:64},elements,outliner:[{name:'root',uuid:uid('bow:root'),origin:[5.6,8,8],children:groups}],textures:[{name:'echo.png',id:'0',width:64,height:64,uv_width:64,uv_height:64,source:'data:image/png;base64,'+texture.toString('base64')}],animations:[{name:'draw',uuid:uid('bow:draw'),length:1,loop:'hold',snapping:20,animators}],display});
write(path.join(root,'assets/models/bow-rig-validation.json'),{states:11,symmetricLimbs:true,fixedSegmentLengths:true,equalFixedStringHalves:true,drawPoints:poses.map(p=>p.draw),source:'resonant_bow.bbmodel',runtime:'native using_item/use_duration'});
console.log('Generated 11 symmetric native bow poses and matching editable bone animation.');

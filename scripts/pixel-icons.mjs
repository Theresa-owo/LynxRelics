// SPDX-License-Identifier: GPL-3.0-or-later
// Original 16x16 pixel drawings, authored on a fixed pixel grid; no rescaled concept art.
export function icon(id, palette) {
  const p=Array.from({length:16},()=>Array.from({length:16},()=>[0,0,0,0]));
  const c=palette.map(rgb=>[...rgb,255]), outline=[30,25,35,255], iron=[170,180,190,255], light=[225,234,225,255], wood=[113,76,49,255], gold=[203,155,66,255];
  const dot=(x,y,color)=>{if(x>=0&&x<16&&y>=0&&y<16)p[y][x]=color;};
  const rect=(x0,y0,x1,y1,color)=>{for(let y=y0;y<=y1;y++)for(let x=x0;x<=x1;x++)dot(x,y,color);};
  const line=(x0,y0,x1,y1,color,width=1)=>{let dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy;for(;;){rect(x0,y0,x0+width-1,y0+width-1,color);if(x0===x1&&y0===y1)break;const e=2*err;if(e>=dy){err+=dy;x0+=sx;}if(e<=dx){err+=dx;y0+=sy;}}};
  const poly=(points,color)=>{for(let y=0;y<16;y++)for(let x=0;x<16;x++){let inside=false;for(let i=0,j=points.length-1;i<points.length;j=i++){const a=points[i],b=points[j];if(((a[1]>y+.5)!==(b[1]>y+.5))&&(x+.5<(b[0]-a[0])*(y+.5-a[1])/(b[1]-a[1])+a[0]))inside=!inside;}if(inside)dot(x,y,color);}};
  const disc=(cx,cy,r,color)=>{for(let y=0;y<16;y++)for(let x=0;x<16;x++)if((x-cx)**2+(y-cy)**2<=r*r)dot(x,y,color);};
  const border=()=>{const old=p.map(row=>row.map(v=>[...v]));for(let y=0;y<16;y++)for(let x=0;x<16;x++)if(old[y][x][3])for(const [dx,dy]of [[1,0],[-1,0],[0,1],[0,-1]])if(x+dx>=0&&x+dx<16&&y+dy>=0&&y+dy<16&&!old[y+dy][x+dx][3])dot(x+dx,y+dy,outline);};
  const gem=(cx=8,cy=8,r=4)=>{poly([[cx,cy-r],[cx+r,cy],[cx,cy+r],[cx-r,cy]],c[1]);poly([[cx,cy-r],[cx,cy],[cx-r,cy]],c[3]);poly([[cx,cy],[cx+r,cy],[cx,cy+r]],c[2]);};
  if(id.endsWith('_necklace')){
    line(3,2,3,6,gold);line(12,2,12,6,gold);line(3,6,7,10,gold);line(12,6,8,10,gold);rect(4,1,11,1,gold);gem(8,11,3);dot(7,10,c[3]);
  }else if(id.endsWith('_belt')){
    rect(1,6,14,10,wood);rect(1,5,14,6,c[0]);rect(1,10,14,10,c[1]);rect(5,4,10,11,gold);rect(6,5,9,10,c[0]);rect(7,6,8,9,c[2]);dot(7,6,c[3]);
  }else if(id.endsWith('_gauntlet')){
    rect(5,6,12,11,c[1]);for(let x=5;x<=11;x+=2){rect(x,3,x+1,7,c[2]);dot(x,3,c[3]);}rect(3,7,5,9,c[2]);rect(6,11,11,14,c[0]);rect(6,12,11,12,gold);gem(8,8,2);
  }else if(id.endsWith('_cloak')||id==='raid_cloak_pattern'){
    poly([[4,2],[11,2],[13,14],[8,12],[2,14]],c[1]);poly([[4,3],[7,3],[7,12],[3,13]],c[2]);line(10,3,12,13,c[0]);rect(4,2,11,3,light);dot(7,4,c[3]);
  }else if(id.includes('workstation')){
    poly([[2,8],[8,5],[14,8],[8,11]],c[2]);poly([[2,8],[8,11],[8,14],[2,11]],c[0]);poly([[8,11],[14,8],[14,11],[8,14]],c[1]);
    if(id.startsWith('dragon')){disc(8,5,2.2,c[0]);rect(3,5,4,7,light);rect(11,5,12,7,light);dot(3,6,c[2]);dot(11,6,c[2]);dot(8,3,c[3]);}
    else if(id.startsWith('echo')){for(const x of [4,8,12]){rect(x,3,x,7,iron);dot(x,3,c[3]);}line(5,12,10,12,c[3]);}
    else {rect(5,3,10,7,light);rect(6,5,9,7,c[2]);rect(7,2,8,2,gold);}
  }else if(id==='resonance_box'){
    rect(3,3,12,13,wood);rect(4,4,11,12,c[0]);disc(8,8,3.2,c[2]);disc(8,8,1.5,c[0]);rect(5,2,10,3,c[1]);rect(6,2,9,2,outline);
  }else if(id==='endmarrow_ore'){
    poly([[2,5],[8,2],[14,5],[8,8]],light);poly([[2,5],[8,8],[8,14],[2,11]],[190,186,137,255]);poly([[8,8],[14,5],[14,11],[8,14]],[164,164,116,255]);line(4,7,6,8,c[2]);dot(5,10,c[3]);line(10,9,12,8,c[2]);dot(9,12,c[2]);
  }else if(id==='speed_rail'){
    for(const y of [3,6,9,12])rect(2,y,13,y+1,wood);rect(3,1,4,14,iron);rect(11,1,12,14,iron);rect(3,1,3,14,light);rect(11,1,11,14,light);rect(6,6,9,10,c[0]);rect(7,7,8,9,c[3]);
  }else if(id.endsWith('_helmet')){
    rect(3,3,12,11,c[1]);rect(4,2,11,4,c[2]);rect(5,6,10,9,[0,0,0,0]);rect(7,5,8,7,c[3]);rect(3,10,4,12,c[0]);rect(11,10,12,12,c[0]);
  }else if(id.endsWith('_chestplate')){
    poly([[2,3],[5,2],[6,4],[9,4],[10,2],[14,3],[13,7],[11,6],[11,13],[4,13],[4,6],[2,7]],c[1]);line(5,5,5,11,c[2]);line(6,12,10,12,c[3]);rect(2,3,4,4,c[2]);rect(11,3,13,4,c[2]);
  }else if(id.endsWith('_leggings')){
    rect(3,2,12,5,c[2]);rect(3,5,6,13,c[1]);rect(9,5,12,13,c[1]);rect(4,7,4,12,c[3]);rect(10,7,10,12,c[2]);rect(3,2,12,2,c[0]);
  }else if(id.endsWith('_boots')){
    rect(3,3,6,11,c[1]);rect(9,3,12,11,c[1]);rect(1,10,6,13,c[2]);rect(9,10,14,13,c[2]);rect(1,13,6,13,c[0]);rect(9,13,14,13,c[0]);line(3,4,5,4,c[3]);line(9,4,11,4,c[3]);
  }else if(/sword|rift_blade/.test(id)){
    line(3,13,7,9,wood,2);poly([[6,8],[11,2],[14,1],[14,4],[8,10]],c[1]);line(8,7,13,2,c[3]);line(5,7,9,11,gold,2);dot(3,13,c[2]);
  }else if(/pickaxe|_axe|shovel|_hoe/.test(id)){
    line(3,13,10,6,wood,2);line(4,13,10,7,[172,125,76,255]);
    if(id.includes('pickaxe')){poly([[5,2],[10,2],[14,6],[14,10],[12,8],[11,5],[7,4]],c[2]);line(6,2,10,3,c[3]);}
    else if(id.endsWith('_axe')){poly([[8,2],[13,2],[14,7],[11,8],[8,5],[6,4]],c[2]);line(9,2,12,2,c[3]);}
    else if(id.includes('shovel')){poly([[9,2],[13,1],[14,5],[11,8],[8,5]],c[2]);line(10,2,12,2,c[3]);}
    else{line(6,2,12,4,c[2],2);rect(12,4,13,6,c[1]);line(7,2,10,3,c[3]);}
  }else if(id==='tide_spear'){
    line(2,14,10,6,wood);line(3,14,11,6,c[2]);line(8,3,12,7,c[1]);line(8,3,11,0,c[3]);line(10,5,14,1,c[3]);line(12,7,15,4,c[3]);
  }else if(id==='echo_shield'){
    poly([[3,2],[12,2],[12,9],[10,12],[8,14],[5,12],[3,9]],iron);poly([[4,3],[11,3],[11,9],[8,12],[4,9]],c[0]);line(5,5,9,5,c[2]);line(9,5,9,8,c[2]);line(6,8,8,8,c[3]);
  }else if(id==='resonant_bow'){
    line(4,2,10,4,c[2],2);line(10,4,12,8,c[2],2);line(12,8,10,12,c[1],2);line(10,12,4,14,c[1]);line(4,2,4,14,light);line(9,6,10,10,wood);dot(7,3,c[3]);
  }else if(id.endsWith('_arrow')){
    line(3,13,12,4,wood);poly([[10,2],[14,1],[14,5]],iron);line(2,11,4,13,c[2],2);dot(3,12,c[3]);
  }else if(id==='cruise_firework'){
    poly([[6,4],[8,1],[10,4]],c[2]);rect(6,4,10,11,light);rect(6,6,10,7,c[2]);rect(6,10,10,11,c[1]);line(6,12,4,14,gold);line(10,12,12,14,gold);dot(8,13,c[3]);
  }else if(id==='bound_pearl'||id==='condensed_tide'||id==='jelly_pearl'){
    disc(8,8,5.2,c[0]);disc(7,7,4.3,c[1]);poly([[5,4],[8,3],[10,5],[7,6]],c[3]);line(4,9,7,12,c[2]);dot(11,8,c[2]);
  }else if(id==='dragon_essence'){
    rect(6,1,9,2,wood);rect(7,3,8,5,light);poly([[5,5],[10,5],[12,8],[11,13],[4,13],[3,8]],light);rect(5,8,10,12,c[2]);rect(4,9,4,11,c[1]);dot(5,7,c[3]);
  }else if(id.endsWith('_dust')){
    poly([[3,11],[5,8],[7,8],[9,5],[11,8],[13,11],[11,13],[4,13]],c[1]);rect(4,11,11,12,c[2]);dot(9,6,c[3]);dot(6,9,c[3]);dot(3,6,c[2]);
  }else if(id.endsWith('_ingot')||id==='tideforged_copper'){
    poly([[3,5],[11,4],[14,8],[12,11],[3,12],[1,9]],c[0]);poly([[3,5],[11,4],[13,7],[4,9],[2,8]],c[2]);line(4,5,10,4,c[3]);line(4,10,11,9,c[1]);
  }else if(id==='resonant_bone'){
    line(4,12,11,5,light,2);disc(3,12,1.5,light);disc(5,13,1.5,light);disc(11,3,1.5,light);disc(13,5,1.5,light);dot(8,8,c[2]);
  }else if(id==='tide_conch'||id==='reef_shell'){
    poly([[3,11],[3,7],[6,3],[11,3],[14,7],[12,11],[7,13]],id==='reef_shell'?c[1]:[209,184,132,255]);line(6,9,6,6,light);line(6,6,10,6,light);line(10,6,10,9,light);line(10,9,8,9,c[2]);dot(12,5,c[2]);
  }else if(id.includes('scale')){
    poly([[3,4],[8,2],[13,5],[11,10],[6,13],[3,9]],c[1]);poly([[4,4],[8,3],[11,5],[6,10],[4,8]],c[2]);line(5,5,8,4,c[3]);
  }else if(id.includes('template')||id==='rift_emblem'){
    rect(4,2,11,13,id==='relic_upgrade_template'?[203,186,142,255]:c[0]);rect(5,3,10,12,c[1]);gem(8,7,3);dot(8,11,c[3]);
  }else if(id.includes('compass')){
    disc(8,8,5.8,iron);disc(8,8,4.2,c[0]);line(8,4,7,9,[219,66,67,255]);line(7,9,9,11,light);dot(8,8,gold);
  }else if(id.includes('ring')||id==='iron_rim'){
    disc(8,8,5.1,id==='iron_rim'?iron:gold);disc(8,8,3.5,[0,0,0,0]);rect(6,2,9,4,c[2]);dot(6,2,c[3]);
  }else if(id.includes('string')){
    for(const x of [3,7,11]){line(x,3,x+2,6,light);line(x+2,6,x,10,light);line(x,10,x+2,13,c[2]);}
  }else if(id==='steady_rest'||id==='breaching_fitting'||id==='deflector'){
    rect(3,4,12,7,iron);rect(5,7,7,12,c[1]);rect(9,7,10,11,c[0]);gem(8,5,2);
  }else if(id==='empty_embryo_shell'){
    poly([[3,8],[5,11],[7,9],[9,11],[12,8],[11,13],[4,13]],light);dot(4,11,c[1]);
  }else if(id.endsWith('_scrap')){
    poly([[3,10],[4,5],[8,3],[10,6],[13,6],[12,11],[7,13]],c[1]);line(5,6,8,4,c[3]);rect(7,8,9,9,c[2]);
  }else{gem(8,8,id.includes('core')?5:4);if(id.includes('core')){for(const [x,y]of [[3,3],[12,3],[3,12],[12,12]])rect(x,y,x+1,y+1,gold);}else line(6,10,9,5,c[3]);}
  border();
  return p;
}

export function icon32(id,palette) {
  const base=icon(id,palette);
  const pixels=Array.from({length:32},(_,y)=>Array.from({length:32},(_,x)=>[...base[y>>1][x>>1]]));
  // One-pixel bevels and small route marks use the extra resolution, with crisp native-style edges.
  for(let y=1;y<31;y++)for(let x=1;x<31;x++){
    const c=pixels[y][x];if(!c[3])continue;
    if(!pixels[y-1][x][3]||!pixels[y][x-1][3])for(let k=0;k<3;k++)c[k]=Math.min(255,c[k]+22);
    else if(!pixels[y+1][x][3])for(let k=0;k<3;k++)c[k]=Math.max(0,c[k]-12);
  }
  if(/core|gauntlet|belt|template|shield|ingot|endforged/.test(id)){
    for(const [x,y]of [[15,14],[16,15],[15,16]])if(pixels[y][x][3])pixels[y][x]=[...palette[3],255];
  }
  return pixels;
}

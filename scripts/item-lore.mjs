// SPDX-License-Identifier: GPL-3.0-or-later
const line=(zh,en,args=[])=>({zh,en,args});
const action=(zh,en,key)=>({...line(zh,en,['key:key.lynxrelics.'+key]),role:'use'});
const detail={
  dragon_essence:[line('从龙息中萃取的紫色能量。','Violet energy distilled from dragon breath.'),line('右键补充 %s 燃息，满值时保留物品。','Use to restore %s breath; retained at full capacity.',['balance:breath.refill'])],
  dragon_shell_dust:[line('萃取辅助料，提高单批产出至6份。','Extraction additive: increases a batch to six essences.')],
  dragonbound_ingot:[line('龙系装备与专精工艺材料。','Metal for dragon relics and their specializations.')],
  shed_dragon_scale:[line('龙系专精材料，可通过照料或战斗取得。','A dragon specialization material from care or combat.')],
  embryo_crystal:[line('封存龙息幼灵的容器。','A vessel for a dragon wisp.')],
  empty_embryo_shell:[line('失去幼灵的外壳，用于重聚。','An empty vessel used to restore its wisp.')],
  echo_dust:[line('回声调谐台精炼的基础材料。','The primary product of the echo tuner.')],
  resonant_bone:[line('回响装备与专精工艺材料。','A resonant frame for echo equipment.')],
  moth_scale:[line('照料回声蛾取得的薄鸣鳞。','A delicate scale obtained by caring for echo moths.')],
  keeper_shard:[line('回响专精的可回收催化物。','A reusable catalyst for echo specializations.')],
  condensed_tide:[line('调压台精炼材料，不是技能弹药。','A pressure-refined crafting material, not skill ammunition.')],
  tideforged_copper:[line('潮汐装备与专精工艺材料。','Refined copper for tide equipment.')],
  jelly_pearl:[line('照料潮灯水母取得，可替代精炼用螺壳。','A cared-for jellyfish pearl; replaces shells in refining.')],
  reef_shell:[line('潮汐专精的可回收催化物。','A reusable catalyst for tide specializations.')],
  endmarrow_scrap:[line('冶炼界髓矿取得，用于合成界髓锭。','Smelt endmarrow ore, then combine scraps into ingots.')],
  endmarrow_ingot:[line('锻造台升级下界合金装备的材料。','Smithing material for upgrading netherite equipment.')],
  rift_emblem:[line('末地强敌的纹章，用于高级界铸工艺。','An End encounter trophy for advanced forging.')],
  endforged_template:[line('锻造模板：下界合金装备＋界髓锭。','Smithing template: netherite equipment + endmarrow ingot.'),line('锻造时消耗，可通过配方复制。','Consumed by smithing; can be duplicated by crafting.')],
  perpetual_core:[line('三系材料铸成的永续用品核心。','A three-route core for perpetual travel items.')],
  dragonheart_prism:[line('龙战取得的高级附魔工艺材料。','A dragon raid material for advanced enchantment crafts.')],
  raid_cloak_pattern:[line('龙战战利品，保留为披风外观工艺材料。','A raid trophy retained as a cloak cosmetic crafting material.')],
  relic_upgrade_template:[line('原版盾牌、弓、弩的改造模板。','A modification template for vanilla shields, bows and crossbows.')],
  iron_rim:[line('盾牌改造件：耐久＋25%，举盾更沉重。','Shield fitting: +25% durability, with heavier blocking.')],
  deflector:[line('盾牌改造件：短窗口偏转普通箭。','Shield fitting: a brief window to deflect ordinary arrows.')],
  breaching_fitting:[line('盾牌改造件：有效格挡后蓄势盾推。','Shield fitting: stores effective blocks for a shield bash.')],
  quickdraw_string:[line('弓改造件：蓄力更快，箭矢伤害略低。','Bow fitting: faster draw with slightly lower arrow damage.')],
  steady_rest:[line('弩改造件：站定后强化主箭。','Crossbow fitting: strengthens the main shot after standing still.')],
  magnetic_ring:[line('对磁石使用安装；紫水晶碎片提供16分钟燃料。','Use on a lodestone to install; an amethyst shard fuels 16 minutes.'),line('半径6格，每秒最多4个物品实体；相邻容器接收。','6-block radius, up to 4 item entities/s into adjacent containers.'),line('潜行使用剪刀拆回；服从拾取归属与视线。','Sneak-use shears to recover; respects pickup ownership and sight.')],
  expedition_compass:[line('对磁石使用记录，最多保存三个据点。','Use on lodestones to record up to three waypoints.'),line('潜行使用切换；铁砧名称作为新据点名。','Sneak-use to cycle; anvil names label new waypoints.'),line('显示维度、距离和失效状态；不提供传送。','Shows dimension, distance and validity; grants no teleport.')],
  stabilizing_ring:[line('安装到龙印绑定磁石，缩短返回吟唱。','Install at a bound lodestone to shorten recall channeling.')],
  abundance_core:[line('四层信标核心：定期恢复饥饿与饱和。','A full-beacon core for periodic food and saturation.')],
  craftsmanship_core:[line('四层信标核心：少量概率免除耐久损耗。','A full-beacon core that can prevent durability loss.')],
  shelter_core:[line('四层信标核心：间歇减轻坠落伤害。','A full-beacon core that periodically softens falls.')],
  homing_dragon_necklace:[action('看向磁石，潜行＋%s 绑定据点。','Look at a lodestone; sneak + %s to bind it.','gauntlet'),action('在其它位置潜行＋%s：吟唱归巢。','Elsewhere, sneak + %s to channel recall.','gauntlet'),line('12燃息 · 吟唱6秒 · 冷却300秒。','12 breath · 6s channel · 300s cooldown.'),line('同维度；移动或受伤会中断。','Same dimension; movement or damage interrupts.')],
  dragonbound_belt:[line('燃息容量40→50。','Breath capacity 40 → 50.')],
  dragonthroat_gauntlet:[action('按住 %s：蓄力0.75秒，喷吐2.4秒。','Hold %s: charge 0.75s, then breathe for 2.4s.','gauntlet'),line('7格锥形 · 6段×4伤害 · 8燃息。','7-block cone · 6 × 4 damage · 8 breath.'),line('龙压＋18 · 冷却10秒 · 喷吐时减速。','+18 pressure · 10s cooldown · slower while breathing.')],
  dragonwing_cloak:[action('空中按 %s 扑翼，也可再次按跳跃。','Press %s in midair to flap, or jump again.','cloak'),line('每次离地一次；稳定着地0.5秒重置。','Once per takeoff; land steadily for 0.5s to reset.'),line('8燃息 · 龙压＋15 · 冷却8秒。','8 breath · +15 pressure · 8s cooldown.'),line('滑翔中改为4燃息的短转向修正。','While gliding: a brief steering correction for 4 breath.')],
  resonant_necklace:[line('储响保留＋2秒。','Stored echo lasts 2s longer.'),action('潜行＋%s：在视线落点产生调查声源。','Sneak + %s: place a lure where you aim.','gauntlet'),line('半径12格 · 冷却16秒。','12-block radius · 16s cooldown.'),line('引导僵尸、骷髅、苦力怕和裂隙潜猎者前往查看。','Lures zombies, skeletons, creepers and rift hunters to investigate.'),line('不引走首领或正在6格内近战的目标。','Does not distract bosses or targets in close combat within 6 blocks.')],
  silent_belt:[line('潜行0.6秒后消除步行脚步与振动。','Sneak for 0.6s to quiet walking steps and vibrations.'),line('最多6秒/6步 · 冷却18秒。','Up to 6s / 6 steps · 18s cooldown.'),line('奔跑、离地或受伤结束；攻击仍有声。','Sprint, leaving ground or damage ends it; attacks stay audible.')],
  keeper_gauntlet:[line('配合盾牌：首次举盾后0.30秒内迎击敌对攻击。','With a shield: face a hostile attack within 0.30s of raising.'),line('普通盾也能精准招架；回声盾可消耗储响反击。','Enables precision parries with ordinary shields; an Echo Shield can spend the stored echo.'),line('窗口冷却6秒；反复举盾不会重置。','6s window cooldown; raising repeatedly does not reset it.')],
  murmur_cloak:[line('受到敌对直接伤害时留下2秒残响。','Leaves a 2s decoy after direct hostile damage.'),line('冷却20秒；不隐身，不清除首领仇恨。','20s cooldown; no invisibility or boss aggro reset.')],
  watermirror_necklace:[action('箭即将命中时潜行＋%s：开启反射。','Just before an arrow hits, sneak + %s to reflect.','gauntlet'),line('窗口0.25秒 · 成功40潮势 · 冷却12秒。','0.25s window · 40 tide on success · 12s cooldown.'),line('只反射一枚普通箭，伤害最高8。','Reflects one ordinary arrow, capped at 8 damage.'),line('未命中窗口：消耗5潮势，冷却3秒。','Missed window: 5 tide spent, 3s cooldown.')],
  undertow_belt:[line('潮势容量100→120，额外空气20秒。','Tide capacity 100 → 120; 20s extra air.'),line('普通空气耗尽后使用储备；离水逐渐补充。','Reserve air starts after normal air runs out; refills gradually out of water.')],
  tidepull_gauntlet:[action('按 %s：近距离锥形潮引。','Press %s: a short conical undertow.','gauntlet'),line('5格 · 最多3目标×6伤害 · 牵引最多1格。','5 blocks · up to 3 × 6 damage · pulls at most 1 block.'),line('25潮势 · 冷却10秒；默认不牵引玩家。','25 tide · 10s cooldown; does not pull players by default.')],
  undertow_cloak:[line('花费40潮势吸收一次最多4点战斗伤害。','Spends 40 tide to absorb up to 4 combat damage.'),line('冷却20秒；不处理虚空、饥饿或自伤。','20s cooldown; excludes void, starvation and self damage.')],
  rift_blade:[action('右键或 %s：蓄力0.6秒后自动短冲。','Use or press %s: dash automatically after charging 0.6s.','weapon'),line('最多6格、3目标各12伤害。','Up to 6 blocks, hitting 3 targets for 12 damage each.'),line('10燃息 · 龙压＋25 · 冷却12秒。','10 breath · +25 pressure · 12s cooldown.')],
  tide_spear:[action('右键或 %s：蓄力0.6秒后发出浪锋。','Use or press %s: charge 0.6s, then release a wave.','weapon'),line('8格 · 最多3目标各10伤害。','8 blocks · up to 3 targets for 10 damage each.'),line('25潮势 · 冷却8秒。','25 tide · 8s cooldown.'),line('潜行使用改为6格冲行：40潮势，冷却12秒。','Sneak-use for a 6-block dash: 40 tide, 12s cooldown.'),line('冲行需要站在地面或处于水中。','Dash requires the ground or water.')],
  echo_shield:[line('沿用盾牌防御；精准招架与储响反击。','Shield defense, with precision parries and stored echo.'),line('潜行重新举盾释放储响，额外损耗12耐久。','Sneak and raise again to bash; costs 12 extra durability.'),line('招架冷却6秒；储响最高6点。','6s parry cooldown; stores up to 6 echo.')],
  resonant_bow:[{...line('按住右键拉弓，松开射击。','Hold Use to draw; release to shoot.'),role:'use'},line('背包需有箭；潜行搭配余响箭，射向方块表面。','Requires arrows; sneak-shoot Echo Arrows at a block surface.'),line('形成6秒调查声源 · 冷却4秒；特殊声源不造成伤害。','Creates a 6s lure · 4s cooldown; the special lure deals no damage.'),line('普通箭照常攻击；声源只引导尚未近战的受适配生物。','Normal arrows deal damage; lures guide supported mobs outside close combat.')],
  echo_arrow:[line('余响长弓的特殊箭，形成短暂声源。','A resonant bow arrow that creates a temporary sound source.')],
  scouting_arrow:[line('命中敌对生物，标记4秒。','Marks a struck hostile mob for 4s.')],
  foam_arrow:[line('熄灭目标普通火焰，短暂减速10%。','Extinguishes ordinary fire and briefly slows by 10%.')],
  tide_conch:[action('右键或 %s：水下探物声纹。','Use or press %s: reveal underwater items.','weapon'),line('24格 · 15潮势 · 冷却12秒。','24 blocks · 15 tide · 12s cooldown.'),line('潜行使用：吟唱4秒返回记录的水面点。','Sneak-use: channel 4s to return to the recorded surface.'),line('25潮势 · 冷却90秒 · 距离96格/垂直64格内。','25 tide · 90s cooldown · within 96 blocks / 64 vertical.')],
  cruise_firework:[line('可重复使用的鞘翅助推器，无需补充弹药。','A reusable Elytra booster; needs no ammunition.'),{...line('滑翔时右键助推 · 冷却 %s 秒。','Use while gliding to boost · %ss cooldown.',['seconds:infinite.fireworkCooldown']),role:'use'},line('地面使用仅放火花；不能作弩的弹药。','On the ground: decorative sparks; not crossbow ammunition.'),line('不消耗本体；所有副本和双手共用冷却。','Retains the item; all copies and both hands share cooldown.')],
  bound_pearl:[line('可重复投掷的珍珠；传送仍会造成伤害。','A reusable pearl; teleporting still causes damage.'),{...line('右键投掷 · 冷却 %s 秒。','Use to throw · %ss cooldown.',['seconds:infinite.pearlCooldown']),role:'use'},line('最多一枚在途；同维度，最长15秒。','One in flight; same dimension, up to 15s.'),line('落点不安全则失败；所有副本共用冷却。','Unsafe landing fails; all copies share cooldown.')],
  dragon_workstation:[line('龙蛋催化，8瓶龙息萃取4份精粹。','Dragon egg catalyst: 8 dragon breaths yield 4 essences.'),line('加工20秒；返还8空瓶，催化物保留。','20s process; returns 8 bottles and retains the catalyst.')],
  echo_workstation:[line('回响碎片催化，调谐回响尘与鸣骨。','An echo-shard catalyst refines echo dust and resonant bone.'),line('右键选择工艺；催化物可回收。','Use to select a craft; the catalyst is recoverable.')],
  tide_workstation:[line('海洋之心催化，精炼凝潮珠与潮铸铜。','A heart-of-the-sea catalyst refines tide materials.'),line('返还空桶；输出满时保留材料。','Returns empty buckets; full outputs preserve ingredients.')],
  resonance_box:[line('放入唱片和回响尘；每份燃料60秒。','Insert a disc and echo dust; each fuel lasts 60s.'),line('空手右键切换安抚、振奋、干扰模式。','Empty-hand use cycles calm, inspiration and distraction.'),line('潜行空手取回唱片；拆下保留唱片与剩余燃料。','Sneak empty-hand to recover the disc; pickup retains disc and fuel.')],
  endmarrow_ore:[line('末地外岛深埋矿物，需下界合金级镐。','Buried in outer End islands; requires a netherite-tier pick.'),line('熔炉或高炉冶炼为界髓碎屑。','Smelt or blast into endmarrow scrap.')],
  speed_rail:[line('右键切换4/8/16/24格每秒档位。','Use to cycle 4 / 8 / 16 / 24 blocks per second.'),line('潜行反向切换；弯道限速8格每秒。','Sneak to cycle backward; curves are capped at 8 blocks/s.')]
};
export const specializations={
  dragonthroat_gauntlet:[['A 长流：喷吐射程9格。','A Longflow: 9-block breath range.'],['B 聚焰：35度锥形，分段伤害＋20%。','B Focused flame: 35-degree cone, +20% pulse damage.']],
  rift_blade:[['A 穿阵：冲刺最多命中5个目标。','A Breakthrough: up to 5 dash targets.'],['B 返身：冲刺4格，冷却9秒。','B Return: 4-block dash, 9s cooldown.']],
  dragonwing_cloak:[['A 翔升：扑翼向上冲量0.55。','A Updraft: 0.55 upward flap impulse.'],['B 定向：扑翼水平冲量1.05。','B Direction: 1.05 horizontal flap impulse.']],
  homing_dragon_necklace:[['A 行军：基础归巢吟唱4秒。','A March: 4s base recall channel.'],['B 安稳：归巢冷却240秒。','B Shelter: 240s recall cooldown.']],
  echo_shield:[['A 长响：基础储响保留12秒。','A Long echo: 12s base echo retention.'],['B 反震：基础反击5，储响上限仍6。','B Rebound: 5 base bash damage, echo cap remains 6.']],
  resonant_bow:[['A 久响：声源9秒，冷却6秒。','A Enduring echo: 9s source, 6s cooldown.'],['B 偏音：调查半径16格。','B Offset: 16-block investigation radius.']],
  silent_belt:[['A 细步：最多8次静音脚步。','A Fine step: up to 8 quiet steps.'],['B 轻音：静音冷却15秒。','B Soft sound: 15s silence cooldown.']],
  resonance_box:[['A 广域：范围10格，每份燃料45秒。','A Broad field: 10-block radius, 45s fuel.'],['B 节流：范围8格，每份燃料90秒。','B Efficient: 8-block radius, 90s fuel.']],
  tide_spear:[['A 远潮：浪锋10格。','A Distant tide: 10-block wave.'],['B 奔潮：冲行8格，费用仍40。','B Running tide: 8-block dash, cost remains 40.']],
  undertow_cloak:[['A 深潜：配合潜潮腰带，额外空气30秒。','A Deep dive: 30s extra air with an undertow belt.'],['B 稳潮：潮罩冷却16秒。','B Stable tide: 16s ward cooldown.']],
  tide_conch:[['A 寻流：探物半径32格。','A Seek current: 32-block survey radius.'],['B 返潮：水面回程冷却60秒。','B Returning tide: 60s surface-return cooldown.']],
  watermirror_necklace:[['A 明镜：基础反射窗口0.30秒。','A Clear mirror: 0.30s base reflection window.'],['B 回光：反射消耗35潮势。','B Reflected light: reflection costs 35 tide.']]
};
export function loreFor(id) {
  const lines=[];
  const route=/endforged|endmarrow|rift_emblem|perpetual|bound_pearl/.test(id)?'end':/resonant|silent|keeper|murmur|echo|moth/.test(id)?'echo':/tide|undertow|watermirror|jelly|reef|foam/.test(id)?'tide':'dragon';
  const tint={end:'#b78ce8',dragon:'#cf92e8',echo:'#7dd7e3',tide:'#78d6b5'}[route];
  const slot=id.endsWith('_necklace')?['项链','Necklace']:id.endsWith('_belt')?['腰带','Belt']:id.endsWith('_gauntlet')?['护手','Gauntlet']:id.endsWith('_cloak')?['披风','Cloak']:null;
  const heading=(zh,en)=>({...line(zh,en),role:'section',color:tint,bold:true});
  if(id.startsWith('endforged_')){
    const armor=/helmet|chestplate|leggings|boots/.test(id);
    lines.push(heading(armor?'◆ 界铸 · 抗魔护甲':'◆ 界铸 · 魔法武装',armor?'◆ Endforged · Arcane ward':'◆ Endforged · Arcane weapon'));
    if(armor){
      lines.push({...line('魔法伤害减免：每件 %s%。','Magic damage reduction: %s% per piece.',['percent:forging.magicResistancePerPiece']),role:'stats'});
      lines.push({...line('四件合计上限 %s%。','Four-piece reduction is capped at %s%.',['percent:forging.magicResistanceCap']),role:'stats'});
      lines.push(line('对魔法、间接魔法、龙息与界铸打击生效。','Applies to magic, indirect magic, dragon breath and endforged strikes.'));
      lines.push({...line('不减少物理攻击或摔落伤害。','Does not reduce physical attacks or falling damage.'),role:'rule'});
    }else{
      lines.push(line('近战造成魔法伤害，穿透护甲。','Melee strikes deal magic damage and bypass armor.'));
      lines.push({...line('保护附魔与抗性效果仍然生效。','Protection enchantments and resistance still apply.'),role:'rule'});
      if(id==='endforged_axe'||id==='endforged_sword'){
        const type=id.endsWith('axe')?'axe':'sword';
        lines.push({...line('攻击伤害 %s · 攻击速度 %s/秒。','Attack damage %s · Attack speed %s/s.',[`balance:forging.${type}AttackDamage`,`balance:forging.${type}AttackSpeed`]),role:'stats'});
      }
      if(!id.endsWith('sword'))lines.push(line('合适方块的挖掘速度提升10%。','Mines suitable blocks 10% faster.'));
    }
    lines.push(heading('修复','Repair'));
    lines.push(line('在铁砧中使用界髓锭修复。','Repair with an Endmarrow Ingot at an anvil.'));
    return lines.map(row=>({...row,color:row.color??(row.role==='rule'?'dark_gray':'gray')}));
  }
  if(slot)lines.push(heading(`◆ ${slot[0]} · ${route==='dragon'?'龙精粹':route==='echo'?'回响':'潮汐'}`,`◆ ${slot[1]} · ${route==='dragon'?'Dragon':route==='echo'?'Echo':'Tide'}`));
  else if(/workstation/.test(id))lines.push(heading('◆ 遗珍工艺台','◆ Relic workstation'));
  else if(/_core$/.test(id))lines.push(heading(id==='perpetual_core'?'◆ 永续工艺核心':'◆ 信标核心',id==='perpetual_core'?'◆ Perpetual crafting core':'◆ Beacon core'));
  else if(/rift_blade|tide_spear|resonant_bow|echo_shield|tide_conch/.test(id))lines.push(heading('◆ 路线武装','◆ Relic equipment'));
  if(slot)lines.push({...action('右键装备；%s 打开饰品栏。','Use to equip; %s opens accessory slots.','accessories'),role:'use'});
  if(['dragonbound_belt','silent_belt','keeper_gauntlet','murmur_cloak','undertow_belt','undertow_cloak'].includes(id))lines.push({...line('被动装备：满足条件后自动生效。','Passive equipment: activates automatically when its conditions are met.'),color:'green'});
  if(['rift_blade','tide_spear','tide_conch'].includes(id))lines.push({...action('副手持盾时用 %s 释放主手技。','With a shield offhand, use %s for the held skill.','weapon'),role:'use'});
  lines.push(...(detail[id]??[line('用于遗珍工艺。','Used in relic crafting.')]));
  if(['dragonthroat_gauntlet','tidepull_gauntlet','rift_blade','tide_spear','echo_shield'].includes(id))lines.push({...line('技能默认只伤敌对生物，不伤牲畜、盔甲架或友军。','Skills target hostile mobs by default, excluding livestock, armor stands and allies.'),role:'rule'});
  if(['dragonthroat_gauntlet','rift_blade','homing_dragon_necklace','dragonwing_cloak'].includes(id))lines.push(line('右键龙精粹补充燃息；饰品栏可查看剩余资源。','Use Dragon Essence to restore breath; check resources in accessory slots.'));
  if(['tidepull_gauntlet','tide_spear','tide_conch','watermirror_necklace','undertow_cloak'].includes(id))lines.push(line('全身入水每秒恢复5潮势，有效近战每秒最多恢复3。','Underwater: +5 tide/s; effective melee: up to +3 tide/s.'));
  if(slot){
    lines.push(heading('套装共鸣','Set resonance'));
    lines.push({... (route==='dragon'?line('2件：龙压积累−10%。','2-piece: 10% less pressure gain.'):route==='echo'?line('2件：余响声源＋1秒，最高12秒。','2-piece: +1s sound duration, up to 12s.'):line('2件：离水流失等待延至8秒。','2-piece: decay waits 8s after leaving water.')),role:'set'});
    lines.push({... (route==='dragon'?line('4件：每20秒，下次燃息消耗−2。','4-piece: every 20s, next breath cost -2.'):route==='echo'?line('4件：招架缩短一项主动冷却2秒，每8秒一次。','4-piece: a parry cuts one active cooldown by 2s, once per 8s.'):line('4件：入水恢复6/秒，潮罩冷却18秒。','4-piece: 6 tide/s underwater; 18s ward cooldown.')),role:'set'});
    if(id!=='homing_dragon_necklace'){
      lines.push(heading('维护','Maintenance'));
      const repair=route==='dragon'?['缚龙锭','Dragonbound Ingots']:route==='echo'?['鸣骨','Resonant Bones']:['潮铸铜','Tideforged Copper'];
      lines.push(line(`在铁砧使用${repair[0]}修复。`,`Repair at an anvil with ${repair[1]}.`));
    }
  }
  return lines.map(row=>({...row,color:row.color??(row.role==='section'?tint:row.role==='use'?'aqua':row.role==='set'?'light_purple':'gray')}));
}

/** Inserts styled arguments without putting formatting control codes in translated sentences. */
export function richLine(row){
 const scan=s=>[...s.matchAll(/%s|\d+(?:\.\d+)?/g)].map(m=>m[0]);
 const a=scan(row.zh),b=scan(row.en);
 if(a.length!==b.length||a.some((v,i)=>v!==b[i]))return row;
 if(a.filter(v=>v==='%s').length!==row.args.length)throw new Error('Lore argument mismatch: '+row.zh);
 let input=0;const args=a.map(v=>v==='%s'?row.args[input++]:'gold:'+v);
 const replace=s=>{let i=0;return s.replace(/%s|%|\d+(?:\.\d+)?/g,v=>v==='%'?'%%':`%${++i}$s`)};
 return {...row,zh:replace(row.zh),en:replace(row.en),args};
}

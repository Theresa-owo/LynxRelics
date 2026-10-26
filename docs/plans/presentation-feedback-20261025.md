# 手测反馈修正：模型材质、翻译、Lore、弓、界铸与龙AI

## 2026-10-26 18:19 最新运行状态

图集修复后的构建/分发及3项JUnit通过。实际客户端18:18:25进入Test，mod纹理/模型缺失警告0，未启用其它资源包；关键运行资源与JAR哈希一致。当前会话**78731**、PID**67036**（需续接时重核），保持运行交给用户手测，禁止自动输入或重启。没有独立测试服。源码和资源已冻结本轮候选，不在客户端运行期间重建共享输出。

当前候选`deliverables/20261026-stage12`，JAR SHA256=7F76C6B0EE11D87C4004B084A8C9D5424D52CF1F46FA23D6D3FA4B911CF68E54。43项服务端组件、3项JUnit、70图标/Lore/158模型/双图集结构检查均通过。实际视觉与全战斗仍由用户手测，加载证据docs/validation/20261026-stage12-client-load.json。

按用户原先明确要求准备向Theresa-owo/LynxRelics公开源码；gh已确认该仓库公开且为空。仅提交本项目源码/设计/模型及验证记录，忽略build、run、.local、deliverables；不发布正式Release或操作生产。此后仓库结果需要实际push回执确认。

## 18:27 源码交付回执

已按上述授权初始化本项目Git，首次实现提交`5d621d96bae6f9a58a7ee73270b8a71830064b39`，873文件，使用已认证Theresa-owo账号的noreply提交身份，没有修改全局Git身份或认证方法。`git push --set-upstream origin main`退出0；GitHub API复核main指向同一SHA，仓库公开且非空。源码/模型/文档/验证报告已托管于https://github.com/Theresa-owo/LynxRelics，未创建Release。

build、run、.local、deliverables均确认未进入暂存区；Gradle wrapper保留内置LICENSE。Git交付后只追加本回执文档，不改已验证玩法/运行资源。当前本地候选仍是stage12：JAR/ZIP/源码包/sha256.json及client-load.json。18:27复核客户端PID67036仍运行，继续交由用户手测；43项组件与实际0条Mod缺失纹理加载警告的边界不变。

## 2026-10-26 18:01 恢复开发

用户已明确“继续实现”，暂停结束。本轮重新核对了源码、无Git状态、无活动开发Java进程及旧报告，未操作生产和Lunar。沿用long-pipeline检查点，不新建任务或代理。

剩余项链检查的定位：26.3 Entity.pick使用partialTick=0时插值至旧坐标和旧视角；原服务端两处raycast错误使用0。已改归巢绑定、鸣纹声源为1，回归将old pose故意放在远处并瞄准真实磁石表面。最初新增的“空中12格端点”断言不应假设实体视角/试验场为空，已改成真实目标表面断言；没有放宽当前位置与旧位置的区分。

最新隔离服**42/42通过**，证据docs/validation/20261026-stage10-aim-lore-skills.json。新Lore刷新会更新本Mod自有v3行，同时保留个人备注、名称、耐久和专精。70图标/Lore/158模型/双语键/UV/11阶段弓结构校验通过。隔离服会话91062于18:00:36正常stop并退出0；53311也已退出。无活动隔离服。

已补docs/使用与配置.md，包含全部路线装备触发/用途、资源和目标限制、当前171个配置键的分组及实际验收边界。下一步串行build/distribution，校验JAR及打包内容，再启动Vulkan隔离Test客户端交由用户操作。不能将组件通过当作视觉或全龙战验收。

18:03 build/distribution通过，3项JUnit通过；核对通用JAR双端元数据、GPL3+、Kotlin内置、语言/Mixin/魔法/弓资源及ZIP内JAR一致性。JAR SHA256=E9723C66702C04B630361BFE2395C7136BC9824638D3559F4908F4DCB612FD99。当前再补入这份构建记录到ZIP，然后启动客户端；没有Git或生产变更。

18:08收尾核对发现防御被动的玩家敌对来源没有遵守skills.pvp，已统一门控，补充原生PvP允许而Mod关闭时玩家不可充能/僵尸仍有效的回归。最新**43/43通过**，docs/validation/20261026-stage11-passive-pvp.json；隔离服37381正常stop并最终观察退出0。重新build成功，3项JUnit通过。最终JAR SHA256=C412BF56725656474FD240E2E7FE5832ED37486366B9BF15407FD66CD4F6D164，3,128,998字节，替代18:03版本。下一步仅重新打包/留存stage11再启动客户端。

## 18:17 实际客户端纹理图集回归

18:10会话13076/PID58024确实进入Test，但日志发现材质缺失，不能将stage11包标记最终材质通过。根因为26.3已分离blocks/items图集，而自定义material/目录没有入图集，PNG存在性校验不足。已新增assets/minecraft/atlases/blocks.json与items.json，各仅追加4个single来源；校验器同时核对atlas登记。实际SDK确认sources按资源栈合并。改动仅资源与检查，无玩法变化，43项服务端结果仍适用。

用户明确授权现在重启；Sky首次捕获遮挡画面，重新选择并前置后正确捕获Minecraft。两次AltF4均被工具以检测到用户输入拒绝，没有越过这个拒绝。用户随后明确自行保存退出；18:16:28日志正常保存，13076最终退出0，当前没有本项目客户端。准备重建并再次启动；已保留stage10/11历史包，下一正式候选留存stage12。

## 21:20 暂停检查点（覆盖下文旧状态）

用户明确要求“先暂停开发”。当前工作已暂停，等待用户明确恢复；不继续修改实现、构建、启动客户端或执行外部交付。

- 用户原手测客户端于20:54正常退出。本轮未再启动客户端，也未操作用户的Lunar客户端或生产环境。
- 隔离服务端会话4879已于21:19:55收到stop并正常退出0；先前4621、68449也已正常退出。没有本轮需要继续等待的构建/隔离服会话。
- 已完成材质烘焙生成器：修正方块0–16 UV、原生版型护甲遮罩、三路线饰品与七种生物材质；11张材质原图和可编辑bbmodel保留。弓改为11个上下臂对称、弦长固定的原生3D姿态。锄图已采用明确单侧平头的v4，原图保留于assets/icons/source。70图标、158模型、双语键/Lore和UV结构校验通过；未重新取得实际客户端视觉验收。
- 本轮双语UI/服务端提示、彩色Lore、界铸斧11/1.2和剑9/1.8、魔法近战/每件8%抗性（四件32%）、烟花15tick/珍珠100tick、龙II/III永久悬停恢复均已写入。早先这一批隔离服38/38通过，报告保留于docs/validation/20261025-stage9-magic-ai-feedback.json。
- 后续技能排查修正了主/副手通道核对、客户端右键向副手盾牌让行、持副手潮汐物品的充能、创造模式资源测试、空中H键、失败原因及释放反馈；补充全部技能的资源/目标/动作说明及开发环境/relicstest status诊断。未改为无条件伤害牲畜/友军。
- 21:18的build通过，JUnit通过；随后仅将tickWeapon改internal以便无网络连接的组件探针直接调用，并完善探针断言诊断；21:19 runServer已重新编译通过。最终JAR/分发ZIP尚未按这一最后源码状态统一重新打包。
- 最新组件报告run/server/verification.json时间21:19:43，**40/41通过**。副手浪锋真实命中敌对生物与盾牌让行/V键蓄力回归已通过。剩余一项是三项链探针中鸣纹项链声源位置查询断言：错误为“Echo necklace did not create a lure: skill.lynxrelics.echo_mark, cooldown=320, wear=1”。已经走到释放提示、冷却和耐久，尚未区分声源位置查询的探针边界与玩法缺陷，不应宣称全部技能通过。暂停后未继续调查。
- 没有Git初始化、commit、push、发布或生产安装。下文早期“未编译/仍在生成/客户端运行”等描述是历史过程，不代表当前状态。

恢复后的第一步：重新确认源码与进程状态，调查剩余鸣纹项链探针的声源位置查询，完成相应检查后再统一构建/分发，并按用户要求启动隔离客户端交由用户手测。暂停期间不执行这些步骤。

用户已手动测试客户端，截图确认：界铸穿戴仍是灰模且头盔遮罩错误；工作台/持握材质越界条纹；英文环境里UI混入硬编码中文；工艺长内部ID溢出；Lore全灰且含实现性废话。补充要求弓上下臂一起弯曲、锄与镐清晰区分；烟花/珍珠冷却过长；龙II/III阶段原地转圈；界铸武器工具改魔法伤害，护甲加魔法抗性。

保留当前用户手测窗口（会话84819、游戏PID69304，启动后可能变化要核对），不自动输入或关闭。源码/资源先改，准备好后协调用户退出再统一build/服务器检查/重启；当前改动尚未编译，不能复用35/35为这轮改动背书。没有Git初始化/commit/push，生产和兄弟项目不动。

## 接受的数值变更

- 界铸斧总伤害11、攻速1.2；界铸剑总伤害9、攻速1.8；新增Balance配置。覆盖旧设计攻速不变条款，用户本轮明确要求提升。
- 烟花0.75秒（15tick）、珍珠5秒（100tick），共同持久冷却和一枚在途限制保留。仅run/client与run/server两份对应旧默认配置已按用户要求改，未改生产。
- 界铸五种手持工具/剑近战使用endforged_magic伤害类型，绕护甲而保留保护/药水抗性；原生魔法、间接魔法、龙息及界铸魔法归magic_damage标签。界铸护甲每件8%，四件32%（可配置），只算四个原生护甲栏。已写源码/数据/Mixin，未编译验证。

## 已改源码

- RelicScreens.kt重写：所有标签/提示改translatable，配方仅短编号+本地化名字，长名字省略号；材料/催化/返还/专精提示彩色；双语名称搜索；亮色标题。translations.mjs为中英统一字典，generate-assets导入。原版26.3 KeyMapping.Category真实前缀为单数`key.category`，旧plural键错，已补正确键。
- 多数服务端提示/龙Boss条/技能预警已换translatable。剩余AnchorAddons、Resonator、Mining以及Creative tab仍须换，扫CJK literal确认。
- RaidSkills.releaseFlight跟踪flightControlled，预警/吐息不再强塞HOVERING；只在冲锋/离场控制原生CHARGING；退出只释放自有控制，不覆盖原生坐地/降落/死亡。DragonRaid空参战者分支无条件cancel，清除legacy hover；阶段变更不再set HOVER；DYING保护。已识别一个确定死锁：创造模式没有有效survival participant，阶段变更先HOVER，然后空参与者只在skill非空才cancel，导致永久hover转圈。需要新增回归（II/III无参战者、保留原生坐地、阶段存档等）及用户真实龙战复测。
- EndforgedDamageMixin调用ItemStack.getDamageSource返回钩子委派Kotlin；26.3 Player.createAttackSource实际调用此方法，不能注入Player.attack里的DamageSources.playerAttack（它只在lambda中）。新增damage_type/endforged_magic，magic_damage tag、bypasses_armor、witch_resistant_to；Combat.adjustedDamage接入MR。须编译及真实伤害检查。
- Endforged.kt数值提升已写；Balance新参数写；旧Lore尚未重写、魔法介绍尚未加入，必须移除用户截图中的继承说明，保留于文档。

## 材质与动画待继续完成

- 11张内置imagegen材质原图已全部成功并复制到`assets/materials/source`，catalog.json已登记：metal_dragon/echo/tide/endforged、bone、wood、stone、cloth_dragon/echo/tide、ore_endmarrow。都是不透明满幅平铺材质，不是图标，不要重复生成。图源函数store('material-'+id)也仍在，但以后用已复制路径。
- 需要写bake-model-materials.mjs：Sharp编译材质图集、修正所有block/item face UV至0–16（原代码32px数值当UV，造成截图条纹）、替换灰模护甲贴图并保留正确原版型透明遮罩；三路线饰品也换真正材质。现有generate-accessories CubeListBuilder的UV是像素坐标（与BlockModel不同），不要除4。可扩展mob材质，但先把用户截图的穿戴/工作台/持握修好。
- 原生Netherite64×32alpha遮罩已从SDK分析：.local/art-reference/netherite_humanoid.png与humanoid_leggings.png，只作参考不发布；当前generate-assets的faceWindow4×4明显错误。可按原生UV网重新构造mask，不发布原版PNG。mask分析在本轮工具输出；需要时用Sharp读alpha打印64×32“#.”网格，已合法只读分析。
- 计划用确定性对称3D弓姿态替换三张AI拉弦图：11个阶段，上下臂节点镜像、每臂3个固定长度段；弓弦中心随拉力移动且两半等长。Native模型element旋转选沿X/Y主轴确保角在±45；.bbmodel保存同源骨骼/关键帧。现旧三PNG确实只下臂变化，不要只再换不一致PNG。原版bow显示变换和items/bow.json分派已核对过。
- 新锄单侧L/Γ形短刃图正生成 **functions cell308**，成功结果store('polished-icon-endforged_hoe-v3')，完成取output_hint复制source/endforged_hoe-v3.png，改polished.json再compile-icons。不能重复生成已有70图。
- 所有资源变更只在src/原始assets，用户当前程序仍旧版本。不要并发写build/resources/classes；需用户正常退出后fresh构建。上一次Gradle组合命令的test/distribution任务可能还在runClient后排队，结束后观察退出才启动新构建。

## 接下来

1. 完成剩余本地化、彩色详实Lore（仅有用能力/用法，不放继承实现细节）；语言键校验。
2. 材质UV烘焙、正确护甲mask、独特工作台及持握变换；对称弓模型与独特锄图；验证所有UV范围、法线和骨骼。
3. 增补与本次真实问题相符的回归：魔法属性/减伤不减物理、龙hover死锁与坐地保护、新攻速/短CD、UI键/材质存在性。
4. 与用户协调退出隔离客户端，串行build和隔离服验证，启动修正版让用户手测。阶段状态精确记录，不编造视觉验收。

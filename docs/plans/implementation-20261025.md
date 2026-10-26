# v0.2 实现与验证检查点

## 2026-10-26 当前进度（覆盖下文旧运行状态）

用户“继续实现”已恢复暂停工作。具体修正、43/43隔离服报告及当前构建记录见[手测反馈检查点](presentation-feedback-20261025.md)和[验证记录](../VALIDATION.md)。没有活动隔离服；修正版Vulkan Test客户端已启动，会话78731、PID67036，交用户手测，不接管输入。新候选位于deliverables/20261026-stage12，3项JUnit、70图标/158模型及双图集结构检查通过，实际加载缺失纹理警告0。新说明位于[使用与配置](../使用与配置.md)。用户要求的GitHub仓库已核对公开且为空，准备提交源码；没有生产安装或Release发布。

## 范围与基线

目标：Java 26.3 / Fabric / JDK25 双端内容 Mod，严格以设计 06/07/08 与最新紫色界铸稿覆盖旧冲突；追加晴天激流/引雷、变速铁轨。

本目录最初只有设计与概念图，无 Git 仓库、无实现。后续用户明确要求 GPL-3.0-or-later 开源并放到 https://github.com/Theresa-owo/LynxRelics，授权为此初始化/提交/推送该仓库。生产服和现用客户端安装仍不在授权范围。LynxTools/LynxTags 均有用户未提交工作，不修改。读取安装目录仅用于 SDK/API 核对。

用户补充：Mod 主体使用 Kotlin，JDK25。核对 Fabric 官方 Kotlin 文档 limitations 后，Mixin 只用 Java 薄注入，委派 Kotlin；不引入 Kotlin Mixin 兼容层。Kotlin stdlib 随 JAR 分发，保留其 Apache-2.0 声明。

已读 README、design/00–08、art/3D制作计划.md；不存在磁盘上的适用 AGENTS.md，遵循会话提供的 AGENTS.md。

实测工具链：Corretto 25.0.4.1+10；现用 Fabric API 0.163.0+26.3；Loader 0.19.5 库；26.3 命名客户端/服务端 JAR 缓存。构建属性可覆盖 SDK 路径，编译不将 Minecraft/Fabric/Elytra 打入 Mod。

Elytra Slot 1.2 实际包 `com.equinox`；接口 `ElytraInventoryHolder.equinox$getElytraInventory()`；存档根 `EquinoxElytraSlot`。Lynx 使用自有 `LynxRelics` 存档根与独立菜单，绝不添加或重编号 InventoryMenu 槽。

## 实现顺序与验收

1. 四饰品容器、持久化/死亡复制、网络和独立界面；萃取台原子加工/返瓶；龙喉技能、披风与真实 Elytra 接口；构建与隔离服。
2. 三工艺/全部配方/专精与三系技能、七种生物和可编辑模型，先灰模再贴图/动画；持续构建。
3. 界髓与锻造继承、永续用品、原版改造、磁石/信标；3000 HP 原龙持久战斗。
4. 16 附魔、互斥/递归边界、真实鞘翅读取；晴天三叉戟与变速轨；综合验证与包。

验证分开记录：纯逻辑单测、编译/JAR、隔离服务器实际加载/探针、真实客户端 UI/持握/飞行。未做的场景不记通过。全量边界包含死亡/keepInventory/重连/维度/满背包/Shift/鞘翅损坏与修复/披风，工艺满输出与催化回收，Boss 重载与重复掉落。

## 回滚与当前状态

新增文件全部局限于 LynxRelics。保留原设计与概念源。回滚为移除本任务新增实现及构建产物，不删除原 docs/assets/deliverables。隔离世界仅在本项目 `run/`，端口与生产分离，不加载玩家私有存档。

当前：Kotlin 主体 + Java 薄 Mixin；官方 Loom 1.15.5 处理 26.3 命名 API 与 Fabric 访问拓宽。首轮 build/3 测试通过；隔离服实际加载+7/7 组件探针通过。服务端因 stdin 关闭在探针完成后终止 PID 68676，已观察退出，无遗留本任务服务端。详细边界见 ../VALIDATION.md。

下一步：独立开发客户端验证四槽/加工/灰模与鞘翅并存，再扩展其余玩法。未向 GitHub 推送。源码、资源、模型生成器和 19 个灰模在本目录；原概念图未变。

## 2026-10-25 16:25 续接检查点

- 后续 build 通过；最新已构建版本的隔离服组件探针 10/10 通过，保存在 `docs/validation/20261025-stage2-components.json`。新增验证为原龙实际 HP/最大 HP=3000、保留1800HP/阶段2/召回4/治疗123的存档重载、普通玩家HP与全局1024属性范围不变、界铸实际耐久及名称/附魔/损耗继承、Old/New两种矿车行为方法读取4档速度。探针不是整场龙战或真实矿车线路验收。
- 已实现但仍需扩大验证：界铸9件、原生锻造损耗比例、永续火箭/珍珠及共享持久冷却、有限自召水晶/治疗预算/原生BossBar/追加掉落的原龙战斗原型、晴天激流/引雷、4档变速轨。龙战技能目前仍是原型，与第07章的完整轨迹、追踪扫线、终焰三连等尚有差距。
- 首次OpenGL客户端及排除Lynx/Elytra的Fabric基线均原生退出0xC0000005。进一步以 `--graphicsBackend vulkan` 启动成功；Zulu25基线和Corretto25带Mod客户端均进入项目内 `run/client/saves/Test`。无需更改显卡驱动、生产客户端或系统设置。旧“客户端被环境阻塞”结论已由Vulkan路径解除。
- 带Mod客户端使用 `gradlew.ps1 runClient --console=plain '--args=--graphicsBackend vulkan --username RelicsTester --quickPlaySingleplayer Test'`。开发专用 `/relicstest` 建立四龙饰品/真Elytra槽/初始工艺库存；此命令在普通安装JAR环境不注册。真实客户端已打开独立四槽菜单、同步燃息50并看到原鞘翅；截图 `run/client/screenshots/2026-10-25_16.19.42.png`。四槽菜单仍有英文标题缺失；K/G/V原写死GLFW数字导致SDL按键错误；裂界之刃/潮锋戟多1攻击，已在源码修正为InputConstants和4f参数，但这些修正尚未重新构建。
- 后续源码又加入 `EndmarrowFeature.kt`、`worldgen/feature`/`placed_feature`、`OreItemMixin.java`，参考实际26.3的Feature接口和FEATURE_TYPE注册；**尚未编译或运行验证**。不要复用此前10/10报告为这些后续变动背书。
- 模型/美术：70个运行物品资产、24个可编辑灰模；大多为粗糙灰模，动画时间线仍为占位，没有玩家穿戴渲染层，不能标记模型/动画完成。用户明确反馈“建模、缩略图不完整或错误，缩略图要原版简约风格；物品无Lore、无介绍”。优先改为独立16x16原版风格图标，通过实际 trident.json 的 `minecraft:select`/`minecraft:display_context` 分离GUI图标和持握模型；为全部物品补齐定位/槽位/操作/费用/冷却/套装介绍，不将未实装能力写成已可用。`ItemLore(List<Component>)`与 `Item.appendHoverText` API已核对，但还未实现Lore或新图标。
- 用户已明确允许重启隔离测试窗口。其后窗口工具再次收到物理Escape停止信号，要求本轮停止电脑操控；已遵守，不继续输入、关闭或通过进程工具绕过。**终端会话62193的带Mod开发客户端可能仍在运行**；下轮先确认用户允许恢复电脑操控、检查窗口/进程，不直接使用旧句柄/坐标。此前所有隔离服均已观察正常 `stop` 退出，无当前任务服务端。
- 测试客户端 `run/client/options.txt` 中本任务旧默认仍为page.up/scroll.lock/keypad.subtract；在客户端确认退出后，只改该隔离配置的三个Lynx绑定为k/g/v，保留其它按键，不修改Lunar的options。
- 尚无Git初始化/提交/推送，目标公开仓库已确认存在且空：Theresa-owo/LynxRelics。GPL-3.0-or-later的LICENSE/NOTICE与metadata已写入。用户授权提交推送目标仓库，不授权生产服/现用客户端部署。
- 大范围待办：完整第06章被动/2与4件套、回响/潮汐实战、专精/仪式、七种生物、全部书籍工艺与16附魔、磁石/信标/原版改造、完整灰模/贴图/骨骼动画及客户端边界验收。不要将注册物品/配方或灰模存在计为玩法实现完成。

## 2026-10-25 18:10 续接检查点（覆盖上文当前状态）

- 用户继续要求一直执行；图标反馈从“32×32原版简约”改为可提高分辨率、要有特色，并否定了程序化平涂稿缺乏质感。已使用内置imagegen逐件重做8件：dragonthroat_gauntlet、rift_blade、dragon_essence、dragonbound_ingot、tide_spear、echo_shield、resonant_bow、bound_pearl。源在 `assets/icons/source/*-v2.png`，透明运行图64×64由 `scripts/compile-icons.mjs` 编译；编译依赖Sharp0.35.5（当前用 bundled node_modules，命令有 `--modules`）。`polished.json`使通用生成器不覆盖已完成图；`compiled.json`含SHA256。原提示词见 docs/art/icons-v2*.md；第一批4件及第二批4件已保存，原版风格参考仅在 .local，不发布。其余62件仍是用户否定的平涂占位稿，需要继续替换。64px预览发现余响长弓弦太细缩放后几乎消失，需要针对性加粗图源再编译，不把大图观感代替运行尺寸QA。
- 70件物品已有原生ItemLore，含中英介绍、用途/槽位/操作/费用/冷却及套装说明；键名用Component.keybind。尚未实装的能力用原型提示标注，功能完成后应逐项移除。K/G/V已改实际InputConstants SDL常量；只修了 run/client/options.txt 本测试的三个旧错误默认，不触及Lunar。
- 新构建已通过，隔离服最新18/18组件探针通过，记录 `docs/validation/20261025-stage4-entities-enchanting.json`。新增：7种生物真实属性/体型，独立失巢龙裔8人1746HP锁定及999HP重载，跨维度注册表一人一幼灵，6段×4有效伤害且不绕护甲，原生书籍工艺与专精保留损耗/名称，附魔目标互斥，静音状态包精确往返。仅组件运行，不是完整实战或客户端模型验收。
- 七种生物源在 mob/：RelicMob、RelicMobKind、RelicMobs、Companions。统一服务端预警/生效/恢复状态与有限AI、照料产物/拾取上限、精英与独立Boss逻辑；Companions使用26.3 SavedDataType(Identifier,Supplier,Codec,DataFixTypes.SAVED_DATA_COMMAND_STORAGE)。个体生命属性覆盖扩展到Brood1746，未改全局1024范围。自然生成已加加载范围种群限制；仍需实战调校、全部关键参数配置化与补充精确规则。
- `scripts/generate-mob-models.mjs` 产出7个真实骨骼灰模、纹理、原生 geometry JSON及实际关键帧动画。源在 assets/models/mobs，运行 assets/lynxrelics/geometry，RelicMobRenderer读取并烘焙原生ModelPart，使用服务端ACTION切换动画。`scripts/generate-accessories.mjs --modules <bundled-node-modules>`产出4种穿戴骨骼+3材质图集及bbmodel；AccessoryLayer/AvatarRelicsMixin/AccessoryRenderStateMixin叠加原生body/arm并支持第一人称护手，滑翔时隐藏自己的披风、保留原皮肤披风。源码已编译，**这些新穿戴渲染Mixin和七生物模型尚未实际客户端加载验证**。
- 16附魔定义/原生互斥/目标tag及N01–N06多选工艺已生成（generate-enchantments.mjs）；单件饰品最多两种Mod附魔的Anvil钩子已加载，resultSlots必须通过父ItemCombinerMenu的Accessor，不能在AnvilMenu上Shadow继承字段。CraftTransaction支持书籍/升书/专精/换专精，单一16格容器原子扣料输出。E11与R02失巢仪式、D11重聚已加入。Workstation实现WorldlyContainer，漏斗只进0–8、只出10–15，不能抽催化9格。
- 第06章被动和部分附魔实际效果已写Combat/Skills/Conch：精准招架共用储响/冷却、静音仅脚步/STEP、回响调查源、潮罩、反射走原生deflection且标记反射箭、迟发回声不递归、导潮、破界末地敌对tag、容量/节律/归巢/明镜/余韵/缓冲。Gauntlet6段采用专用skill_pulse伤害类型仅跳过hurt cooldown，不绕护甲；按段扣料/龙压、4件减费首段消耗状态，冷却首段即写入防重连。龙刃/潮戟改为12tick前摇，潮戟两个技能共用40tick锁；归巢磁石绑定与同维成功后扣料，海螺探物/水面点返回已写。仍需更多真实动作边界验收。
- VanillaMods与Smithing/Bow/Crossbow薄Mixin已写铁箍/快取弦/驻射架等；V05/V07/V09原生配方已生成。偏转件/破阵件的实效还待补齐。巡航附魔、精炼、寻脉、饰品修补候选接入尚待完成。磁石/信标/共鸣匣还未实装，不把其注册物品算作功能完成。
- 最新18/18之后为便于开源构建，ElytraCompat改成缓存反射调用**已核对的公开1.2接口**（getMethod `equinox$getElytraInventory`），不再要求外部JAR参与编译；安装该Mod才访问，接口不符明确报错；原生胸槽只作未安装/无可用独立鞘翅时的正常回退。DevCommands/RuntimeProbe已改调用该适配，build.gradle移除compileOnly本机JAR，仅本机有时加入开发runtime。**这些最后的便携性改动还未重新构建/验证**。同时修正distribution目录结构与Kotlin许可证单独保留。
- 当前隔离服会话11472已发正常stop并观察保存/构建成功；无当前任务服务端。测试客户端会话32358此前已经退出（进程检查为空）；不要凭旧会话假定仍存活。本轮Computer Use多次因用户实体Esc触发停止；**当前活动回合不要再调用Computer Use工具或通过别的OS输入方式绕过**。继续源码/隔离服验证；真实客户端UI应在后续明确恢复的回合重新选择窗口。用户并未取消整个开发目标。
- Git仍未初始化/提交/推送，目标公开空仓库Theresa-owo/LynxRelics已获用户授权。下一步：补剩余附魔/磁石信标/原版改造，完整龙战技能，配置化、工艺列表界面和验收；模型/图标持续做最终质量，完善README/发布包后分阶段提交推送，不发布正式Release或部署生产。

## 18:42 续接检查点

- 最新真实隔离服组件探针25/25通过，保存 `docs/validation/20261025-stage5-crafting-compatibility.json`；服务端会话57547已正常stop且观察退出0，无当前服务端。先前stage5夹具缺网络导致的23/24报告保留，不以生产guard掩盖问题。
- 新增Kotlin Mining/OreIndex/RelicDurability、精确公开Elytra反射ABI、原生饰品Mending、BeaconCores/AnchorAddons/Resonator已编译并实际加载。新探针覆盖Elytra破损修复+披风、Mending候选、真实矿物loot+额外耐久、工艺分段存档、信标满包拆回/无基座不扣料、共鸣匣打包、配方目录codec；磁引环真实转运、信标作用/燃料时序和共鸣匣声音/安抚仍需补验。
- V03偏转盾0.2秒单普通箭/8秒CD/伤害8封顶、V04近战有效格挡3次的潜行盾推已接Combat。反射共用标记，水镜和偏转盾不会重复反射同一箭。死亡饰品尊重原生PREVENT_EQUIPMENT_DROP消失诅咒。Conch已补水母4秒发光标记，Resonator安抚改变友善跟随/闲游并受攻击打断。实战未验收。
- 工艺首次启动复制到config/lynxrelics/workstation_recipes.json，解析有数量/模式/ID边界；服务端发送实际目录至客户端，GUI新增搜索、7行分页选择与材料/催化/专精提示；选择仍由服务端距离和索引验证。最新这轮GUI与目录源码已编译、服务器目录codec已通过；GUI仍待实际加载。
- 图标管线：最初8件＋3台/烟花＋龙项链/腰带/披风/水镜＋回响4饰品共20件已生成源；其中后4已复制但还没运行compile-icons，polished.json已登记。长弓改用v3粗弦源（原v2保留），64px预览已能看到完整弦。潮汐剩余3饰品+海螺正在imagegen cell204；界铸4护甲图标cell210。imagegen结果存 functions.store 的 polished-icon-<id>，完成后取output_hint/默认路径复制，登记polished.json、运行compile-icons并检查运行尺寸。不得打印base64。
- 下一步持续完成全部图标和模型材质，改良第07章原龙真实轨迹/追踪吐息/终焰组合，再扩大配置和运行验收。当前构建、运行状态随后续修改失效，不复用25/25为后续代码背书。Computer Use仍因本回合物理Esc保持停止。

## 19:23 续接检查点

- 用户问开发进度，已简短回应后继续实现。原始完整Mod目标仍在进行；不要把该状态问询替换为新的独立任务。当前没有客户端，Computer Use本回合仍停止，未调用CUA或以其它OS输入绕过。
- 最新构建成功（会话64422，19:15）；此后又补探针并runServer，真实 **34/34** 通过，报告 `docs/validation/20261025-stage7-paths-magnet-config.json`，上一轮30/30报告 `20261025-stage6-dragon-trident.json`。服务端会话58628已发stop，接续先观察退出码/进程（输出此前显示正常保存；若工具仍给session_id则再poll）。不盲目重启同一运行目录。
- RaidSkills.kt替换原定点圆伤原型：原生CHARGING_PLAYER冲锋阶段/头部轨迹扫掠、HOVERING预警、有限角速度追踪与掩体裁剪的扫线吐息、固定脚下标记/三地面裂线、分段终焰扑击→短吐息→离场和共享跨段命中保护；专用阶段抑制原生接触重复伤害，不改原龙实体/出口/进度/复活。自召水晶、治疗预算、人数不复制战利品继续保存；余焰Endermite限4、原生24HP/4攻击、TTL/死亡/空场清理。RaidState保存目标/朝向/预警/当前段/命中集合，重载不重新计伤。新增native phase、combo保存、ember TTL探针通过，尚非实际整场龙战。
- 晴天引雷已改成Java Enchantment WrapMethod + WeatherCheck极薄注入：ThreadLocal只覆盖实际原版CHANNELING的thundering判断，其余原生目标/天空/避雷针/声音/召唤归属全部走原效果；finally清理。不再在三叉戟TAIL额外造一道雷。晴天干燥原生Trident.use/releaseUsing、原生onHitBlock只对避雷针生效与上下文恢复均通过。26.3 Blocks.LIGHTNING_ROD是WeatheringCopperCollection，不是Block；测试按真实注册ID取，天气使用get/setRainLevel/ThunderLevel（旧setWeatherParameters已不存在）。
- 新实效/修复：浪锋用宽1.5格路径而非锥形、遇方块停止；裂界刃使用真正冲刺路径命中集合；path/Magnet4实体每秒+满箱无损+归属过滤已运行通过。G松开按键不受同tick其它按键去重丢弃；去重改为每action独立tick。路线开关接入技能/被动/装备/自然生成。矿物索引只在CHUNK_LOAD建，不从worldgen抢先写未加载索引；高度配置会约束扫描区间。
- 配置：Balance新增原龙各技能/预警/恢复/掉落、7生物属性/权重、照料产物/存量/人数上限、矿物高度/矿脉、磁石/信标/共鸣匣等。缺少的新键仅合并默认值，保留现有值；范围校验仍可继续完善。当前仍有部分能力/生物招式常量待配置化，不声称所有关键参数已全部覆盖。
- Workstation：用RecipeID+定义保存进度，重算进程内Item hash；输入修改立即重置。可配置目录首次copy到config，并S2C同步给新搜索/分页/材料提示GUI。专精目录修正为仅定义的每路线4件；新增共鸣匣A/B；新加入饰品保持第06章基础型，不收材料提供无效分支。重新生成后150条工艺/配方索引（有多选变体），不是改变设计71个编号。只对hash与旧默认完全相同的run/server工艺配置更新生成输出，用户覆写不动。
- 所有70物品有Lore；移除已实现功能的泛泛“原型能力未补”行，补绑定/操作/共鸣匣/磁石/海螺介绍。专精添加独立原生Lore（中英），更换只替换本Mod专精行，保留自定义Lore。共鸣匣打包原始物品组件，保留名称/唱片/燃料/模式/专精，不把原始CONTAINER嵌套复制。Brood新增阶段I短冲、II裂线/裂浪、III吐息/爪击按计数交替，保存脉冲/护壳/适应抗性/命中集合；仍需实战。
- 图标目前 **54/70已编译**，所有源/修订保留、64px透明输出和SHA见compiled.json。新增54中包括全部12饰品、9界铸、7路线武器/装置与三工作台、永续两件及多种材料。最后16件正在内置imagegen：cell261硬件8（iron_rim、deflector、breaching_fitting、quickdraw_string、steady_rest、magnetic_ring、expedition_compass、stabilizing_ring）；cell263核心/箭/方块8（abundance_core、craftsmanship_core、shelter_core、echo_arrow、scouting_arrow、foam_arrow、endmarrow_ore、speed_rail）。每个成功结果存functions.store('polished-icon-'+id)，output_hint里实际路径，复制到assets/icons/source/<id>-v2.png，apply_patch登记polished.json后compile-icons。两个cell均Promise.allSettled，不会一件失败丢弃其余。早先cell222一次网络发送失败，3件成功已回收，其余通过240/241重做完成；不要重做已完成图。
- 图标编译：node scripts/compile-icons.mjs --modules C:\Users\Theresa\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules。通用generate-assets会保留polished条目PNG，但会重写灰模、Lore与模型配置；需要将后续模型定向修复也加入生成器，或用单独最终编译脚本收尾。
- 接下来：接入最后16图并在运行尺寸检查；修SpeedRail模型所有shape（当前''单模型不能正确旋转曲线/坡道）及弓拉弦状态，原生API参考已读 `minecraft/blockstates/rail.json`、`models/block/rail_flat/rail_curved/template_rail_raised_ne/sw`、`items/bow.json`。全部模型仍灰、穿戴/持握客户端未验收，不宣称模型完成。继续完善配置、资料、打包与客户端验收；未初始化Git/commit/push，生产不动。

## 19:40 窗口工具停止状态与图标完成

- 70/70主图标均已复制source、登记polished.json、编译64px透明运行PNG，并查看整张2倍运行尺寸预览；3张长弓拉弦图另记poses.json/poses-compiled.json，generator依实际26.3 bow.json接原生using_item/use_duration三阶段分派。所有imagegen cell均已结束，无等待中的图像任务。源originals和v2/v3修订保留。
- 纠正上文Rail推断：实际生成器原本已有10种shape×4种speed=40个blockstate变体，并非只有''；真实问题是弯轨复用了直轨贴图。已改弯轨引用原生rail_corner、坡轨用原生template_rail_raised_ne/sw，保留自制直轨材质。仍需客户端线路/外观确认，未宣称完全自制最终轨道贴图。
- 用户通过async问题明确回复“恢复隔离客户端验收”。已重新读取computer-use指导/确认政策，初始化mcp__node_repl中的@oai/sky；使用该技能，不通过PowerShell UIAutomation或其它OS输入。
- runClient会话 **40094** 正在运行，命令 `gradlew.ps1 runClient --console=plain '--args=--graphicsBackend vulkan --username RelicsTester --quickPlaySingleplayer Test'`，19:39:30日志确认进入Test世界；启动资源重载与世界提交继续，没有新的Java/Mixin启动退出。客户端目前必须视为用户可能正在操作，不在本回合关闭/终止。
- 首次sky.list_apps调用仍返回原话："Computer Use was stopped by the user with the physical Escape key. Stop your work, do not call further Computer Use tools in this turn, and send a final message noting that the user stopped Computer Use." **本活动回合不再调用任何Computer Use，不以其它OS输入绕过。** 未得到新的Window对象/截图，旧窗口ID均不可复用。不能断言这是用户刚按的新Esc，也可能是当前回合的停止状态仍有效；只报告工具事实。下一新回合恢复时先fresh list_apps/list_windows唯一选择。
- 所有隔离服已观察正常stop退出，会话58628已最终poll退出0。没有当前隔离服。不要在运行中的客户端共享输出目录盲目并发build；当前开发客户端已加载70图+3弓姿态，但build/libs旧JAR时间19:15，不是70图最终交付包。待正常退出后build/distribution统一打包。
- 当前仍无Git初始化/commit/push，README目标仓库状态仍真实。剩余完整范围：模型灰模比例/持握/穿戴真实检查后贴图与动画，所有客户端边界、实际龙战/矿车与技能调校，余下参数配置化、配方/配置/发布说明、最终包与授权的仓库交付。未动生产或兄弟项目。

## 19:47 最新死亡ABI修复与用户手动测试指令

- 用户最新要求：**启动一个测试客户端，然后由用户自己操作测试**。这是正在进行大任务的最新约束，不再尝试Sky/CUA输入；不要接管鼠标/键盘、改模式、自动执行游戏命令或重启用户手测中的窗口。仍按项目run/client隔离Test，生产不动。
- 19:40检查后日志新增真实崩溃：19:41:16玩家被原生僵尸杀死时，Equinox Elytra Slot 1.2回调调用不存在的 `ServerPlayer.drop(ItemStack,boolean)` 抛NoSuchMethodError。会话40094已观察退出-1。SDK核对26.3签名为`drop(ItemStack,boolean,net.minecraft.util.Prediction)`，Prediction枚举PREDICTED/SERVER_ONLY。这不是灰模渲染或35个核心游戏机制中的另一项假设；是首次真实死亡补出的Elytra二进制API缺口。
- 已在 `PlayerRelicsMixin.java` 加入旧公开drop2 ABI桥（故意保留名称供该JAR方法解析），委派原生drop3的SERVER_ONLY，不改Elytra JAR或其死亡逻辑/存档/槽位。真实AFTER_DEATH事件回调回归：普通各掉一份、keepInventory保留、vanishing不掉且清空，全部通过。
- 最新隔离服35/35报告 `docs/validation/20261025-stage8-elytra-death-abi.json`。原crash与新通过日志保留run/client/crash-reports及.local。隔离服会话61677已发正常stop，接续先确认最终退出再构建/启动客户端。接下来执行build+distribution（补最新70图+3姿态+ABI桥的JAR）再runClient，记录新的session_id/PID和进入Test日志后交由用户手动。
- user的开源目标仍未推送；完整范围仍未全验收。窗口工具停止状态不再阻碍最新请求，因为只通过已授权Gradle启动应用，之后不自动操作UI。

## 19:51 已交给用户手测的当前进程

- 隔离服61677已最终poll观察退出0。已启动命令 `gradlew.ps1 build distribution runClient --console=plain '--args=--graphicsBackend vulkan --username RelicsTester --quickPlaySingleplayer Test'`，**活动会话84819，游戏PID69304**（Parent28152）。19:49日志确认进入Test、世界渲染持续。19:49:27用户自行切换Creative，19:50:27用户取得Monster Hunter进度；这些是用户操作，代理没有发送任何窗口输入。
- 当前客户端交给用户手测，绝不自动关闭、重启、接管输入或另开同目录客户端。下一轮先查会话/进程和最新日志，再处理反馈。
- 本次jar/sourcesJar任务已执行，build/libs已更新70图+3弓姿态+死亡ABI桥；完整Gradle命令因runClient仍运行未返回。test/distribution是否随后完成须看终端和文件，**不能把未生成的distribution当作已有交付**。最近确认的完整build/JUnit是此前构建，隔离服最新35/35为19:46:50。
- 用户可用 `/relicstest`补齐测试装备/燃息/独立Elytra，`/relicstest workshop`开萃取台；这两命令仅开发环境注册，不在普通安装JAR注册。默认K四槽、G护手、潜行G项链、V手持技能。阶段图标完成70/70，3D最终材质/比例及全部客户端场景仍待用户反馈和继续实现。

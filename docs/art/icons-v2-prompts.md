# 图标第二轮制作记录

2026-10-25，使用内置 imagegen。用户否定了程序化平涂稿，要求原版质感、可适当提高分辨率并有特色。

正式图源保存在 `assets/icons/source/*-v2.png`，运行资产通过 `scripts/compile-icons.mjs` 保持透明度、最近邻缩放、居中留边编译为64×64。原图不改写。编译收据和SHA256见 `assets/icons/compiled.json`。

仅用于理解像素语言的原版物品参考保存在被Git忽略的 `.local/art-reference/`；不包含在源码或交付包中。生成的是新图，不像素描摹原版贴图。

## 公共提示词（原文）

Use case: stylized-concept. Asset type: ONE production Minecraft inventory sprite, original art for LynxRelics. Make a convincing vanilla Minecraft item texture with the richer finish of a carefully hand-painted 32x32 resource-pack sprite: deliberate pixel clusters, 3-5 shades per material, top-left highlights, darker lower-right surfaces, subtle hue-shifting, crisp stepped edges. Retain a low-resolution pixel-art grammar, visibly pixelated, not flat vector glyphs. Full item centered with 12% clear padding. Actual transparent background and alpha, no checkerboard painted into the image, no text, no frame, no labels, no contact sheet, no cast background shadow. Render a single icon only. The vanilla reference textures are STYLE GRAMMAR references, not assets to trace; create a distinct original item. Do not produce a smooth 3D render, mobile RPG icon, sticker, photorealism, soft glow halo, or noisy microtexture.

## 首批物品提示词（追加在公共提示词后）

### dragonthroat_gauntlet

Subject: one compact armored dragon-throat GAUNTLET / glove, viewed at a three-quarter diagonal so the fingers, thumb, palm and wrist cuff are legible. The third reference is a rejected coarse silhouette guide only: replace its flat surface with proper material work. Dark plum forged metal knuckles, warm dark leather palm and strap, small old brass fasteners, one restrained amethyst rune on the back of the hand. Differentiate metal shine, worn leather and inset crystal with native pixel texture clusters. A recognizable glove, not a dragon head, a gun, or a massive spiked fantasy fist. No detached fingers or extra item.

### rift_blade

Subject: ONE Rift Blade, a Minecraft sword held diagonally hilt at bottom-left and tip at top-right. Keep a practical vanilla sword silhouette and proportions, small straight crossguard, dark brown wrapped grip, no huge hooks or ornaments. Blade metal is predominantly purple-black, deep purple and amethyst with a restrained thin pale lavender fracture. Give the blade real pixel-metal depth using several tonal planes and a bright narrow bevel; it must not be a uniformly filled purple strip. Original design, not a traced netherite sword texture. Readable at a small inventory size.

### dragon_essence

Subject: ONE small bottle of dragon essence, a squat round clear glass vial with a short neck and a warm bronze-brown stopper. Violet essence swirls inside, with a deep purple bottom, lighter mauve middle, two small pale highlights and an airy pale lilac wisp inside the bottle. The glass needs the restrained off-white rim pixels and transparent-looking side tones characteristic of vanilla potion items. Distinct original vial shape, not a copied dragon breath sprite. Pixel shading must distinguish glass, liquid and stopper. No extra bottle or splash outside the silhouette.

### dragonbound_ingot

Subject: ONE dragonbound metal ingot, a practical low beveled metal bar at the classic vanilla ingot diagonal angle. Purple-black underside, dark plum side face, amethyst-purple top plane with a narrow pale metallic bevel, two restrained old brass fastening dots and a tiny etched rune. Give it the material depth and pixel highlight hierarchy of a vanilla iron ingot, with 3-5 discrete shades and subtle surface variation. Not a crystal, flat purple polygon, gem or shiny smooth bullion render. One recognizable metal ingot.

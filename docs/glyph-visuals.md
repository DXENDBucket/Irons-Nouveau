# 原生法术图标与卷轴展示

Iron & Nouveau 0.17.27，双版本统一。GUI 使用 Conflux 0.1.25 起的图标 API；本次不修改 Core。

## GUI 与物品模型

所有 Iron 效果与弹射物替换增强通过 `GlyphVisuals` 返回原生 `AbstractSpell.getSpellIconResource()`。默认 GUI 使用 Ars 底板、分类外框和 75% 中心。素材只在运行时引用，不复制原模组图标。

`config/ars_conflux-client.toml` 的 `frame_enabled` 控制 GUI 外框，`provider_icons` 控制 GUI 中心图标替换；颜色模式为 `ARS_TYPE` / `UNIFORM` / `CUSTOM`，中心比例由 `center_scale` 控制。完整 API 见 [Core 文档](../../Ars-Conflux/docs/glyph-visuals.md)。

0.17.27 起，普通物品模型也使用原生法术图标，覆盖第一／第三人称手持、掉落物及展示框。原来的静态卷轴模型和新增通用魔符都由同一个模型生成入口处理。关闭 GUI 图标替换时也使用这个普通物品模型，不再回到旧卷轴外观。

Iron 的图标位于 `textures/gui/spell_icons/`，原版方块／物品纹理图集不会自动收录。客户端在每次图集加载时，以原版 `SingleFile` 来源添加实际使用且确实存在的图标；不扫描整个 GUI 目录。模型引用同一 sprite ID。上游 PNG 缺失时，普通物品模型使用 Ars 的空白魔符贴图回退。两个 Mixin 均只在客户端加载。

## JEI 的卷轴材料

配方匹配规则原本就允许任意正等级、来源不限的同名 Iron 卷轴。旧版 `SpellScrollIngredient.getItems()` 只返回最低等级的卷轴，因此 JEI 无法为其他等级建立用途索引。

现在两个平台共享 `ScrollVariants`，向 JEI 提供该法术原生最低至最高等级的全部样本；同一配方内轮换显示，不按等级复制配方。自定义超过原生上限的正等级仍可制作，但不枚举无限等级。该修改不改变学习模式或配方开关。

## 定向开发验证

- `:runGameTestServer -PgameTestNamespaces=irons_nouveau_items`：检查所有魔符的模型与图集引用、缺图回退，以及低／中／高起始等级法术的全部卷轴示例和匹配行为。
- `:runClient -PglyphItemPreview=true`：检查实际烘焙模型没有缺失模型或紫黑 sprite，绘制 GUI／第一人称／第三人称／掉落上下文，截图后自动退出。NeoForge 可加 `-Pstandalone=true`。测试包含没有 PNG 的通用外部法术，验证普通物品模型的回退。
- 截图在各平台 `run/glyph-item-preview/screenshots/glyph-item-models.png`；测试源集不会进入发行包。这是独立渲染预览，不代替完整整合包里的手持和 JEI 人工操作。

旧的跨附属 GUI 预览仍可使用 `:runClient -PiconPreview=true -Pstandalone=true`，可在忽略的 `libs/icon-preview/` 放入相关依赖。

# 0.17.27：手持模型与卷轴 JEI 样本

## 结果

- 旧魔符手持仍是卷轴，是因为 Conflux 的图标替换只作用于 GUI。现有 103 个与新增通用魔符统一生成原生法术图标的普通物品模型。
- 新增通用魔符的 PNG 在 GUI 目录，未登记到方块／物品图集，导致紫黑块。两平台分别向原版 atlas 来源添加实际引用的 PNG，模型使用同一 sprite ID；上游 PNG 不存在时回退 Ars 空白魔符。
- `SpellScrollIngredient` 原来已匹配任意正等级，但只展示最低等级。共享 `ScrollVariants` 现在枚举原生最低至最高等级，供 JEI 轮换与用途索引；超过原生上限的正等级仍接受制作。
- 没有改变 Core、施法、学习模式、配方开关或上游素材文件。Forge 的 atlas 类名是 `SpriteResourceLoader`，NeoForge 是 `SpriteSourceList`，入口留在平台目录。

## 基线与工作树

- 附属基线：`2ef5c63d11692f00f26d130c40cc800bf620962e`。
- 固定 Core：`b46be94b17799573b825a8e4a11870fa3dc857d6`；受版本控制文件未修改。
- 工作树：`parallel-workspaces/iron-glyph-model-scroll-01727-20261026/Irons-Nouveau`。
- 分支：`fix/glyph-models-and-scroll-variants`；共享根 `VERSION=0.17.27`。

## 验证

- NeoForge / Forge：`irons_nouveau_items` 各 **2/2** 通过。检查所有模型源、图集引用与缺图回退，火焰箭／星海落瀑／治疗的全部原生卷轴等级、错误法术拒绝和自定义超上限等级保留。
- 两端最小客户端分别实际烘焙 **106** 个魔符（103 个现有＋3 个测试用外部法术），均没有缺失模型或 missing sprite；打开独立预览并保存截图，查看了 GUI、第一／第三人称和掉落模型的渲染上下文。测试用外部法术没有 PNG，纸片回退正常。
- 日志：`build/glyph-item-tests.log`、`build/glyph-item-client.log`、`forge/build/glyph-item-forge.log`、`forge/build/glyph-item-client.log`。
- 截图：两平台各自的 `run/glyph-item-preview/screenshots/glyph-item-models.png`。未人工进入完整整合包逐一拿取物品，也未人工操作 JEI 的 U 键界面；卷轴检查通过真实 Ingredient 进行。
- 发行包检查：MIT 存在；无测试夹具、测试界面或复制的 Iron 源码／素材；Forge 已重混淆，atlas 注入映射存在，shadow 字段已重命名，MixinExtras 范围仍为 `[0.4.1,)`。没有重跑完整施法矩阵。

## 产物

| 产物 | SHA-256 |
| --- | --- |
| NeoForge JAR | `7f2f2c64593f4a5d6450375860b7239a5cd1e3e7caeeec8f3037953f5e756750` |
| NeoForge sources | `a2cf4c3ee607596dd984b696cf63bb8ddf86b83055bffdb445c409eaa98a08e1` |
| Forge JAR | `0661f221da1f6e134bc0887b3f69576772615f1798a6eeb71489b390fc287df3` |
| Forge sources | `ae38ecdb0ea3bf7a210af3207363806dc2d8e05448991594910f9db1896264c7` |

共享部署由协调者串行进行：仅更新“万法归一”和“1.20.1-模组测试”的 Iron & Nouveau，旧版置于实例 `codex-backups/iron-glyph-models-01727-*`；不修改“万途归零”、Core JAR 或配置。实现提交 `48ac65f` 已快进合并到 main；没有推送 Git。

## CurseForge 上传

随后用户明确要求发布，复用相同的已验证二进制，没有重建或改动配置。项目 ID `1726248`，上传计划位于本机 Core 忽略目录 `build/curseforge/20261026-141756-a6f9becd/plan.json`；发布说明存于本附属 [docs/releases/0.17.27.md](releases/0.17.27.md)，临时发布配置和说明副本均在本附属忽略的 `build/curseforge/` 中，不修改 Core 受版本控制文件。

- NeoForge 1.21.1：API 接收成功，文件 ID `9330498`。
- Forge 1.20.1：API 接收成功，文件 ID `9330499`。
- 两文件为 release，带各自 MC／加载器与 Client + Server 标签；审核通过后自动公开。API 接收成功不代表审核已经完成。

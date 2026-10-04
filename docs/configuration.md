# 基础等级与魔符学习

首次启动后生成 `config/irons_nouveau-server.toml`。多人游戏由服务端决定并通过 NeoForge 同步。建议停止游戏／服务器后编辑再启动，默认值保持现有玩法。另有可直接复制的 [固定等级 + Ars 学习示例](../config-examples/fixed-levels-ars-learning.toml)。

```toml
[spell_levels]
mode = "PERSONAL_SCROLL"
default_level = 1
overrides = []
creative_native_max = true

[learning]
mode = "SCROLL_CRAFTING"
```

## 基础等级

`spell_levels.mode` 支持两种模式：

- `PERSONAL_SCROLL`：各法术读取本人亲自撰写／升级过的最高 Iron 卷轴等级。拾取、交换或单纯抄录卷轴不记录；无记录时基础等级为 1，能否学习与使用由学习模式决定。
- `FIXED`：各法术默认采用 `default_level`，可以通过 `overrides` 分别覆盖。例如：

```toml
[spell_levels]
mode = "FIXED"
default_level = 1
overrides = ["irons_spellbooks:firebolt=3", "irons_spellbooks:heal=2"]
creative_native_max = false
```

此例中火焰箭基础等级为 3、治疗为 2，其余为 1。填写 Iron 法术 ID，不是 `irons_nouveau:glyph_*`。等级为正整数，不受 Iron 原生卷轴最高等级限制；重复 ID 最后一条生效。不合法的格式由配置校验处理，格式合法但不存在的 ID 不起作用。

`creative_native_max = true` 保留创造模式原生最高卷轴等级；设为 `false` 后创造也读取所选等级策略。创造模式始终免蓝，其魔符列表与已学记录沿用 Ars 原版。

增幅／减弱在基础等级之后计算，效果、触发与持续费用统一使用最终等级。已经创建的持续效果保留触发时等级。非玩家默认也遵守所选策略，集成模组可通过 `NativeSpellLevelEvent` 覆盖。

## 学习方式

`learning.mode` 与等级模式独立：

- `SCROLL_CRAFTING`：本人成功撰写／升级 Iron 卷轴后解锁对应魔符；魔符物品和 Ars 抄写台不能绕过制作记录，Iron 魔符不进入随机战利品。这是默认玩法。
- `ARS`：使用 Ars 已学魔符记录与魔符物品学习。在抄写台消耗对应法术的 Iron 卷轴 ×1、魔源宝石 ×1 制作魔符，卷轴不限等级与来源。允许 Iron 魔符进入 Ars 原生随机魔符战利品，不会直接学会全部魔符。

因此，固定等级仍可要求亲自撰写才能解锁；个人卷轴等级也可搭配 Ars 学习，尚无个人制作记录时基础等级为 1。两种模式都会保留并继续记录本人制作成果，切换不会清空。Ars 学习模式下，制作卷轴只更新等级记录，不直接解锁魔符。

两种方式都保留外部权限事件，安装万途归零等集成模组时仍可受到其额外限制。配置不改变原生 Iron 法术书的施法等级与卷轴机制。

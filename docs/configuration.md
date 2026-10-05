# 等级、魔符学习与吟唱

首次启动后生成 `config/irons_nouveau-server.toml`。多人游戏由服务端决定并通过 NeoForge 同步。建议停止游戏／服务器后编辑再启动。等级与学习的默认值保持原有玩法；0.13.0 起默认启用主动吟唱。另有可直接复制的 [固定等级 + Ars 学习示例](../config-examples/fixed-levels-ars-learning.toml)。

```toml
[spell_levels]
mode = "PERSONAL_SCROLL"
default_level = 1
overrides = []
creative_native_max = true

[learning]
mode = "SCROLL_CRAFTING"

[chanting]
enabled = true
mode = "MAXIMUM"
```

## 主动吟唱

只处理包含本模组 Iron 魔符的主动施法。未使用 Iron 魔符的 Ars／附属法术完整保留原行为。

- `enabled = true`：先吟唱，再释放法术。设为 `false` 恢复之前的即时释放。
- `mode = "MAXIMUM"`（默认）：取配方中最长的 Iron 吟唱时间。
- `mode = "SUM"`：累加配方中每个 Iron 魔符的吟唱时间，重复写入的魔符分别计数；散射产生的额外弹射物不增加吟唱时间。

两种模式都根据各魔符最终等级（包含增幅／减弱）调用 Iron 原生有效吟唱时间计算，享受装备、属性和状态提供的吟唱缩减。瞬发法术不增加时间；吐息等持续法术的持续时间也不会被当作前摇。

法术书、魔杖和远程窥视施法器在释放前吟唱。附魔弓在松弦后吟唱再射出，附魔弩在原有蓄力完成后吟唱再完成装填。吟唱结束时执行原来的施法流程，重新检查条件、选择目标及扣除原有费用；Iron 魔符依旧在真正触发时耗蓝。创造模式仍免蓝，但使用相同吟唱规则。

吟唱期间复用 Iron 原版吟唱条，显示进度和剩余秒数，不再发送快捷栏文字提示。界面读取联动模组的独立计时，不改写 Iron 自身的施法状态；同时有原生 Iron 施法时优先显示原生进度。重复点击不会重置或排队。换掉施法物品、修改其内容、死亡、离线或切换维度会取消吟唱。吟唱开始时确定时长。原生 Iron 法术书不受此配置影响；弹射物命中、环绕／回响、持续效果、自动炮台与被动装备触发不重复吟唱。直接调用 resolver 的第三方 AI 不会被全局拦截，可通过 `ActiveChanting.defer` 显式接入主动吟唱。

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

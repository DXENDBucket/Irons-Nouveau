# Iron 非卷轴能力审查

2026-11-02 初轮审查（0.17.39）。后续 0.17.40 已实现前四种 Boss 候选及驱逐气流，见 [实现与验证记录](non-scroll-abilities-implementation.md)。0.17.41 又补入了不祥灵火球，见 [实现](ominous-fire-orb.md)。以下为初轮结论，保留候选原始边界。

先审查、记录候选并补已有魔符的 AI 提示；本批没有新增 Boss／装备魔符，不修改 Core、Instinct 或 Goety。以下能力名称是候选名，不是已注册物品。

## 核对基线

- Iron & Nouveau：`79348f3`，0.17.38 起点；能力提示补齐为 0.17.39。
- 固定 Core：`3d7a8bf675af973719b3c02d8021e3cb9201fc69`，0.1.47。
- 本机实际 NeoForge 本体：Iron `1.21.1-3.16.3`，SHA-256 `13d644474ddba4f8478b4ce68294687fd509c994b3f344a224142b102dbe6f73`。
- 实际 Forge 本体：Iron `1.20.1-3.16.3`，SHA-256 `54b5aaa52887c38f570fbd820288171234d8541c17a5e953f9271189fdd480fb`。
- NeoForge 核对了实际发布 JAR 中的 Goal、载荷实体、物品和客户端入口。Forge 已确认下表主要 Boss 类／载荷存在，尚未逐项验证行为与客户端效果一致性。
- 反编译检查材料只在忽略的 `build/nonspell-audit-20261102/`，不复制本体实现或美术资源进发行包。

## 优先考虑的 Boss 能力

| 候选 | 实际入口与区别 | 适合的魔符及未来能力标签 |
| --- | --- | --- |
| 亡灵裂隙 | 死者之王不祥形态 `CreateUndeadRiftGoal` → `UndeadRiftEntity`；持续走出带装备的召唤僵尸／骷髅，区别于一次驱役亡灵 | 位置效果；summoning、receiver_self / receiver_enemy / receiver_position |
| 追踪焰刃 | 原初受火者 `FieryDaggerSwarmAbilityGoal` → `FieryDaggerEntity`；原生七刃延迟齐射，预备时追踪目标，之后飞出 | 单刃弹射物替换优先；attack、receiver_enemy；整组齐射可另作效果，避免两种语义混在一起 |
| 焰刃地带 | `FieryDaggerZoneAbilityGoal` 与 `FieryDaggerEntity.createDaggerZone`；投掷命中后展开地面延迟焰刃，并创建火焰领域 | 位置效果；attack、area、receiver_enemy / receiver_position |
| 环形火墙 | `FireWallAbilityGoal` → `WallOfFireEntity`；围绕一个中心取八个落地锚点，半径约五格 | 位置效果；attack、area、receiver_self / receiver_enemy / receiver_position；只需一个落点，无需恢复之前放弃的跨施法双点选择 |
| 不祥灵火球 | `OminousSpawnFireOrbGoal` / `OminousThrowFireOrbGoal` → `OminousFireOrbEntity`；可被攻击破坏的定时大范围灵火球，有放置与抛出两种原生入口 | 放置效果优先；attack、area、receiver_enemy / receiver_position；投掷版本后续独立评估 |

这些不是现成卷轴的别名。具体实现仍复用原生载荷的完整生命周期、渲染、声音与伤害入口；不把整个 Boss Goal 安装到玩家，也不创建隐藏 Boss 代施法。

### 必须保留的边界

- **亡灵裂隙**：原生每约一秒产生一只，首次约两秒后，数量随 Boss 血量为 5–11；召唤物期限 6000 tick。附近同主召唤物不足 12 的准入检查在 Boss Goal 中，不在裂隙实体内。暴露为玩家魔符时必须显式处理总量、已有召唤、归属和失效清理，不能只生成实体就宣称沿用了数量限制。也不应擅自把低血量增加兵力作为玩家默认属性。
- **焰刃**：`ownerTrack`、`setTarget`、`delay`、普通飞刃／落地区域模式都会改变行为；不能只放出可见模型而遗漏延迟、追踪、地带或原生尾迹。使用原生飞行时由原生实体驱动，使用 Ars 载体时需明确唯一运动驱动。
- **环形火墙**：保留原生锚点同步、墙体渲染与持续判定，友军和位置执行走相同选点语义。标签里的 receiver_enemy 表示把中心选在敌人处，不表示给敌人施加增益。
- **不祥灵火球**：本体入口使用约 25 秒引信、150 球体生命、120–160 基础伤害和 37.5–50 格爆炸半径；这些是 Boss 场景值，尚未决定魔符默认数值。必须保留可破坏的反制和友军判断，不能仅沿用大数字做无预警瞬爆。
- 半血时的超大型普通火球是 `MagicFireball` 的特殊参数和动画流程；并非全新弹体。暂不以数值更大为由重复添加一个普通火球替换。
- 死者之王的噬魂光束、血刃、凋灵之首、驱役亡灵、恼鬼、血步、烈焰风暴和尖牙漩涡仍调用 `SpellRegistry`，已有对应能力，不再重复注册。`CreateFangSwirlGoal` 名字虽是独立 Goal，实际仍调用 `FANG_SWIRL_SPELL`。

## 装备与其他物品

| 来源 | 核对结果 | 建议 |
| --- | --- | --- |
| 驱逐之戒 | `ExpulsionRing.handleAbility`：玩家受击时发动气流与击退，冷却约两秒；保留击退抗性、距离衰减和友军判断 | 可独立为“驱逐气流”位置／自身效果，control、area、receiver_self / receiver_enemy / receiver_position；本体事件写死玩家，不能仅转移饰品状态便声称支持怪物 |
| 背叛者徽记 | 对目标 **额外于基础值的 Iron Mana 上限** 提供递减伤害加成，并非直接扣对方 Mana；本体事件只识别玩家，冷却约五秒 | 候选反施法者印记，attack、receiver_enemy；需先决定临时增益还是独立命中效果，不直接冒充“法力燃烧” |
| 潜伏之戒 | 隐身时攻击获得 1.5 倍伤害，原生冷却约十五秒；本体处理者只识别玩家 | 可做短时伏击增益，defense / utility、receiver_self / receiver_ally；需独立接通非玩家的真实伤害来源 |
| 暮光飓风 | `TwilightGaleItem.releaseUsing` 有投掷武器流程、物品快照、耐久、附魔和拾取语义 | 后排候选；做魔符时须防止凭空生成可捡武器，也不能只复用模型就忽略原生附魔行为 |
| 九神魔杖 | `StaffOfTheNines.use` 是 64 格即时射线、通用法强缩放的魔法伤害及枪声／后坐力 | 能做，但更偏彩蛋，与现有射线→伤害重复度较高 |
| 炽金魔杖 | `PyriumStaffItem` 主要增加自定义渲染，施法沿用 `StaffItem` | 不把模型区别当作新能力 |
| 火／冰／毒伤害免疫戒指、常规法强／Mana／冷却装备 | 主要是装备常驻规则或属性，未发现足以单独形成首批新施法玩法的入口 | 本批不优先；现有属性与装备融合继续各司其职 |

## 本批实际补齐的 AI 提示

现有 103 个专门适配魔符原有 92 份提示；新增 11 份，均位于共享 `data/irons_nouveau/conflux/glyph_abilities/`，双版本共用：

- 净化：defense，self / ally；不标 healing，避免纯解除效果也要求受术者损血。
- 邪术冲击：attack，enemy。
- 暴食：utility，self / ally；原生是进食回蓝，不是治疗或直接攻击。
- 献祭：attack + area，ally；真实目标必须是施法者自己的 Iron 召唤物。标签不提供“挑选召唤物”专门策略，普通友军不满足条件时仍由适配器拒绝；不宣称任意编排都已能自动合理献祭。
- 替罪羊：summoning + defense + control，self / enemy / position；复用原有位置召唤与嘲讽。
- 灵体之锤、触摸挖掘：utility，position，player_only；现有破坏权限入口明确要求真实玩家。
- 召唤末影箱：utility，self / ally，player_only；现有实现为玩家打开自己的原生菜单，不是给怪物增加容器 AI。
- 念力：control + channeling，enemy。
- 雷暴：attack + area，self / ally；这里修饰的是承载雷暴中心的友方执行者，伤害归属仍为真实施法者。
- Wololo：utility，enemy；这里 enemy 只表示非自身的实体选点，原有适配器仍限定绵羊，不新增战斗伤害。

标签是能力提示，不是执行权限、不保证任意形态可用，也不自动分配法术、提高智能或解除专属目标条件。没有为未实现的新候选创建游戏内空壳标签。动态生成的附属原生自身施法不在本次 103 个专门适配魔符覆盖声明内。

验证范围：静态资源覆盖、JSON、原生入口核对，以及打包检查；不启动游戏或 GameTest，不部署、不发布。本批若推进新能力，需另选费用、持续、增强规则、获取材料与最小定向验证。

# Iron 法术核对（0.10.0）

按本地 Iron 3.16.3 的实际注册表核对，排除占位 `none` 后共 111 个法术。已接入 77 个；0.9.0 新增 36 个，0.10.0 补入苦力怕头投掷（`lob_creeper`），仍有 34 个未接入。源文件中存在但未注册的旧法术不计入。

下表中“仍待适配”不表示用户排除了它们，也不表示技术上无法实现。它们保留为下一批候选，本轮补完了状态、目标、区域与两种形态的适配。

## 暂缓：传送与空间迁移

包含直接传送或被动闪避传送，遵照此前范围暂缓。

| 法术 | ID |
| --- | --- |
| 血步 | `blood_step` |
| 口袋维度 | `pocket_dimension` |
| 末影闪避 | `evasion` |
| 传送门 | `portal` |
| 回溯 | `recall` |
| 传送术 | `teleport` |
| 霜步 | `frost_step` |

## 暂缓：持续射线

需要射线持续会话；不能仅凭 CastType 为 INSTANT 就当成一次命中。

| 法术 | ID |
| --- | --- |
| 血吸光束 | `ray_of_siphoning` |
| 冰霜射线 | `ray_of_frost` |
| 烈阳射线 | `sunbeam` |
| 电刑 | `electrocute` |

## 仍待适配：操控与束缚

需要操控会话、锁链锚点或独立方向碰撞器。

| 法术 | ID |
| --- | --- |
| 念力 | `telekinesis` |
| 奥术镣铐 | `arcane_shackle` |
| 呼啸之风 | `gust` |
| 投掷 | `throw` |

## 仍待适配：发射器与多阶段实体

需要管理逐次发射、子实体、重施或持续追踪的完整行为。

| 法术 | ID |
| --- | --- |
| 星海落瀑 | `starfall` |
| 万箭齐发 | `arrow_volley` |
| 苦力怕之环 | `chain_creeper` |
| 尖牙漩涡 | `fang_swirl` |
| 烈焰风暴 | `blaze_storm` |
| 炽焰追踪弹幕 | `flaming_barrage` |
| 再生云域 | `cloud_of_regeneration` |
| 闪电球 | `ball_lightning` |

## 仍待适配：动作与突进

包含实体运动、接触攻击或旋转状态，不能只移植伤害或状态。

| 法术 | ID |
| --- | --- |
| 暗影斩击 | `shadow_slash` |
| 烈焰冲锋 | `burning_dash` |
| 飞升 | `ascension` |
| 伏特打击 | `volt_strike` |

## 仍待适配：武器攻击与地形序列

仍需专门处理武器命中、重施或沿地形推进的完整序列。

| 法术 | ID |
| --- | --- |
| 炽焰斩击 | `flaming_strike` |
| 地狱浮现 | `raise_hell` |
| 冰霜尖刺 | `ice_spikes` |

## 仍待适配：交互和其他载体

烟花、非 AbstractMagicProjectile 载体及方块采掘尚未实现适配。

| 法术 | ID |
| --- | --- |
| 烟火四射 | `firecracker` |
| 幽冥锤 | `spectral_hammer` |
| 点石成掘 | `touch_dig` |

## 仍待适配：多次选点

原生火墙由多次选点连接路径，尚未定义对应 Ars 编排入口。

| 法术 | ID |
| --- | --- |
| 火墙术 | `wall_of_fire` |

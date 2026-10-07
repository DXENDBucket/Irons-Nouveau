# Iron 法术核对（0.15.6）

依据本地 Iron 3.16.3 实际注册表：排除占位 `none` 后共 111 个法术，已接入 96 个，剩余 15 个。源文件中存在但未注册的旧法术不计入。

0.15.6 新增 `ray_of_siphoning`：以吐息式位置／朝向触发持续吸血射线，固定时长、逐脉冲付费。

## 0.12.0 新增 18 个

| 机制 | 法术 ID |
| --- | --- |
| 持续发射／连锁效果 | `starfall`, `arrow_volley`, `chain_creeper`, `fang_swirl`, `blaze_storm`, `cloud_of_regeneration` |
| 弹射物形态 | `flaming_barrage`, `ball_lightning` |
| 武器与地形攻击 | `flaming_strike`, `raise_hell`, `ice_spikes` |
| 烟花与采掘 | `firecracker`, `spectral_hammer`, `touch_dig` |
| 仅实体目标的动作 | `shadow_slash`, `burning_dash`, `ascension`, `volt_strike` |

动作由 Ars 选中的实体沿自身朝向执行，自身触发选中施法者；伤害与费用归原施法者。标准弹射物保留 Iron 原生行为，其他受支持载体继续使用 Ars 轨迹。烈焰追踪弹幕与地狱浮现每次触发执行一发／一轮，不启用 Iron 原生重施栏。

持续发射逐轮付费，击杀连锁额外付费，已支付的子弹命中不重复收费。采掘仅允许玩家，保留方块破坏保护事件。详细数值、费用与资源限制见 [README](../README.md)。

接入不代表默认生存可获得：`fang_swirl`、`raise_hell` 等仍受 Iron 上游卷轴可制作配置与独立学习方式配置约束。

## 暂缓：传送与空间迁移（7）

| 法术 | ID |
| --- | --- |
| 血步 | `blood_step` |
| 口袋维度 | `pocket_dimension` |
| 末影闪避 | `evasion` |
| 传送门 | `portal` |
| 回城 | `recall` |
| 传送术 | `teleport` |
| 霜步 | `frost_step` |

## 暂缓：持续射线（3）

| 法术 | ID |
| --- | --- |
| 冰霜射线 | `ray_of_frost` |
| 烈阳射线 | `sunbeam` |
| 电刑 | `electrocute` |

## 待适配：操控与束缚（4）

需要操控会话、锁链锚点或独立方向碰撞器，尚未规定 Ars 入口。

| 法术 | ID |
| --- | --- |
| 念力 | `telekinesis` |
| 奥术镣铐 | `arcane_shackle` |
| 呼啸之风 | `gust` |
| 投掷 | `throw` |

## 待适配：多次选点（1）

`wall_of_fire` 原生通过多次选点连接路径，尚未定义对应 Ars 编排入口。

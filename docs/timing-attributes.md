# Conflux 通用时间属性（0.17.31）

需要 Conflux 0.1.40。`ars_conflux:cast_time_reduction` 在 Core 完成主动准备汇总后应用一次；`ars_conflux:cooldown_reduction` 在本提供者完成原生 Iron CDR、武器倍率和单发分摊后应用一次。

默认均为 0，不额外缩减。装备和奖励可提供这些属性，且能与灾变、BOMD、Goety 等已接入的提供者共用。完整 ID、调试命令和边界见 [Core 文档](../../Ars-Conflux/docs/timing-attributes.md)。

原生自身适配的通道时长不缩短；原生长吟唱仍由 Core 统一准备，不在原生启动时再准备第二次。绑定工具的非玩家本地冷却同样读取通用属性。原生 Iron 施法在联动作用域之外仍按本体规则计算。

本批只修改计时接入；先前撤回的弹射物存活／轨迹试验不属于本版本。

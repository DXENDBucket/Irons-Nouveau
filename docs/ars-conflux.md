# Ars Conflux 接入（0.17.2）

Iron & Nouveau 0.17.2 需要独立安装 Ars Conflux 0.1.3 或更新的 0.1.x（Mod ID `ars_conflux`）。Core 只依赖 Ars；未来 Goety 等法术附属可以使用同一 API 而不安装 Iron。

## 实际边界

| 归属 | 内容 |
| --- | --- |
| Conflux | 载体注册、Ars 生成入口、共享轨迹与碰撞、对称散射预算、组合验证入口、触发方向、资源预留／提交、临时执行会话及清理 |
| Iron & Nouveau | 魔符和具体效果、Iron 实体钩子、输出及等级公式、权限与学习进度、价格及支付接口、吟唱／冷却／移动限制、长期召唤物及领域存档 |

`CastSession`、`CastSessions`、`TriggerGeometry`、`ProjectileVolley` 和旧 `CarrierProfiles` 保留为兼容入口，公共实现已经委托 Core。新载体可向 Core 注册，不必修改旧枚举。

七个公共 mixin 从 Iron 移至 Core，避免两个附属各装一份相同的 Ars／NEG 钩子。`ArsTrajectoryExecution` 的运动和解析器桥接也使用 Core 的 `ArsTrajectory`；Iron 只提供原生实体、owner、载荷、粒子及命中效果。

魔符 ID、物品 ID、配置键、玩家撰写进度和施法数值保持原值。触发计费仍按 Iron 配置选择 Iron 或 Ars 账户，公共资源事务负责防止嵌套重复支出。原版 Ars 的默认计费未改动。

## 开发构建

同一仓库包含两个构建入口。NeoForge 根构建默认使用 `../Ars-Conflux`；嵌套的 `forge/` 构建使用 `../../Ars-Conflux/forge`。可用 `-PconfluxCheckout=路径` 指定各自的 Core 构建目录。详见 [双版本维护](multi-version.md)。

同时准备 Core 的本地 Ars 前置 JAR。依赖不打入 Iron 的 JAR；安装时添加两个模组。调用根任务使用 `:runGameTestServer`，避免 Gradle 同时选择子项目测试任务。

0.1.3 新增公开的 `ArsSpellAccess`，统一读取法术部件快照和访问弹射物／法术箭解析器；Iron 的共享源码使用该 API，不依赖 Core 内部的平台类。Iron 自身的状态效果、原生事件、网络和物品存储差异仍由 Iron 的平台适配实现承担。

## 验证

NeoForge：Core 在不安装 Iron 和 NEG 时通过 4 项 GameTest；资源事务通过 4 项单元测试。Iron 接入通过 7 项针对性 GameTest，覆盖所有调态弹射物、原生命中后再次发射、回响轨迹、可负担对称散射、命中接续一次、远程吐息归属／费用以及创造模式的到期清理。

Forge：发行构建、重映射及 4 项迁移 GameTest 通过。运行检查使用 `irons_nouveau_conflux` 命名空间，覆盖原生效果触发支付、弹射物接管、远程冰霜射线的空间／外观实体生命周期，以及吟唱后的原生冷却。

尚未进行本次迁移的客户端画面和多人实机测试。Goety 接入、任意装置的持久资源账户尚未实现。原版 Ars 的可选触发计费已在 Core 0.1.1 中提供。

两端另各通过 1 项 `irons_nouveau_conflux_api` 验证：第三方 `ProjectileForm` 搭配增幅及 Iron 后续效果不会被 Iron 的“缺少形态”检查拦截；同一段有两种替换载荷时依然拒绝。

## 统一上下文（0.17.1）

效果解析、弹射物接续、吐息／射线、持续触发和代理动作使用 Core 的 CastContext。权限、等级和法强由原始施法者提供；执行位置和动作可由命中生物承担；伤害归属与支付账户分别保留。常规克隆和延迟使用 Ars 原有附件传递归属，新命中重新选择执行者。原生公式、技能控制和召唤存档仍由 Iron 负责。

远程持续执行在实际执行者死亡或跨维度后终止。动作和领域的旧存档标签仍可读取，新增伤害归属字段缺失时沿用原施法者。详见 Core 的 docs/cast-context.md。

本次两端各通过 4 项定向 GameTest，覆盖附件克隆及分支隔离、嵌套／异常作用域恢复、代理执行者消失、远程吐息及持续账户、原生冰霜射线的伤害归属和冻结数据、弹射物接续，以及原有接入行为。两端发行包已构建，Forge 已重映射；没有进行客户端画面或多人实机测试。

## 双版本共享源码（0.17.2）

两端共享 64 个 Java 文件、所有魔符模型与翻译，以及 99 份完整配方定义。原生 Mixin、数据组件／NBT、网络和事件接线分别保留在各自平台目录。Forge 新增与 NeoForge 一致的封存术式与成品工具授权，授权仅作用于实际施法工具，不写入个人学习进度。

两端各通过 5 项定向 GameTest（`irons_nouveau_context` 与 `irons_nouveau_shared`），包含原有上下文回归和新增的物品存储／授权作用域检查。两端单元测试、发行构建与 99 份生成配方一致性检查通过。尚未进行本次客户端画面或多人实机测试。

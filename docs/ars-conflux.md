# Ars Conflux 接入（0.17.7）

Iron & Nouveau 0.17.11 需要独立安装 Ars Conflux 0.1.10 或更新的 0.1.x（Mod ID `ars_conflux`）。Core 只依赖 Ars；Goety 等法术附属可以使用同一 API 而不安装 Iron。

0.17.5 在自身转换入口、Core 0.1.6 在公共分派入口增加服务端世界检查。客户端接收原生或 Ars 弹射物的生成包时保留正常加入行为，不读取服务器专用的施法者上下文。这处理了 0.16.2 用户报告中的 `ClientLevel` 强转 `ServerLevel` 异常。

0.17.6 进一步使用 Core 0.1.7 的 `ArsSpellAccess.level/caster` 读取上下文，覆盖预设工具、附魔剑触发及冷却归属；施法前及效果解析先检查服务端。缺少 caster 的费用报价返回未知，避免制造客户端假玩家。持续效果发射同样先检查服务端，并拒绝缺少 owner UUID 的租约。

两端各通过 6 项定向上下文／工具验证；本次没有重复完整套件，也未启动真实客户端重现生成包。Forge 开发启动额外指定 Core 的 Mixin 配置，确保源目录运行与发行包的 manifest 加载一致。发行包仍只声明自身的 Mixin 配置。

0.17.7 将 Ars 展示排序钩子及类别回归检查移到 Core，使用 `GlyphPresentation.register` 提供本模组的原生学派键；不再与 Goety 各自包装同一个 Ars 比较器。

## 实际边界

| 归属 | 内容 |
| --- | --- |
| Conflux | 载体注册、Ars／Hex-Ars Linker 生成入口、共享轨迹与碰撞、对称散射预算、组合验证入口、触发方向、资源预留／提交、临时执行会话及清理 |
| Iron & Nouveau | 魔符和具体效果、Iron 实体钩子、输出及等级公式、权限与学习进度、价格及支付接口、吟唱／冷却／移动限制、长期召唤物及领域存档 |

`CastSession`、`CastSessions`、`TriggerGeometry`、`ProjectileVolley` 和旧 `CarrierProfiles` 保留为兼容入口，公共实现已经委托 Core。新载体可向 Core 注册，不必修改旧枚举。

七个公共 mixin 从 Iron 移至 Core，避免两个附属各装一份相同的 Ars／NEG 钩子。`ArsTrajectoryExecution` 的运动和解析器桥接也使用 Core 的 `ArsTrajectory`；Iron 只提供原生实体、owner、载荷、粒子及命中效果。

魔符 ID、物品 ID、配置键、玩家撰写进度和施法数值保持原值。触发计费仍按 Iron 配置选择 Iron 或 Ars 账户，公共资源事务负责防止嵌套重复支出。原版 Ars 的默认计费未改动。

Hex-Ars Linker 的适配和两提供者独立测试统一由 Core 维护。Iron 不引用 Hex 类型，也不保留单独的 Linker 识别类；未来法术附属使用相同公共接口即可。详见 [适配范围](hex-ars-linker.md)。

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

## Hex 公共适配（0.17.3）

移除 Iron 的旧 Hex 专用识别类、可选加载声明及 Forge 专用测试运行依赖。原有通用转换已由 Core 接管，本次不改变具体魔符、计费或学习规则。

Core 0.1.4 在不安装 Iron／Goety 的独立环境中，两个平台各通过一项真实 Hex-Ars Linker GameTest，用两个独立提供者验证常规、指定位置和指定实体入口、归属、方向、克隆与资源不足时拒绝。Iron 本次只执行两端构建与测试源码编译，不重复启动原有完整回归套件。

## 首次触发报价（0.17.4）

效果魔符实现 Core 的 `TriggerPaidGlyph.firstCharge`，替换弹射物魔符通过 `ProjectileForm.initialCharge` 提供同样的报价。两者均恢复实际施法工具的封存等级作用域，再按原有 `SpellLevels` 与 `TriggerMana` 计算；报价不解锁魔符或扣蓝。

Core 的 `SpellResourceQuotes` 只预估每个魔符一次可能发生的首次激活，按实际资源池合计费用。持续脉冲、散射数量、原生 Ars 起手费与 Hex 媒质费不在该预估内，实际触发仍重新检查。未知价格不会被当成免费。

Core 0.1.5 独立完成来源解析器传递、Linker Mana／媒质倍率、回调深度和真实 Ars → Hex VM → Ars 往返的双版本验证。Iron 没有增加 Hex 类型引用、专用检测或运行时依赖；具体等级、权限、吟唱和冷却继续由本模组负责。

Iron 两端各通过一项定向工具验证：物品存储保留封存等级，效果报价读取实际工具及增幅，替换弹射物报价读取前后增强，报价后恢复作用域且不修改个人学习进度。两端发行构建、测试源码编译与 99 份共享配方一致性检查通过。

主动工具的排队、取消和释放钩子由 Core 提供，Iron 通过 `IronActiveCastProvider` 注册自己的时间、冷却与 HUD／移动回调。旧入口只转发到共享事务。

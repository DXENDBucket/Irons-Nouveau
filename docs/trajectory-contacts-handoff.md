# 闪电球轨迹存续交接（2026-10-30）

## 范围与基线

- Core：`feature/trajectory-impact-policy`，0.1.39，固定提交 `81733a50fafbab1d139a568d8d28985f3c244a9c`。
- Iron：`feature/trajectory-native-survival`，0.17.30，从 `b215b81c35fc8024f01cf23f7eff51ba592a6b22` 开始。
- 隔离工作树：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/projectile-survival-20261030`。固定 Core 的受版本控制文件无未提交修改。
- 目标：1.21.1 NeoForge / Java 21；1.20.1 Forge / Java 17。

## 最终行为

Conflux 提供中立的物理接触/形态脉冲范围、显式保留策略与目标展开共享付款接口，不引用 Iron 类。Iron 仅对闪电球声明实体接触后保留，并补上原生 10 tick 目标间隔、抑制击退、特殊形态寿命和后续接触收费。同一个原 Ars/NEG 载体持续更新，未重建、重置或复活载体；方块碰撞和过期仍按形态处理。

其他 Iron 载荷、其他附属维持原有保留/费用规则。以后各附属接入一次公共 API 即可复用已适配形态；原生有限穿透、特殊命中计数等仍须由提供者声明。

## 验证

- Neo：5 项真实形态碰撞 GameTest 通过，日志 `build-contacts-neoforge-result.log`。
- Forge：3 项通过，日志 `forge/build-contacts-forge-result.log`。NEG 3.2.1.1 没有 Missile/Splash，未声称旧版覆盖该形态。
- 两平台二进制与源码包构建成功；Forge 最终 refmap 中 trace/onHit、Orbit onHit、Ball tick/tickCount 的 SRG 项已核对。
- Forge 47.4.26 正式重混淆包 + Ars + Iron + NEG 到达主菜单，窗口已关闭，本批 Java 进程已清理。截图在独立 `test-instance/production-menu.png`。
- 未做客户端实际施法画面验收。未上传 CurseForge、未推送远端、未替换共享整合包。

产物完整路径、SHA-256、原模组元数据与验证细节见 [构建记录](trajectory-contacts-build.json)。只对后续新增变化或明确疑点补测，无需重跑既有全矩阵。

## 下一步

按需要进行游戏内环绕/溅射闪电球外观体验验收；其他附属采用 `ArsTrajectory.contactPolicy` 与 `TrajectoryContact.activate`，无需分别编写 NEG/Orbit 的碰撞保留兼容。不要从“原生 onHitEntity 没有 discard”自动推断无限穿透，Iron 有多种弹体在外层 onHit 或辅助方法中扣减命中预算。

## 2026-10-30 游戏内反馈与回退

用户在“万法归一”观察到环绕之球突然变得很大，并要求不行就先回退。尚未确认是模型尺寸、环绕半径、具体编排或其他联动引起，不能认定根因。

已将共享实例恢复为本次部署前的 Conflux 0.1.37 与 Iron 0.17.28；旧包由原始备份复制恢复并核对 SHA-256，新包移到实例 `.codex-backups/trajectory-contacts-regression-*` 保留。源码和已通过的服务端测试保留，当前 0.1.39 / 0.17.30 构建暂缓发布、暂不重新部署。

此前的服务端测试和 Forge 主菜单启动验证仍属实，但没有覆盖该客户端实际施法表现。下一步若继续，应先复现具体编排并确认“变大”的对象，再决定修复范围，不以现有服务端通过记录替代视觉验收。部署回退记录在 `MinecraftDev/releases/trajectory-contacts-20261030/wanfaguiyi-rollback.json`。

## 2026-10-30 模型变大原因核查

用户进一步确认是球体模型本身变大（例如“环绕→闪电球”），不是环绕半径。已核对实际 Iron 1.21.1-3.16.3 发布 JAR，其 SHA-256 与万法归一安装文件一致；Forge 1.20.1-3.16.3 也有相同渲染逻辑。

明确的代码缺陷在 Iron 联动的寿命扩展与原生渲染之间：`BallLightning.tick()` 原本在超过 100 tick 时删除实体；`BallLightningRenderer.render()` 在超过 90 tick 时启用消失动画，外层缩放为 `2 + (tickCount + partialTicks - 95) * 0.4`，内层则减去该项。0.17.30 的 `BallLightningImpactMixin` 仅让 Ars 托管弹体跳过 100 tick 删除，未关闭原生结束动画，实际实体年龄仍持续增长，故渲染比例无界增长，内层还会变成负缩放。以 partialTicks=0 计算，120 tick 外层缩放为 12，200 tick 为 44，而稳定外层缩放为 2。

修复边界：仅对 `NativeCastCarrier.ironsNouveau$arsDriven()` 的闪电球跳过原生固定 5 秒结束动画，保留真实年龄、纹理帧切换和旋转。不要把渲染方法内所有 tickCount 一律封顶或重置，也不需要改 Conflux 的环绕半径。若以后要按 Ars 形态剩余寿命做结束动画，应单独同步寿命进度。

本次是源码/发布 JAR 核查，未启动新的客户端或重新部署；尚未实施该修复。整套弹体存续推广仍搁置，万法归一维持回退版本。已识别的渲染缺陷不等于此前提及的所有其他异常都已解释。

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

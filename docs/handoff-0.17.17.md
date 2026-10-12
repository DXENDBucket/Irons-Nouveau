# Iron0.17.17 执行锚点交付

2026-10-13。工作树 `parallel-workspaces/iron-siphon-render-fix/Irons-Nouveau`，分支 `fix/forge-siphon-render`。固定Core代码 `c4ed6f50f853b20da06246ed32e66cdb2880bb57`（0.1.20），源码副本干净。两平台最低Core要求均改为0.1.20。

吐息和血吸光束共用Core实体／位置锚点、友军授权和实际actor共享引导声明。原等级、费用、冷却、持续时长、玩家移动限制与原生视觉入口保留；远端血吸和客户端端点改按真正射线圈选目标，与冰霜射线共享几何。差异详情见execution-anchor-migration.md。

- Neo `build-anchor-migration-final.log`：13项required全部通过；包含实际NEG转换、原生Ars延迟、远端命中及授权／引导。Forge `forge/build-anchor-migration.log`：9项通过，最终兼容基线下 `forge/build-anchor-release-final.log` 又验证4项锚点／授权。
- 最终c4ed6f5源代码固定后，两平台 `build-anchor-package-current.log` 及 `forge/build-anchor-package-current.log` 包装成功。新增中立授权重载没有修改本附属所用的便利入口行为。
- 两平台原生血吸渲染三个注入点继续匹配实际JAR；最终Forge发行包refmap检查通过。75份共享Java、103份配方生成一致。
- 灾变独立可选共存用例实际加载本包并通过，未增加Iron到灾变的硬依赖。没有启动图形客户端，实际光束绘图需人工确认。
- Neo二进制SHA256 `16a9bb96c561b297f91df430673e8437ef96c6765593547e2f8ab5ae7bf841a3`；源码 `4a7df6d7b1c7d7f7d89403f0675faa763815156231c6dfc2d0b80daa4cec5c07`。
- Forge二进制SHA256 `82bbb8ef85e575a0027ccc5a9ab4fd0a56d026464120ba283c11ec3899331e80`；源码 `085fda74b5de54aa1a510a69221176653e7c472e9bdc38c52477ba4c42393ed6`。
- 产物在 `build/libs` 和 `forge/build/libs`，均为 `irons-nouveau-0.17.17-*`；Forge部署完整重混淆jarJar包。

与Core一起串行部署两个既有测试实例，旧包保存至 `mod-backups/2026-10-13-execution-anchors`；测试进程已结束，未推送或发布。

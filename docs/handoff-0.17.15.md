# irons-nouveau 0.17.15：Forge 内嵌库范围修复

外部报告：<https://paste.kostromdan.dev/mclogsarray/h1DbRK6a47Tl0E0eMn4y5pBc>，崩溃文本 `/mclogs/y8VvLkA`。原报告使用 Conflux 0.1.11、Iron & Nouveau 0.17.14；其 MixinExtras `[0.4.1,0.5)` 与其他模组要求 `[0.5.5,)` 无交集，导致 Forge JarInJarDependencyLocator 拒绝启动。随后日志报多项前置缺失；核对 `modlist.txt` 发现 Kotlin for Forge 4.12.0、Flywheel、Ponder、L2Harvester、ZeroModsCore、XaeroLib、Expandability 实际均已包含，多数作为内嵌库。按扫描先失败的时序判断，这些是 Jar-in-Jar 整体拒绝加载后的连锁报错，不应直接让报告者重复安装。

仅将 Forge `jarJar.ranged` 改为 `[0.4.1,)`，继续编译／自带 0.4.1 回退库，但允许整合包选择新版。遵循上游无次版本上限形式：<https://github.com/LlamaLad7/MixinExtras#setup>。Java 行为不变，两平台版本号同步。

## 验证

- Neo 和 Forge 发行／源码包构建通过；Forge 执行 `jarJar reobfJarJar sourcesJar`，验证最终重混淆包的版本、MIT 和内嵌库元数据。
- 工具 `Ars-Conflux/tools/check-forge-jarjar-ranges.py` 和 `ForgeJarJarRangeCheck.java` 调用实际 Forge JarJarSelector 0.3.19，读取两个最终包：无其他要求时选 0.4.1；加入要求 0.5.5 的模拟提供者后选 0.5.5；恢复旧上限按预期失败。负例日志的 ERROR 是预期。
- 日志 `Ars-Conflux/build/forge-jarjar-range-check.log`；使用本地缓存，不启动服务器。模拟提供者只验证依赖选择，不能称为已在游戏中运行 0.5.5。
- 不重复跑战斗矩阵：Core 0.1.18 的双平台 API 及灾变/BOMD 回归仍适用，此修复不改 Java 行为。Iron Neo 构建另跑既有 JUnit。
- 未在报告者完整整合包人工启动。应先更新两个修复包后重新启动，再看是否还有真实的前置／兼容问题；不要把本次缺失列表当成独立下载清单。

固定 Core 构建副本：`dab8b9f6f1f92cfa0a78d56768f6a01a57901714`（0.1.18），受版本控制文件未改。工作树 `parallel-workspaces/iron-mixinextras-range/Irons-Nouveau`，分支 `fix/forge-mixinextras-range`。

## 产物 SHA-256

- `build/libs/irons-nouveau-0.17.15-mc1.21.1-neoforge.jar`：`987f8027f0d3452ac738cd9a3760c00646db8096ca33c871f5254b73c109090a`
- `build/libs/irons-nouveau-0.17.15-sources.jar`：`a6a698d5c50c6497227f47487fbcc65ec2cd01a3348199c33d7a33dae0c13e47`
- `forge/build/libs/irons-nouveau-0.17.15-mc1.20.1-forge.jar`：`69ea1eb8280dc7703a3146018316edf351a266d225e98bd3d00667dae81d055d`
- `forge/build/libs/irons-nouveau-0.17.15-sources.jar`：`332631860270844f4ddc6a19a0efe563e0da3d8a850353a1949887f4d1f24137`

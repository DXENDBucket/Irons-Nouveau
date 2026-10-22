# Iron 0.17.23：重复命中续接

2026-10-23，独立工作树 `parallel-workspaces/iron-repeat-continuation-01723-20261023/Irons-Nouveau`，分支 `fix/repeated-projectile-continuation`，起点 `be8c82ac262f644b33cd16ffe1585b57e3298941`。固定 Conflux `ed44ebb86bd087f3546a1bc6a315d2692f70262c`（0.1.28）；其受版本控制文件无改动。双平台共用 VERSION 0.17.23。

## 最终行为

删除 NativeCasting 对非 Ars 驱动弹体额外套用的 OncePerTargetContinuation，以及已无用途的包装类。其终生目标集合、128 目标上限和终生一次方块命中限制均不再存在。存活的同一弹体以后再次成功伤害同一目标，可以再次续接 Ars 编排；原生区域或状态成功建立仍走原有通知。

没有改伤害成功判定、原生受击冷却、原生自身目标集合、贯穿次数、费用或寿命。无敌帧／拒绝伤害仍不会续接。ComplexImpacts 仍只在同一次复杂原生回调内合并同目标的多次伤害／状态通知，后一次回调重新计算。未改 Core、Mixin 接线或原生实现。

## 验证与限制

- NeoForge 1.21.1 / 21.1.250 / Java 21 / Iron 1.21.1-3.16.3：一次运行 `:runGameTestServer :jar :sourcesJar -PgameTestNamespaces=irons_nouveau_repeat_continuation --offline --console=plain`，2/2 required 首轮通过，Gradle BUILD SUCCESSFUL。
- 一项复用已有球形闪电集成夹具：真实 Ars 编排生成原生球，第一次成功伤害续接照明；立刻再次命中的原生无敌帧失败不续接；后续空蓝失败不续接；付费后同弹体同目标再次伤害且续接。原有包含锤子方块权限的广域用例也复用此段，但本轮未重跑其无关部分。
- 一项受控原生复杂伤害回调：真实 DamageSources.applyDamage 两次成功加一次状态通知只续接一次；下一次成功伤害回调可再续接；无敌帧失败不续接。这验证原生伤害钩子和回调作用域，不冒充完整火球爆炸的端到端实测。
- Forge 1.20.1 / 47.4.0 / Java 17 / Iron 1.20.1-3.16.3：`:jarJar :reobfJarJar :sourcesJar --offline --console=plain` 编译、重混淆和发行打包通过。本轮未运行 Forge GameTest 或图形客户端，未跑全法术矩阵。
- 两端发行及源码 ZIP 检查通过：版本和 LICENSE 存在；二进制含 THIRD_PARTY_NOTICES；无测试类、已删除包装类、原生 Iron/Ars 类或素材、开发依赖。Forge 最终 Jar-in-Jar 的 MixinExtras 范围仍为 `[0.4.1,)`，嵌入版本 0.4.1；本次未改范围。
- 外部日志：工作树上一级 `iron-01723-neoforge.log`、`iron-01723-forge.log`；同处 `iron-01723-artifact-check.json` 保存 ZIP 检查和完整哈希。
- 没有合并、部署、推送或上传 CurseForge；由协调者串行集成。

## 产物 SHA256

- `build/libs/irons-nouveau-0.17.23-mc1.21.1-neoforge.jar`：`a666d01d019ae4e4745c42b6b6d40cc671e39e2d78c1d8e2656979e09c144b79`。
- `build/libs/irons-nouveau-0.17.23-sources.jar`：`2ecdb48fe13089ecb71bbb4796e153c00c7f32f1e40906401914351e04f253ec`。
- `forge/build/libs/irons-nouveau-0.17.23-mc1.20.1-forge.jar`：`82639ad8b372e5076303ebe0618259a83ed2696802cafb9c4e3d6649f1d9bc19`。
- `forge/build/libs/irons-nouveau-0.17.23-sources.jar`：`bc9f76fb5450c58415048ec2029139182c46b1e788e15f5557063bd4faa76eac`。

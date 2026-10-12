# Iron 与新生魔艺 0.17.16 渲染修复

2026-10-13 修复 Forge 1.20.1 下血吸光束渲染的必需注入点错误，报告：https://paste.kostromdan.dev/mclogsarray/sIQ6Z3fUO7FBeA0gISoW3pWK 。原报告为 Iron 3.16.3、Iron & Nouveau 0.17.15、Conflux 0.1.19，在渲染亡灵术士时出现 `NativeSiphonRenderingMixin` endpoint 注入0次。

实际 Forge JAR 使用静态 `Utils.raycastForEntity(Level, Entity, float, boolean)`；NeoForge JAR 使用 `RaycastBuilder.build`。只修改Forge endpoint包装器及其参数，普通渲染完整调用原方法，联动作用域取已提供的几何。保留所有默认必需注入和两项眼部位置包装。NeoForge只同步版本，没有渲染行为改动；未修改伤害、资源、冷却、Core或锚点机制。

工作树 `parallel-workspaces/iron-siphon-render-fix/Irons-Nouveau`，分支 `fix/forge-siphon-render`。Core固定 `9611f5087e3dd5adad6cd21517bb9563dd9a83dd`（0.1.19），受版本控制文件未改变。

## 验证

- Forge `forge/build-siphon-render-fix.log`：Java17，`jarJar reobfJarJar sourcesJar`，BUILD SUCCESSFUL（43s）。发行包是最终重混淆完整包。
- NeoForge `build-siphon-render-fix.log`：Java21，`:jar :sourcesJar`，BUILD SUCCESSFUL（16s）。两个构建均使用离线缓存、单worker和768MB Gradle堆。
- `tools/check_siphon_render_hooks.py`：两平台三个必需目标均存在于各自实际Iron JAR。Forge从最终发行包读取refmap，确认两个Minecraft眼部调用的SRG映射和新的原生endpoint。用0.17.15的Forge Mixin源码作负例，明确只拒绝原错误的RaycastBuilder入口。
- 构建后检查MIT、版本元数据、最终Forge类中新的调用目标、无GameTest类混入，以及仍保留MixinExtras `[0.4.1,)`；74份共享Java和103份配方的双平台生成结果一致。
- 不重复跑服务端战斗测试，它们不加载此客户端Mixin。没有启动完整客户端：本机当时可用内存不足300MB。上述字节码检查不能等同于实际Mixin转换、绘图或报告者完整整合包共存验证。需报告者更换0.17.16后确认亡灵术士渲染与联动光束。
- 两个构建会话已结束，未停止用户游戏或其他Java。只串行部署既有两个测试实例，未替换Core、未推送／发布。

## 产物

- NeoForge：`build/libs/irons-nouveau-0.17.16-mc1.21.1-neoforge.jar`，SHA-256 `7ca69d17f8471e2d160201e3f84712eab28a78fcdc2be3c6b657fc47ce4258a0`。
- NeoForge源码：`build/libs/irons-nouveau-0.17.16-sources.jar`，SHA-256 `a6a698d5c50c6497227f47487fbcc65ec2cd01a3348199c33d7a33dae0c13e47`；该平台源码与0.17.15一致，文件名版本同步。
- Forge：`forge/build/libs/irons-nouveau-0.17.16-mc1.20.1-forge.jar`，SHA-256 `6b076b627ae8c8e340e79527f20fc4aecaa100482b7318db8e981aecf68da98a`。
- Forge源码：`forge/build/libs/irons-nouveau-0.17.16-sources.jar`，SHA-256 `954feb2e1d40e9123d0e30dbbebdbb7b2357d3dca4bf147638a8836b623319c1`。
- 旧0.17.15测试包保存至各实例 `mod-backups/2026-10-13-iron-siphon-render-01716`。

专项审计中的远程射线范围、执行占用和友军授权仍待后续处理。本次仅修直接崩溃。

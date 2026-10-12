# Forge 血吸光束渲染注入修复

0.17.16 修复 MC 1.20.1 Forge／Iron 3.16.3 下的 `NativeSiphonRenderingMixin` 致命注入错误。用户报告使用 Iron & Nouveau 0.17.15 与 Conflux 0.1.19，在渲染亡灵术士时首次加载原生 `SpellRenderingHelper`；因此不需要主动释放联动光束也可能触发。

实际 Forge Iron 的 `renderRayOfSiphoning` 调用静态 `Utils.raycastForEntity(Level, Entity, float, boolean)`。此前错误地使用 NeoForge 的 `RaycastBuilder.build` 注入点。Forge 适配现在包装真实静态入口：仅显式联动渲染作用域内替换命中几何，否则以完整原始参数执行原版调用。眼部位置的两个既有注入保留，严格的默认注入要求保留。

NeoForge 渲染实现没有改动；它的原生调用确实是 `RaycastBuilder.build`。双方版本统一更新至0.17.16，不修改Core，不改变伤害、等级、Mana、冷却或现有锚点规则。专项审计中的远程射线候选范围和执行占用问题不在此修复范围内。

## 轻量回归检查

`tools/check_siphon_render_hooks.py` 用 javap 检查实际原生 JAR 中的渲染方法调用，Forge 使用最终重混淆发行包里的refmap解析Minecraft方法名称。所有三个必需注入点都必须存在，不能只检查第一个入口。

```powershell
python tools/check_siphon_render_hooks.py --platform neoforge
python tools/check_siphon_render_hooks.py --platform forge --artifact forge/build/libs/irons-nouveau-0.17.16-mc1.20.1-forge.jar
```

可通过 `--mixin-source` 指定旧版本源码验证此检查能发现原报告的缺陷。此工具不启动Minecraft，不证明完整Mixin转换或实际渲染效果；客户端画面仍需人工验证。服务端GameTest不会加载这个客户端Mixin，重复整个服务端矩阵不能替代该检查。

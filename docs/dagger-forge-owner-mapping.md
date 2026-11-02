# Forge 焰刃领域归属注入修复：0.17.44

用户反馈 `AbilityDaggerMixin` 必需注入 0/1 命中。检查实际 Forge Iron 3.16.3 发布 JAR 后确认：`FieryDaggerEntity.createFireField()` 中的调用是 `FireField.m_5602_(Entity)`，而 0.17.43 的 refmap 未包含该 `setOwner` 调用的映射。

`createFireField` 是 Iron 自定义方法，应保留原名称；`FireField.setOwner` 则继承 Minecraft 的 `Projectile.setOwner`，必须单独映射。Mixin 类级 `remap=false` 不能直接覆盖所有内部调用。本次只给该 `@At(INVOKE)` 增加 `remap=true`，不改方法选择器、不写死 SRG 名称，也不降低必需命中数。

## 验证

- 两平台 0.17.44 二进制和源码包离线构建通过；Forge 完成 `reobfJarJar`。
- 最终 Forge refmap 明确包含 `FireField.setOwner(Entity) → FireField.m_5602_(Entity)`。
- 新增 `tools/check_ability_mixin_hooks.py`，覆盖本批五个 `Ability*` Mixin 的方法选择器、调用描述符与调用数量、墙体 lifetime 字段。每平台共 20 项，均匹配实际原生 Iron JAR。
- 同一检查器对旧 0.17.43 Forge 包恰好在本次报告的 `FireField.setOwner` 处失败，对 0.17.44 两平台均通过。
- 将检查接入 `tools/build_all.ps1` 的双平台构建末尾，保留独立轻量运行入口。它不启动游戏，不能替代实际 Mixin 转换或客户端外观验证。

```powershell
python tools/check_ability_mixin_hooks.py --platform forge --artifact forge/build/libs/irons-nouveau-0.17.44-mc1.20.1-forge.jar --javap "你的JDK/bin/javap.exe"
python tools/check_ability_mixin_hooks.py --platform neoforge --artifact build/libs/irons-nouveau-0.17.44-mc1.21.1-neoforge.jar --javap "你的JDK/bin/javap.exe"
```

本次未运行游戏、GameTest，未部署实例或上传 CF。构建回执见 `dagger-forge-owner-0.17.44-build.json`。Conflux 没有源码改动。

# Hex-Ars Linker 可选联动

从 `0.15.4-forge1201-alpha.2` 起，安装 Hex-Ars Linker（`hex_ars_link`）时自动启用弹射物适配。Hex Casting、Linker 及其依赖仍需自行安装；未安装时照常工作，不加载任何 Hex 类，也不打包第三方模组。

Linker 常规施法原本就经过 Ars 发射逻辑；指定位置发射会直接创建普通 Ars 弹射物。现在本模组在实体加入世界时识别 Linker 的 `PatternResolver`，复用 `NativeCasting.convert` 转换。没有 Iron 形态魔符时保持原状，不接管其他解析器。

保留位置、方向、速度、施法者、等级、后续效果及原有解锁和耗蓝检查。正常 Ars 发射流程已经转换的弹射物不会重复转换。

此次只补齐弹射物转换：Linker 指定实体／位置入口仍不经过主动吟唱和冷却；常规施法保留现有规则，吟唱不会暂停整个 Hex 程序。

## 集成验证

锁定 Hex Casting `0.11.4`、Linker `1.20.1-0.9.5.1` 及依赖，见 `hex-forge-dependencies.json`。测试用依赖不参与默认构建。

```text
python tools/fetch_hex_test_dependencies.py
gradlew.bat runGameTestServer -PwithHexLinker=true -x downloadAssets
```

Forge 测试通过反射调用真实 Linker 的指定位置发射动作，检查解锁限制、单次转换、原 Ars 弹射物取消和实际耗蓝。默认不带 Hex 的测试运行只检查可选适配能加载且不误识别普通解析器，其余基础施法测试正常执行。

2026-10-06：新版不装 Hex 的四项基础 GameTest 通过（`build/hex-compat-no-linker.log`）；安装上述 Hex／Linker 后，包含指定位置发射集成测试的五项 GameTest 通过（`build/hex-compat-linker.log`）。未逐个实测所有法术及多人组合。

上游实现：[OpShootCast](https://github.com/YukkuriC/HexArsLinker/blob/main/src/main/java/io/yukkuric/hex_ars_link/action/OpShootCast.kt)。

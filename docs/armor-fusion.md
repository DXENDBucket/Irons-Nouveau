# 0.17.32 起迁移至 Ars Fusion

创建新融合与按 Ars 等级增加韧性已迁往独立的 Ars Fusion。旧 `armor_fusion` 配置仅保留兼容，不再启用新功能。

旧装备在未安装 Ars Fusion 时仍保留原融合属性与名称。共装 Ars Fusion 0.1.0 后，其统一接管旧装备，Iron & Nouveau 不再重复添加；新模块同时支持读取历史组件/NBT。

以下为 0.17.25–0.17.31 的历史实现记录：

# 可选护甲融合

从 0.17.25 起，NeoForge 1.21.1 和 Forge 1.20.1 共用这项功能。默认不改变生存玩法。

在世界的 `serverconfig/irons_nouveau-server.toml` 中配置；整合包可在 `defaultconfigs` 提供新世界默认值：

```toml
[armor_fusion]
enabled = false
ars_toughness_per_tier = 0.0
```

启用 `enabled` 后，在普通铁砧左侧放 Ars 护甲，右侧放相同部位的 Iron 护甲，消耗双方各一件、1 级经验。每件 Ars 护甲只能融合一次；不能换供体，也不提供逆向拆分。输出名称为“Ars 护甲名—Iron 护甲名融合护甲”，手工重命名仍可覆盖显示名称。

输出仍是原来的 Ars 物品，保留其图标、穿戴模型、染色、耐久、附魔、丝线及升级数据；原生 Ars 护甲升级继续可用。加入 Iron 供体的属性修饰，唯独排除护甲值和护甲韧性；保留各加成的原生数值、运算方式和穿戴部位，使用独立修饰 ID 避免覆盖 Ars 属性。不会合并供体的附魔或耐久。

如果供体原生支持法术注入，输出获得同样的 Iron 法术容器，并保留其中已有法术；空容器可继续在 Iron 奥术铁砧注入。原生 Iron 的法术轮盘、装备槽识别和注入提示仍按容器工作。没有注入能力的供体不会凭空增加注入槽。此功能不会把单独写在 Iron 物品类中的其他特殊被动行为转移过来。

关闭 `enabled` 仅停止新融合，不删除已融合装备的属性和法术。融合数据随物品保存、同步；不创建替代物品注册 ID，也不拦截其他正常铁砧操作。

`ars_toughness_per_tier` 是独立可选规则，适用于采用 Ars 护甲丝线数据的护甲，包含未融合装备。默认 `0.0` 保留原值；设为 `1.0` 时，每件显示为 1／2／3 级的 Ars 护甲分别额外获得 1／2／3 韧性。它不转移 Iron 供体韧性。升级后按新等级计算；运行中改值后应重新穿戴。整合包可以只用此规则而不启用融合。

实现不替换任何客户端渲染器；服务端原生菜单验证与客户端人工观察分别记录。


## 0.17.25 验证与交付记录（2026-10-26）

- 工作树：`C:/Users/57991/Documents/MinecraftDev/parallel-workspaces/iron-armor-fusion-01725-20261026/Irons-Nouveau`，分支 `feature/armor-fusion`；起点 `3831cc176c8e3f737e33c276d84dcfe9fd78d282`。
- 固定 Core：同工作区相邻 `Ars-Conflux`，提交 `199c1f7253650507ddefcc3f9fa9abc4c7877ce6`（0.1.29）；受版本控制文件无改动，不要求新 Core API。
- 核对的实际发布 JAR：NeoForge Ars 5.13.1、Iron 1.21.1-3.16.3；Forge Ars 4.12.7、Iron 1.20.1-3.16.3。JDK 分别为 21、17。
- 共用逻辑：`ArmorFusion`；平台适配：两端 `platform/FusionPlatform`；名称：`FusedArmorNameMixin`。NeoForge 使用原生不可变 `ItemContainerContents` 保存供体，不能直接把可变 `ItemStack` 当作组件值；Forge 使用独立 NBT。
- 原生入口：两端 `AnvilUpdateEvent`、属性查询事件、Iron `IPresetSpellContainer` / `ISpellContainer` / `Utils.canImbue` / `ArcaneAnvilMenu`、Ars `ArmorUpgradeRecipe`。不复制升级或注入流程，不替换渲染器。

两端只运行新增 namespace `irons_nouveau_armor`，各 **3/3 通过**：实际普通铁砧禁用、部位与一次融合限制、消耗双方与一级经验；四部位属性转移及排除护甲／韧性；原生奥术铁砧注入、原生 Ars 升级、存储及网络往返。Neo 初次发现可变组件被加载器拒绝，改为不可变快照后通过；没有重复执行既有全部测试矩阵。

Neo 命令（根目录）：

```powershell
./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain --no-daemon '-Dorg.gradle.jvmargs=-Xmx768m' -PgameTestNamespaces=irons_nouveau_armor
```

Forge 命令（`forge/`）：

```powershell
./gradlew.bat :runGameTestServer :jarJar :reobfJarJar :sourcesJar --offline --console=plain --no-daemon '-Dorg.gradle.jvmargs=-Xmx768m' -PgameTestNamespaces=irons_nouveau_armor
```

日志：工作树 `build/armor-neo.log`、`build/armor-forge.log`；两端均正常停止测试服务器。发行包包含 MIT 许可，无 Ars/Iron 本体类文件，名称键和 Mixin 均已检查。客户端没有人工启动，尚未观察实际穿戴画面、法术轮盘及混合整合包环境。

| 产物（工作树内） | SHA-256 |
| --- | --- |
| `build/libs/irons-nouveau-0.17.25-mc1.21.1-neoforge.jar` | `35271935f03f660d40243a542f3eedd0f333ffe87e6f2a809948dafb7f5f533b` |
| `build/libs/irons-nouveau-0.17.25-sources.jar` | `95030474fd72fed3ec97da3b850f682cbeb22c6c08e5baead67047654b6ae501` |
| `forge/build/libs/irons-nouveau-0.17.25-mc1.20.1-forge.jar` | `4b87eb39cb7ebd04eb6d44ae126d272130d5e75fd20a1e4795654b79d167d89e` |
| `forge/build/libs/irons-nouveau-0.17.25-sources.jar` | `30528db797a5df84c319101cd4a79ab316cf6b0270d1609657dcddf10b89f1a4` |

Neo 已部署到 `C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/mods`；Core 保持原有 0.1.29，其他联动 JAR 未改。该实例 `config`、`defaultconfigs` 的 Iron 配置追加 `enabled = true`、`ars_toughness_per_tier = 1.0`；现有存档没有此文件，未额外创建覆盖。备份：`C:/Users/57991/Desktop/.minecraft/versions/1.21.1-万法归一/codex-backups/iron-armor-fusion-20261026-051406`。未改“万途归零”，未部署 Forge 人工测试实例，未推送或上传 CurseForge。


## 2026-10-26 CurseForge 上传

用户授权后复用上述二进制发行包上传，项目 `1726248`：NeoForge 文件 ID `9327416`，Forge 文件 ID `9327417`。台账均为 `uploaded`，进程退出码 0；release 类型，审核通过后自动公开。API 接收成功不等于已经审核。

计划：`C:/Users/57991/Documents/MinecraftDev/Ars-Conflux/build/curseforge/20261026-060747-efa90a59/plan.json`；公开发行说明：相邻 Core 的 `release-notes/iron/0.17.25.md`。没有重新构建、重跑测试或改动发行包哈希，没有推送 Git。

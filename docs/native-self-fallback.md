# Iron 附属法术的通用自身施法

0.17.26，NeoForge 1.21.1 与 Forge 1.20.1。Conflux 固定基线 `b46be94b17799573b825a8e4a11870fa3dc857d6`（0.1.29）；本次不修改 Core。

## 使用

标准登记的外部 Iron 法术自动获得效果魔符，ID 为 `irons_nouveau:glyph_native/<原命名空间>/<原法术路径>`。不同附属同名法术互不覆盖。名称和图标引用原法术；物品提示和法术书说明注明“仅支持玩家自身施法”。

- 编排 `自身 → 对应魔符`。原法术从玩家自身位置与朝向执行，由它自己选择目标；攻击法术仍可攻击瞄准的敌人。
- 不接受友军、敌人或方块作为执行锚点，也不会把这些命中改为自身。无效触发及原生前置条件失败不付触发费用；Ars 已发生的形态费用仍遵循 Ars。
- 只支持增幅／减弱，各改变一级原生等级。不开放范围、时长、弹速修饰。后续 Ars 效果仍按当前自身命中继续，原生弹体不会携带后续编排。
- Ars 学习模式下，任意来源、等级的对应 Iron 卷轴＋魔源宝石，在抄写台制作魔符（27 经验，与现有配方相同）。配方真实进入 RecipeManager 和 JEI；数据包可用相同 ID 覆盖。个人撰写解锁模式与基础等级配置也沿用现有规则，固定等级覆盖现在接受外部命名空间。
- 瞬发与已经完成 Conflux 吟唱的长吟唱法术执行完整原生开始、施放、客户端回调及完成；不会重复吟唱。持续法术保留原生施法状态、客户端动画、移动限制、脉冲间隔及结束行为，同时只能有一个原生持续施法。已适配效果仍使用原来的独立会话。
- 原生费用事件完成后，从原 Conflux 账户支付实际脉冲；保留 `mana.use_iron_mana` 与外部账户选择，避免原生再扣一份。空蓝停止持续施法并执行原生取消清理。原生预付的重施次数不重复支付；原生冷却共享并遵循本模组冷却开关。

总开关在 `irons_nouveau-server.toml`：

```toml
[native_self]
enabled = true
```

自动生成的每个魔符也拥有 Ars 通常的独立配置，可以单独禁用。

## 接入边界

通过加载器的 DeferredRegister 收集法术 ID，不执行、复制或提前调用附属的工厂。物品注册阶段创建 Ars 魔符及物品；原生法术加载完成后才读取其配置、图标和学派。没有解冻注册表或改写本体 JAR。

只处理外部命名空间；Iron 本体已有／尚未适配的法术不变。排除 Ars ’n Spells 的代理法术命名空间，避免把 Ars 编排代理再次导入 Ars。名称、图标和配方均在运行时引用；发行包不含本体或附属素材。

这不是对任意 Iron 附属的完整认证：在物品注册之后才直接登记、要求特定原生工具来源、重写基础 castSpell/施法生命周期、依赖特殊交互 UI 的法术可能仍需适配。首版针对真实玩家，不由假玩家代施，也未开放生物自身或友军代施。

内部用原生 `CastSource.NONE` 防止本体重复扣款；通过受控会话接回原生费用事件、冷却与 Conflux 账户。特殊附属若仅接受 SPELLBOOK/SWORD 来源，应单独适配。原生持续施法自己的移动控制继续由 Iron 管理。

Ars ’n Spells 3.4.0 会把豁免费用的原生调用的费用事件归零。可选 Mixin 仅在本模块会话、同一玩家和同一法术的调用内保留事件原始费用，交给 Conflux 账户支付；不改变普通 Iron 施法的计费。该联动没有新增硬依赖。

## 验证

测试夹具按外部模组的标准方式登记瞬发、长吟唱、持续三种法术，调用实际 Iron 火焰箭实现；不在发行包登记测试法术。

定向范围：物品及真实抄写配方生成、原生图标模型引用、编排验证、保持原有 103 个魔符、前置失败、非自身拒绝、账户不重复扣费、长吟唱不重复、持续脉冲与空蓝取消、原生 Iron 未接管的施法仍正常扣费和冷却。

客户端图标、动画和实际第三方附属尚未人工逐项验证；服务端通过不代表这些画面已经验证。

### 2026-10-26 交付记录

- NeoForge：Ars 5.13.3、Iron 3.16.3、Iron's Lib 2.2.0、Ars ’n Spells 3.4.0、NEG 4.6.1 共存，`iss_primary` 下 **5/5**；采用“万法归一”相同的 `ars_primary` 和关闭 ANS 自带导入魔符的配置，**5/5**。日志 `build/native-self-coexist.log`、`build/native-self-ars-primary.log`。
- Forge：Ars 4.12.7、Iron 3.16.3，不安装 ANS，**5/5**。日志 `forge/build/native-self-forge.log`；最终产物已经重混淆。
- 没有重跑全部旧测试矩阵。测试夹具补充在线 UUID 和 Ars 基础魔力初始化，避免把未登录模拟玩家／超过上限的魔力当作共装计费问题。旧 Forge 的参数注入换用 WrapOperation，避免加载器生成 Args 类失败。
- 固定 Core 副本受版本控制文件保持干净；发行包含 MIT 与既有第三方说明，检查无测试夹具、原模组类或原模组资源目录，Forge MixinExtras 范围仍为 `[0.4.1,)`。

命令（在任务工作树根目录／forge 目录，使用对应 JDK 21／17）：

```powershell
./gradlew.bat :runGameTestServer :jar :sourcesJar --offline --console=plain --no-daemon '-Dorg.gradle.jvmargs=-Xmx512m' -PgameTestNamespaces=irons_nouveau_native_self -PnativeSelfTests=true -PgameTestHeap=1100m
# Forge
./gradlew.bat :runGameTestServer :jarJar :reobfJarJar :sourcesJar --offline --console=plain --no-daemon '-Dorg.gradle.jvmargs=-Xmx640m' -PgameTestNamespaces=irons_nouveau_native_self -PnativeSelfTests=true
```

| 产物 | SHA-256 |
| --- | --- |
| NeoForge 0.17.26 | `a97485198dcd4e4a286939cd96aa777607ceb84deb828b8901d36c1e24da79e3` |
| NeoForge sources | `2b86e97c1528d358078040592a8cca3a4346dddbc750a34193479431970f7191` |
| Forge 0.17.26 | `5a07da370d9d2ab384b4b8e68c00f943f66209c619f48a98c4a6816af36a9cab` |
| Forge sources | `5f695d2d4153c92904ddc6014969b476f700a1a80b96d0e2357b7dd790b08f72` |

任务工作树：`parallel-workspaces/iron-self-native-01726-20261026/Irons-Nouveau`，分支 `feature/native-self-fallback`。已串行更新“万法归一”和“1.20.1-模组测试”的 Iron 联动 JAR；各实例 `codex-backups/iron-self-native-01726-20261026-134240` 保存旧版。没有修改其他模组或配置，没有推送或发布 CurseForge。

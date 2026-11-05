# Wind's Spellbooks 手动适配

Iron & Nouveau 0.17.45；NeoForge 1.21.1 对照 Wind 1.0.5，Forge 1.20.1 对照 Wind 1.0.3。使用 Conflux 0.1.51，固定源码基线 `816c8ac4d55a84a0b0c93510567761a231bad665`，本批没有修改 Core。

## 七种法术

| 原生法术 | Ars 入口 | 保留的原生行为 | 增强 |
| --- | --- | --- | --- |
| Wind Blade | 弹射物替换增强 | 原生刃体、增长、贯穿、粒子与伤害成功后的编排续接 | 等级；弹速与散射由 Ars／Conflux 处理 |
| Tornado | 效果：命中实体或位置 | 原生龙卷风、牵引、抛升、火焰转化、声音与模型 | 等级、范围、持续时间 |
| Almighty Push | 效果：命中实体或位置 | 原生多部件风障、排斥与原生 Airborne 效果 | 等级、范围、持续时间 |
| Tailwind | 效果：自身或友军 | 原生顺风效果、属性与客户端行为 | 等级、附近友军、持续时间 |
| Wind Jump | 效果：自身或友军 | 原生跃升、阵风、羽毛粒子、缓降、玩家客户端冲量回调 | 等级、附近友军、缓降持续时间 |
| Iron Slash | 效果：自身或友军 | 原生冲刺、停点实体、延迟斩击、轨迹渲染与冰霜联动 | 等级、附近友军 |
| Aeropic | 效果：自身或友军 | 原生空中突进、回旋状态、命中回弹、羽毛与阵风粒子 | 等级、附近友军 |

没有重绘原生外观或自行替代其服务端实体。原模组自身的效果差异（如风障排斥对象、风刃每个目标的原生命中记录）仍由原生实现决定。Aeropic 每次 Ars 触发执行一次原生突进；原生法术书的命中确认／重施交互没有变成额外的 Ars 交互阶段，不能据此承诺完全相同的连续操作。

三种需要实际身体移动的法术不能由空气落点执行，也不会把敌对目标悄悄改为施法者。敌对受术者、非生物受术者、未加载落点等无效触发不扣实际触发费用。范围增强选择附近友军；实际执行者使用自己的头部视角，伤害、武器附加伤害与法强仍来自原施法者。没有攻击属性的非玩家施法者按零武器附加伤害处理。

龙卷风与风障支持独立落点；移动类与顺风支持触摸、射线或弹射物选中友军，不限于自身形态。风刃复用 Conflux 的载体表与 ArsTrajectory，环绕轨迹由 Ars 驱动，不叠加原生移动和碰撞，也不会被原生 50 tick 寿命提前删除。NEG／Elemental 仍然是可选联动，不为 Wind 单独写一套附属兼容。

## 获取、兼容与配置

- 沿用已存在的 `irons_nouveau:glyph_native/wind_spellbooks/<法术>` ID，替换对应的通用自身施法条目，不额外注册重复魔符。风刃从效果类变为弹射物替换增强，旧的“自身→风刃”编排需要改写。
- 真实抄写台配方：任意等级、来源的对应卷轴＋魔源宝石，27 经验；JEI 继续使用现有卷轴材料展示。沿用当前学习、基础等级、吟唱、冷却与资源池配置。
- 增幅／减弱仍修改原生等级，不新增原生最高等级封顶；高阶增强通过已有 Conflux 等价接口处理。魔符本身零 Mana，成功触发时支付原生费用，持续原生区域一次支付创建费用。
- 手动条目不再受 `native_self.enabled` 控制，也不带“仅玩家自身”提示。尚无专门适配的其他附属法术继续沿用通用兜底。
- 名称引用原法术翻译，图标引用其原生法术图标；本附属提供中英文的 Ars 编排说明和七份 Conflux／Instinct 能力提示。没有 Instinct 硬依赖。
- Wind 未安装时不注册这些条目。可选 Mixin 使用 `@Pseudo`；Forge 继承原版的覆写方法同时列出已核对的开发名与 SRG 名，避免可选类没有生成 refmap 时在发行环境找不到目标。

## 实现边界

身份选择位于 `WindManualCatalog`，原生效果位于 `WindAdapters`／`WindNativeAction`，弹射物位于 `WindBladeExecution`。平台仅处理同步数据、效果持有者、客户端回调与版本不同的原生声音。Core 和其他联动附属均不依赖 Wind。

通用 Iron 弹射物执行接口新增可覆盖的 `withTrajectory`，不再把所有执行器强制转成只支持 AbstractMagicProjectile 的实现。原有 Iron 弹射物仍走原有执行器。

Wind 的公开项目页声明保留所有权利，JAR 元数据则写有 MIT；本实现按更保守的边界处理，只运行时调用类、方法和资源路径，不包含其类、素材、反编译源码或 JAR：
https://www.curseforge.com/minecraft/mc-mods/winds-spellbooks-irons-spells-n-spellbooks-addon

## 开发与验证

本地编译依赖：NeoForge 的 `libs/wind-spellbooks.jar`，Forge 的 `forge/libs/wind-spellbooks-forge1201.jar`；均为忽略目录，不随发行包分发。开发运行用 `-PwithWind=true` 才加载可选依赖。

```powershell
# Java 21，NeoForge：七种法术与原生／环绕两条弹射物执行路径
./gradlew.bat :runGameTestServer :jar :sourcesJar --offline -PwithWind=true -Pstandalone=true -PgameTestNamespaces=irons_nouveau_wind -PgameTestHeap=900m
# 不安装 Wind 的启动／注册验证
./gradlew.bat :runGameTestServer --offline -Pstandalone=true -PgameTestNamespaces=irons_nouveau_wind_absent -PgameTestHeap=900m
# Java 17，Forge
./forge/gradlew.bat -p forge :runGameTestServer :jarJar :reobfJarJar :sourcesJar --offline -PwithWind=true -PgameTestNamespaces=irons_nouveau_wind -PgameTestHeap=900m
```

定向验证涵盖真实配方、能力提示、远程区域落点／归属／清理、友军与敌人区别、友军头部视角、原生动作、风刃伤害成功续接、环绕寿命与单一轨迹。客户端模型、音效和粒子是否完全符合预期仍需人工游玩观察，不能以服务端通过代替画面验证。

2026-11-05：NeoForge 八项 Wind 定向测试通过；与 Ars ’n Spells／NEG 共存时这八项同样通过，另一个 NEG 溅射弹测试确认两个目标均受到原生风刃伤害。不安装 Wind 的一项独立启动／注册测试通过。Forge 同批八项测试通过，最终产物重混淆打包。新增 `tools/check_wind_mixin_hooks.py` 核对真实发布 JAR 的覆写方法名、可选 Mixin 类、Forge refmap、七份提示数据与禁止夹带的原模组内容；原有 Boss 能力注入核对继续执行。

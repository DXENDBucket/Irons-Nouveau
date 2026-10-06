# Forge 1.20.1 移植记录

当前版本为 `0.15.5-forge1201-alpha.3`，包含 [Hex-Ars Linker 可选弹射物联动](hex-ars-linker.md)及默认开启的 [Iron 蓝池配置](configuration.md#魔力来源)。安装产物为 `build/libs/irons-nouveau-0.15.5-forge1201-alpha.3-all.jar`。下文保留初始 alpha.1 的移植和验证记录。

日期：2026-10-06。分支：`1.20.1-forge`。基于 `5eb95ad`（0.15.3），测试版版本号 `0.15.3-forge1201-alpha.1`。1.21.1 主工作目录和 `main` 不受影响。

## 构建

- Java 17，Gradle 8.8，ForgeGradle 6.0.36，Forge 47.4.0；目标为 Forge 47.4.x。
- Ars Nouveau 4.12.7，Iron's Spells 'n Spellbooks 1.20.1-3.16.3。
- 依赖下载链接和 SHA-512 固定在 `forge-dependencies.json`，运行 `python tools/fetch_forge_dependencies.py` 获取。依赖 JAR 不提交到仓库。
- `gradlew.bat jarJar` 生成并重混淆 `build/libs/irons-nouveau-0.15.3-forge1201-alpha.1-all.jar`。安装这个 `all` 文件；普通薄 JAR 不包含新版 MixinExtras。
- `all` JAR 内含 MIT 声明、Mixin refmap 和 MixinExtras Forge 0.4.1。上游模组仍需单独安装。

## 已移植内容

保留 95 个魔符（18 个弹射物转换、77 个效果），未因版本回退主动缩减法术范围。

- Forge 注册、事件、配置、战利品修饰器和自定义卷轴原料。
- 1.20.1 配方目录和 Ars 旧版 GlyphRecipe 格式。
- 玩家亲自撰写/升级记录改为持久化 NBT，补齐死亡复制、登录、重生和换维度同步。
- 网络同步改用 Forge SimpleChannel，保持服务端决定进度和施法规则。
- 适配 Ars 旧版 SpellCaster / ScryCaster、弓弩施法入口，以及吟唱、冷却、移动限制。
- 适配旧版状态效果、属性、实体同步和伤害接口；保留实际触发计费、持续效果和召唤生命周期管理。
- 排序使用旧版 Ars 的比较器；旧版没有新版强化说明接口，强化功能仍保留。

## 验证范围

使用 `gradlew.bat runGameTestServer -x downloadAssets` 进行独立服务端集中验证，Forge 专用测试位于 `src/forgeTest`，不打包进发行 JAR。覆盖注册、治疗与空蓝拒绝、原生弹射物转换、真实卷轴撰写解锁、死亡复制、主动吟唱释放及共享冷却。

本轮 4 项 Forge GameTest 和 4 项轻量单元测试全部通过，日志 `build/forge-validation.log`。正式重混淆 JAR 另在 Forge 47.4.26 客户端完成模组及资源加载，未安装 Ars 'n Spells 或 Not Enough Glyphs；日志 `build/forge-client-console.log`。这一检查修复了只在发行环境中出现的弹射物 Invoker 和闪电球方块命中方法映射问题。客户端测试为独立临时目录，没有使用正式存档。

`src/gameTest` 保留 1.21.1 基线测试供后续迁移参考，不属于此分支的可执行 source set；不能把历史测试报告当作 Forge 验证结果。`gradlew.bat test` 执行与 Minecraft 启动无关的排序和吟唱计时测试。

本版是移植 alpha，尚未逐个实测所有法术及组合。Ars 'n Spells 和魔符附属为可选内容，本轮未验证其 1.20.1 组合，不构成硬前置。

运行时 Iron 3.16.3 自身的 `crypt_loot` / `citadel_tomes` 战利品表会报告格式错误，来源为上游 JAR，本项目没有覆盖其数据。

## 上游源码依据

- [Ars Nouveau 1.20](https://github.com/baileyholl/Ars-Nouveau/tree/2c74064bc753600d9b6600f102f8163e2d6764dd)
- [Iron 当前 1.20.1 分支](https://github.com/iron431/Irons-Spells-n-Spellbooks/tree/cae63a6999e24ed3deaa012ecb8c262e0e377816)，分支名为 `not-1.20.1`；旧的 `1.20.1-legacy` 并非本次目标。

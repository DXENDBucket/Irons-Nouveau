# Building the Forge 1.20.1 version

Use **JDK 17** and the included **Gradle 8.8 wrapper**. Run `python tools/fetch_forge_dependencies.py` to fetch the pinned dependencies listed in `forge-dependencies.json`; these local JARs are not committed or bundled in the release.

Run `gradlew.bat build jarJar` from this branch. Install `build/libs/irons-nouveau-0.16.0-mc1.20.1-forge.jar`. It is reobfuscated and includes MixinExtras, its third-party notice, the MIT license and the Mixin refmap. Do not install the thin JAR or sources JAR. Ars Nouveau, Iron's Spells 'n Spellbooks and their required dependencies are installed separately.

For the new rays, run `gradlew.bat runGameTestServer -PgameTestNamespaces=irons_nouveau_rays -x downloadAssets`. Five targeted server tests passed in the Forge environment, covering native frost geometry/damage/freezing, unchanged native Iron casts, electrocute upkeep/anchors, sunbeam delayed damage, siphon upkeep and Forge visual-packet serialization. Client visuals have not been manually verified in this release. Details and earlier checks are in [the port record](forge-1.20.1-port.md).

## 双平台发行版本

模组版本号在 `main`（1.21.1 NeoForge）与 `1.20.1-forge` 中统一为 `0.16.0`。游戏版本和加载器写入发行 JAR 文件名，模组元数据仍只使用 `0.16.0`。后续同批更新同步递增两个分支的版本号。NeoForge 使用 Java 21 执行 `gradlew.bat build`；Forge 使用 Java 17 执行 `gradlew.bat build jarJar`。

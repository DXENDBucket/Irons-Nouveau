# Building from source

0.17.0 needs a sibling Ars Conflux checkout: NeoForge uses `../Ars-Conflux`, Forge uses `../Ars-Conflux/forge`. Override with `-PconfluxCheckout=path` and prepare Core's Ars dependencies as well. Install both mod JARs. See [the migration notes](ars-conflux.md).

Use **JDK 21** and the included **Gradle 9.2.1 wrapper**. This project currently uses local dependency JARs; a fresh clone requires the setup below before it can compile. Dependencies are not bundled with the repository or the resulting mod.

## Prepare dependencies

Create `libs/` in the repository root. Obtain the Minecraft **1.21.1 NeoForge** releases from each mod's official distribution, and copy/rename them as follows. Do not use Fabric builds, source JARs, or server packs.

| Local filename | Mod | Tested version |
| --- | --- | --- |
| `ars.jar` | Ars Nouveau | 5.13.1 |
| `iron.jar` | Iron's Spells 'n Spellbooks | 1.21.1-3.16.3 |
| `curios.jar` | Curios API | 9.5.1+1.21.1 |
| `geckolib.jar` | GeckoLib | 4.9.2 |
| `irons-lib.jar` | Iron's Lib | 1.21.1-2.1.0 |
| `player-animation.jar` | Player Animator | 2.0.4+1.21.1 |
| `caelus.jar` | Caelus API | 7.0.1+1.21.1 |
| `ars-spells.jar` | Ars 'n' Spells | 3.3.2 |
| `not-enough-glyphs.jar` | Not Enough Glyphs | 4.6.1 |
| `jei.jar` | Just Enough Items | 19.54.0.429 |

This table describes the development environment. Optional integration dependencies may be needed to compile integration tests even though players do not need to install those integrations. The `standalone` property excludes Ars 'n' Spells, Not Enough Glyphs and JEI from the runtime, not the compilation classpath. Transitive mods embedded by upstream dependencies do not need to be extracted into `libs/`.

## Build

Set `JAVA_HOME` to your JDK 21 installation, then run from the repository root:

```powershell
.\gradlew.bat assemble
```

On Linux or macOS:

```sh
./gradlew assemble
```

The main mod and sources JARs are written to `build/libs/`. Install the main JAR, not the `-sources.jar`. Both include the MIT license. Gradle downloads its toolchain dependencies on the first build; an internet connection is required.

## Targeted validation

The standalone server check covers configuration, learning policies, actual Ars mana payment, projectile splitting, creative behavior and preserved scroll progress:

```powershell
.\gradlew.bat :runGameTestServer -Pstandalone=true -PgameTestNamespaces=irons_nouveau_standalone
```

It uses `run/gametest-standalone/`, without Path-To-Zero or the optional integrations at runtime. It does not use an existing personal world. Other test namespaces are `irons_nouveau_policy` and `irons_nouveau`; the latter runs the broader integration suite.

To start a development client, use `runClient`. GameTest success does not substitute for checking client rendering or a multiplayer connection.

## Files kept local

`libs/`, `build/`, `run/`, Gradle caches and generated IDE launch configurations are ignored. Do not commit dependency JARs, logs, worlds, account data, or generated files. The checked-in Gradle wrapper JAR is build tooling, not a bundled mod dependency.

## 双平台发行版本

模组版本号在 `main`（1.21.1 NeoForge）与 `1.20.1-forge` 中统一为 `0.17.0`。游戏版本和加载器写入发行 JAR 文件名，模组元数据仍只使用 `0.17.0`。后续同批更新同步递增两个分支的版本号。NeoForge 使用 Java 21 执行 `gradlew.bat build`；Forge 使用 Java 17 执行 `gradlew.bat build jarJar`，发布上述带游戏版本的完整 JAR，勿发布 Forge 未包含 MixinExtras 的薄 JAR。

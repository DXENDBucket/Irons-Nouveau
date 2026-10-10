# Building from source

Both Minecraft targets now build from **one checkout and one main branch**. The root build targets 1.21.1 NeoForge; `forge/` targets 1.20.1 Forge. Both read the root `VERSION`, shared Java sources, assets and recipe definitions. The old `1.20.1-forge` branch is a historical snapshot, not a second development line.

Prepare Ars Conflux **0.1.9 or newer within 0.1.x** beside this checkout:

```text
MinecraftDev/
  Ars-Conflux/
  Irons-Nouveau/
    common/
    forge/
```

Ars Conflux remains a separate mod. Install its matching release JAR alongside Iron & Nouveau; it is not bundled into Iron's JAR.

## Dependencies and Java

Use JDK 21 for NeoForge and a Java 17 toolchain for Forge. The wrappers are Gradle 9.2.1 and 8.8 respectively. JDK 21 can run both wrappers; Forge's compilation and game runtime still use its Java 17 toolchain.

With Python 3.11 or newer, fetch the exact development releases and verify their SHA-512 checksums:

```powershell
python tools/fetch_dependencies.py
```

`dependencies.lock.json` records versions, official distribution URLs and hashes. The script fills ignored `libs/`, `forge/libs/` and Core's dependency directories. Use `--platform neoforge` or `--platform forge` for one target, and `--conflux C:/path/to/Ars-Conflux` for a different Core checkout. An existing different JAR is preserved unless `--replace` is explicitly supplied. Dependency JARs are neither committed nor redistributed.

The lock includes Ars, Iron, their required libraries and the JEI / Not Enough Glyphs development APIs. Ars 'n Spells and Hex-Ars Linker remain optional runtime integrations; no JAR from either mod is needed for a release build.

## Build both releases

Set `JAVA_HOME` to JDK 21, then run from the repository root:

```powershell
.\tools\build_all.ps1
```

For explicit Java and Core paths:

```powershell
.\tools\build_all.ps1 -NeoJavaHome C:/Java/jdk-21 -ForgeJavaHome C:/Java/jdk-21 -ConfluxCheckout C:/MinecraftDev/Ars-Conflux
```

This runs both builds, their unit tests, compilation of the focused runtime checks, both Core release builds, and shared-source / generated-recipe validation. It does not start Minecraft by default. `-RuntimeChecks` additionally runs only `irons_nouveau_shared` and `irons_nouveau_context` in disposable development worlds.

Release files for 0.17.8:

| Target | Iron & Nouveau | Ars Conflux |
| --- | --- | --- |
| NeoForge | `build/libs/irons-nouveau-0.17.8-mc1.21.1-neoforge.jar` | `../Ars-Conflux/build/libs/ars-conflux-0.1.9-mc1.21.1-neoforge.jar` |
| Forge | `forge/build/libs/irons-nouveau-0.17.8-mc1.20.1-forge.jar` | `../Ars-Conflux/forge/build/libs/ars-conflux-0.1.9-mc1.20.1-forge.jar` |

Install the full release JAR, not `-sources.jar` or Forge's `-thin.jar`. Forge's full `jarJar` output includes MixinExtras and is reobfuscated. Both releases contain the MIT license and third-party notices.

## Build one target

NeoForge, from the root:

```powershell
.\gradlew.bat :test :jar :sourcesJar :ars-conflux:jar
```

Forge, from `forge/`:

```powershell
.\gradlew.bat :test :jarJar :sourcesJar :ars-conflux:jarJar
```

On Linux / macOS use `./gradlew` and the same tasks. `-PconfluxCheckout=path` selects the Core root for NeoForge, or Core's `forge/` directory for Forge. Root tasks use the leading colon so Gradle does not also select identically named subproject tasks.

## Focused runtime validation

Run the following command in either platform's build directory:

```powershell
.\gradlew.bat :runGameTestServer '-PgameTestNamespaces=irons_nouveau_shared,irons_nouveau_context'
```

The checks cover sealed-tool persistence, scoped authorization and dry first-trigger quotes, shared casting context, effect ownership, ongoing payments and projectile continuation. Quote the comma-separated argument in PowerShell. Existing broader namespaces remain available for changes that need them; routine edits do not require running every suite.

`runClient` starts a development client. Server GameTests do not verify client rendering or multiplayer connections.

See [source ownership and the update workflow](multi-version.md). `libs/`, `build/`, `run/`, caches and generated IDE files stay local. The checked-in Gradle wrapper JAR is build tooling.

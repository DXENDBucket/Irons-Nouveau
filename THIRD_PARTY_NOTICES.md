# Third-party notices

The code of Iron & Nouveau is licensed under the MIT License in `LICENSE`, copyright 2026 EndXiom. That license does not relicense its dependencies or referenced upstream assets.

## Gradle wrapper

`gradlew`, `gradlew.bat` and `gradle/wrapper/gradle-wrapper.jar` are Gradle wrapper components. Their original copyright and Apache-2.0 notices are retained. The wrapper JAR also contains `META-INF/LICENSE`; a copy is provided at `gradle/wrapper/LICENSE`.

## Runtime dependencies

Ars Nouveau, Iron's Spells 'n Spellbooks and their dependencies are installed separately and remain under their respective licenses. Optional integrations are also installed separately. Their JARs are not committed to this repository or included in the release JAR.

The Forge 1.20.1 `all` artifact includes MixinExtras Forge 0.4.1 as a nested Jar-in-Jar dependency. MixinExtras is copyright LlamaLad7, distributed under the MIT License. Its original `LICENSE_MixinExtras` is preserved inside the nested JAR. See https://github.com/LlamaLad7/MixinExtras.

Glyph models reference Iron's installed scroll models and textures by resource identifier. Those upstream resources are not copied into this repository and are not covered by this project's MIT license.

The project icon was generated specifically for Iron & Nouveau (then named Iron's Nouveau) using an image-generation tool. Its original generation prompt is documented in `docs/icon-generation.txt`.

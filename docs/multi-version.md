# Maintaining both Minecraft versions

The `main` branch owns both targets. Change shared behavior once, compile it independently against Ars 5 / NeoForge and Ars 4 / Forge, then publish two JARs with the same root `VERSION`. No Architectury or Stonecutter dependency is introduced.

| Directory | Responsibility |
| --- | --- |
| `common/src/main/java` | Glyph catalog, formulas, composition adapters, payment, chanting, cooldowns, execution lifetimes and sealed-tool behavior |
| `common/src/main/resources` | Glyph models, language files and other shared assets |
| `common/recipes/glyphs.json` | Complete canonical glyph recipe definitions |
| `src/main/java` | NeoForge bootstrap, Ars 5 / Iron / Minecraft adapters and native Mixins |
| `forge/src/main/java` | Forge bootstrap, Ars 4 / Iron / Minecraft adapters and native Mixins |
| `gradle/shared-sources.gradle` | Source-set wiring and Ars 4 / Ars 5 recipe generation |

Common code must compile with Java 17. Minecraft, Ars and Iron types with compatible signatures can be used directly; a loader-neutral substitute for every upstream class would add complexity without helping maintenance.

## Where differences belong

Use the public Core API `ArsSpellAccess` for part snapshots and projectile resolvers. These are Ars version differences that other glyph addons also need. Do not import Core's internal `platform` classes.

Iron's `platform` helpers isolate native differences in status-effect holders, Iron events, packet delivery, item serialization, registries and event wiring. They expose the same Java methods in each target. A specific glyph's Iron behavior still belongs in this addon, not in Core.

Keep native Mixins separate when their target fields, descriptors, names or lifecycle differ. Keep player progression storage and network packet formats native to their target. Sharing those files is optional; preserving correct behavior is the priority.

The sealed-tool implementation is common. NeoForge stores `irons_nouveau:bound_weapon_spell` as the existing data component; Forge stores the same logical payload in item NBT. The NeoForge `BoundSpellWeapons.BINDING` field and nested `Binding.CODEC` remain available for existing callers. Prefer the common methods for new integrations.

## Adding or changing a glyph

1. Edit the shared glyph catalog / adapters when both upstream versions have the required spell API. Put a genuinely different native implementation in both platform directories.
2. Add its model and translations to shared resources.
3. Add the full recipe to `common/recipes/glyphs.json`. The build emits `recipe/` with Ars 5's schema and `recipes/` with Ars 4's schema; do not maintain generated JSON copies by hand.
4. Update the single `VERSION` for a release, then use `tools/build_all.ps1`. Add a focused runtime check only when the change warrants one.

`tools/check_shared_layout.py --generated` rejects duplicate common classes and checks both generated recipe sets. A platform-specific facade is allowed when its common behavior lives under a different class name.

The old `1.20.1-forge` branch and its worktree retain earlier history. Future updates use the nested Forge build on `main`; they do not require cherry-picking between branches. Ars Conflux continues to have its own repository and release tags.

This restructuring provides reusable abilities and APIs. Pack-specific survival recipes, progression, world generation and mob AI remain in the pack's own core mod.

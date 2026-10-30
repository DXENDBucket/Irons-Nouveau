# Thunderstorm ownership fix — 0.17.37

The native Iron status assumes its holder is its caster. Ars can attach the same
status to a different hit recipient. Before this fix the original caster still
paid upkeep, but native target selection, lightning owner and damage power used
the holder. A projectile-applied storm could therefore target and damage its
actual caster.

`StatusBilling.stormOwner` resolves the existing saved owner UUID for bridge
statuses. The native candidate predicate retains holder-centred range and line
of sight, but its native `shouldHealEntity` query uses the caster (plus Conflux
allies on transferred storms). The native per-target callback receives the real
caster for lightning ownership and damage calculation. No native lightning
entity, particles, lifetime, range or damage formula is replaced. The holder
remains excluded as a strike centre. Unmanaged native Iron effects keep their
original semantics; no global lightning immunity is installed.

The lambda names and descriptors were checked in both actual Iron 3.16.3 release
JARs. Neo's healing helper takes two LivingEntity arguments; Forge's takes two
Entity arguments. Those optional native methods are not MCP-mapped. Existing
Forge effect tick entry remains remapped normally.

Validation: 2/2 targeted native tests passed on NeoForge 1.21.1 and 2/2 on Forge
1.20.1. A transferred status creates real native strikes around its host, excludes
the original caster and a Core-only ally, pays the original pool once, deals
native area damage with the real caster credit, excludes that caster even at
two blocks from the enemy, and produces no further strikes with an empty pool.
A separate unmanaged native storm retains its original owner. Test enemies are
hostile mobs: Iron's ALWAYS_HEAL tag excludes cows from a neutral host's targets.

Logs: `build/thunderstorm-owner-neoforge.log` and
`forge/build/thunderstorm-owner-forge.log`. Both runtime and source distributions
were built; Forge was reobfuscated, declared refmaps and license checked, no
test classes bundled. No client visual validation or CurseForge upload.

Core baseline: 0.1.45 `ab3dc55710e7cd05140f188d0540240e88702366`, unchanged.
Runtime SHA-256:

- NeoForge: `da0d324a3b63b00de56adc3f93fe9c4b17a4ef9dbffb932855fab387f204fcb0`
- Forge: `8d12d1e304317a951633edc5357eb121a7d12b212c4d555833e7a22f5a7e4014`

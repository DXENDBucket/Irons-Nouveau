# Iron & Nouveau — percentage power

Core 0.1.42 owns the shared percentage formula and raw-native damage scope. Iron retains native spell power, damage, healing, timing and payment. Motion NBT, native-self leases and paid emitter leases preserve per-effect school weights; paid emitters restore their profile before the first pulse after a reload. Ordinary summon melee is not opted in. Both loaders build from the shared code. Attributes default to zero; schools are not preset.

General attribute: `ars_conflux:confluence_spell_power`; `0.2` is +20%, `-0.2` is -20%. General power also applies without school declarations. General and weighted school percentages add before the final nonnegative clamp. Optional school attributes are declared with `schools.power_attributes` in Core startup config, which requires matching client/server lists and a restart.

Verification and distribution hashes are recorded in Core’s `docs/spell-power-handoff-0.1.42.md` and the local release manifest. No new permanent progression or equipment is granted.

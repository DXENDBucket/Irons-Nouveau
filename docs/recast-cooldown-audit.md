# Single-shot recast cooldowns (0.17.13)

Pinned native versions: Iron 3.16.3 (NeoForge 1.21.1) and the existing Forge 1.20.1 dependency. Both FlamingBarrageSpell.getRecastCount return 5; EldritchBlastSpell returns 2 + level. RecastInstance stores totalRecasts - 1 after the first cast, so these are total shot counts including the initial shot. Both are instant attacks and each bridge execution emits one native attack.

Do not divide every getRecastCount: summon recall, cancellation, alternate actions and multi-point selection are not repeated attacks. Continuous spells already run their complete channels and retain native cooldowns.

RecastCooldowns resolves occurrence levels using the same base progression/preset tool scope and Ars action augments as casting. For duplicates it keeps the lowest shot divisor (longest cooldown). A narrowly scoped loader-local MagicManager return injection divides native effective cooldown after CDR/sword multipliers, before the original Pre event. The original manager still owns cancellation, overrides, storage, Post event and packet synchronization. Original Iron casts outside the scope are unchanged. Bound non-player tools use the same divisor on their existing local expiry timestamps.

Two focused real-server cases in irons_nouveau_recast_cooldown cover an actual native barrage projectile, native cooldown events/storage/blocking/expiry, sword multiplier and original casting isolation; actual-level, amplified/preset/duplicate policies, recall/channel exclusions, tick rounding and event override/cancellation. This change does not alter projectile resource charges, native recast queues or Core APIs.

Validation: both loaders report `All 2 required tests passed` and `BUILD SUCCESSFUL`. Release/source artifacts pass metadata, 103 glyph models, native mixin presence, Java 21/17 class version, license and test/dependency isolation checks. Client native HUD rendering remains a manual acceptance check; no Core changes.

package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.mixin.NativeProjectileHitAccess;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.TelekinesisData;
import io.redspace.ironsspellbooks.entity.mobs.frozen_humanoid.FrozenHumanoid;
import io.redspace.ironsspellbooks.entity.spells.ender_chain.EnderChain;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class SelectedEntityTestCases {
    private SelectedEntityTestCases() {}
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var entity = h.spawn(EntityType.COW, x, 2, z);
        entity.setNoAi(true); entity.setNoGravity(true); return entity;
    }
    private static BridgeGlyph glyph(String id) {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static ResourceAccount account(AtomicInteger balance) {
        return new ResourceAccount() {
            public Object reservationKey() { return balance; }
            public boolean canSpend(int amount) { return balance.get() >= amount; }
            public void spend(int amount) { balance.addAndGet(-amount); }
        };
    }
    private static void cast(GameTestHelper h, LivingEntity caster, LivingEntity executor,
                             String id, HitResult hit, ResourceAccount account) {
        var glyph = glyph(id);
        var ars = new SpellContext(h.getLevel(), new Spell(MethodTouch.INSTANCE, glyph), caster,
                new LivingCaster(caster), ItemStack.EMPTY);
        CastContexts.bind(ars, CastContext.of(ars, null, account).withExecutor(executor));
        glyph.onResolve(hit, h.getLevel(), caster, new SpellStats.Builder().build(), ars, new SpellResolver(ars));
    }
    public static void steps(GameTestHelper h) {
        var caster = cow(h, 1, 1); var proxy = cow(h, 1, 7); var target = cow(h, 5, 4);
        var origin = caster.position(); var proxyOrigin = proxy.position(); var targetOrigin = target.position();
        var balance = new AtomicInteger(10000); var account = account(balance);
        var nativeData = MagicData.getPlayerMagicData(caster);
        var saved = new TelekinesisData(6, proxy, 6); nativeData.setAdditionalCastData(saved);
        cast(h, caster, proxy, "blood_step", new EntityHitResult(target), account);
        h.assertTrue(caster.distanceTo(target) < 3 && caster.position().distanceTo(origin) > 2,
                "Step moves the original caster beside the selected target");
        h.assertTrue(proxy.position().equals(proxyOrigin) && target.position().equals(targetOrigin),
                "Neither proxy executor nor destination entity is teleported");
        h.assertTrue(dev.ironsnouveau.platform.Effects.has(caster, MobEffectRegistry.TRUE_INVISIBILITY), "Native invisibility is retained");
        int before = balance.get(); var after = caster.position();
        var dead = cow(h, 8, 7); dead.discard();
        cast(h, caster, proxy, "blood_step", new EntityHitResult(dead), account);
        cast(h, caster, proxy, "frost_step", new BlockHitResult(new Vec3(Double.NaN, 0, 0), Direction.UP, caster.blockPosition(), false), account);
        h.assertTrue(balance.get() == before && caster.position().equals(after), "Dead entity and invalid position do not charge or move");
        cast(h, caster, proxy, "frost_step", new EntityHitResult(proxy), account);
        var shadows = h.getLevel().getEntitiesOfClass(FrozenHumanoid.class, caster.getBoundingBox().inflate(20));
        h.assertTrue(shadows.size() == 1 && shadows.get(0).position().distanceTo(after) < .01,
                "Frost Step leaves the real native decoy at the old position");
        h.assertTrue(nativeData.getAdditionalCastData() == saved, "Steps never overwrite native Iron cast data");
        for (String id : new String[]{"blood_step", "frost_step"}) {
            var self = cow(h, 1, id.equals("blood_step") ? 10 : 12);
            self.setYRot(-90); self.setXRot(0);
            var selfOrigin = self.position();
            before = balance.get();
            cast(h, self, proxy, id, new EntityHitResult(self), account);
            h.assertTrue(self.getX() > selfOrigin.x + 4 && Math.abs(self.getZ() - selfOrigin.z) < .75,
                    id + " self target executes native forward step, not proxy facing");
            h.assertTrue(balance.get() == before - SpellRegistry.getSpell(glyph(id).definition().spellId()).getManaCost(1),
                    id + " self step pays exactly once");
            h.assertTrue(self.getYRot() == -90 && self.getXRot() == 0, "Self solver preserves real orientation");

            var originBlock = h.absolutePos(new BlockPos(1, 2, 15));
            self.teleportTo(originBlock.getX() + .5, originBlock.getY(), originBlock.getZ() + .5);
            self.setYRot(0); self.setXRot(0); // Looking away from the selected point.
            var wall = h.absolutePos(new BlockPos(7, 2, 15));
            h.getLevel().setBlock(wall, Blocks.DEEPSLATE.defaultBlockState(), 3);
            var point = new Vec3(wall.getX(), self.getEyeY(), wall.getZ() + .5);
            before = balance.get();
            cast(h, self, proxy, id, new BlockHitResult(point, Direction.WEST, wall, false), account);
            h.assertTrue(self.getX() > originBlock.getX() + 3 && Math.abs(self.getZ() - (originBlock.getZ() + .5)) < .75,
                    id + " block step travels toward selected point, not live facing");
            h.assertTrue(balance.get() == before - SpellRegistry.getSpell(glyph(id).definition().spellId()).getManaCost(1)
                            && h.getLevel().noCollision(self), "Directional step pays once and fits at native landing");
            h.assertTrue(self.getYRot() == 0 && self.getXRot() == 0, "Directional solver never mutates caster rotation");
            // Both spell variants use the same fixture, without an earlier caster occupying its landing.
            self.discard();
        }
        h.assertTrue(CastContexts.current() == null, "Scope is restored");
        h.succeed();
    }
    public static void telekinesis(GameTestHelper h) {
        var caster = cow(h, 1, 1); var proxy = cow(h, 1, 7); var target = cow(h, 5, 4);
        caster.setYRot(-90); caster.setXRot(0);
        int cost = SpellRegistry.getSpell(glyph("telekinesis").definition().spellId()).getManaCost(1);
        var balance = new AtomicInteger(cost * 2); var pool = account(balance);
        var nativeData = MagicData.getPlayerMagicData(caster);
        var saved = new TelekinesisData(6, proxy, 6); nativeData.setAdditionalCastData(saved);
        cast(h, caster, proxy, "telekinesis", new EntityHitResult(target), pool);
        h.assertTrue(balance.get() == cost && target.getDeltaMovement().lengthSqr() > 0,
                "Native telekinesis starts on the selected target with one payment");
        h.assertTrue(nativeData.getAdditionalCastData() == saved && !nativeData.isCasting(),
                "Isolated channel does not replace native player casting state");
        h.runAtTickTime(24, () -> {
            h.assertTrue(balance.get() == 0, "Deferred channel uses the same account and stops when empty");
            balance.set(cost * 10);
            target.setDeltaMovement(Vec3.ZERO);
            h.runAtTickTime(30, () -> {
                h.assertTrue(balance.get() == cost * 10 && target.getDeltaMovement().lengthSqr() < .00001,
                        "Restoring mana does not restart a failed channel");
                h.assertTrue(nativeData.getAdditionalCastData() == saved, "Channel cleanup leaves native cast data intact");
                h.succeed();
            });
        });
    }
    public static void telekinesisPlayer(GameTestHelper h, net.minecraft.server.level.ServerPlayer player) {
        var proxy = cow(h, 1, 7); var target = cow(h, 5, 4);
        var definition = glyph("telekinesis").definition();
        int cost = SpellRegistry.getSpell(definition.spellId()).getManaCost(1);
        var balance = new AtomicInteger(cost * 2);
        var nativeData = MagicData.getPlayerMagicData(player);
        var saved = new TelekinesisData(6, proxy, 6); nativeData.setAdditionalCastData(saved);
        cast(h, player, proxy, "telekinesis", new EntityHitResult(target), account(balance));
        h.assertTrue(MovementRestrictions.active(player) && !MovementRestrictions.active(target),
                "Only the real player caster receives native movement restriction");
        boolean enabled = dev.ironsnouveau.config.SpellLevelConfig.MOVEMENT_ENABLED.get();
        try {
            dev.ironsnouveau.config.SpellLevelConfig.MOVEMENT_ENABLED.set(false);
            h.assertTrue(!MovementRestrictions.active(player), "Movement config disables telekinesis restriction");
        } finally { dev.ironsnouveau.config.SpellLevelConfig.MOVEMENT_ENABLED.set(enabled); }
        var data = new dev.ironsnouveau.network.TelekinesisVisualState(java.util.UUID.randomUUID(),
                h.getLevel().dimension().location(), player.getId(), player.getUUID(), target.getId(), target.getUUID(), 10);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            data.write(buffer);
            h.assertTrue(data.equals(dev.ironsnouveau.network.TelekinesisVisualState.read(buffer)),
                    "Visual packet preserves dimension, entity identities and lease duration");
        } finally { buffer.release(); }
        h.runAtTickTime(24, () -> {
            h.assertTrue(balance.get() == 0 && !MovementRestrictions.active(player) && !CastSessions.isCasting(player),
                    "Empty account ends channel and releases movement/casting occupancy");
            h.assertTrue(nativeData.getAdditionalCastData() == saved && !nativeData.isCasting(),
                    "Visual and movement integration never replaces native casting data");
            balance.set(cost * 10);
            cast(h, player, proxy, "telekinesis", new EntityHitResult(target), account(balance));
            h.assertTrue(MovementRestrictions.active(player), "New channel reacquires restriction");
            target.discard();
        });
        h.runAtTickTime(28, () -> {
            h.assertTrue(!MovementRestrictions.active(player) && !CastSessions.isCasting(player),
                    "Lost target releases movement immediately on session cleanup");
            player.discard(); proxy.discard(); h.succeed();
        });
    }
    public static void shackle(GameTestHelper h) {
        var caster = cow(h, 1, 1); var victim = cow(h, 5, 4);
        var id = dev.ironsnouveau.platform.Locations.id("irons_spellbooks", "arcane_shackle");
        var spell = SpellRegistry.getSpell(id); var payload = new ComplexProjectilePayload("arcane_shackle");
        var impacts = new AtomicInteger();
        var plan = new CastPlan(dev.ironsnouveau.platform.Locations.id("irons_nouveau", "glyph_arcane_shackle"),
                id, 1, spell.getSpellPower(1, caster), new CastModifiers(0, 1), 100);
        var session = new CastSession(h.getLevel(), caster, plan,
                AimSource.fixed(new CastAim(caster.getEyePosition(), new Vec3(1, 0, 0), null)),
                new CastExecution() {
                    public boolean start(CastSession s) { return true; }
                    public boolean tick(CastSession s) { return false; }
                    public void close(CastSession s, CastSession.EndReason reason) {}
                }, hit -> impacts.incrementAndGet());
        h.assertTrue(session.start(), "Native projectile session starts");
        var projectile = payload.create(h.getLevel(), caster); payload.configure(session, projectile);
        projectile.setPos(victim.position()); projectile.setOwner(caster); projectile.setYRot(-90);
        ((NativeCastCarrier)projectile).ironsNouveau$session(session); h.getLevel().addFreshEntity(projectile);
        ((NativeProjectileHitAccess)projectile).ironsNouveau$hit(new EntityHitResult(victim));
        var chains = h.getLevel().getEntitiesOfClass(EnderChain.class, victim.getBoundingBox().inflate(20));
        h.assertTrue(chains.size() == 3 && chains.stream().allMatch(c -> c.getOwner() == caster && c.getVictim() == victim),
                "Entity impact creates all three native chains with correct owner and victim");
        h.assertTrue(impacts.get() == 1, "Native entity impact continues Ars effects exactly once");
        chains.forEach(c -> c.discard());
        var visual = payload.create(h.getLevel(), caster); visual.setYRot(-90); visual.setDeltaMovement(1, 0, 0);
        h.assertTrue(payload.detonate(session, visual,
                new BlockHitResult(victim.position(), Direction.UP, victim.blockPosition().below(), false)), "Ars-driven block impact invokes native callback");
        chains = h.getLevel().getEntitiesOfClass(EnderChain.class, victim.getBoundingBox().inflate(20));
        h.assertTrue(chains.stream().anyMatch(c -> c.getVictim() == victim), "Block impact also shackles nearby entities");
        h.assertTrue(impacts.get() == 2, "Ars-driven block impact continues effects once");
        session.finish(CastSession.EndReason.COMPLETED);
        h.succeed();
    }
}

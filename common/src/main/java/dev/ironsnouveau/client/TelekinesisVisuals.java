package dev.ironsnouveau.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.SpellTargetingLayer;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import dev.ironsnouveau.network.TelekinesisVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.lang.ref.WeakReference;
import java.util.*;

/** Complete native target cage and player animation, independently leased from native Iron state. */
public final class TelekinesisVisuals {
    private record Lease(TelekinesisVisualState data, long expires) {}
    private static final Map<UUID, Lease> ACTIVE = new LinkedHashMap<>();
    private static final Set<UUID> ANIMATED = new HashSet<>();
    private static final ThreadLocal<Boolean> RENDERING = ThreadLocal.withInitial(() -> false);
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private TelekinesisVisuals() {}
    public static Vector3f nativeColor() {
        return RENDERING.get() ? new Vector3f(SpellRegistry.TELEKINESIS_SPELL.get().getTargetingColor()) : null;
    }
    private static ClientLevel level() {
        var level = Minecraft.getInstance().level;
        if (level != world.get()) { ACTIVE.clear(); ANIMATED.clear(); world = new WeakReference<>(level); }
        return level;
    }
    public static void accept(TelekinesisVisualState data) {
        var level = level();
        if (level == null || !level.dimension().location().equals(data.dimension())) return;
        if (data.remaining() <= 0) { ACTIVE.remove(data.token()); stopAnimation(level, data); return; }
        if (ACTIVE.size() >= 256 && !ACTIVE.containsKey(data.token())) return;
        ACTIVE.put(data.token(), new Lease(data, level.getGameTime() + Math.min(10, data.remaining())));
        var caster = level.getEntity(data.casterId());
        if (caster instanceof AbstractClientPlayer player && player.getUUID().equals(data.casterUuid())
                && !ClientMagicData.getSyncedSpellData(player).isCasting() && ANIMATED.add(player.getUUID()))
            SpellRegistry.TELEKINESIS_SPELL.get().getCastStartAnimation().getForPlayer()
                    .ifPresent(animation -> AnimationHelper.animatePlayerStart(player, animation));
    }
    private static void stopAnimation(ClientLevel level, TelekinesisVisualState data) {
        if (ACTIVE.values().stream().anyMatch(l -> l.data.casterUuid().equals(data.casterUuid()))) return;
        var caster = level.getEntity(data.casterId());
        if (caster instanceof AbstractClientPlayer player && player.getUUID().equals(data.casterUuid())
                && ANIMATED.remove(player.getUUID()) && !ClientMagicData.getSyncedSpellData(player).isCasting())
            AnimationHelper.cancelPlayerAnimation(player);
    }
    public static void tick() {
        var level = level();
        if (level == null) return;
        for (var lease : List.copyOf(ACTIVE.values())) {
            var target = level.getEntity(lease.data.targetId());
            // A packet may arrive before entity tracking; keep its short lease until the entity arrives.
            if (lease.expires <= level.getGameTime() || target != null && (!target.isAlive()
                    || !target.getUUID().equals(lease.data.targetUuid()))) {
                ACTIVE.remove(lease.data.token()); stopAnimation(level, lease.data);
            }
        }
    }
    public static void render(PoseStack poses, MultiBufferSource.BufferSource buffers, Vec3 camera, float partial) {
        var level = level();
        var local = Minecraft.getInstance().player;
        if (level == null || local == null) return;
        boolean drew = false;
        for (var lease : ACTIVE.values()) {
            if (lease.expires <= level.getGameTime()) continue;
            var entity = level.getEntity(lease.data.targetId());
            if (!(entity instanceof LivingEntity target) || !target.getUUID().equals(lease.data.targetUuid())
                    || !target.isAlive() || local.distanceToSqr(target) > 96 * 96) continue;
            var nativeTargeting = ClientMagicData.getTargetingData();
            if (nativeTargeting.isTargeted(target) && SpellRegistry.TELEKINESIS_SPELL.get().getSpellId().equals(nativeTargeting.spellId)) continue;
            var position = target.getPosition(partial);
            poses.pushPose();
            boolean previous = RENDERING.get();
            try {
                // Native layer normally receives model-space transforms. Cancel its model-height offset in world space.
                poses.translate(position.x - camera.x, position.y - camera.y + target.getBbHeight() - 1.5, position.z - camera.z);
                RENDERING.set(true);
                SpellTargetingLayer.renderTargetLayer(poses, buffers, target);
                drew = true;
            } finally { RENDERING.set(previous); poses.popPose(); }
        }
        if (drew) buffers.endBatch(RenderType.energySwirl(SpellTargetingLayer.TEXTURE, 0, 0));
    }
}

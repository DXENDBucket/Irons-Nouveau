package dev.ironsnouveau.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ironsnouveau.IronsNouveau;
import dev.ironsnouveau.network.SiphonRayVisualPayload;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Calls Iron's complete ray renderer without taking over the entity's native casting state. */
@EventBusSubscriber(modid = IronsNouveau.MOD_ID, value = Dist.CLIENT)
public final class SiphonRayVisuals {
    private record Lease(SiphonRayVisualPayload data, long expires) {}
    private static final Map<UUID, Lease> ACTIVE = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    /** Only the ray's geometry is substituted; Iron still owns animation, segmentation and materials. */
    public record NativeView(Vec3 origin, HitResult hit) {}
    private static final ThreadLocal<NativeView> NATIVE_VIEW = new ThreadLocal<>();
    public static NativeView nativeView() { return NATIVE_VIEW.get(); }
    private record Frame(LivingEntity renderer, Vec3 origin, Vec3 far, Vec3 visualOrigin, HitResult hit) {}
    private SiphonRayVisuals() {}
    private static ClientLevel level() {
        var level = Minecraft.getInstance().level;
        if (level != world.get()) { ACTIVE.clear(); world = new WeakReference<>(level); }
        return level;
    }
    public static void accept(SiphonRayVisualPayload data) {
        var level = level();
        if (level == null || !level.dimension().location().equals(data.dimension())) return;
        if (data.remaining() <= 0) { ACTIVE.remove(data.token()); return; }
        if (!Double.isFinite(data.origin().lengthSqr()) || !Double.isFinite(data.direction().lengthSqr())
                || !Double.isFinite(data.offset().lengthSqr()) || data.direction().lengthSqr() < 1e-8
                || data.range() <= 0 || !Float.isFinite(data.range())) return;
        if (ACTIVE.size() >= 256 && !ACTIVE.containsKey(data.token())) ACTIVE.remove(ACTIVE.keySet().iterator().next());
        ACTIVE.put(data.token(), new Lease(data, level.getGameTime() + Math.min(10, data.remaining())));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var level = level();
        var player = Minecraft.getInstance().player;
        if (level == null) return;
        ACTIVE.values().removeIf(l -> l.expires <= level.getGameTime());
        if (player == null || Minecraft.getInstance().isPaused()) return;
        for (var lease : ACTIVE.values()) {
            var frame = frame(level, player, lease.data, 1);
            if (frame == null || player.distanceToSqr(frame.origin) > 96 * 96) continue;
            // Iron emits these at every ray endpoint, including blocks and maximum-range misses.
            // Its client event has no public particle entry point, so retain the native particle,
            // cadence, offset and velocity here rather than reimplementing the particle itself.
            Vec3 endpoint = frame.hit.getLocation().subtract(0, .25, 0);
            for (int i = 0; i < 8; i++) {
                Vec3 offset = new Vec3(Utils.getRandomScaled(.2), Utils.getRandomScaled(.2), Utils.getRandomScaled(.2));
                level.addParticle(ParticleHelper.SIPHON, endpoint.x + offset.x, endpoint.y + offset.y,
                        endpoint.z + offset.z, offset.x, offset.y, offset.z);
            }
        }
    }
    private static Frame frame(ClientLevel level, LivingEntity player, SiphonRayVisualPayload data, float partial) {
        Vec3 origin = data.origin(), direction = data.direction();
        var entity = data.anchorId() < 0 ? null : level.getEntity(data.anchorId());
        LivingEntity anchor = entity instanceof LivingEntity living && living.getUUID().equals(data.anchorUuid()) ? living : null;
        if (data.anchorId() >= 0 && (anchor == null || !anchor.isAlive())) return null;
        if (anchor != null) {
            origin = (data.eyeAnchored() ? anchor.getEyePosition(partial) : anchor.getPosition(partial)).add(data.offset());
            direction = anchor.getViewVector(partial);
        }
        Vec3 far = origin.add(direction.normalize().scale(data.range()));
        var hit = RaycastBuilder.begin(level, anchor == null ? player : anchor).start(origin).end(far)
                .checkForBlocks(true).bbInflation(.15f)
                .filter(e -> !e.getUUID().equals(data.ownerUuid()) && !e.getUUID().equals(data.anchorUuid())
                        && e.canBeHitByProjectile()).build();
        Vec3 visualOrigin = anchor != null && data.eyeAnchored() ? origin.add(0, -.2 * anchor.getEyeHeight(), 0) : origin;
        return new Frame(anchor == null ? player : anchor, origin, far, visualOrigin, hit);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var level = level(); var client = Minecraft.getInstance();
        if (level == null || client.player == null || ACTIVE.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var buffers = client.renderBuffers().bufferSource();
        var core = RenderType.entityTranslucent(SpellRenderingHelper.BEACON, true);
        var glow = RenderType.entityTranslucent(SpellRenderingHelper.TWISTING_GLOW);
        for (var lease : ACTIVE.values()) {
            if (lease.expires <= level.getGameTime()) continue;
            var frame = frame(level, client.player, lease.data, partial);
            if (frame == null || !event.getFrustum().isVisible(new AABB(frame.origin, frame.far).inflate(.5))) continue;
            PoseStack poses = event.getPoseStack();
            poses.pushPose();
            var previous = NATIVE_VIEW.get();
            try {
                var camera = event.getCamera().getPosition();
                // The native renderer adds 80% of eye height itself. Cancel that translation
                // here so a block/target trigger can retain its own origin without fake entities.
                poses.translate(frame.visualOrigin.x - camera.x,
                        frame.visualOrigin.y - camera.y - .8f * frame.renderer.getEyeHeight(), frame.visualOrigin.z - camera.z);
                NATIVE_VIEW.set(new NativeView(frame.origin, frame.hit));
                SpellRenderingHelper.renderRayOfSiphoning(frame.renderer, poses, buffers, partial);
            } finally {
                if (previous == null) NATIVE_VIEW.remove(); else NATIVE_VIEW.set(previous);
                poses.popPose();
            }
        }
        buffers.endBatch(core); buffers.endBatch(glow);
    }
}

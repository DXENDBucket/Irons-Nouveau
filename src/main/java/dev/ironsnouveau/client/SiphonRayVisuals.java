package dev.ironsnouveau.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ironsnouveau.IronsNouveau;
import dev.ironsnouveau.network.SiphonRayVisualPayload;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Native Iron beam geometry/materials, driven by bridge sessions instead of native MagicData. */
@EventBusSubscriber(modid = IronsNouveau.MOD_ID, value = Dist.CLIENT)
public final class SiphonRayVisuals {
    private record Lease(SiphonRayVisualPayload data, long expires) {}
    private static final Map<UUID, Lease> ACTIVE = new LinkedHashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
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
                || data.range() <= 0 || !Float.isFinite(data.range())) return;
        if (ACTIVE.size() >= 256 && !ACTIVE.containsKey(data.token())) ACTIVE.remove(ACTIVE.keySet().iterator().next());
        ACTIVE.put(data.token(), new Lease(data, level.getGameTime() + Math.min(10, data.remaining())));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var level = level();
        if (level != null) ACTIVE.values().removeIf(l -> l.expires <= level.getGameTime());
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
            var data = lease.data;
            Vec3 origin = data.origin(), direction = data.direction();
            var entity = data.anchorId() < 0 ? null : level.getEntity(data.anchorId());
            if (entity instanceof LivingEntity anchor && anchor.getUUID().equals(data.anchorUuid())) {
                if (!anchor.isAlive()) continue;
                origin = (data.eyeAnchored() ? anchor.getEyePosition(partial) : anchor.getPosition(partial)).add(data.offset());
                direction = anchor.getViewVector(partial);
            }
            Vec3 far = origin.add(direction.normalize().scale(data.range()));
            if (!event.getFrustum().isVisible(new AABB(origin, far).inflate(.5))) continue;
            var hit = RaycastBuilder.begin(level, entity == null ? client.player : entity).start(origin).end(far)
                    .checkForBlocks(true).bbInflation(.15f)
                    .filter(e -> !e.getUUID().equals(data.ownerUuid()) && !e.getUUID().equals(data.anchorUuid())
                            && e.canBeHitByProjectile()).build();
            // Like Iron's native beam, draw slightly below the eyes so first-person users
            // see the beam's length rather than looking straight down its axis.
            Vec3 visualOrigin = entity instanceof LivingEntity anchor && data.eyeAnchored()
                    && anchor.getUUID().equals(data.anchorUuid()) ? origin.add(0, -.2 * anchor.getEyeHeight(), 0) : origin;
            Vec3 delta = hit.getLocation().subtract(visualOrigin);
            float length = (float)delta.length();
            if (length < .01) continue;
            PoseStack poses = event.getPoseStack();
            poses.pushPose();
            var camera = event.getCamera().getPosition();
            poses.translate(visualOrigin.x - camera.x, visualOrigin.y - camera.y, visualOrigin.z - camera.z);
            var unit = delta.normalize();
            poses.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), new Vector3f((float)unit.x, (float)unit.y, (float)unit.z)));
            float scroll = -((level.getGameTime() + partial) % 10) * .2f;
            var end = new Vec3(0, 0, length);
            SpellRenderingHelper.drawHull(Vec3.ZERO, end, .12f, .12f, poses.last(), buffers.getBuffer(core),
                    178, 0, 0, 255, scroll, scroll + length * .5f);
            var halo = buffers.getBuffer(glow);
            SpellRenderingHelper.drawQuad(Vec3.ZERO, end, .48f, 0, poses.last(), halo, 178, 0, 0, 255, scroll, scroll + length * .5f);
            SpellRenderingHelper.drawQuad(Vec3.ZERO, end, 0, .48f, poses.last(), halo, 178, 0, 0, 255, scroll, scroll + length * .5f);
            poses.popPose();
        }
        buffers.endBatch(core); buffers.endBatch(glow);
    }
}

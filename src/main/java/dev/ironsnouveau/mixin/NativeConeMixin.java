package dev.ironsnouveau.mixin;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Set;

@Mixin(value = AbstractConeProjectile.class, remap = false)
public abstract class NativeConeMixin implements ConeState {
    @Unique private static final EntityDataAccessor<Boolean> ironsNouveau$MANAGED = SynchedEntityData.defineId(AbstractConeProjectile.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Integer> ironsNouveau$ANCHOR = SynchedEntityData.defineId(AbstractConeProjectile.class, EntityDataSerializers.INT);
    @Unique private static final EntityDataAccessor<Vector3f> ironsNouveau$OFFSET = SynchedEntityData.defineId(AbstractConeProjectile.class, EntityDataSerializers.VECTOR3);
    @Unique private static final EntityDataAccessor<Float> ironsNouveau$SCALE = SynchedEntityData.defineId(AbstractConeProjectile.class, EntityDataSerializers.FLOAT);
    @Shadow protected abstract void onHitEntity(EntityHitResult hit);
    @Shadow protected abstract Set<Entity> getSubEntityCollisions();
    @Shadow protected int age;
    @Unique private SynchedEntityData ironsNouveau$data() { return ((AbstractConeProjectile)(Object)this).getEntityData(); }
    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void ironsNouveau$define(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(ironsNouveau$MANAGED, false); builder.define(ironsNouveau$ANCHOR, -1);
        builder.define(ironsNouveau$OFFSET, new Vector3f()); builder.define(ironsNouveau$SCALE, 1f);
    }
    @Override public boolean ironsNouveau$managed() { return ironsNouveau$data().get(ironsNouveau$MANAGED); }
    @Override public int ironsNouveau$anchor() { return ironsNouveau$data().get(ironsNouveau$ANCHOR); }
    @Override public Vec3 ironsNouveau$offset() { return new Vec3(ironsNouveau$data().get(ironsNouveau$OFFSET)); }
    @Override public float ironsNouveau$scale() { return ironsNouveau$data().get(ironsNouveau$SCALE); }
    @Override public void ironsNouveau$bind(int anchor, Vec3 offset, float scale) {
        ironsNouveau$data().set(ironsNouveau$MANAGED, true); ironsNouveau$data().set(ironsNouveau$ANCHOR, anchor);
        ironsNouveau$data().set(ironsNouveau$OFFSET, offset.toVector3f()); ironsNouveau$data().set(ironsNouveau$SCALE, scale);
    }
    @Override public Set<Entity> ironsNouveau$targets() { return getSubEntityCollisions(); }
    @Override public void ironsNouveau$hit(EntityHitResult hit) { onHitEntity(hit); }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$tick(CallbackInfo ci) {
        if (!ironsNouveau$managed()) return;
        var cone = (AbstractConeProjectile)(Object)this;
        age++;
        if (cone.level().isClientSide) BreathVisuals.tick(cone);
        ci.cancel();
    }
}

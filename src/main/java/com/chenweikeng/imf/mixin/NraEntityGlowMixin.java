package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.ServerState;
import com.chenweikeng.imf.nra.tracker.QuestTriangulationTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make entities near the estimated quest target glow. This helps players identify NPCs or
 * objects at the triangulated location.
 */
@Mixin(Entity.class)
public class NraEntityGlowMixin {

  private static final double GLOW_RADIUS_SQUARED = 9.0; // 3 block radius

  @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
  private void addQuestTargetGlow(CallbackInfoReturnable<Boolean> cir) {
    // Don't override if already glowing
    if (cir.getReturnValue()) {
      return;
    }

    Entity self = (Entity) (Object) this;

    // Only on client side
    if (!self.level().isClientSide()) {
      return;
    }

    // Only on ImagineFun server
    if (!ServerState.isImagineFunServer()) {
      return;
    }

    // Check if we have a confident target estimate
    QuestTriangulationTracker tracker = QuestTriangulationTracker.getInstance();
    Vec3 target = tracker.getEstimatedTarget();

    if (target == null || !tracker.hasConfidentEstimate()) {
      return;
    }

    // Check distance to target (centered on block)
    double dx = target.x + 0.5 - self.getX();
    double dy = target.y - self.getY();
    double dz = target.z + 0.5 - self.getZ();
    double distanceSquared = dx * dx + dy * dy + dz * dz;

    if (distanceSquared <= GLOW_RADIUS_SQUARED) {
      cir.setReturnValue(true);
    }
  }
}

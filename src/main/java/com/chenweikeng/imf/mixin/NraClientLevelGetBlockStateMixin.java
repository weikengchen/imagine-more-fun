package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.spacemountain.SpaceMountainBlockOverride;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the result of {@code Level.getBlockState(BlockPos)} with whatever {@link
 * SpaceMountainBlockOverride#apply} returns. Currently used to cover the PeopleMover viewing window
 * in Hyperspace Mountain's south wall; see that class for the full rule set and rationale.
 *
 * <p>Mixin targets {@code Level} (not {@code ClientLevel}) because that's where the method is
 * declared — {@code ClientLevel} just inherits it. This file is registered in the {@code client}
 * section of {@code imf.mixins.json}, so it never loads on the server. The override is additionally
 * gated by {@link com.chenweikeng.imf.nra.spacemountain.SpaceMountainOverride#isActive}, which
 * requires connection to ImagineFun, so it's a no-op everywhere except inside the dome on a real
 * ride.
 *
 * <p>The mixin runs at {@code RETURN} so the override sees the real block state first and only
 * takes effect when the gate is active. Off-ride the override fast-paths back to the original
 * state, so this inject is essentially free outside the dome.
 */
@Mixin(Level.class)
public class NraClientLevelGetBlockStateMixin {
  @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
  private void imf$applySpaceMountainOverrides(
      BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
    BlockState original = cir.getReturnValue();
    BlockState replacement = SpaceMountainBlockOverride.apply(original, pos);
    if (replacement != original) {
      cir.setReturnValue(replacement);
    }
  }
}

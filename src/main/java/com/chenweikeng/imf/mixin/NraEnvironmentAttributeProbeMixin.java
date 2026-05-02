package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.spacemountain.SpaceMountainOverride;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces fog and sky colors to pure black inside Space/Hyperspace Mountain. The dim's real fog/sky
 * values (live values: FOG_COLOR=0xA4B8DB pale blue, SKY_COLOR=0x658CD7 daytime blue) lift distant
 * objects out of black via the camera's fog blend, which breaks the "dark ride" feel. Returning 0
 * from the probe at HEAD bypasses the per-frame value cache without disturbing it — once the
 * override condition flips off the cache is repopulated normally on the next tick.
 */
@Mixin(EnvironmentAttributeProbe.class)
public class NraEnvironmentAttributeProbeMixin {
  @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
  private void imf$overrideFogAndSky(
      EnvironmentAttribute<?> attribute, float partialTicks, CallbackInfoReturnable<Object> cir) {
    if (!SpaceMountainOverride.isActive()) return;
    if (attribute == EnvironmentAttributes.FOG_COLOR
        || attribute == EnvironmentAttributes.SKY_COLOR
        || attribute == EnvironmentAttributes.SKY_LIGHT_COLOR) {
      cir.setReturnValue(Integer.valueOf(0));
    }
  }
}

package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.canoe.CanoeHelperClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forwards every action-bar update (overlay message) into the canoe helper. The helper decides
 * whether the message looks like a canoe speed bar and logs it.
 */
@Mixin(Gui.class)
public class CanoeGuiSetOverlayMessageMixin {

  @Inject(at = @At("HEAD"), method = "setOverlayMessage")
  private void imf$canoeOnSetOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
    CanoeHelperClient.get().onActionBar(message);
  }
}

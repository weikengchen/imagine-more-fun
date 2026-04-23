package com.chenweikeng.imf.nra.dailyplan;

import com.chenweikeng.imf.nra.dailyplan.ui.DailyPlanScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class DailyPlanKeybind {
  private static final KeyMapping.Category CATEGORY =
      KeyMapping.Category.register(Identifier.fromNamespaceAndPath("imaginemorefun", "main"));

  private static KeyMapping openPlan;

  private DailyPlanKeybind() {}

  public static void register() {
    openPlan =
        KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.imaginemorefun.open_rideplan",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY));
  }

  public static void tick() {
    if (openPlan == null) {
      return;
    }
    while (openPlan.consumeClick()) {
      Minecraft client = Minecraft.getInstance();
      if (client == null) {
        continue;
      }
      if (client.screen == null) {
        client.setScreen(new DailyPlanScreen(null));
      }
    }
  }
}

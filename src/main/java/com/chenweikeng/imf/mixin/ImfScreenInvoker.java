package com.chenweikeng.imf.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@link Screen}'s {@code protected static defaultHandleClickEvent} so non-mixin code can
 * route a {@link ClickEvent} through Minecraft's standard URL/click dispatcher (which pops the "Are
 * you sure you want to open this URL?" confirmation for {@code OpenUrl} actions). Used by the
 * alphabet-table doc-link clicks.
 */
@Mixin(Screen.class)
public interface ImfScreenInvoker {

  @Invoker("defaultHandleClickEvent")
  static void imf$defaultHandleClickEvent(ClickEvent event, Minecraft minecraft, Screen screen) {
    throw new AssertionError("ImfScreenInvoker mixin not applied");
  }
}

package com.chenweikeng.imf.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface PimScreenAccessor {
  @Accessor("font")
  Font pim$getFont();

  @Accessor("title")
  Component pim$getTitle();
}

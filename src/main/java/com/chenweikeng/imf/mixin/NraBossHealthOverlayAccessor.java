package com.chenweikeng.imf.mixin;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BossHealthOverlay.class)
public interface NraBossHealthOverlayAccessor {
  @Accessor("events")
  Map<UUID, LerpingBossEvent> getEvents();
}

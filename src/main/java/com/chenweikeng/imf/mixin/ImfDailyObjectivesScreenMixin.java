package com.chenweikeng.imf.mixin;

import com.chenweikeng.imf.nra.dailyplan.DailyQuest;
import com.chenweikeng.imf.nra.dailyplan.DailyQuestParser;
import com.chenweikeng.imf.nra.dailyplan.DailyQuestSnapshot;
import com.chenweikeng.imf.nra.dailyplan.DailyQuestState;
import com.chenweikeng.imf.nra.ride.RideCountManager;
import com.chenweikeng.imf.nra.ride.RideName;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the player's pending daily quests off the server's Daily Objectives chest GUI. Gated on
 * the same {@code 丳} title-marker the alphatable mixin uses, so we only fire on the Quest window.
 *
 * <p>The screen ships with empty slots on the very first tick after open, so capture runs from
 * {@code containerTick} and idles once a non-empty parse has succeeded for this screen instance.
 * Quest items live as {@code minecraft:diamond_shovel} stacks in the chest's first row (slots
 * 9–17), with the actual quest text in their {@link DataComponents#LORE LORE} component.
 */
@Mixin(AbstractContainerScreen.class)
public class ImfDailyObjectivesScreenMixin {

  private static final String TITLE_MARKER = "丳";
  private static final int FIRST_QUEST_SLOT = 9;
  private static final int LAST_QUEST_SLOT = 17;

  @Unique private boolean imf$dailyQuestCaptured;

  @Inject(at = @At("TAIL"), method = "containerTick")
  public void imf$captureDailyQuests(CallbackInfo ci) {
    if (imf$dailyQuestCaptured) {
      return;
    }
    AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
    Component title = self.getTitle();
    if (title == null || !title.getString().contains(TITLE_MARKER)) {
      return;
    }
    AbstractContainerMenu menu = self.getMenu();
    if (menu == null) {
      return;
    }
    int slotCount = menu.slots.size();
    if (slotCount <= LAST_QUEST_SLOT) {
      return;
    }

    List<DailyQuest> parsed = new ArrayList<>();
    boolean anyQuestSlotPopulated = false;
    for (int idx = FIRST_QUEST_SLOT; idx <= LAST_QUEST_SLOT; idx++) {
      Slot slot = menu.slots.get(idx);
      if (slot == null) {
        continue;
      }
      ItemStack stack = slot.getItem();
      if (stack.isEmpty() || !stack.is(Items.DIAMOND_SHOVEL)) {
        continue;
      }
      anyQuestSlotPopulated = true;
      ItemLore lore = stack.get(DataComponents.LORE);
      if (lore == null) {
        continue;
      }
      List<String> raw = new ArrayList<>();
      for (Component line : lore.lines()) {
        raw.add(line.getString());
      }
      Optional<DailyQuest> quest = DailyQuestParser.parse(raw);
      quest.ifPresent(parsed::add);
    }

    // Wait for the server to populate the chest — empty rows mean the open packet hasn't fully
    // landed yet. (A real "no quests" state still surfaces at least one Quest Info shovel.)
    if (!anyQuestSlotPopulated) {
      return;
    }

    DailyQuestSnapshot snap = new DailyQuestSnapshot();
    snap.capturedAtEpochMs = System.currentTimeMillis();
    snap.capturedDate = LocalDate.now().toString();
    snap.quests = parsed;
    RideCountManager counts = RideCountManager.getInstance();
    for (DailyQuest q : parsed) {
      RideName ride = RideName.fromMatchString(q.rideMatchName);
      snap.rideCountsAtCapture.put(q.rideMatchName, counts.getRideCount(ride));
    }
    DailyQuestState.getInstance().setSnapshot(snap);
    imf$dailyQuestCaptured = true;
  }
}

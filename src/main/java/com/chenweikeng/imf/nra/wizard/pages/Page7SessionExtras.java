package com.chenweikeng.imf.nra.wizard.pages;

import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.wizard.WizardPage;
import com.chenweikeng.imf.nra.wizard.layout.RenderBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Page7SessionExtras extends WizardPage {

  private static final List<RideName> SUGGESTED_ADVANCE_NOTICE_RIDES =
      List.of(
          RideName.BIG_THUNDER_MOUNTAIN_RAILROAD,
          RideName.CHIP_N_DALES_GADGET_COASTER,
          RideName.DISNEYLAND_MONORAIL,
          RideName.HEIMLICHS_CHEW_CHEW_TRAIN,
          RideName.INCREDICOASTER,
          RideName.INDIANA_JONES_ADVENTURE,
          RideName.MAIN_STREET_CARRIAGES,
          RideName.RADIATOR_SPRINGS_RACERS,
          RideName.RED_CAR_TROLLEY,
          RideName.SPLASH_MOUNTAIN,
          RideName.TOM_SAWYER_ISLAND_RAFTS);

  public Page7SessionExtras() {
    super(6);
  }

  @Override
  public Component getTitle() {
    return literal("Daily, Audio, Advance Notice");
  }

  @Override
  public List<RenderBlock> getBlocks(Minecraft client) {
    List<RenderBlock> blocks = new ArrayList<>();

    // Left column: Session Stats + OpenAudioMC
    List<RenderBlock> leftColumn = new ArrayList<>();

    leftColumn.add(text(sessionStatsSection()));
    leftColumn.add(spacer(5));
    Identifier sessionStatsImg =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/session-stats.png");
    leftColumn.add(image(sessionStatsImg, 228, 72));

    leftColumn.add(separator(20));

    leftColumn.add(text(openAudioMcSection()));
    leftColumn.add(spacer(5));
    Identifier openAudioImg =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/open-audio-mc.png");
    leftColumn.add(image(openAudioImg, 237, 90));

    // Right column: Advance Notice
    List<RenderBlock> rightColumn = new ArrayList<>();
    rightColumn.add(text(advanceNoticeHeader()));
    for (RideName ride : SUGGESTED_ADVANCE_NOTICE_RIDES) {
      rightColumn.add(text(advanceNoticeRideRow(ride)));
    }

    blocks.add(
        row(
            column(leftColumn.toArray(new RenderBlock[0])),
            column(rightColumn.toArray(new RenderBlock[0]))));

    return blocks;
  }

  private Component sessionStatsSection() {
    boolean isEnabled = ModConfig.currentSetting.showSessionStats;
    Component statusText =
        isEnabled
            ? colored("Enabled", ChatFormatting.GREEN)
            : colored("Disabled", ChatFormatting.RED);

    Component content = literal("");
    content = append(content, bold("Daily Session Stats"));
    content = append(content, literal("\n\n"));
    content =
        append(
            content,
            literal(
                "Display daily ride count, ride time, rides per hour, and streak in the bottom-right corner.\n\n"));
    content = append(content, literal("Status: "));
    content = append(content, statusText);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isEnabled, "showSessionStats"));
    return content;
  }

  private Component openAudioMcSection() {
    boolean isEnabled = ModConfig.currentSetting.enableOpenAudioMc;
    Component statusText =
        isEnabled
            ? colored("Enabled", ChatFormatting.GREEN)
            : colored("Disabled", ChatFormatting.RED);

    Component content = literal("");
    content = append(content, bold("Auto-Connect OpenAudioMC"));
    content = append(content, literal("\n\n"));
    content =
        append(
            content,
            literal(
                "Automatically connect to OpenAudioMC audio sessions using a headless browser, so you don't need to open a separate browser tab.\n\n"));
    content = append(content, literal("Status: "));
    content = append(content, statusText);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isEnabled, "enableOpenAudioMc"));
    return content;
  }

  private Component advanceNoticeHeader() {
    Component content = literal("");
    content = append(content, bold("Advance Notice"));
    content = append(content, literal("\n\n"));
    content =
        append(
            content,
            literal(
                "Play a sound before a ride ends, so you can get ready. Set the number of seconds before the ride ends (0 = disabled).\n"));
    return content;
  }

  private Component advanceNoticeRideRow(RideName ride) {
    int current = ModConfig.currentSetting.getAdvanceNoticeSeconds(ride);
    String displayValue = current == 0 ? "Off" : current + "s";

    Component content = literal("");
    content = append(content, literal("\n"));
    content = append(content, colored(ride.getDisplayName(), ChatFormatting.GOLD));
    content = append(content, literal(": "));
    content = append(content, colored(displayValue, ChatFormatting.GREEN));
    content = append(content, literal("  "));

    int min = Math.max(0, current - 1);
    int max = Math.min(30, current + 1);
    content =
        append(content, link("[-]", "config:advanceNotice:" + ride.toMatchString() + ":" + min));
    content = append(content, literal("  "));
    content =
        append(content, link("[+]", "config:advanceNotice:" + ride.toMatchString() + ":" + max));

    return content;
  }

  private Component isEnabledLink(boolean current, String configKey) {
    boolean newValue = !current;
    return link(newValue ? "Enable" : "Disable", "config:" + configKey + ":" + newValue);
  }

  private Component append(Component base, Component toAppend) {
    return base.copy().append(toAppend);
  }
}

package com.chenweikeng.imf.nra.wizard.pages;

import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.wizard.WizardPage;
import com.chenweikeng.imf.nra.wizard.layout.RenderBlock;
import com.chenweikeng.imf.nra.wizard.layout.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Page4UiHiding extends WizardPage {

  public Page4UiHiding() {
    super(3);
  }

  @Override
  public Component getTitle() {
    return literal("UI Hiding");
  }

  @Override
  public List<RenderBlock> getBlocks(Minecraft client) {
    List<RenderBlock> blocks = new ArrayList<>();

    List<RenderBlock> leftColumn = new ArrayList<>();
    leftColumn.add(text(intro()));
    leftColumn.add(separator(20));
    leftColumn.addAll(scoreboardSectionBlocks());
    leftColumn.add(separator(20));
    leftColumn.addAll(healthBarSectionBlocks());
    leftColumn.add(separator(20));
    leftColumn.addAll(chatSectionBlocks());

    List<RenderBlock> rightColumn = new ArrayList<>();
    rightColumn.addAll(xpLevelSectionBlocks());
    rightColumn.add(separator(20));
    rightColumn.addAll(nameTagSectionBlocks());
    rightColumn.add(separator(20));
    rightColumn.addAll(hotbarSectionBlocks());
    rightColumn.add(separator(20));
    rightColumn.addAll(lovePotionSectionBlocks());

    blocks.add(
        row(
            column(leftColumn.toArray(new RenderBlock[0])),
            column(rightColumn.toArray(new RenderBlock[0]))));

    return blocks;
  }

  private Component intro() {
    return literal("Hide UI elements for a cleaner experience while grinding rides.");
  }

  private List<RenderBlock> scoreboardSectionBlocks() {
    List<RenderBlock> blocks = new ArrayList<>();

    Component header = literal("");
    header = append(header, bold("Scoreboard"));
    blocks.add(text(header));
    blocks.add(spacer(10));

    boolean isHidden = ModConfig.currentSetting.hideScoreboard;
    Component statusText =
        isHidden ? colored("Hidden", ChatFormatting.GREEN) : colored("Visible", ChatFormatting.RED);

    Identifier imgScoreboard =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/scoreboard.png");

    List<RenderBlock> rowBlocks = new ArrayList<>();
    rowBlocks.add(image(imgScoreboard, 68, 93));

    Component content = Component.empty();
    content = append(content, literal("Hide scoreboard while tracking:\n"));
    content = append(content, statusText);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isHidden, "hideScoreboard", true));
    rowBlocks.add(text(content));

    blocks.add(row(VerticalAlignment.CENTER, rowBlocks.toArray(new RenderBlock[0])));

    return blocks;
  }

  private List<RenderBlock> chatSectionBlocks() {
    List<RenderBlock> blocks = new ArrayList<>();

    boolean isHidden = ModConfig.currentSetting.hideChat;

    Component content = literal("");
    content = append(content, bold("Chat"));
    content = append(content, literal("\n\n"));
    content = append(content, hideOptionStatus("Hide chat messages", isHidden));
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isHidden, "hideChat", true));
    blocks.add(text(content));

    return blocks;
  }

  private List<RenderBlock> healthBarSectionBlocks() {
    List<RenderBlock> blocks = new ArrayList<>();

    Component header = literal("");
    header = append(header, bold("Health Bar"));
    blocks.add(text(header));
    blocks.add(spacer(10));

    boolean isHidden = ModConfig.currentSetting.hideHealth;
    Component statusText =
        isHidden ? colored("Hidden", ChatFormatting.GREEN) : colored("Visible", ChatFormatting.RED);

    Identifier imgHealthBar =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/healthbar.png");

    List<RenderBlock> rowBlocks = new ArrayList<>();
    rowBlocks.add(image(imgHealthBar, 96, 43));

    Component content = literal("");
    content = append(content, literal("Hide health bar:\n"));
    content = append(content, statusText);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isHidden, "hideHealth", true));
    rowBlocks.add(text(content));

    blocks.add(row(VerticalAlignment.CENTER, rowBlocks.toArray(new RenderBlock[0])));

    return blocks;
  }

  private List<RenderBlock> nameTagSectionBlocks() {
    List<RenderBlock> blocks = new ArrayList<>();

    Component header = literal("");
    header = append(header, bold("Player Name Tags"));
    blocks.add(text(header));
    blocks.add(spacer(10));

    boolean isHidden = ModConfig.currentSetting.hideNameTag;
    Component statusText =
        isHidden ? colored("Hidden", ChatFormatting.GREEN) : colored("Visible", ChatFormatting.RED);

    Identifier imgNameTag =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/nametag.png");

    List<RenderBlock> rowBlocks = new ArrayList<>();
    rowBlocks.add(image(imgNameTag, 110, 50));

    Component content = Component.empty();
    content = append(content, literal("Hide player name tags:\n"));
    content = append(content, statusText);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isHidden, "hideNameTag", true));

    rowBlocks.add(text(content));
    blocks.add(row(VerticalAlignment.CENTER, rowBlocks.toArray(new RenderBlock[0])));

    return blocks;
  }

  private List<RenderBlock> hotbarSectionBlocks() {
    List<RenderBlock> blocks = new ArrayList<>();

    Component header = literal("");
    header = append(header, bold("Hotbar"));
    blocks.add(text(header));
    blocks.add(spacer(10));

    boolean isHidden = ModConfig.currentSetting.hideHotbar;
    Component statusText =
        isHidden ? colored("Hidden", ChatFormatting.GREEN) : colored("Visible", ChatFormatting.RED);

    Identifier imgHotbar =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/hotbar.png");

    List<RenderBlock> rowBlocks = new ArrayList<>();
    rowBlocks.add(image(imgHotbar, 185, 51));

    Component content = Component.empty();
    content = append(content, literal("Hide hotbar: "));
    content = append(content, statusText);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isHidden, "hideHotbar", true));

    rowBlocks.add(text(content));

    blocks.add(row(VerticalAlignment.CENTER, rowBlocks.toArray(new RenderBlock[0])));

    return blocks;
  }

  private List<RenderBlock> xpLevelSectionBlocks() {
    List<RenderBlock> blocks = new ArrayList<>();

    boolean isHidden = ModConfig.currentSetting.hideExperienceLevel;

    Component content = literal("");
    content = append(content, bold("XP Level"));
    content = append(content, literal("\n\n"));
    content = append(content, hideOptionStatus("Hide experience level number", isHidden));
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isHidden, "hideExperienceLevel", true));
    blocks.add(text(content));

    return blocks;
  }

  private List<RenderBlock> lovePotionSectionBlocks() {
    List<RenderBlock> blocks = new ArrayList<>();

    Component header = literal("");
    header = append(header, bold("Love Potion Messages"));
    blocks.add(text(header));
    blocks.add(spacer(10));

    boolean isHidden = ModConfig.currentSetting.hideLovePotionMessages;
    Component statusText =
        isHidden ? colored("Hidden", ChatFormatting.GREEN) : colored("Visible", ChatFormatting.RED);

    Identifier imgLovePotion =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/lovepotion.png");

    blocks.add(image(imgLovePotion, 355, 33));

    Component content = Component.empty();
    content = append(content, literal("Hide love potion system messages: "));
    content = append(content, statusText);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(isHidden, "hideLovePotionMessages", true));

    blocks.add(text(content));

    return blocks;
  }

  private Component hideOptionStatus(String description, boolean isHidden) {
    Component status =
        isHidden ? colored("Hidden", ChatFormatting.GREEN) : colored("Visible", ChatFormatting.RED);
    return Component.empty().append(description).append(literal(": ")).append(status);
  }

  private Component isEnabledLink(boolean current, String configKey, boolean invertLabel) {
    boolean newValue = !current;
    String label = (invertLabel ? (newValue ? "Hide" : "Show") : (newValue ? "Enable" : "Disable"));
    return link(label, "config:" + configKey + ":" + newValue);
  }

  private Component append(Component base, Component toAppend) {
    return base.copy().append(toAppend);
  }
}

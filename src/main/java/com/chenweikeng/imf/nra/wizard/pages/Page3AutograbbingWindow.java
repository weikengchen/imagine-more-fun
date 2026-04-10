package com.chenweikeng.imf.nra.wizard.pages;

import com.chenweikeng.imf.nra.config.CursorReleaseTiming;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.config.WindowMinimizeTiming;
import com.chenweikeng.imf.nra.wizard.WizardPage;
import com.chenweikeng.imf.nra.wizard.layout.RenderBlock;
import com.chenweikeng.imf.nra.wizard.layout.VerticalAlignment;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class Page3AutograbbingWindow extends WizardPage {

  public Page3AutograbbingWindow() {
    super(2);
  }

  @Override
  public Component getTitle() {
    return literal("Autograbbing & Window");
  }

  @Override
  public List<RenderBlock> getBlocks(Minecraft client) {
    List<RenderBlock> blocks = new java.util.ArrayList<>();

    List<RenderBlock> leftColumn = new java.util.ArrayList<>();
    leftColumn.addAll(introBlocks());
    leftColumn.add(separator(30));
    leftColumn.addAll(showAutograbRegionSectionBlocks());

    List<RenderBlock> rightColumn = new java.util.ArrayList<>();
    rightColumn.addAll(cursorWindowSectionBlocks());

    blocks.add(
        row(
            column(leftColumn.toArray(new RenderBlock[0])),
            column(rightColumn.toArray(new RenderBlock[0]))));

    return blocks;
  }

  private List<RenderBlock> introBlocks() {
    List<RenderBlock> blocks = new java.util.ArrayList<>();

    blocks.add(text(intro()));
    blocks.add(spacer(10));

    Identifier img =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/autograbdetection.png");
    blocks.add(image(img, 388, 25));

    return blocks;
  }

  private Component intro() {
    return literal("Automate cursor and window management while grinding rides.");
  }

  private List<RenderBlock> showAutograbRegionSectionBlocks() {
    List<RenderBlock> blocks = new java.util.ArrayList<>();

    boolean showRegions = ModConfig.currentSetting.showAutograbRegions;
    Component status =
        showRegions
            ? colored("Enabled", ChatFormatting.GREEN)
            : colored("Disabled", ChatFormatting.RED);

    Component header = literal("");
    header = append(header, bold("Show Autograb Regions"));
    header = append(header, literal("\n"));
    blocks.add(text(header));

    Identifier img =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/autograbregion.png");
    blocks.add(image(img, 221, 159));

    Component content = literal("");
    content =
        append(content, literal("Display visual outlines for autograb regions when nearby.\n\n"));
    content = append(content, literal("Show autograb regions? "));
    content = append(content, status);
    content = append(content, literal("  "));
    content = append(content, isEnabledLink(showRegions, "showAutograbRegions"));
    blocks.add(text(content));

    return blocks;
  }

  private List<RenderBlock> cursorWindowSectionBlocks() {
    List<RenderBlock> blocks = new java.util.ArrayList<>();

    Component cursorHeader = literal("");
    cursorHeader = append(cursorHeader, bold("Cursor Release Timing"));
    blocks.add(text(cursorHeader));
    blocks.add(spacer(10));

    Component cursorContent = literal("");
    cursorContent =
        append(cursorContent, literal("When to automatically release the mouse cursor.\n\n"));
    cursorContent = append(cursorContent, cursorTimingLinks());

    Identifier cursorImg =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/releasecursor.png");

    blocks.add(
        row(
            VerticalAlignment.CENTER,
            column(image(cursorImg, 123, 105)),
            column(text(cursorContent))));

    blocks.add(separator(20));

    Component windowHeader = literal("");
    windowHeader = append(windowHeader, bold("Window Minimize Timing"));
    blocks.add(text(windowHeader));
    blocks.add(spacer(20));

    Component windowContent = literal("");
    windowContent =
        append(windowContent, literal("When to automatically minimize the game window.\n\n"));
    windowContent = append(windowContent, windowTimingLinks());

    Identifier minimizeImg =
        Identifier.fromNamespaceAndPath("not-riding-alert", "textures/minimize.png");

    blocks.add(
        row(
            VerticalAlignment.CENTER,
            column(image(minimizeImg, 118, 112)),
            column(text(windowContent))));

    return blocks;
  }

  private Component cursorTimingLinks() {
    CursorReleaseTiming current = ModConfig.currentSetting.cursorReleaseTiming;

    Component content = Component.literal("");
    content =
        append(
            content, timingLink("Never", CursorReleaseTiming.NONE, current, "cursorReleaseTiming"));
    content = append(content, literal("\n"));
    content =
        append(
            content,
            timingLink(
                "When entering autograbbing area",
                CursorReleaseTiming.ON_ZONE_ENTRY,
                current,
                "cursorReleaseTiming"));
    content = append(content, literal("\n"));
    content =
        append(
            content,
            timingLink(
                "When ride starts",
                CursorReleaseTiming.ON_VEHICLE_MOUNT,
                current,
                "cursorReleaseTiming"));
    return content;
  }

  private Component windowTimingLinks() {
    WindowMinimizeTiming current = ModConfig.currentSetting.minimizeWindow;

    Component content = Component.literal("");
    content =
        append(content, timingLink("Never", WindowMinimizeTiming.NONE, current, "minimizeWindow"));
    content = append(content, literal("\n"));
    content =
        append(
            content,
            timingLink(
                "When entering autograbbing area",
                WindowMinimizeTiming.ON_ZONE_ENTRY,
                current,
                "minimizeWindow"));
    content = append(content, literal("\n"));
    content =
        append(
            content,
            timingLink(
                "When ride starts",
                WindowMinimizeTiming.ON_VEHICLE_MOUNT,
                current,
                "minimizeWindow"));
    return content;
  }

  private <T extends Enum<T>> Component timingLink(
      String label, T mode, T current, String configKey) {
    if (mode == current) {
      return colored(label, ChatFormatting.GREEN);
    }
    return link(label, "config:" + configKey + ":" + mode.name());
  }

  private Component append(Component base, Component toAppend) {
    return base.copy().append(toAppend);
  }

  private Component isEnabledLink(boolean current, String configKey) {
    boolean newValue = !current;
    return link(newValue ? "Enable" : "Disable", "config:" + configKey + ":" + newValue);
  }
}

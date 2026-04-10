package com.chenweikeng.imf.nra.wizard;

import com.chenweikeng.imf.nra.wizard.layout.ColumnBlock;
import com.chenweikeng.imf.nra.wizard.layout.FloatImageBlock;
import com.chenweikeng.imf.nra.wizard.layout.ImageBlock;
import com.chenweikeng.imf.nra.wizard.layout.RenderBlock;
import com.chenweikeng.imf.nra.wizard.layout.RowBlock;
import com.chenweikeng.imf.nra.wizard.layout.SeparatorBlock;
import com.chenweikeng.imf.nra.wizard.layout.SpacerBlock;
import com.chenweikeng.imf.nra.wizard.layout.TextBlock;
import com.chenweikeng.imf.nra.wizard.layout.VerticalAlignment;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public abstract class WizardPage {
  protected final int pageIndex;
  private static final String ACTION_PREFIX = "wizard_action:";

  protected WizardPage(int pageIndex) {
    this.pageIndex = pageIndex;
  }

  public abstract Component getTitle();

  public abstract List<RenderBlock> getBlocks(Minecraft client);

  public int getPageIndex() {
    return pageIndex;
  }

  public void onPageOpen(Minecraft client) {}

  public void onPageClose(Minecraft client) {}

  protected TextBlock text(String content) {
    return new TextBlock(Component.literal(content));
  }

  protected TextBlock text(Component content) {
    return new TextBlock(content);
  }

  protected ImageBlock image(Identifier texture, int width, int height) {
    return new ImageBlock(texture, width, height);
  }

  protected FloatImageBlock floatImage(
      Identifier texture, double widthPercent, boolean left, Component text) {
    return new FloatImageBlock(texture, widthPercent, left, text);
  }

  protected RowBlock row(RenderBlock... columns) {
    return new RowBlock(List.of(columns));
  }

  protected RowBlock row(VerticalAlignment alignment, RenderBlock... columns) {
    return new RowBlock(List.of(columns), alignment);
  }

  protected ColumnBlock column(RenderBlock... blocks) {
    return new ColumnBlock(List.of(blocks));
  }

  protected SpacerBlock spacer(int height) {
    return new SpacerBlock(height);
  }

  protected SeparatorBlock separator(int height) {
    return new SeparatorBlock(height);
  }

  protected Component link(String text, String action) {
    return link(text, action, ChatFormatting.AQUA);
  }

  protected Component link(String text, String action, ChatFormatting color) {
    return link(text, action, color, true);
  }

  protected Component link(String text, String action, ChatFormatting color, boolean underline) {
    String command = ACTION_PREFIX + action;
    ClickEvent clickEvent = new ClickEvent.RunCommand(command);
    HoverEvent hoverEvent =
        new HoverEvent.ShowText(Component.literal(getActionDescription(action)));

    Style linkStyle =
        Style.EMPTY
            .withColor(color)
            .withUnderlined(underline)
            .withClickEvent(clickEvent)
            .withHoverEvent(hoverEvent);

    return Component.literal(text).withStyle(linkStyle);
  }

  protected Component bold(String text) {
    return Component.literal(text).withStyle(Style.EMPTY.withBold(true));
  }

  protected Component italic(String text) {
    return Component.literal(text).withStyle(Style.EMPTY.withItalic(true));
  }

  protected Component colored(String text, ChatFormatting color) {
    return Component.literal(text).withStyle(color);
  }

  protected Component literal(String text) {
    return Component.literal(text);
  }

  protected Component newline() {
    return Component.literal("\n");
  }

  protected boolean readyToGoNext() {
    return true;
  }

  private String getActionDescription(String action) {
    if (action.startsWith("page:")) {
      return "Go to page " + action.substring(5);
    } else if (action.startsWith("config:")) {
      return "Change setting";
    } else if (action.equals("open:config")) {
      return "Open settings";
    } else if (action.equals("finish")) {
      return "Finish tutorial";
    }
    return action;
  }
}

package com.chenweikeng.imf.nra.wizard;

import com.chenweikeng.imf.nra.wizard.pages.*;
import java.util.List;

public final class TutorialPages {
  private static final List<WizardPage> PAGES =
      List.of(
          new Page1AlertSettings(),
          new Page2VisualPreferences(),
          new Page3AutograbbingWindow(),
          new Page4UiHiding(),
          new Page5RideSelection(),
          new Page6TrackerSettings(),
          new Page7SessionExtras(),
          new Page7Profiles());

  private TutorialPages() {}

  public static WizardPage getPage(int index) {
    if (index < 0 || index >= PAGES.size()) {
      return null;
    }
    return PAGES.get(index);
  }

  public static int getPageCount() {
    return PAGES.size();
  }

  public static boolean hasPages() {
    return !PAGES.isEmpty();
  }
}

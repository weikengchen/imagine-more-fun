package com.chenweikeng.imf.nra.wizard;

public enum TutorialState {
  NOT_STARTED(-1),
  PAGE_1(0),
  PAGE_2(1),
  PAGE_3(2),
  PAGE_4(3),
  PAGE_5(4),
  FINISHED(-1);

  private final int pageIndex;

  TutorialState(int pageIndex) {
    this.pageIndex = pageIndex;
  }

  public int getPageIndex() {
    return pageIndex;
  }

  public boolean isFinished() {
    return this == FINISHED;
  }

  public boolean isNotStarted() {
    return this == NOT_STARTED;
  }

  public boolean isActive() {
    return pageIndex >= 0 && !isFinished();
  }

  public TutorialState getNext() {
    return switch (this) {
      case NOT_STARTED -> PAGE_1;
      case PAGE_1 -> PAGE_2;
      case PAGE_2 -> PAGE_3;
      case PAGE_3 -> PAGE_4;
      case PAGE_4 -> PAGE_5;
      case PAGE_5, FINISHED -> FINISHED;
    };
  }

  public TutorialState getPrevious() {
    return switch (this) {
      case NOT_STARTED -> NOT_STARTED;
      case PAGE_1 -> NOT_STARTED;
      case PAGE_2 -> PAGE_1;
      case PAGE_3 -> PAGE_2;
      case PAGE_4 -> PAGE_3;
      case PAGE_5 -> PAGE_4;
      case FINISHED -> FINISHED;
    };
  }

  public static TutorialState fromPageIndex(int index) {
    for (TutorialState state : values()) {
      if (state.pageIndex == index) {
        return state;
      }
    }
    return NOT_STARTED;
  }
}

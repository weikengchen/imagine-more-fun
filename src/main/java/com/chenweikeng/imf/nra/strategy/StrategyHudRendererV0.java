package com.chenweikeng.imf.nra.strategy;

import com.chenweikeng.imf.nra.GameState;
import com.chenweikeng.imf.nra.NotRidingAlertClient;
import com.chenweikeng.imf.nra.Timing;
import com.chenweikeng.imf.nra.config.ClosestRideMode;
import com.chenweikeng.imf.nra.config.ModConfig;
import com.chenweikeng.imf.nra.config.SortingRules;
import com.chenweikeng.imf.nra.ride.AutograbHolder;
import com.chenweikeng.imf.nra.ride.ClosestRideHolder;
import com.chenweikeng.imf.nra.ride.CurrentRideHolder;
import com.chenweikeng.imf.nra.ride.RideName;
import com.chenweikeng.imf.nra.util.TimeFormatUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Handles rendering of strategy recommendations on the HUD. */
public class StrategyHudRendererV0 {
  private static List<RideGoal> topGoals = new ArrayList<>();
  private static int updateCounter = 0;
  private static final int UPDATE_INTERVAL_TICKS = Timing.HUD_UPDATE_INTERVAL_TICKS;
  private static String currentError = null; // Stores the latest error message

  private record LayoutInput(
      Minecraft client,
      List<RideGoal> goals,
      RideName currentRide,
      RideName autograbRide,
      RideName closestRide,
      RideName effectiveRide,
      boolean currentRideInTop,
      boolean isPassenger,
      String error,
      int availableWidth,
      int gap) {}

  private record LayoutDecision(boolean useShortNames, boolean twoColumns, boolean visible) {}

  private enum RideStatus {
    NORMAL,
    RIDING,
    AUTOGRABBING,
    CLOSEST
  }

  private static class FormattedRide {
    private final String name;
    private final RideStatus status;

    FormattedRide(String name, RideStatus status) {
      this.name = name;
      this.status = status;
    }

    String getName() {
      return name;
    }

    RideStatus getStatus() {
      return status;
    }
  }

  /** Updates the top goals to display. Should be called periodically. */
  public static void update() {
    updateCounter++;
    if (updateCounter >= UPDATE_INTERVAL_TICKS) {
      updateCounter = 0;
      int displayCount = ModConfig.currentSetting.rideDisplayCount;
      topGoals = StrategyCalculator.getTopGoals(displayCount);
    }
  }

  /**
   * Sets an error message to display on the HUD.
   *
   * @param error The error message (null to clear)
   */
  public static void setError(String error) {
    currentError = error;
  }

  /** Gets the current error message. */
  public static String getError() {
    return currentError;
  }

  /**
   * Renders the strategy recommendations on the HUD.
   *
   * @param context The GUI graphics context
   */
  public static void render(GuiGraphics context, DeltaTracker tickCounter) {
    if (!NotRidingAlertClient.isImagineFunServer()) {
      return;
    }

    Minecraft client = Minecraft.getInstance();
    if (client == null || client.player == null || client.font == null) {
      return;
    }

    update();

    int screenWidth = client.getWindow().getGuiScaledWidth();
    int xLeft = 50;
    int xRight = screenWidth - 50;
    int yStart = 50;
    int lineHeight = 10;
    int colorNormal = ModConfig.currentSetting.trackerNormalColor;
    int colorRiding = ModConfig.currentSetting.trackerRidingColor;
    int colorAutograbbing = ModConfig.currentSetting.trackerAutograbbingColor;
    int errorColor = ModConfig.currentSetting.trackerErrorColor;
    int colorClosest = ModConfig.currentSetting.trackerClosestRideColor;

    int displayCount = ModConfig.currentSetting.rideDisplayCount;

    RideName currentRide = CurrentRideHolder.getCurrentRide();
    RideName autograbRide = AutograbHolder.getRideAtLocation(client);
    RideName closestRide = filterClosestRide(ClosestRideHolder.getClosestRide());
    RideName effectiveRide =
        currentRide != null ? currentRide : (autograbRide != null ? autograbRide : closestRide);
    boolean currentRideInTop =
        effectiveRide != null && topGoals.stream().anyMatch(g -> g.getRide() == effectiveRide);
    boolean isPassengerForLayout = GameState.getInstance().isValidPassenger(client.player);

    int topGoalsSize = topGoals.size();
    boolean wantsTwoColumns = topGoalsSize >= 8;
    int availableWidth = xRight - xLeft;
    int gap = 10;
    List<RideGoal> goalsForFit = displayCount > 0 ? topGoals : List.of();

    LayoutInput layoutInput =
        new LayoutInput(
            client,
            goalsForFit,
            currentRide,
            autograbRide,
            closestRide,
            effectiveRide,
            currentRideInTop,
            isPassengerForLayout,
            currentError,
            availableWidth,
            gap);
    LayoutDecision decision =
        decideLayout(layoutInput, ModConfig.currentSetting.displayShortName, wantsTwoColumns);
    if (!decision.visible) {
      return;
    }
    boolean useShortNames = decision.useShortNames;
    boolean twoColumns = decision.twoColumns;
    boolean isPassenger = GameState.getInstance().isValidPassenger(client.player);

    List<RideGoal> leftGoals;
    List<RideGoal> rightGoals;
    if (displayCount > 0) {
      if (!twoColumns) {
        leftGoals = topGoals;
        rightGoals = List.of();
      } else {
        int leftCount = (topGoalsSize + 1) / 2;
        leftGoals = topGoals.subList(0, Math.min(leftCount, topGoalsSize));
        rightGoals =
            topGoalsSize > leftCount ? topGoals.subList(leftCount, topGoalsSize) : List.of();
      }
    } else {
      leftGoals = List.of();
      rightGoals = List.of();
    }

    int maxColumnHeight = Math.max(leftGoals.size(), rightGoals.size());

    boolean hasError = currentError != null && !currentError.isEmpty();
    boolean hasExtraRide =
        effectiveRide != null && effectiveRide != RideName.UNKNOWN && !currentRideInTop;

    int maxWidth = 0;
    if (hasError) {
      maxWidth = Math.max(maxWidth, client.font.width("ERROR: " + currentError));
    }
    for (RideGoal goal : leftGoals) {
      FormattedRide formattedRide =
          formatRideName(
              goal.getRide(), currentRide, autograbRide, closestRide, useShortNames, isPassenger);
      String text = formatGoalText(formattedRide, goal, ModConfig.currentSetting.sortingRules);
      maxWidth = Math.max(maxWidth, client.font.width(text));
    }
    if (twoColumns) {
      for (RideGoal goal : rightGoals) {
        FormattedRide formattedRide =
            formatRideName(
                goal.getRide(), currentRide, autograbRide, closestRide, useShortNames, isPassenger);
        String text = formatGoalText(formattedRide, goal, ModConfig.currentSetting.sortingRules);
        maxWidth = Math.max(maxWidth, client.font.width(text));
      }
    }
    if (hasExtraRide) {
      RideGoal currentGoal = StrategyCalculator.getGoalForRide(effectiveRide);
      FormattedRide formattedRide =
          formatRideName(
              effectiveRide, currentRide, autograbRide, closestRide, useShortNames, isPassenger);
      String text;
      if (currentGoal != null) {
        text = formatGoalText(formattedRide, currentGoal, ModConfig.currentSetting.sortingRules);
      } else if (formattedRide.getStatus() == RideStatus.RIDING) {
        text = "Riding: " + formattedRide.getName();
      } else {
        text = formattedRide.getName();
      }
      maxWidth = Math.max(maxWidth, client.font.width(text));
    }

    int y = yStart;

    if (hasError) {
      context.drawString(client.font, "ERROR: " + currentError, xLeft, y, errorColor, false);
      y += lineHeight;
    }

    if (displayCount > 0) {
      for (int i = 0; i < leftGoals.size(); i++) {
        RideGoal goal = leftGoals.get(i);
        FormattedRide formattedRide =
            formatRideName(
                goal.getRide(), currentRide, autograbRide, closestRide, useShortNames, isPassenger);
        String text = formatGoalText(formattedRide, goal, ModConfig.currentSetting.sortingRules);
        int color =
            getColorForStatus(
                formattedRide.getStatus(),
                colorNormal,
                colorRiding,
                colorAutograbbing,
                colorClosest);
        context.drawString(client.font, text, xLeft, y + (i * lineHeight), color, false);
      }

      if (twoColumns) {
        for (int i = 0; i < rightGoals.size(); i++) {
          RideGoal goal = rightGoals.get(i);
          FormattedRide formattedRide =
              formatRideName(
                  goal.getRide(),
                  currentRide,
                  autograbRide,
                  closestRide,
                  useShortNames,
                  isPassenger);
          String text = formatGoalText(formattedRide, goal, ModConfig.currentSetting.sortingRules);
          int color =
              getColorForStatus(
                  formattedRide.getStatus(),
                  colorNormal,
                  colorRiding,
                  colorAutograbbing,
                  colorClosest);
          int textWidth = client.font.width(text);
          context.drawString(
              client.font, text, xRight - textWidth, y + (i * lineHeight), color, false);
        }
      }
    }

    if (hasExtraRide) {
      int extraY = yStart + ((hasError ? 1 : 0) + maxColumnHeight + 1) * lineHeight;
      RideGoal currentGoal = StrategyCalculator.getGoalForRide(effectiveRide);
      FormattedRide formattedRide =
          formatRideName(
              effectiveRide, currentRide, autograbRide, closestRide, useShortNames, isPassenger);
      String text;
      if (currentGoal != null) {
        text = formatGoalText(formattedRide, currentGoal, ModConfig.currentSetting.sortingRules);
      } else if (formattedRide.getStatus() == RideStatus.RIDING) {
        text = "Riding: " + formattedRide.getName();
      } else {
        text = formattedRide.getName();
      }
      int color =
          getColorForStatus(
              formattedRide.getStatus(), colorNormal, colorRiding, colorAutograbbing, colorClosest);
      context.drawString(client.font, text, xLeft, extraY, color, false);
    }
  }

  public static List<RideGoal> getTopGoals() {
    return new ArrayList<>(topGoals);
  }

  private static RideName filterClosestRide(RideName ride) {
    if (ride == null) {
      return null;
    }
    ClosestRideMode mode = ModConfig.currentSetting.closestRideMode;
    if (mode == ClosestRideMode.NEVER) {
      return null;
    }
    if (mode == ClosestRideMode.ONLY_IN_PROGRESS) {
      RideGoal goal = StrategyCalculator.getGoalForRide(ride);
      if (goal == null) {
        return null;
      }
    }
    return ride;
  }

  private static String getAnimatedDots() {
    long currentTimeMillis = System.currentTimeMillis();
    int quarterSecond = (int) ((currentTimeMillis % 2000) / 500);
    return switch (quarterSecond) {
      case 0 -> "";
      case 1 -> ".";
      case 2 -> "..";
      case 3 -> "...";
      default -> "";
    };
  }

  private static FormattedRide formatRideName(
      RideName ride,
      RideName currentRide,
      RideName autograbRide,
      RideName closestRide,
      boolean useShortNames,
      boolean isPassenger) {
    String rideName = useShortNames ? ride.getShortName() : ride.getDisplayName();
    RideStatus status = RideStatus.NORMAL;

    if (currentRide != null && ride == currentRide) {
      Integer elapsed = CurrentRideHolder.getElapsedSeconds();
      Integer progress = CurrentRideHolder.getCurrentProgressPercent();
      if (elapsed != null && progress != null) {
        int totalSeconds = ride.getRideTime();
        int remainingSeconds = totalSeconds - elapsed;
        if (remainingSeconds < 0) {
          remainingSeconds = 0;
        }
        String timeLeft = TimeFormatUtil.formatDuration(remainingSeconds);
        rideName += " (" + progress + "%, " + timeLeft + " left)";
      }
      status = RideStatus.RIDING;
    } else if (currentRide == null
        && autograbRide != null
        && ride == autograbRide
        && !isPassenger) {
      rideName += " (Autograbbing" + getAnimatedDots() + ")";
      status = RideStatus.AUTOGRABBING;
    } else if (currentRide == null
        && autograbRide == null
        && closestRide != null
        && ride == closestRide) {
      rideName += " (Closest)";
      status = RideStatus.CLOSEST;
    }

    return new FormattedRide(rideName, status);
  }

  private static String formatGoalText(
      FormattedRide formattedRide, RideGoal goal, SortingRules sortingRules) {
    int ridesNeeded;
    long timeNeeded;
    if (sortingRules == SortingRules.TOTAL_TIME_ASC
        || sortingRules == SortingRules.TOTAL_TIME_DESC) {
      ridesNeeded = goal.getMaxRidesNeeded();
      timeNeeded = goal.getMaxTimeNeeded();
    } else {
      ridesNeeded = goal.getNextGoalRidesNeeded();
      timeNeeded = goal.getNextGoalTimeNeeded();
    }
    if (formattedRide.getStatus() == RideStatus.CLOSEST && ridesNeeded == 0) {
      return formattedRide.getName();
    }
    return String.format(
        "%s - %d more, %s",
        formattedRide.getName(), ridesNeeded, TimeFormatUtil.formatDuration(timeNeeded));
  }

  private static LayoutDecision decideLayout(
      LayoutInput layoutInput, boolean baseUseShortNames, boolean wantsTwoColumns) {
    boolean useShortNames = baseUseShortNames;
    boolean twoColumns = wantsTwoColumns;
    if (fitsLayout(layoutInput, useShortNames, twoColumns)) {
      return new LayoutDecision(useShortNames, twoColumns, true);
    }
    useShortNames = true;
    if (fitsLayout(layoutInput, useShortNames, twoColumns)) {
      return new LayoutDecision(useShortNames, twoColumns, true);
    }
    twoColumns = false;
    if (fitsLayout(layoutInput, useShortNames, twoColumns)) {
      return new LayoutDecision(useShortNames, twoColumns, true);
    }
    return new LayoutDecision(useShortNames, twoColumns, false);
  }

  private static boolean fitsLayout(
      LayoutInput layoutInput, boolean useShortNames, boolean twoColumns) {
    int maxWidth = 0;
    if (layoutInput.error != null && !layoutInput.error.isEmpty()) {
      maxWidth = Math.max(maxWidth, layoutInput.client.font.width("ERROR: " + layoutInput.error));
    }

    if (!layoutInput.goals.isEmpty()) {
      int goalsSize = layoutInput.goals.size();
      if (!twoColumns || goalsSize < 8) {
        int leftMax = computeMaxWidth(layoutInput, layoutInput.goals, useShortNames);
        maxWidth = Math.max(maxWidth, leftMax);
      } else {
        int leftCount = (goalsSize + 1) / 2;
        List<RideGoal> leftGoals = layoutInput.goals.subList(0, Math.min(leftCount, goalsSize));
        List<RideGoal> rightGoals =
            goalsSize > leftCount ? layoutInput.goals.subList(leftCount, goalsSize) : List.of();

        int leftMax = computeMaxWidth(layoutInput, leftGoals, useShortNames);
        int rightMax = computeMaxWidth(layoutInput, rightGoals, useShortNames);
        if (leftMax + rightMax + layoutInput.gap > layoutInput.availableWidth) {
          return false;
        }
        maxWidth = Math.max(maxWidth, leftMax);
        maxWidth = Math.max(maxWidth, rightMax);
      }
    }

    if (layoutInput.effectiveRide != null
        && layoutInput.effectiveRide != RideName.UNKNOWN
        && !layoutInput.currentRideInTop) {
      RideGoal currentGoal = StrategyCalculator.getGoalForRide(layoutInput.effectiveRide);
      FormattedRide formattedRide =
          formatRideName(
              layoutInput.effectiveRide,
              layoutInput.currentRide,
              layoutInput.autograbRide,
              layoutInput.closestRide,
              useShortNames,
              layoutInput.isPassenger);
      String text;
      if (currentGoal != null) {
        text = formatGoalText(formattedRide, currentGoal, ModConfig.currentSetting.sortingRules);
      } else if (formattedRide.getStatus() == RideStatus.RIDING) {
        text = "Riding: " + formattedRide.getName();
      } else {
        text = formattedRide.getName();
      }
      maxWidth = Math.max(maxWidth, layoutInput.client.font.width(text) / 2);
    }

    return maxWidth <= layoutInput.availableWidth;
  }

  private static int computeMaxWidth(
      LayoutInput layoutInput, List<RideGoal> goals, boolean useShortNames) {
    int max = 0;
    for (RideGoal goal : goals) {
      FormattedRide formattedRide =
          formatRideName(
              goal.getRide(),
              layoutInput.currentRide,
              layoutInput.autograbRide,
              layoutInput.closestRide,
              useShortNames,
              layoutInput.isPassenger);
      String text = formatGoalText(formattedRide, goal, ModConfig.currentSetting.sortingRules);
      max = Math.max(max, layoutInput.client.font.width(text));
    }
    return max;
  }

  private static int getColorForStatus(
      RideStatus status,
      int colorNormal,
      int colorRiding,
      int colorAutograbbing,
      int colorClosest) {
    return switch (status) {
      case RIDING -> colorRiding;
      case AUTOGRABBING -> colorAutograbbing;
      case CLOSEST -> colorClosest;
      case NORMAL -> colorNormal;
    };
  }
}

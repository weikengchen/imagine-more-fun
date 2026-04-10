package com.chenweikeng.imf.nra;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;

public class GameState {
  private static final GameState INSTANCE = new GameState();

  private long absoluteTickCounter = 0;
  private boolean riding = false;
  private boolean automaticallyReleasedCursor = false;
  private long windowRestoreGraceUntilTick = -1;
  private volatile boolean monkeyAttached = false;
  private long lastSitCommand = -400;
  private boolean sitting = false;
  private boolean autograbFailureActive = false;
  private boolean rubberBandActive = false;
  private double rubberBandX;
  private double rubberBandY;
  private double rubberBandZ;
  private long rubberBandUntilTick = -1;

  private GameState() {}

  public static GameState getInstance() {
    return INSTANCE;
  }

  public long getAbsoluteTickCounter() {
    return absoluteTickCounter;
  }

  public void incrementTickCounter() {
    absoluteTickCounter++;
  }

  public boolean isRiding() {
    return riding;
  }

  public void setRiding(boolean riding) {
    this.riding = riding;
  }

  public boolean isAutomaticallyReleasedCursor() {
    return automaticallyReleasedCursor;
  }

  public void setAutomaticallyReleasedCursor(boolean released) {
    this.automaticallyReleasedCursor = released;
  }

  /** Suppress pause-on-focus-loss until this tick, giving GLFW time to process window focus. */
  public void setWindowRestoreGrace(int ticks) {
    this.windowRestoreGraceUntilTick = absoluteTickCounter + ticks;
  }

  public boolean isWithinWindowRestoreGrace() {
    return absoluteTickCounter <= windowRestoreGraceUntilTick;
  }

  public boolean isMonkeyAttached() {
    return monkeyAttached;
  }

  public void setMonkeyAttached(boolean attached) {
    this.monkeyAttached = attached;
  }

  public void setLastSitCommand() {
    this.lastSitCommand = absoluteTickCounter;
  }

  public boolean isSitting() {
    return sitting;
  }

  public void setSitting(boolean sitting) {
    this.sitting = sitting;
  }

  public void updateSittingState(boolean wasPassenger, boolean isPassenger) {
    if (!wasPassenger && isPassenger && (absoluteTickCounter - lastSitCommand < 400)) {
      sitting = true;
    }
  }

  public void clearSittingIfNotPassenger(boolean isPassenger) {
    if (!isPassenger) {
      sitting = false;
    }
  }

  public boolean isAutograbFailureActive() {
    return autograbFailureActive;
  }

  public void setAutograbFailureActive(boolean active) {
    this.autograbFailureActive = active;
  }

  public boolean isRubberBandActive() {
    return rubberBandActive;
  }

  public double getRubberBandX() {
    return rubberBandX;
  }

  public double getRubberBandY() {
    return rubberBandY;
  }

  public double getRubberBandZ() {
    return rubberBandZ;
  }

  public long getRubberBandUntilTick() {
    return rubberBandUntilTick;
  }

  public void armRubberBand(double x, double y, double z, long untilTick) {
    this.rubberBandActive = true;
    this.rubberBandX = x;
    this.rubberBandY = y;
    this.rubberBandZ = z;
    this.rubberBandUntilTick = untilTick;
  }

  public void updateRubberBandAnchor(double x, double y, double z) {
    this.rubberBandX = x;
    this.rubberBandY = y;
    this.rubberBandZ = z;
  }

  public void clearRubberBand() {
    this.rubberBandActive = false;
    this.rubberBandUntilTick = -1;
  }

  public boolean isValidPassenger(LocalPlayer player) {
    if (player == null) {
      return false;
    }

    if (sitting) {
      return false;
    }

    if (!player.isPassenger()) {
      return false;
    }

    if (!(player.getVehicle() instanceof ArmorStand)) {
      return false;
    }

    return true;
  }

  public void reset() {
    absoluteTickCounter = 0;
    riding = false;
    automaticallyReleasedCursor = false;
    windowRestoreGraceUntilTick = -1;
    lastSitCommand = -400;
    sitting = false;
    autograbFailureActive = false;
    rubberBandActive = false;
    rubberBandUntilTick = -1;
  }
}

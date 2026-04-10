package com.chenweikeng.imf.pim.trader;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class PinTrader {
  private final BlockPos npcPosition;
  private final Vec3 npcVec3;
  private final PinTraderLocation location;

  public PinTrader(int x, int y, int z, PinTraderLocation location) {
    this.npcPosition = new BlockPos(x, y, z);
    this.npcVec3 = new Vec3(x + 0.5, y, z + 0.5);
    this.location = location;
  }

  public PinTrader(BlockPos npcPosition, PinTraderLocation location) {
    this.npcPosition = npcPosition;
    this.npcVec3 = new Vec3(npcPosition.getX() + 0.5, npcPosition.getY(), npcPosition.getZ() + 0.5);
    this.location = location;
  }

  public BlockPos getNpcPosition() {
    return npcPosition;
  }

  public Vec3 getNpcVec3() {
    return npcVec3;
  }

  public PinTraderLocation getLocation() {
    return location;
  }
}

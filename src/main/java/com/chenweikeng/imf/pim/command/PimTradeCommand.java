package com.chenweikeng.imf.pim.command;

import com.chenweikeng.imf.pim.PimClient;
import com.chenweikeng.imf.pim.PimState;
import com.chenweikeng.imf.pim.tracker.BossBarTracker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class PimTradeCommand {

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:trade")
            .requires(src -> PimClient.isImagineFunServer())
            .executes(
                context -> {
                  boolean newState = !PimState.isEnabled();
                  PimState.setEnabled(newState);

                  if (newState) {
                    PimState.resetWarpPoint();
                    context
                        .getSource()
                        .sendFeedback(
                            Component.literal(
                                "§6✨ §e[Pim] §fUse the IFone to warp to the first pin trader."));
                  } else {
                    BossBarTracker.getInstance().disable();
                    context
                        .getSource()
                        .sendFeedback(
                            Component.literal("§6✨ §e[Pim] §fPin trading has been stopped."));
                  }
                  return 1;
                }));
  }
}

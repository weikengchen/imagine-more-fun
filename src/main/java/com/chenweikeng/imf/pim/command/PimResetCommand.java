package com.chenweikeng.imf.pim.command;

import com.chenweikeng.imf.pim.PimClient;
import com.chenweikeng.imf.pim.screen.PinBookHandler;
import com.chenweikeng.imf.pim.screen.PinDetailHandler;
import com.chenweikeng.imf.pim.screen.PinRarityHandler;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class PimResetCommand {

  public static void register(
      com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher) {
    dispatcher.register(
        ClientCommandManager.literal("pim:reset")
            .requires(src -> PimClient.isImagineFunServer())
            .executes(
                context -> {
                  PinRarityHandler.getInstance().reset();
                  PinBookHandler.getInstance().reset();
                  PinDetailHandler.getInstance().reset();
                  PimFmvCommand.resetCache();
                  context
                      .getSource()
                      .sendFeedback(
                          Component.literal(
                              "§6✨ §e[IMF] §fAll pin data has been reset successfully."));
                  return 1;
                }));
  }
}

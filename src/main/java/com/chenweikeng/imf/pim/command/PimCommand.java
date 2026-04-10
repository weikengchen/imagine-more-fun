package com.chenweikeng.imf.pim.command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class PimCommand {

  public static void registerCommands() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) -> {
          // Register all sub-commands
          PimTradeCommand.register(dispatcher);
          PimResetCommand.register(dispatcher);
          PimComputeCommand.register(dispatcher);
          PimExportCommand.register(dispatcher);
          PimValueCommand.register(dispatcher);
          PimFmvCommand.register(dispatcher);
        });
  }
}

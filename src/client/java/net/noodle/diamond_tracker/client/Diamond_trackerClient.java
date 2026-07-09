package net.noodle.diamond_tracker.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;

public class Diamond_trackerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DiamondInventoryTracker.loadData();
        DiamondHudRenderer.register();




        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("diamondtracker")
                    .then(ClientCommands.literal("reset")
                            // Command handler for resetting EVERYTHING: /diamondtracker reset
                            .executes(context -> {
                                DiamondInventoryTracker.currentInventoryDiamonds = 0;
                                DiamondInventoryTracker.cachedEnderChestDiamonds = 0;
                                DiamondInventoryTracker.cachedMiscDiamonds = 0;
                                DiamondInventoryTracker.trackedChestCoords.clear();

                                // Commit the wiped variables straight to the save file
                                DiamondInventoryTracker.saveData();

                                context.getSource().sendFeedback(Component.literal("§a[Diamond Tracker] All diamond totals and tracked chests have been reset!"));
                                return 1; // Success return status code
                            })
                            // Subcommand for resetting ONLY misc chests: /diamondtracker reset misc
                            .then(ClientCommands.literal("misc")
                                    .executes(context -> {
                                        DiamondInventoryTracker.cachedMiscDiamonds = 0;
                                        DiamondInventoryTracker.trackedChestCoords.clear();
                                        DiamondInventoryTracker.saveData();

                                        context.getSource().sendFeedback(Component.literal("§a[Diamond Tracker] Misc chest counts and toggles have been cleared!"));
                                        return 1;
                                    })
                            )
                     )
            );
        });
    }
}

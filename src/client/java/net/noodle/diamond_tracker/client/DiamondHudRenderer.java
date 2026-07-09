package net.noodle.diamond_tracker.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

public class DiamondHudRenderer {
    private static ItemStack diamondStack = null;
    private static ItemStack closedEnderChestStack = null;
    private static ItemStack regularChestStack = null; // New icon wrapper for Misc Diamonds!

    private static final Identifier OPEN_ENDER_CHEST_TEXTURE = Identifier.fromNamespaceAndPath("diamond_tracker","textures/gui/open_ender_chest.png");
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath("diamond_tracker", "display");

    public static void register() {
        HudElementRegistry.addLast(ELEMENT_ID, (guiGraphics, deltaTracker) -> {
            Minecraft client = Minecraft.getInstance();

            if (!(guiGraphics instanceof GuiGraphicsExtractor extractor)) {
                return;
            }

            if (client.player == null || client.gui.hud.isHidden() || client.getDebugOverlay().showDebugScreen()) {
                return;
            }

            // --- CRITICAL FIX: PUT PURPOSE BACK ---
            // This runs your newly restored loop to dynamically scan the pocket inventory every frame!
            DiamondInventoryTracker.scanPlayerInventory();

            // Safe lazy initializations
            if (diamondStack == null) diamondStack = new ItemStack(Items.DIAMOND);
            if (closedEnderChestStack == null) closedEnderChestStack = new ItemStack(Items.ENDER_CHEST);
            if (regularChestStack == null) regularChestStack = new ItemStack(Items.CHEST); // Normal chest icon

            // --- LIVE SCREEN CHECK ---
            boolean isEnderChestCurrentlyOpen = false;
            if (client.gui.screen() instanceof AbstractContainerScreen<?> containerScreen) {
                if (containerScreen.getMenu() instanceof ChestMenu chestMenu && chestMenu.slots.size() == 63) {
                    String screenTitle = containerScreen.getTitle().getString().toLowerCase();
                    if (screenTitle.contains("ender")) {
                        isEnderChestCurrentlyOpen = true;
                    }
                }
            }

            int x = 10;
            int y = 10;

            // DRAW LAYER 1: Icons
            extractor.nextStratum();

            // 1. Player Inventory Diamond Icon
            extractor.fakeItem(diamondStack, x, y);

            // 2. Ender Chest Tracking Icon (Swaps when opened)
            if (isEnderChestCurrentlyOpen) {
                extractor.blit(
                        RenderPipelines.GUI_TEXTURED,
                        OPEN_ENDER_CHEST_TEXTURE,
                        x + 98,
                        y - 5,
                        0.0F,
                        0.0F,
                        22,
                        22,
                        22,
                        22
                );
            } else {
                extractor.fakeItem(closedEnderChestStack, x + 100, y);
            }

            // 3. New Misc Diamonds Chest Icon (Shifted over to the right by 200 pixels)
            extractor.fakeItem(regularChestStack, x + 200, y);

            // DRAW LAYER 2: Text
            extractor.nextStratum();

            int inventoryCount = DiamondInventoryTracker.currentInventoryDiamonds;
            int enderCount = DiamondInventoryTracker.cachedEnderChestDiamonds;
            int miscCount = DiamondInventoryTracker.cachedMiscDiamonds; // Grab our new toggle pool numbers!

            var textCollector = extractor.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

            // Render text strings matching up to each structural layout item
            textCollector.accept(x + 20, y + 4, Component.literal("x " + inventoryCount));
            textCollector.accept(x + 120, y + 4, Component.literal("x " + enderCount));
            textCollector.accept(x + 220, y + 4, Component.literal("x " + miscCount)); // Displays Misc Pool!
        });
    }
}
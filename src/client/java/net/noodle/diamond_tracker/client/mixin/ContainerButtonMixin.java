package net.noodle.diamond_tracker.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.noodle.diamond_tracker.client.DiamondInventoryTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class ContainerButtonMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void addTrackerButton(CallbackInfo ci) {
        AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) (Object) this;

        // Only run this layout if it's a standard block container like a chest/barrel
        if (containerScreen.getMenu() instanceof ChestMenu chestMenu) {
            String title = containerScreen.getTitle().getString().toLowerCase();
            if (title.contains("ender")) return;

            // --- 1. FIND THE EXACT BLOCK COORDINATES ---
            Minecraft mc = Minecraft.getInstance();
            String coordKey = "unknown_chest";

            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
                coordKey = pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
            } else {
                // Fallback to size identifier if the raycast missed (like opening a container via a plugin/mod command)
                coordKey = "menu_" + chestMenu.slots.size();
            }

            int buttonX = (containerScreen.width / 2) + 92;
            int buttonY = (containerScreen.height - 166) / 2 + 6;

            // --- 2. PRE-CALCULATE INITIAL STATE ---
            boolean isCurrentlyTracked = DiamondInventoryTracker.trackedChestCoords.contains(coordKey);
            Component buttonText = isCurrentlyTracked ? Component.literal("✔") : Component.literal("◆");

            final String finalCoordKey = coordKey;

            Button trackButton = Button.builder(buttonText, button -> {
                        // Calculate current diamonds inside the container slots
                        int diamondsInChest = 0;
                        int containerSize = chestMenu.slots.size() - 36;
                        for (int i = 0; i < containerSize; i++) {
                            ItemStack stack = chestMenu.slots.get(i).getItem();
                            if (stack.is(Items.DIAMOND)) {
                                diamondsInChest += stack.getCount();
                            }
                        }

                        // Execute the clean tracking toggle switch
                        DiamondInventoryTracker.toggleChest(finalCoordKey, diamondsInChest);

                        // Instantly swap the button's icon text without reloading the screen!
                        if (DiamondInventoryTracker.trackedChestCoords.contains(finalCoordKey)) {
                            button.setMessage(Component.literal("✔"));
                        } else {
                            button.setMessage(Component.literal("◆"));
                        }
                    })
                    .bounds(buttonX, buttonY, 18, 18)
                    .tooltip(Tooltip.create(Component.literal("Toggle this chest's tracking data inside your global pool.")))
                    .build();

            ((ScreenAccessor) this).invokeAddRenderableWidget(trackButton);
        }
    }
}
package net.noodle.diamond_tracker.client.mixin; // Update to match your mixin package path

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.noodle.diamond_tracker.client.DiamondInventoryTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class EnderChestScanMixin {

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void scanEnderChestOnTick(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        // Verify the screen title contains "ender" to ensure we only scan Ender Chests!
        String screenTitle = screen.getTitle().getString().toLowerCase();
        if (!screenTitle.contains("ender")) {
            return;
        }

        // Double check we are looking at a single chest container layout structure (27 slots + player bag)
        if (screen.getMenu() instanceof ChestMenu chestMenu && chestMenu.slots.size() == 63) {
            int totalDiamonds = 0;

            // Loop through only the first 27 slots (the actual contents of the chest)
            for (int i = 0; i < 27; i++) {
                ItemStack itemStack = chestMenu.slots.get(i).getItem();
                if (itemStack.is(Items.DIAMOND)) {
                    totalDiamonds += itemStack.getCount();
                }
            }

            // Push our counted value straight into the permanent tracker class cache
            DiamondInventoryTracker.updateEnderChestCount(totalDiamonds);
        }
    }
}
package net.noodle.diamond_tracker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class DiamondInventoryTracker {
    // --- Core Diamond Counts ---
    public static int currentInventoryDiamonds = 0;
    public static int cachedEnderChestDiamonds = 0;
    public static int cachedMiscDiamonds = 0;

    // --- Persistent Toggle Registry ---
    public static final Set<String> trackedChestCoords = new HashSet<>();

    // Master configuration save file
    private static final Path CONFIG_FILE = Paths.get("config", "diamond_tracker.txt");

    /**
     * THE MAIN PURPOSE: Automatically scans the client player's inventory
     * loops through all slots, and counts up the diamonds dynamically!
     */
    public static void scanPlayerInventory() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int totalDiamonds = 0;
        Inventory inventory = mc.player.getInventory();

        // Loop through all 36 slots of the player's main inventory + hotbar
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.DIAMOND)) {
                totalDiamonds += stack.getCount();
            }
        }

        // Only save if the count actually shifted to prevent lag loops!
        if (currentInventoryDiamonds != totalDiamonds) {
            currentInventoryDiamonds = totalDiamonds;
            System.out.println("[DIAMOND TRACKER] Scanned Inventory! Found: " + currentInventoryDiamonds);
            saveData();
        }
    }

    /**
     * Updates the running Ender Chest cache from your mixin.
     */
    public static void updateEnderChestCount(int count) {
        if (cachedEnderChestDiamonds != count) {
            cachedEnderChestDiamonds = count;
            System.out.println("[DIAMOND TRACKER] Scanned Ender Chest! Cached: " + cachedEnderChestDiamonds);
            saveData();
        }
    }

    /**
     * Toggles a physical world container on or off.
     */
    public static void toggleChest(String coordKey, int diamondCount) {
        if (trackedChestCoords.contains(coordKey)) {
            trackedChestCoords.remove(coordKey);
            cachedMiscDiamonds -= diamondCount;
            System.out.println("[DIAMOND TRACKER] Removed chest at " + coordKey + " (-" + diamondCount + ")");
        } else {
            trackedChestCoords.add(coordKey);
            cachedMiscDiamonds += diamondCount;
            System.out.println("[DIAMOND TRACKER] Added chest at " + coordKey + " (+" + diamondCount + ")");
        }

        if (cachedMiscDiamonds < 0) cachedMiscDiamonds = 0;
        saveData();
    }

    /**
     * Writes all active data variables cleanly to a configuration file.
     */
    public static void saveData() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE)) {
                writer.write(String.valueOf(currentInventoryDiamonds));
                writer.newLine();

                writer.write(String.valueOf(cachedEnderChestDiamonds));
                writer.newLine();

                writer.write(String.valueOf(cachedMiscDiamonds));
                writer.newLine();

                for (String coord : trackedChestCoords) {
                    writer.write(coord);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("[DIAMOND TRACKER] Failure writing config data: " + e.getMessage());
        }
    }

    /**
     * Reads and restores all data properties from the configuration file on game startup.
     */
    public static void loadData() {
        if (!Files.exists(CONFIG_FILE)) return;

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE)) {
            String line = reader.readLine();
            if (line != null) currentInventoryDiamonds = Integer.parseInt(line.trim());

            line = reader.readLine();
            if (line != null) cachedEnderChestDiamonds = Integer.parseInt(line.trim());

            line = reader.readLine();
            if (line != null) cachedMiscDiamonds = Integer.parseInt(line.trim());

            trackedChestCoords.clear();
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    trackedChestCoords.add(line.trim());
                }
            }
            System.out.println("[DIAMOND TRACKER] Config loaded. Registered Containers: " + trackedChestCoords.size());
        } catch (IOException | NumberFormatException e) {
            System.err.println("[DIAMOND TRACKER] Failure loading config data: " + e.getMessage());
        }
    }
}
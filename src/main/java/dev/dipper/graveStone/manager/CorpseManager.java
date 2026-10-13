package dev.dipper.graveStone.manager;

import dev.dipper.graveStone.CorpseChest;
import dev.dipper.graveStone.block.BlockData;
import dev.dipper.graveStone.block.BlockInventory;
import dev.dipper.graveStone.block.BlockKey;
import dev.dipper.graveStone.sqlite.StorageManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CorpseManager {
    private final Map<UUID, BlockData> deathChest = new ConcurrentHashMap<>();
    private final Map<BlockKey, UUID> chestLookUp = new ConcurrentHashMap<>();
    private final Material chestBlock = Material.ANDESITE_WALL;
    private final StorageManager storage;
    private final CorpseChest plugin;

    private static final String LEGACY_FILE_NAME = "corpse_chest.yml";
    private static final String LEGACY_IMPORT_MARKER = "corpse_chest.yml.imported";

    public CorpseManager(CorpseChest plugin) {
        this.plugin = plugin;
        StorageManager initializedStorage = null;
        try {
            initializedStorage = new StorageManager(plugin);
            storage = initializedStorage;
            loadDeathChests();
            importLegacyFile();
        } catch (Exception e) {
            if (initializedStorage != null) {
                initializedStorage.close();
            }
            throw new IllegalStateException("Failed to initialize death chest database", e);
        }
    }

    private void loadDeathChests() throws Exception {
        for (BlockData data : storage.loadAll()) {
            UUID uuid = data.getUuid();
            deathChest.put(uuid, data);
            chestLookUp.put(data.getKey(), uuid);
        }
        plugin.getLogger().info("Loaded " + deathChest.size() + " death chest(s) from SQLite.");
    }

    private void importLegacyFile() throws IOException {
        File legacyFile = new File(plugin.getDataFolder(), LEGACY_FILE_NAME);
        File marker = new File(plugin.getDataFolder(), LEGACY_IMPORT_MARKER);
        if (!legacyFile.isFile() || marker.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(legacyFile);
        ConfigurationSection corpses = config.getConfigurationSection("corpses");
        if (corpses != null) {
            for (String uuidString : corpses.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    if (deathChest.containsKey(uuid)) {
                        continue;
                    }

                    String path = "corpses." + uuidString;
                    String world = config.getString(path + ".world");
                    if (world == null) {
                        throw new IllegalArgumentException("Missing world for corpse " + uuidString);
                    }

                    BlockKey key = new BlockKey(
                            world,
                            config.getInt(path + ".x"),
                            config.getInt(path + ".y"),
                            config.getInt(path + ".z")
                    );
                    BlockInventory inventory = new BlockInventory(
                            readItemStacks(config.getList(path + ".contents")),
                            readItemStacks(config.getList(path + ".armor")),
                            config.getItemStack(path + ".offhand")
                    );
                    BlockData data = new BlockData(
                            uuid,
                            config.getString(path + ".name", "Unknown"),
                            key,
                            config.getLong(path + ".time"),
                            inventory,
                            config.getInt(path + ".level"),
                            (float) config.getDouble(path + ".progress")
                    );
                    add(data, key);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to import legacy death chest " + uuidString, e);
                }
            }
        }

        Files.createFile(marker.toPath());
        plugin.getLogger().info("Imported legacy death chest data from " + LEGACY_FILE_NAME + ".");
    }

    private ItemStack[] readItemStacks(List<?> values) {
        if (values == null) {
            return new ItemStack[0];
        }
        List<ItemStack> items = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof ItemStack item) {
                items.add(item);
            } else if (value != null) {
                plugin.getLogger().warning("Ignoring invalid item in legacy death chest data.");
            }
        }
        return items.toArray(ItemStack[]::new);
    }

    public void add(BlockData data, BlockKey key) {
        storage.save(data);
        UUID uuid = data.getUuid();
        BlockData previous = deathChest.put(uuid, data);
        if (previous != null) {
            chestLookUp.remove(previous.getKey(), uuid);
        }
        chestLookUp.put(data.getKey(), uuid);
    }

    public void remove(BlockData data, BlockKey key) {
        UUID uuid = data.getUuid();
        storage.delete(uuid);
        deathChest.remove(uuid);
        chestLookUp.remove(key, uuid);
    }

    public BlockData get(BlockKey key) {
        UUID uuid = chestLookUp.get(key);
        return uuid == null ? null : deathChest.get(uuid);
    }

    public BlockInventory fullSave(Player player) {
        return new BlockInventory(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getItemInOffHand()
        );
    }

    public BlockKey key(Location location) {
        return new BlockKey(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public Material getChestBlock() {
        return chestBlock;
    }

    public Map<UUID, BlockData> getDeathChest() {
        return deathChest;
    }

    public Map<BlockKey, UUID> getChestLookUp() {
        return chestLookUp;
    }

    public void close() {
        storage.close();
    }
}

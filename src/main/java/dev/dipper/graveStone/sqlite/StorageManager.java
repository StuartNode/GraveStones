package dev.dipper.graveStone.sqlite;

import dev.dipper.graveStone.CorpseChest;
import dev.dipper.graveStone.block.BlockData;
import dev.dipper.graveStone.block.BlockInventory;
import dev.dipper.graveStone.block.BlockKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class StorageManager implements AutoCloseable {
    private final CorpseChest plugin;
    private final Connection connection;

    private static final String UPSERT_SQL = """
            INSERT INTO death_chests (
                uuid, player_name, world_name, x, y, z, created_at, inventory_data, level, progress
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                player_name = excluded.player_name,
                world_name = excluded.world_name,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z,
                created_at = excluded.created_at,
                inventory_data = excluded.inventory_data,
                level = excluded.level,
                progress = excluded.progress
            """;

    public StorageManager(CorpseChest plugin) throws IOException, SQLException {
        this.plugin = plugin;
        Files.createDirectories(plugin.getDataFolder().toPath());
        File databaseFile = new File(plugin.getDataFolder(), "death-chests.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try {
            createSchema();
        } catch (SQLException e) {
            close();
            throw e;
        }
    }

    private void createSchema() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS death_chests (
                    uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    world_name TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    inventory_data BLOB NOT NULL,
                    level INTEGER NOT NULL,
                    progress REAL NOT NULL
                )
                """;
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public List<BlockData> loadAll() throws SQLException, IOException, ClassNotFoundException {
        List<BlockData> loaded = new ArrayList<>();
        try (var statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT uuid, player_name, world_name, x, y, z, created_at,
                            inventory_data, level, progress
                     FROM death_chests
                     """)) {
            while (rows.next()) {
                UUID uuid = UUID.fromString(rows.getString("uuid"));
                BlockKey key = new BlockKey(
                        rows.getString("world_name"),
                        rows.getInt("x"),
                        rows.getInt("y"),
                        rows.getInt("z")
                );

                BlockInventory inventory = deserializeInventory(rows.getBytes("inventory_data"));
                loaded.add(new BlockData(
                        uuid,
                        rows.getString("player_name"),
                        key,
                        rows.getLong("created_at"),
                        inventory,
                        rows.getInt("level"),
                        rows.getFloat("progress")
                ));
            }
        }
        return loaded;
    }

    public void save(BlockData data) {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setString(1, data.getUuid().toString());
            statement.setString(2, data.getName());
            statement.setString(3, data.getKey().world());
            statement.setInt(4, data.getKey().x());
            statement.setInt(5, data.getKey().y());
            statement.setInt(6, data.getKey().z());
            statement.setLong(7, data.getCreatTime());
            statement.setBytes(8, serializeInventory(data.getInventory()));
            statement.setInt(9, data.getLevel());
            statement.setFloat(10, data.getProgress());
            statement.executeUpdate();
        } catch (IOException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save death chest " + data.getUuid(), e);
            throw new IllegalStateException("Failed to save death chest " + data.getUuid(), e);
        }
    }

    public void delete(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM death_chests WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete death chest " + uuid, e);
            throw new IllegalStateException("Failed to delete death chest " + uuid, e);
        }
    }

    private byte[] serializeInventory(BlockInventory inventory) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeObject(inventory.getContents());
            output.writeObject(inventory.getArmor());
            output.writeObject(inventory.getOffhand());
        }
        return bytes.toByteArray();
    }

    private BlockInventory deserializeInventory(byte[] bytes) throws IOException, ClassNotFoundException {
        try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            ItemStack[] contents = (ItemStack[]) input.readObject();
            ItemStack[] armor = (ItemStack[]) input.readObject();
            ItemStack offhand = (ItemStack) input.readObject();
            return new BlockInventory(contents, armor, offhand);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close death chest database", e);
        }
    }
}

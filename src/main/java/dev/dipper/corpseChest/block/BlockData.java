package dev.dipper.corpseChest.block;

import java.util.UUID;

public class BlockData {
    private final UUID uuid;
    private final String name;
    private final BlockKey key;
    private final long creatTime;
    private final BlockInventory inventory;
    private final int level;
    private final float progress;

    public BlockData(UUID uuid, String name, BlockKey key, long creatTime, BlockInventory inventory, int level, float progress) {
        this.uuid = uuid;
        this.name = name;
        this.key = key;
        this.creatTime = creatTime;
        this.inventory = inventory;
        this.level = level;
        this.progress = progress;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public BlockKey getKey() {
        return key;
    }

    public long getCreatTime() {
        return creatTime;
    }

    public BlockInventory getInventory() {
        return inventory;
    }

    public int getLevel() {
        return level;
    }

    public float getProgress() {
        return progress;
    }
}
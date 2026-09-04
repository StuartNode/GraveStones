package dev.dipper.graveStone.block;

import org.bukkit.inventory.ItemStack;

public class BlockInventory {
    private ItemStack[] contents;
    private ItemStack[] armor;
    private ItemStack offhand;

    public BlockInventory(ItemStack[] contents, ItemStack[] armor, ItemStack offhand) {
        this.contents = contents;
        this.armor = armor;
        this.offhand = offhand;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public void setContents(ItemStack[] item) {
        this.contents = item;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public ItemStack getOffhand() {
        return offhand;
    }
}
package dev.dipper.graveStone.menu;

import dev.dipper.graveStone.block.BlockData;
import dev.dipper.graveStone.block.BlockKey;
import dev.dipper.graveStone.manager.CorpseManager;
import dev.nexisApi.menu.PaginatedMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GraveMenu extends PaginatedMenu<ItemStack> {
    private final CorpseManager corpseM;
    private final BlockData data;
    private final boolean adminOpen;

    public GraveMenu(JavaPlugin plugin, CorpseManager corpseM, BlockData data, boolean openCorpse) {
        super(plugin);
        this.corpseM = corpseM;
        this.data = data;
        this.adminOpen = openCorpse;
    }

    @Override
    public String getMenuName() {
        return "Corpse: " + data.getName();
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    protected CompletableFuture<List<ItemStack>> loadDataAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            ItemStack[] contents = data.getInventory().getContents();
            List<ItemStack> items = new ArrayList<>();

            for (ItemStack item : contents) {
                if (item == null) continue;
                items.add(item);
            }
            return items;
        });
    }

    @Override
    protected ItemStack toItem(ItemStack item) {
        return item;
    }

    @Override
    protected void onElementClick(Player player, ItemStack item, InventoryClickEvent event) {}

    @Override
    public void onClose(Player player) {
        if (!adminOpen) return;
        BlockKey key = data.getKey();
        World world = Bukkit.getWorld(key.world());

        if (world == null) return;
        Location location = new Location(
                world,
                key.x(),
                key.y(),
                key.z()
        );

        for (ItemStack item : data.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            player.getWorld().dropItemNaturally(
                    location,
                    item
            );
        }

        location.getBlock().setType(Material.AIR);
        Location above = location.clone().add(0, 1, 0);

        if (above.getBlock().getType() == corpseM.getChestBlock()) above.getBlock().setType(Material.AIR);
        corpseM.remove(data, key);
    }
}

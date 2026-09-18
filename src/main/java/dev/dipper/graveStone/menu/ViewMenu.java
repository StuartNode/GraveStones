package dev.dipper.graveStone.menu;

import dev.dipper.graveStone.block.BlockData;
import dev.dipper.graveStone.block.BlockKey;
import dev.dipper.graveStone.manager.CorpseManager;
import dev.nexisApi.gui.GuiManager;
import dev.nexisApi.menu.PaginatedMenu;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ViewMenu extends PaginatedMenu<BlockData> {
    private final CorpseManager corpseManager;
    private final GuiManager guiManager;

    public ViewMenu(JavaPlugin plugin, CorpseManager corpseManager, GuiManager guiManager) {
        super(plugin);
        this.corpseManager = corpseManager;
        this.guiManager = guiManager;
    }

    @Override
    public String getMenuName() {
        return "All Corpse";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    protected CompletableFuture<List<BlockData>> loadDataAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(corpseManager.getDeathChest().values()));
    }

    @Override
    protected ItemStack toItem(BlockData item) {
        return makeItem(Material.LODESTONE,
                ChatColor.AQUA + "Player: " + item.getName(),
                ChatColor.YELLOW + "Created: " + formatTime(item.getCreatTime()),
                ChatColor.GRAY + item.getKey().toString(),
                ChatColor.RED + "Shift click to Delete");
    }

    @Override
    protected void onElementClick(Player player, BlockData data, InventoryClickEvent event) {
        if (event.isShiftClick()) {
            corpseManager.remove(data, data.getKey());
            player.sendMessage(ChatColor.RED + "Deleted: " + data.getKey());
            removeStone(data);
            requestReload();
            return;
        }

         guiManager.openMenuandLoad(player, new GraveMenu(plugin, corpseManager, data, false));
    }

    private void removeStone(BlockData data) {
        BlockKey key = data.getKey();
        World world = Bukkit.getWorld(key.world());
        if (world == null) {
            return;
        }

        Location location = new Location(
                world,
                key.x(),
                key.y(),
                key.z()
        );

       location.getBlock().setType(Material.AIR);
       Location above = location.clone().add(0, 1, 0);

       if (above.getBlock().getType() == corpseManager.getChestBlock()) {
           above.getBlock().setType(Material.AIR);
       }
    }

    private final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private String formatTime(Long time) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(time));
    }
}

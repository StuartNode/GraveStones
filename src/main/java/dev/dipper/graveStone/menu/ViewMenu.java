package dev.dipper.graveStone.menu;

import dev.dipper.graveStone.block.BlockData;
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
    private final CorpseManager corpseM;
    private final GuiManager guiM;

    public ViewMenu(JavaPlugin plugin, CorpseManager corpseM, GuiManager guiM) {
        super(plugin);
        this.corpseM = corpseM;
        this.guiM = guiM;
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
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(corpseM.getDeathChest().values()));
    }

    @Override
    protected ItemStack toItem(BlockData item) {
        return makeItem(Material.LODESTONE,
                ChatColor.AQUA + "Player: " + item.getName(),
                ChatColor.YELLOW + "Created: " + formatTime(item.getCreatTime()),
                ChatColor.GRAY + item.getKey().toString());
    }

    @Override
    protected void onElementClick(Player player, BlockData item, InventoryClickEvent event) {
         guiM.openMenuandLoad(player, new GraveMenu(plugin, corpseM, item, false));
    }

    private final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private String formatTime(Long time) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(time));
    }
}

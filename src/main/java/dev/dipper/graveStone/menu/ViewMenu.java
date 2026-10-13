package dev.dipper.graveStone.menu;

import dev.dipper.graveStone.block.BlockData;
import dev.dipper.graveStone.block.BlockKey;
import dev.dipper.graveStone.manager.CorpseManager;
import dev.nexisApi.gui.GuiManager;
import dev.nexisApi.input.AnvilNameInput;
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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ViewMenu extends PaginatedMenu<BlockData> {
    private final CorpseManager corpseManager;
    private final GuiManager guiManager;
    private String searchQuery = "";

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
        String query = searchQuery.toLowerCase(Locale.ROOT);
        return CompletableFuture.supplyAsync(() -> {
            List<BlockData> corpses = new ArrayList<>(corpseManager.getDeathChest().values());
            if (query.isEmpty()) {
                return corpses;
            }

            return corpses.stream()
                    .filter(data -> (data.getName() + " " + data.getKey()).toLowerCase(Locale.ROOT).contains(query))
                    .toList();
        });
    }

    @Override
    protected void renderCustomControls() {
        String filter = searchQuery.isEmpty() ? ChatColor.GRAY + "No filter" : ChatColor.AQUA + "Filter: " + ChatColor.WHITE + searchQuery;
        inventory.setItem(slotSearch(), makeItem(Material.NAME_TAG, ChatColor.YELLOW + "Search" + ChatColor.GRAY + " (click)", filter));
    }

    @Override
    protected boolean handleCustomControl(Player player, int slot, InventoryClickEvent event) {
        if (slot != slotSearch()) {
            return false;
        }

        AnvilNameInput input = new AnvilNameInput.Builder()
                .plugin(plugin)
                .title("Search graves")
                .initialText(searchQuery)
                .seedItem(makeItem(Material.NAME_TAG, "Search graves"))
                .onSubmit((target, text) -> {
                    searchQuery = text.trim();
                    page = 0;
                    reopenAfterSearch(target);
                })
                .onCancel(this::reopenAfterSearch)
                .build();
        guiManager.openAnvilInput(player, input);
        return true;
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

         guiManager.openMenuAndLoad(player, new GraveMenu(plugin, corpseManager, data, false));
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

    private void reopenAfterSearch(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> guiManager.openMenuAndLoad(player, this));
    }

    private final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private String formatTime(Long time) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(time));
    }

    private int slotSearch() {
        return firstControlSlot() + 6;
    }
}

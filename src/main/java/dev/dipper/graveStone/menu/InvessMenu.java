package dev.dipper.graveStone.menu;

import dev.nexisApi.menu.PaginatedMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class InvessMenu extends PaginatedMenu<InvessMenu.InventoryItem> {
    private final Player target;
    public record InventoryItem(int targetSlot, ItemStack item) {}

    public InvessMenu(JavaPlugin plugin, Player target) {
        super(plugin);
        this.target = target;
    }

    @Override
    public String getMenuName() {
        return "Viewing: " + target.getName();
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    protected CompletableFuture<List<InventoryItem>> loadDataAsync(Player player) {
        List<InventoryItem> items = new ArrayList<>();

        for (int slot = 0; slot < target.getInventory().getSize(); slot++) {
            ItemStack item = target.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) continue;

            items.add(new InventoryItem(slot, item.clone()));
        }

        return CompletableFuture.completedFuture(items);
    }

    @Override
    protected void onElementClick(Player player, InventoryItem item, InventoryClickEvent event) {
        ItemStack targetItem = target.getInventory().getItem(item.targetSlot());
        if (targetItem == null || targetItem.getType().isAir()) return;

        if (!targetItem.isSimilar(item.item())) {
            requestReload();
            return;
        }

        ItemStack toTransfer = targetItem.clone();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(toTransfer);

        int remaining = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
        int transferred = targetItem.getAmount() - remaining;

        if (transferred <= 0) return;

        if (transferred == targetItem.getAmount()) {
            target.getInventory().setItem(item.targetSlot(), null);
        } else {
            targetItem.setAmount(targetItem.getAmount() - transferred);
            target.getInventory().setItem(item.targetSlot(), targetItem);
        }
        requestReload();
    }

    @Override
    protected ItemStack toItem(InventoryItem item) {
        return item.item();
    }
}

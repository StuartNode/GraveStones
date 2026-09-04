package dev.dipper.graveStone.listener;

import dev.dipper.graveStone.CorpseChest;
import dev.dipper.graveStone.block.BlockInventory;
import dev.dipper.graveStone.block.BlockData;
import dev.dipper.graveStone.block.BlockKey;
import dev.dipper.graveStone.manager.CorpseManager;
import dev.dipper.graveStone.menu.ChestMenu;
import dev.nexisApi.gui.GuiManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class CorpseListener implements Listener {
    private final CorpseChest plugin;
    private final CorpseManager corpseS;
    private final GuiManager guiM;

    public CorpseListener(CorpseChest plugin, CorpseManager corpseS, GuiManager guiM) {
        this.plugin = plugin;
        this.corpseS = corpseS;
        this.guiM = guiM;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().isEmpty()) return;

        Location loc = player.getLocation().getBlock().getLocation();
        Location loc2 = loc.clone().add(0, 1, 0);

        player.sendMessage(
                ChatColor.DARK_RED + "☠════════════════════════════☠\n" +
                        ChatColor.RED + "" + ChatColor.BOLD + "         YOU HAVE DIED\n" +
                        ChatColor.GRAY + "Your soul has departed..." +
                        ChatColor.GRAY + "\nYour equipment has been placed inside a " +
                        ChatColor.GOLD + "Corpse Chest" +
                        ChatColor.GRAY + "." +
                        ChatColor.AQUA + "\n\n📍 X: " + ChatColor.WHITE + loc.getBlockX() +
                        ChatColor.AQUA + "  Y: " + ChatColor.WHITE + loc.getBlockY() +
                        ChatColor.AQUA + "  Z: " + ChatColor.WHITE + loc.getBlockZ() +
                        ChatColor.DARK_RED + "\n☠════════════════════════════☠"
        );

        if (!isAirBlock(loc) && !isAirBlock(loc2)) {
            player.sendMessage(ChatColor.RED + "⚠ An unexpected error prevented your gravestone from being placed. Please contact a server administrator. ⚠");
            player.sendMessage(ChatColor.GRAY + "[Safe Guard] Your items have been dropped at your death location.");
            return;
        }

        BlockInventory full = corpseS.fullSave(player);
        BlockKey key = corpseS.key(loc);

        int level = player.getLevel();
        float progress = player.getExp();

        UUID uuid = UUID.randomUUID();
        BlockData data = new BlockData(
                uuid,
                player.getName(),
                key,
                System.currentTimeMillis(),
                full,
                level,
                progress
        );

        corpseS.add(data, key);

        event.getDrops().clear();
        event.setDroppedExp(0);
        loc.getBlock().setType(corpseS.getChestBlock());
        loc2.getBlock().setType(corpseS.getChestBlock());
    }

    @EventHandler
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        Location loc = block.getLocation();
        BlockKey key = corpseS.key(loc);
        BlockData data = corpseS.get(key);

        if (data == null) return;
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        if (!data.getName().equals(player.getName())) return;

        event.setCancelled(true);
        ChestMenu menu = new ChestMenu(plugin, corpseS, data, true);
        guiM.openMenuandLoad(player, menu);
        player.playSound(player, Sound.ENTITY_SKELETON_DEATH, 1, 1);

        player.setLevel(data.getLevel());
        player.setExp(data.getProgress());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        BlockKey key = corpseS.key(block.getLocation());

        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (block.getType() != corpseS.getChestBlock()) return;

        UUID uuid = corpseS.getChestLookUp().get(key);
        if (uuid == null) return;
        event.setCancelled(true);
    }

    private boolean isAirBlock(Location location) {
        return location.getBlock().getType() == Material.AIR;
    }
}
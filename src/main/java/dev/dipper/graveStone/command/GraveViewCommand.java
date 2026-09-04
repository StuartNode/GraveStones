package dev.dipper.graveStone.command;

import dev.dipper.graveStone.CorpseChest;
import dev.dipper.graveStone.manager.CorpseManager;
import dev.dipper.graveStone.menu.ViewMenu;
import dev.nexisApi.gui.GuiManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GraveViewCommand implements CommandExecutor {
    private final CorpseManager corpseM;
    private final GuiManager guiM;
    private final CorpseChest plugin;

    public GraveViewCommand(CorpseManager corpseM, GuiManager guiM, CorpseChest plugin) {
        this.corpseM = corpseM;
        this.guiM = guiM;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command Command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only player can run this command.");
            return true;
        }

        if (!player.hasPermission("corpsechest.veiwcmd")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        guiM.openMenuandLoad(player, new ViewMenu(plugin, corpseM, guiM));
        return true;
    }
}

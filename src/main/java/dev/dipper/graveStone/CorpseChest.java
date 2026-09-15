package dev.dipper.graveStone;

import dev.dipper.graveStone.command.GraveViewCommand;
import dev.dipper.graveStone.listener.CorpseListener;
import dev.dipper.graveStone.manager.CorpseManager;
import dev.nexisApi.gui.GuiListener;
import dev.nexisApi.gui.GuiManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CorpseChest extends JavaPlugin {
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        guiManager = new GuiManager();
        CorpseManager corpseManager = new CorpseManager(this);

        getServer().getPluginManager().registerEvents(new CorpseListener(corpseManager, guiManager, this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(guiManager), this);

        getCommand("graveview").setExecutor(new GraveViewCommand(corpseManager, guiManager, this));
    }

    @Override
    public void onDisable() {
        if (guiManager != null) {
            guiManager.closeAll();
        }
    }
}

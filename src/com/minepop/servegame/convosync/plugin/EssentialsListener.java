package com.minepop.servegame.convosync.plugin;

import com.earth2me.essentials.Essentials;
import com.minepop.servegame.convosync.net.PlayerVanishMessage;
import java.util.logging.Level;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Blir
 */
public class EssentialsListener implements Listener {

    private ConvoSync plugin;
    private Essentials ess;

    protected EssentialsListener(ConvoSync plugin) {
        plugin.getLogger().info("Essentials Listener registered!");
        this.plugin = plugin;
        Plugin prospective = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (prospective instanceof Essentials) {
            ess = (Essentials) prospective;
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent evt) {
        plugin.getLogger().log(Level.INFO, "isEss: {0} Canceled: {1} Vanished: {2}",
                new Object[]{plugin.isEss, evt.isCancelled(), ess.getUser(evt.getPlayer()).isVanished()});
        if (plugin.isEss && !evt.isCancelled() && evt.getMessage().equalsIgnoreCase("vanish")) {
            plugin.out(new PlayerVanishMessage(evt.getPlayer().getName(), !ess.getUser(evt.getPlayer()).isVanished()), false);
        }
    }
}

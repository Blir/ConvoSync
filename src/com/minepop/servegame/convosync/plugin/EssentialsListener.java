package com.minepop.servegame.convosync.plugin;

import com.earth2me.essentials.Essentials;

import com.minepop.servegame.convosync.net.PlayerVanishMessage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

/**
 * Used to handler Essentials vanishing compatibility.
 * 
 * @author Blir
 */
public class EssentialsListener implements Listener {

    private final ConvoSync plugin;
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
        if (plugin.isEss && !evt.isCancelled() && (evt.getMessage().equalsIgnoreCase("/vanish")
                                                   || evt.getMessage().equalsIgnoreCase("/v"))) {
            plugin.out(new PlayerVanishMessage(evt.getPlayer().getName(),
                                               !ess.getUser(evt.getPlayer()).isVanished()), false);
        }
    }
}

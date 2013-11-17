package com.minepop.servegame.convosync.plugin;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles cross-server AFK notifications.
 *
 * @author Blir
 */
public class EssentialsTask implements Runnable {

    private final ConvoSync plugin;
    private Essentials ess;
    //private final List<ConvoSyncUser> users = new LinkedList<ConvoSyncUser>();
    protected final Map<String, ConvoSyncUser> users = new HashMap<String, ConvoSyncUser>();

    protected EssentialsTask(ConvoSync plugin) {
        this.plugin = plugin;
        Plugin prospective = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (prospective instanceof Essentials) {
            ess = (Essentials) prospective;
            plugin.isEss = true;
        }
    }

    @Override
    public void run() {
        if (plugin.isEss) {
            plugin.getLogger().info("Essentials Task started!");
        }
        while (plugin.connected && plugin.isEss && plugin.isEnabled() && ess.isEnabled()) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                User user = ess.getUser(player);
                ConvoSyncUser csuser = getUser(user);
                if (user.isAfk() != csuser.afk) {
                    csuser.afk = user.isAfk();
                    if (!user.isVanished()) {
                        plugin.out(user.getDisplayName() + ChatColor.DARK_PURPLE
                                   + " is no" + (user.isAfk() ? "w AFK." : " longer AFK."), false);
                    }
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                plugin.isEss = false;
                return;
            }
        }
    }

    private ConvoSyncUser getUser(User user) {
        ConvoSyncUser csuser = users.get(user.getName());
        if (csuser == null) {
            csuser = new ConvoSyncUser();
            csuser.name = user.getName();
            csuser.afk = user.isAfk();
            users.put(csuser.name, csuser);
        }
        return csuser;
    }

    public boolean canChat(Player player) {
        return !ess.getUser(player.getPlayer().getName()).isVanished();
    }

    private static class ConvoSyncUser {

        String name;
        boolean afk;
    }
}

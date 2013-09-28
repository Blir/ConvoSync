package com.minepop.servegame.convosync.plugin;

import com.earth2me.essentials.Essentials;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Blir
 */
public class EssentialsTask implements Runnable {

    private ConvoSync plugin;
    private Essentials ess;
    private List<User> users = new ArrayList<User>();

    protected EssentialsTask(ConvoSync plugin) {
        this.plugin = plugin;
        Plugin prospective = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (prospective != null && prospective instanceof Essentials) {
            ess = (Essentials) prospective;
            if (ess != null) {
                plugin.isEss = true;
            }
        }
    }

    @Override
    public void run() {
        if (plugin.isEss) {
            plugin.getLogger().info("Essentials Task started!");
        }
        while (plugin.connected && plugin.isEss) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                com.earth2me.essentials.User user = ess.getUser(player);
                User csuser = getUser(user);
                if (user.isAfk() != csuser.afk) {
                    csuser.afk = user.isAfk();
                    if (!user.isVanished()) {
                        plugin.out(user.getDisplayName() + ChatColor.DARK_PURPLE + " is no" + (user.isAfk() ? "w AFK." : " longer AFK."), false);
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

    private User getUser(com.earth2me.essentials.User user) {
        for (User ctuser : users) {
            if (ctuser.name.equals(user.getName())) {
                return ctuser;
            }
        }
        User csuser = new User();
        csuser.name = user.getName();
        csuser.afk = user.isAfk();
        users.add(csuser);
        return csuser;
    }

    private User getUser(String name) {
        for (User user : users) {
            if (user.name.equals(name)) {
                return user;
            }
        }
        return null;
    }

    public void remove(String name) {
        users.remove(getUser(name));
    }
    
    public boolean chat(Player player) {
        return !ess.getUser(player.getPlayer().getName()).isVanished();
    }

    private static class User {

        private String name;
        private boolean afk;
    }
}

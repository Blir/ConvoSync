package com.minepop.servegame.convosync;

import com.earth2me.essentials.Essentials;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 *
 * @author Blir
 */
public class AFKThread extends Thread {

    private static ConvoSync plugin;
    private static Essentials ess;
    private List<User> users = new ArrayList<User>();

    @Override
    public void run() {
        plugin.getLogger().info("Essentials AFK Thread started!");
        while (plugin.connected) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                com.earth2me.essentials.User user = ess.getUser(player);
                User csuser = getUser(user);
                if (user.isAfk() != csuser.afk) {
                    csuser.afk = user.isAfk();
                    plugin.out("c" + user.getDisplayName() + "§5 is no" + (user.isAfk() ? "w AFK." : " longer AFK."));
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
            }
        }
    }

    protected static void enable(ConvoSync plugin) {
        AFKThread.plugin = plugin;
        RegisteredServiceProvider<Essentials> rsp;
        rsp = plugin.getServer().getServicesManager().getRegistration(Essentials.class);
        if (rsp != null) {
            ess = rsp.getProvider();
            if (ess != null) {
                plugin.isEss = true;
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

    private static class User {

        private String name;
        private boolean afk;
    }
}

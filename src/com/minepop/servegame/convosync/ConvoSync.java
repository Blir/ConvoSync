package com.minepop.servegame.convosync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Blir
 */
public class ConvoSync extends JavaPlugin implements Listener {

    private enum Action {

        SETIP, SETPORT, RECONNECT, DISCONNECT, STATUS
    }
    private int port;
    private String ip, password;
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;
    protected boolean connected, shouldBe = true, verified, isEss;
    private List<User> users = new ArrayList<User>();
    private List<ChatListener> listeners = new ArrayList<ChatListener>();

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("Essentials")) {
            AFKThread.enable(this);
        }
        try {
            port = getConfig().getInt("port");
            ip = getConfig().getString("ip");
            password = getConfig().getString("password");
        } catch (NumberFormatException ex) {
        } catch (NullPointerException ex) {
        }
        if (ip == null || ip.equals("null") || port == 0 || password == null || password.equals("null")) {
            getLogger().warning("IP, port, or password missing.");
        } else {
            try {
                connect();
            } catch (IOException ex) {
                getLogger().log(Level.INFO, "Error connecting to server: {0}", ex.toString());
                if (getConfig().getBoolean("auto-reconnect.after-connect-fail")) {
                    autoReconnect(getConfig().getInt("auto-reconnect.time-delay-ms"));
                }
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            disconnect();
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Error closing socket: {0}", ex.toString());
        }
        getConfig().set("ip", ip);
        getConfig().set("port", port);
        getConfig().set("password", password);
        getConfig().set("auto-reconnect.after-connect-fail", getConfig().getBoolean("auto-reconnect.after-connect-fail"));
        getConfig().set("auto-reconnect.after-socket-error", getConfig().getBoolean("auto-reconnect.after-socket-error"));
        getConfig().set("auto-reconnect.after-socket-close", getConfig().getBoolean("auto-reconnect.after-socket-close"));
        getConfig().set("auto-reconnect.time-delay-ms", getConfig().getBoolean("auto-reconnect.time-delay-ms"));
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("crosstalk") && args.length > 1) {
            switch (Action.valueOf(args[0].toUpperCase())) {
                case SETIP:
                    if (args.length != 2) {
                        sender.sendMessage("§c/convosync setip <ip>");
                        return true;
                    }
                    if (connected) {
                        sender.sendMessage("§cThe server is currently connected. Please disconnect first.");
                        return true;
                    }
                    ip = args[1];
                    sender.sendMessage("§aNow using IP §9" + ip + "§a.");
                    return true;
                case SETPORT:
                    if (args.length != 2) {
                        sender.sendMessage("§c/convosync setport <port>");
                        return true;
                    }
                    if (connected) {
                        sender.sendMessage("§cThe server is currently connected. Please disconnect first.");
                        return true;
                    }
                    try {
                        port = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("You did not enter a valid number.");
                        return true;
                    }
                    sender.sendMessage("§aNowing using port §9" + port + "§a.");
                    return true;
                case RECONNECT:
                    if (connected) {
                        try {
                            shouldBe = false;
                            disconnect();
                        } catch (IOException ex) {
                            getLogger().log(Level.WARNING, "Error closing socket: {0}", ex.toString());
                        }
                    }
                    try {
                        shouldBe = true;
                        connect();
                    } catch (IOException ex) {
                        getLogger().log(Level.WARNING, "Error connecting to server: {0}", ex.toString());
                    }

                    sender.sendMessage("§aConnection status: §9" + connected);
                    sender.sendMessage("§aAuthentication status: §9" + verified);
                    sender.sendMessage("§aEssentials status: §9" + isEss);
                    return true;
                case DISCONNECT:
                    try {
                        shouldBe = false;
                        disconnect();
                    } catch (IOException ex) {
                        getLogger().log(Level.WARNING, "Error closing socket: {0}", ex.toString());
                    }
                    sender.sendMessage("§aConnection status: §9" + connected);
                    sender.sendMessage("§aAuthentication status: §9" + verified);
                    sender.sendMessage("§aEssentials status: §9" + isEss);
                    return true;
                case STATUS:
                    sender.sendMessage("§aConnection status: §9" + connected);
                    sender.sendMessage("§aAuthentication status: §9" + verified);
                    sender.sendMessage("§aEssentials status: §9" + isEss);
                    return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent evt) {
        out("c" + evt.getFormat().replace("%1$s", evt.getPlayer().getDisplayName()).replace("%2$s", evt.getMessage()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent evt) {
        out("c" + evt.getJoinMessage());
        out("j" + evt.getPlayer().getName());
        if (!connected && evt.getPlayer().hasPermission("convosync.convosync")) {
            evt.getPlayer().sendMessage("§cConvoSync is currently disconnected from the chat server.");
            if (ip == null || ip.equals("null")) {
                evt.getPlayer().sendMessage("§cSonvoSync currently has no IP to connect to.");
            }
            if (port == 0) {
                evt.getPlayer().sendMessage("§cConvoSync currently has no port to connect to.");
            }
            if (password == null || password.equals("null")) {
                evt.getPlayer().sendMessage("§cConvoSync currently has no password to connect with.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent evt) {
        out("c" + evt.getQuitMessage());
        out("q" + evt.getPlayer().getName());
    }

    private void connect() throws IOException {
        if (ip == null || ip.equals("null") || port == 0 || password == null || password.equals("null")) {
            return;
        }
        getLogger().log(Level.INFO, "Connecting to {0}:" + port, ip);
        socket = new Socket(ip, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream());
        connected = true;
        verify();
        new Thread() {
            @Override
            public void run() {
                String input;
                while (connected) {
                    try {
                        input = in.readLine();
                    } catch (IOException ex) {
                        connected = false;
                        getLogger().log(Level.WARNING, "Error reading from socket: {0}", ex.toString());
                        if (socket.isClosed() && getConfig().getBoolean("auto-reconnect.after-socket-close")) {
                            autoReconnect(getConfig().getInt("auto-reconnect.time-delay-ms"));
                        } else {
                            try {
                                disconnect();
                            } catch (IOException ex2) {
                                getLogger().log(Level.WARNING, "Error closing socket: {0}", ex2.toString());
                            }
                            if (getConfig().getBoolean("auto-reconnect.after-socker-error")) {
                                autoReconnect(getConfig().getInt("auto-reconnect.time-delay-ms"));
                            }
                        }
                        return;
                    }
                    if (input != null && input.length() > 0) {
                        switch (input.charAt(0)) {
                            case 'c':
                                for (ChatListener listener : listeners) {
                                    listener.onInput(input.substring(1));
                                }
                                getServer().broadcastMessage(input.substring(1));
                                break;
                            case 'l':
                                sendList();
                                break;
                            case 'v':
                                verified = true;
                                getLogger().info("Connection authenticated.");
                                sendList();
                                out("c has connected.");
                                break;
                            case 'w':
                                verified = false;
                                getLogger().warning("Failed to authenticate with server.");
                                break;
                            default:
                                getLogger().log(Level.WARNING, "Unexpected message: {0}", input);
                                break;
                        }
                    }
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }.start();
        if (isEss) {
            new AFKThread().start();
        }
        getLogger().info(socket.toString());
    }

    private void disconnect() throws IOException {
        String list = "r";
        for (Player player : getServer().getOnlinePlayers()) {
            list += "`" + player.getName();
        }
        out(list);
        out("c has disconnected.");
        out.println("d");
        out.flush();
        shouldBe = false;
        if (socket != null) {
            socket.close();
        }
        connected = false;
    }

    private void autoReconnect(final int pausetime) {
        if (!shouldBe && !isEnabled()) {
            return;
        }
        getLogger().log(Level.INFO, "Attempting to reconnect every {0} seconds...", (int) Math.round(pausetime / 1000));
        new Thread() {
            @Override
            public void run() {
                int attempts = 0;
                do {
                    try {
                        Thread.sleep(pausetime);
                        connect();
                    } catch (InterruptedException ex) {
                        getLogger().log(Level.WARNING, "Error attempting automatic reconnection: {0}", ex.toString());
                        return;
                    } catch (IOException ex) {
                        attempts++;
                    }
                } while (!connected);
                getLogger().log(Level.INFO, "Successfully reconnected after {0} attempts.", attempts);
            }
        }.start();
    }

    public boolean out(String s) {
        if (connected && verified) {
            out.println(s);
            out.flush();
            return true;
        }
        return false;
    }

    private void verify() {
        out.println("tplugin");
        out.flush();
        out.println("v" + password);
        out.flush();
        out.println("n" + getServer().getServerName());
        out.flush();
    }

    private void sendList() {
        String list = "l";
        for (Player player : getServer().getOnlinePlayers()) {
            list += "`" + player.getName();
        }
        out(list);
    }

    protected User getUser(com.earth2me.essentials.User user) {
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

    public boolean addChatListener(ChatListener listener) {
        return listeners.add(listener);
    }

    public interface ChatListener {

        public void onInput(String msg);
    }

    protected static class User {

        protected String name;
        protected boolean afk;
    }
}
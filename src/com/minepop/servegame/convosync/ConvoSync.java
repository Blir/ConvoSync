package com.minepop.servegame.convosync;

import blir.net.AuthenticationRequest;
import blir.net.ChatMessage;
import blir.net.CommandMessage;
import blir.net.Message;
import blir.net.PlayerMessage;
import blir.net.PrivateMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
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

        SETIP, SETPORT, RECONNECT, DISCONNECT, STATUS, SETMAXPLAYERS
    }
    private int port, players, max = 25;
    private String ip, password;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    protected boolean connected, shouldBe = true, verified, isEss;
    private List<ChatListener> listeners = new ArrayList<ChatListener>();

    @Override
    public void onEnable() {
        try {
            port = getConfig().getInt("port");
            ip = getConfig().getString("ip");
            password = getConfig().getString("password");
            max = getConfig().getInt("max-players");
        } catch (NumberFormatException ex) {
        } catch (NullPointerException ex) {
        }
        if (ip == null || ip.equals("null") || port == 0 || password == null || password.equals("null")) {
            getLogger().warning("IP, port, or password missing.");
        } else {
            try {
                getLogger().log(Level.INFO, "Connecting to {0}:" + port, ip);
                connect();
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Error connecting to server: {0}", ex.toString());
                if (getConfig().getBoolean("auto-reconnect.after-connect-fail")) {
                    autoReconnect(getConfig().getInt("auto-reconnect.time-delay-ms"));
                }
            }
        }
        getServer().getPluginManager().registerEvents(this, this);
        players = getServer().getOnlinePlayers().length;
        if (players > max) {
            getLogger().info("Not communicating with the ConvoSync server due to high player count.");
        }
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
        getConfig().set("max-players", max);
        getConfig().set("notif.player-death", getConfig().getBoolean("notif.player-death"));
        getConfig().set("notif.afk", getConfig().getBoolean("notif.afk"));
        getConfig().set("auto-reconnect.after-connect-fail", getConfig().getBoolean("auto-reconnect.after-connect-fail"));
        getConfig().set("auto-reconnect.after-socket-error", getConfig().getBoolean("auto-reconnect.after-socket-error"));
        getConfig().set("auto-reconnect.after-socket-close", getConfig().getBoolean("auto-reconnect.after-socket-close"));
        getConfig().set("auto-reconnect.time-delay-ms", getConfig().getInt("auto-reconnect.time-delay-ms"));
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if (cmd.getName().equals("convosync")) {
                switch (Action.valueOf(args[0].toUpperCase())) {
                    case SETIP:
                        if (args.length != 2) {
                            sender.sendMessage(ChatColor.RED + "/convosync setip <ip>");
                            return true;
                        }
                        if (connected) {
                            sender.sendMessage(ChatColor.RED + "The server is currently connected. Please disconnect first.");
                            return true;
                        }
                        ip = args[1];
                        sender.sendMessage(ChatColor.GREEN + "Now using IP " + ChatColor.BLUE + ip + ChatColor.GREEN + ".");
                        return true;
                    case SETPORT:
                        if (args.length != 2) {
                            sender.sendMessage(ChatColor.RED + "/convosync setport <port>");
                            return true;
                        }
                        if (connected) {
                            sender.sendMessage(ChatColor.RED + "The server is currently connected. Please disconnect first.");
                            return true;
                        }
                        try {
                            port = Integer.parseInt(args[1]);
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(ChatColor.RED + "You did not enter a valid number.");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Nowing using port " + ChatColor.BLUE + port + ChatColor.GREEN + ".");
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
                        status(sender);
                        return true;
                    case DISCONNECT:
                        try {
                            shouldBe = false;
                            disconnect();
                        } catch (IOException ex) {
                            getLogger().log(Level.WARNING, "Error closing socket: {0}", ex.toString());
                        }
                        status(sender);
                        return true;
                    case STATUS:
                        status(sender);
                        return true;
                    case SETMAXPLAYERS:
                        if (args.length != 2) {
                            sender.sendMessage(ChatColor.RED + "/convosync setmaxplayers <max playeres>");
                            return true;
                        }
                        try {
                            max = Integer.parseInt(args[1]);
                        } catch (NumberFormatException ex) {
                            sender.sendMessage(ChatColor.RED + "You did not enter a valid number.");
                            return true;
                        }
                        sender.sendMessage(ChatColor.GREEN + "Now using max player count of " + ChatColor.BLUE + max + ChatColor.GREEN + ".");
                        return true;
                }
            } else if (cmd.getName().equals("csay")) {
                if (args.length == 0) {
                    return false;
                }
                String msg = "";
                for (String arg : args) {
                    msg += " " + arg;
                }
                if (connected) {
                    if (verified) {
                        if (players < max) {
                            out("c" + msg, false);
                        } else {
                            sender.sendMessage(ChatColor.RED + "There are too many players on this server to chat cross-server.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Cannot send message : Connection is not authenticated.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Cannot send message : Disconnected from server.");
                }
                getServer().dispatchCommand(sender, "say" + msg);
                return true;
            } else if (cmd.getName().equals("ctell")) {
                if (args.length < 2) {
                    return false;
                }
                if (!connected) {
                    sender.sendMessage(ChatColor.RED + "Cannot send message : Disconnected from server.");
                    return true;
                }
                if (!verified) {
                    sender.sendMessage(ChatColor.RED + "Cannot send message : Connection is not authenticated.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("console")) {
                    sender.sendMessage("To send a message to a console, use "
                            + "/ccmd <server name> tell console <msg>"
                            + "so that ConvoSync knows which console you wish to PM.");
                    return true;
                }
                String msg = "";
                for (int idx = 1; idx < args.length; idx++) {
                    msg += " " + args[idx];
                }
                msg = msg.substring(1);
                pm(args[0], sender, msg);
                return true;
            } else if (cmd.getName().equals("ccmd")) {
                if (args.length < 2) {
                    return false;
                }
                if (!connected) {
                    sender.sendMessage(ChatColor.RED + "Cannot send command : Disconnected from server.");
                    return true;
                }
                if (!verified) {
                    sender.sendMessage(ChatColor.RED + "Cannot send command : Connection is not authenticated.");
                    return true;
                }
                String command = args[1];
                for (int idx = 2; idx < args.length; idx++) {
                    command += " " + args[idx];
                }
                cmd(sender.getName(), args[0], command);
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent evt) {
        if (!evt.isCancelled() && players < max) {
            out("c" + evt.getFormat().replace("%1$s", evt.getPlayer().getDisplayName()).replace("%2$s", evt.getMessage()), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent evt) {
        players++;
        if (players == max) {
            getLogger().info("No longer communicating with the ConvoSync server due to high player count.");
        }
        if (players < max) {
            out("c " + evt.getJoinMessage(), false);
        }
        out("j" + evt.getPlayer().getName(), false);
        if (!connected && evt.getPlayer().hasPermission("convosync.convosync")) {
            evt.getPlayer().sendMessage(ChatColor.RED + "ConvoSync is currently disconnected from the chat server.");
            if (ip == null || ip.equals("null")) {
                evt.getPlayer().sendMessage(ChatColor.RED + "ConvoSync currently has no IP to connect to.");
            }
            if (port == 0) {
                evt.getPlayer().sendMessage(ChatColor.RED + "ConvoSync currently has no port to connect to.");
            }
            if (password == null || password.equals("null")) {
                evt.getPlayer().sendMessage(ChatColor.RED + "ConvoSync currently has no password to connect with.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent evt) {
        players--;
        if (players == max - 1) {
            getLogger().info("Resuming communication with the ConvoSync server due to reduced player count.");
        }
        if (players < max) {
            out("c " + evt.getQuitMessage(), false);
        }
        out("q" + evt.getPlayer().getName(), false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent evt) {
        if (getConfig().getBoolean("notif.afk") && players < max) {
            out("c " + evt.getDeathMessage(), false);
        }
    }

    private void connect() throws IOException {
        if (ip == null || ip.equals("null") || port == 0 || password == null || password.equals("null")) {
            return;
        }
        socket = new Socket(ip, port);
        in = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        connected = true;
        out(new AuthenticationRequest(getServer().getServerName(), password, "plugin"), true);
        final ConvoSync plugin = this;
        new Thread() {
            @Override
            public void run() {
                Object input;
                String msg;
                while (connected) {
                    try {
                        input = in.readObject();
                        if (!(input instanceof Message)) {
                            getLogger().log(Level.WARNING, "Unexpected message: {0}", input);
                        }
                        if (input != null && input instanceof Message) {
                            if (input instanceof PrivateMessage) {
                                PrivateMessage pm = (PrivateMessage) input;
                                Player player = getServer().getPlayerExact(pm.recip);
                                if (player != null) {
                                    player.sendMessage(ChatColor.GOLD + "[["
                                            + pm.server + "]" + pm.sender
                                            + ChatColor.GOLD + " -> me] "
                                            + ChatColor.WHITE + pm.msg);
                                    out(new PlayerMessage(ChatColor.GOLD + "[me -> " + pm.recip + ChatColor.GOLD + "] " + ChatColor.WHITE + pm.msg, pm.sender), false);
                                }
                                continue;
                            }
                            if (input instanceof CommandMessage) {
                                CommandMessage cmd = (CommandMessage) input;
                                getLogger().log(Level.INFO, "Executing remote command {0}", cmd);
                                try {
                                    getServer().dispatchCommand(new RemoteCommandSender(cmd.sender, plugin), cmd.cmd);
                                } catch (CommandException ex) {
                                    getLogger().log(Level.SEVERE, null, ex);
                                }
                                continue;
                            }
                            if (input instanceof PlayerMessage) {
                                PlayerMessage pm = (PlayerMessage) input;
                                Player player = getServer().getPlayerExact(pm.recip);
                                if (player != null) {
                                    player.sendMessage(pm.msg);
                                }
                                continue;
                            }
                            msg = ((ChatMessage) input).msg;
                            switch (msg.charAt(0)) {
                                case 'c':
                                    for (ChatListener listener : listeners) {
                                        listener.onInput(msg.substring(1));
                                    }
                                    getServer().broadcastMessage(msg.substring(1));
                                    break;
                                case 'l':
                                    sendList();
                                    break;
                                case 'v':
                                    verified = true;
                                    getLogger().info("Connection authenticated.");
                                    sendList();
                                    out("c has connected.", false);
                                    break;
                                case 'w':
                                    verified = false;
                                    getLogger().warning("Failed to authenticate with server.");
                                    break;
                                case 'd':
                                    getLogger().warning("The ConvoSync server has disconnected me!");
                                    connected = false;
                                    shouldBe = false;
                                    verified = false;
                                    break;
                                default:
                                    getLogger().log(Level.WARNING, "Unexpected message: {0}", input);
                                    break;
                            }
                        }
                        Thread.sleep(25);
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
                    } catch (ClassNotFoundException ex) {
                        getLogger().log(Level.SEVERE, "Fatal error.", ex);
                        connected = false;
                    } catch (InterruptedException ex) {
                        getLogger().log(Level.SEVERE, "Fatal error.", ex);
                        connected = false;
                    }
                }
            }
        }.start();
        if (getServer().getPluginManager().isPluginEnabled("Essentials") && getConfig().getBoolean("notif.afk")) {
            new AFKThread(this).start();
        }
        getLogger().info(socket.toString());
    }

    private void disconnect() throws IOException {
        String list = "r";
        for (Player player : getServer().getOnlinePlayers()) {
            list += "`" + player.getName();
        }
        out(list, false);
        out("c has disconnected.", false);
        if (out != null) {
            out("d", true);
        }
        shouldBe = false;
        verified = false;
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
                int attempts = 1;
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

    protected boolean out(String s, boolean override) {
        return out(new ChatMessage(s), override);
    }

    protected boolean pm(String recip, CommandSender sender, String msg) {
        return out(new PrivateMessage(recip, sender instanceof Player ? ((Player) sender).getDisplayName() : sender.getName(), msg, getServer().getServerName()), false);
    }

    protected boolean pm(String recip, String msg) {
        return out(new PlayerMessage(recip, msg), false);
    }

    protected boolean cmd(String sender, String server, String cmd) {
        return out(new CommandMessage(sender, server, cmd), false);
    }

    private boolean out(Object obj, boolean override) {
        if ((connected && verified) || override) {
            try {
                out.writeObject(obj);
                out.flush();
                return true;
            } catch (IOException ex) {
                if (!socket.isClosed()) {
                    getLogger().log(Level.WARNING, "Error writing object: " + obj, ex);
                }
            }
        }
        return false;
    }

    private void sendList() {
        String list = "l";
        for (Player player : getServer().getOnlinePlayers()) {
            list += "`" + player.getName();
        }
        out(list, false);
    }

    private void status(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Connection status: " + ChatColor.BLUE + connected);
        sender.sendMessage(ChatColor.GREEN + "Authentication status: " + ChatColor.BLUE + verified);
        sender.sendMessage(ChatColor.GREEN + "Essentials status: " + ChatColor.BLUE + isEss);
    }

    public boolean addChatListener(ChatListener listener) {
        return listeners.add(listener);
    }

    public interface ChatListener {

        public void onInput(String msg);
    }

    public boolean chat(String s) {
        return out("c" + s, false);
    }
}
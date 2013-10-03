package com.minepop.servegame.convosync.plugin;

import com.minepop.servegame.convosync.Main;
import com.minepop.servegame.convosync.net.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
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
public final class ConvoSync extends JavaPlugin implements Listener {

    private static enum Action {

        SETIP, SETPORT, RECONNECT, DISCONNECT, STATUS, SETMAXPLAYERS, USERS, VERSION
    }
    private int port, players, max = 25;
    private String ip, password;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    protected boolean connected, shouldBe = true, auth, isEss;
    private List<ChatListener> listeners = new ArrayList<ChatListener>();
    private List<User> users = new ArrayList<User>();
    private EssentialsTask essTask;
    private Map<String, String> lastPM = new HashMap<String, String>();

    @Override
    public void onEnable() {
        try {
            new MetricsLite(this).start();
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Couldn't use MetricsLite: {0}", ex.toString());
        }
        try {
            port = getConfig().getInt("port");
            ip = getConfig().getString("ip");
            password = getConfig().getString("password");
            max = getConfig().getInt("max-players");
        } catch (NumberFormatException ex) {
            getLogger().warning("Improper config.");
        } catch (NullPointerException ex) {
            getLogger().warning("Improper config.");
        }
        if (ip == null || ip.equals("null") || port == 0 || password == null
                || password.equals("null") || ip.equals("X") || password.equals("X")) {
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
            getServer().broadcastMessage(ChatColor.RED
                    + "Cross-server chat is disabled due to high player count.");
            out(new SetEnabledProperty(false), false);
        }
    }

    @Override
    public void onDisable() {
        if (connected) {
            try {
                disconnect();
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Error closing socket: {0}", ex.toString());
            }
        }
        getConfig().set("ip", ip);
        getConfig().set("port", port);
        getConfig().set("password", password);
        getConfig().set("max-players", max);
        getConfig().set("allow-cross-server-commands",
                getConfig().getBoolean("allow-cross-server-commands"));
        getConfig().set("notif.player-death",
                getConfig().getBoolean("notif.player-death"));
        getConfig().set("notif.afk", getConfig().getBoolean("notif.afk"));
        getConfig().set("auto-reconnect.after-connect-fail",
                getConfig().getBoolean("auto-reconnect.after-connect-fail"));
        getConfig().set("auto-reconnect.after-socket-error",
                getConfig().getBoolean("auto-reconnect.after-socket-error"));
        getConfig().set("auto-reconnect.after-socket-close",
                getConfig().getBoolean("auto-reconnect.after-socket-close"));
        getConfig().set("auto-reconnect.time-delay-ms",
                getConfig().getInt("auto-reconnect.time-delay-ms"));
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("convosync") && args.length != 0) {
            Action a;
            try {
                a = Action.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                return false;
            }
            switch (a) {
                case SETIP:
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "/convosync setip <ip>");
                        return true;
                    }
                    if (connected) {
                        sender.sendMessage(ChatColor.RED
                                + "The server is currently connected. Please disconnect first.");
                        return true;
                    }
                    ip = args[1];
                    sender.sendMessage(ChatColor.GREEN + "Now using IP "
                            + ChatColor.BLUE + ip + ChatColor.GREEN + ".");
                    return true;
                case SETPORT:
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED + "/convosync setport <port>");
                        return true;
                    }
                    if (connected) {
                        sender.sendMessage(ChatColor.RED
                                + "The server is currently connected. Please disconnect first.");
                        return true;
                    }
                    try {
                        port = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED + "You did not enter a valid number.");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Nowing using port "
                            + ChatColor.BLUE + port + ChatColor.GREEN + ".");
                    return true;
                case RECONNECT:
                    if (connected) {
                        try {
                            shouldBe = false;
                            disconnect();
                        } catch (IOException ex) {
                            getLogger().log(Level.WARNING,
                                    "Error closing socket: {0}", ex.toString());
                        }
                    }
                    try {
                        shouldBe = true;
                        connect();
                    } catch (IOException ex) {
                        getLogger().log(Level.WARNING,
                                "Error connecting to server: {0}", ex.toString());
                    }
                    status(sender);
                    return true;
                case DISCONNECT:
                    try {
                        shouldBe = false;
                        disconnect();
                    } catch (IOException ex) {
                        getLogger().log(Level.WARNING,
                                "Error closing socket: {0}", ex.toString());
                    }
                    status(sender);
                    return true;
                case STATUS:
                    status(sender);
                    return true;
                case SETMAXPLAYERS:
                    if (args.length != 2) {
                        sender.sendMessage(ChatColor.RED
                                + "/convosync setmaxplayers <max playeres>");
                        return true;
                    }
                    try {
                        max = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED
                                + "You did not enter a valid number.");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GREEN
                            + "Now using max player count of " + ChatColor.BLUE
                            + max + ChatColor.GREEN + ".");
                    return true;
                case USERS:
                    out(new UserListRequest(sender.getName()), false);
                    return true;
                case VERSION:
                    sender.sendMessage(ChatColor.GREEN + "v" + Main.VERSION);
                    return true;
            }
        } else if (cmd.getName().equals("csay") && args.length != 0) {
            StringBuilder sb = new StringBuilder();
            for (String arg : args) {
                sb.append(" ").append(arg);
            }
            String msg = sb.toString().substring(1);
            if (connected) {
                if (auth) {
                    if (players < max) {
                        out(msg, false);
                    } else {
                        sender.sendMessage(ChatColor.RED
                                + "There are too many players on this server to chat cross-server.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED
                            + "Cannot send message : Connection is not authenticated.");
                }
            } else {
                sender.sendMessage(ChatColor.RED
                        + "Cannot send message : Disconnected from server.");
            }
            getServer().dispatchCommand(sender, "say " + msg);
            return true;
        } else if (cmd.getName().equals("ctell") && args.length > 1) {
            if (!connected) {
                sender.sendMessage(ChatColor.RED
                        + "Cannot send message : Disconnected from server.");
                return true;
            }
            if (!auth) {
                sender.sendMessage(ChatColor.RED
                        + "Cannot send message : Connection is not authenticated.");
                return true;
            }
            if (args[0].equalsIgnoreCase("console")) {
                sender.sendMessage("You cannot send a private message to a console cross-server (yet).");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int idx = 1; idx < args.length; idx++) {
                sb.append(" ").append(args[idx]);
            }
            out(args[0], sender.getName(), sb.toString().substring(1));
            return true;
        } else if (cmd.getName().equals("ctellr") && args.length > 0) {
            String to = lastPM.get(sender.getName());
            if (to == null) {
                sender.sendMessage(ChatColor.RED
                        + "You haven't received any private messages to reply to.");
                return true;
            }
            if (!connected) {
                sender.sendMessage(ChatColor.RED
                        + "Cannot send message : Disconnected from server.");
                return true;
            }
            if (!auth) {
                sender.sendMessage(ChatColor.RED
                        + "Cannot send message : Connection is not authenticated.");
                return true;
            }
            StringBuilder sb = new StringBuilder();
            for (int idx = 0; idx < args.length; idx++) {
                sb.append(" ").append(args[idx]);
            }
            out(to, sender.getName(), sb.toString().substring(1));
            return true;
        } else if (cmd.getName().equals("ccmd") && args.length > 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED
                        + "You must be a player to use this command.");
                return true;
            }
            if (!connected) {
                sender.sendMessage(ChatColor.RED
                        + "Cannot send command : Disconnected from server.");
                return true;
            }
            if (!auth) {
                sender.sendMessage(ChatColor.RED
                        + "Cannot send command : Connection is not authenticated.");
                return true;
            }
            StringBuilder sb;
            if (args[0].startsWith("\"") && !args[0].endsWith("\"")) {
                sb = new StringBuilder(args[0].substring(1));
                for (int idx = 1; idx < args.length; idx++) {
                    if (args[idx].endsWith("\"")) {
                        String server = sb.append(" ")
                                .append(args[idx].substring(0, args[idx].length() - 1))
                                .toString();
                        if (args.length < idx + 2) {
                            return false;
                        }
                        sb = new StringBuilder(args[idx + 1]);
                        for (int idx2 = idx + 2; idx2 < args.length; idx2++) {
                            sb.append(" ").append(args[idx2]);
                        }
                        cmd(sender.getName(), server, sb.toString());
                        return true;
                    } else {
                        sb.append(" ").append(args[idx]);
                    }
                }
                sender.sendMessage("§cMissing closing quote.");
            } else {
                sb = new StringBuilder(args[1]);
                for (int idx = 2; idx < args.length; idx++) {
                    sb.append(" ").append(args[idx]);
                }
                cmd(sender.getName(), args[0], sb.toString());
                return true;
            }
            return false;
        } else if (cmd.getName().equals("togglecs")) {
            if (sender instanceof Player) {
                sender.sendMessage(ChatColor.GREEN + "Cross-server chat "
                        + ChatColor.BLUE
                        + (getUser(sender.getName()).toggle() ? "enabled" : "disabled")
                        + ChatColor.GREEN + ".");
            } else {
                sender.sendMessage(ChatColor.RED
                        + "You must be a player to use this command.");
            }
            return true;
        } else if (cmd.getName().equals("csregister")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED
                        + "You must be a player to use this command!");
                return true;
            }
            Random rng = new Random();
            StringBuilder sb = new StringBuilder(
                    new String[]{"poptarts", "fedora", "oops", "potato",
                "cabbage", "redhat", "pinkypie", "fluttershy", "badwolf",
                "iPassword", "tuesday", "tardis", "mycroft", "cafebabe",
                "steve", "herobrine", "creeper", "cthulhu", "zezima", "zelda",
                "hyrule"}[rng.nextInt(21)]);
            for (int idx = 0; idx < 4; idx++) {
                sb.append((char) (rng.nextInt(10) + 48));
            }
            String newPassword = sb.toString();
            out(new UserRegistration(sender.getName(), newPassword), false);
            sender.sendMessage(ChatColor.GREEN + "Attempting to register with password \""
                    + ChatColor.BLUE + newPassword + ChatColor.GREEN + "\".");
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent evt) {
        if (!evt.isCancelled() && getUser(evt.getPlayer().getName()).enabled) {
            out(evt.getFormat().replace("%1$s", evt.getPlayer().getDisplayName())
                    .replace("%2$s", evt.getMessage()), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent evt) {
        players++;
        if (players == max) {
            getServer().broadcastMessage(ChatColor.RED
                    + "Cross-server chat is now disabled due to high player count.");
            out(new SetEnabledProperty(false), false);
        }
        if (getUser(evt.getPlayer().getName()).enabled && ((isEss && essTask.chat(evt.getPlayer())) || !isEss)) {
            out(evt.getJoinMessage(), false);
        }
        out(new PlayerListMessage(evt.getPlayer().getName(), true), false);
        if (!connected && evt.getPlayer().hasPermission("convosync.convosync")) {
            evt.getPlayer().sendMessage(ChatColor.RED
                    + "ConvoSync is currently disconnected from the chat server.");
            if (ip == null || ip.equals("null") || ip.equals("X")) {
                evt.getPlayer().sendMessage(ChatColor.RED
                        + "ConvoSync currently has no IP to connect to.");
            }
            if (port == 0) {
                evt.getPlayer().sendMessage(ChatColor.RED
                        + "ConvoSync currently has no port to connect to.");
            }
            if (password == null || password.equals("null") || password.equals("X")) {
                evt.getPlayer().sendMessage(ChatColor.RED
                        + "ConvoSync currently has no password to connect with.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent evt) {
        players--;
        if (players == max - 1) {
            getServer().broadcastMessage(ChatColor.RED
                    + "Cross-server chat is now enabled due to reduced player count.");
            out(new SetEnabledProperty(true), false);
        }
        out(new PlayerListMessage(evt.getPlayer().getName(), false), false);
        if (getUser(evt.getPlayer().getName()).enabled && ((isEss && essTask.chat(evt.getPlayer())) || !isEss)) {
            out(evt.getQuitMessage(), false);
        }
        if (essTask != null) {
            essTask.remove(evt.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent evt) {
        if (getConfig().getBoolean("notif.player-death")) {
            out(evt.getDeathMessage(), false);
        }
    }

    private void connect() throws IOException {
        if (ip == null || ip.equals("null") || port == 0 || password == null
                || password.equals("null") || ip.equals("X") || password.equals("X")) {
            return;
        }
        socket = new Socket(ip, port);
        in = new ObjectInputStream(socket.getInputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        connected = true;
        String[] list = new String[getServer().getOnlinePlayers().length];
        for (int idx = 0; idx < list.length; idx++) {
            list[idx] = getServer().getOnlinePlayers()[idx].getName();
        }
        out(new PluginAuthenticationRequest(getServer().getServerName(), password, Main.VERSION, list), true);
        new Thread(new InputTask()).start();
        if (getServer().getPluginManager().isPluginEnabled("Essentials")) {
            essTask = new EssentialsTask(this);
            if (getConfig().getBoolean("notif.afk")) {
                new Thread(essTask).start();
            }
            getServer().getPluginManager().registerEvents(new EssentialsListener(this), this);
        }
        getLogger().info(socket.toString());
    }

    private void disconnect() throws IOException {
        if (out != null) {
            out(new DisconnectMessage(), true);
        }
        shouldBe = false;
        auth = false;
        if (socket != null) {
            socket.close();
        }
        connected = false;
    }

    private void autoReconnect(int pausetime) {
        if (!shouldBe && !isEnabled()) {
            return;
        }
        getLogger().log(Level.INFO, "Attempting to reconnect every {0} seconds...",
                (int) Math.round(pausetime / 1000));
        new Thread(new AutoReconnectTask(pausetime)).start();
    }

    protected boolean out(String s, boolean override) {
        return out(new ChatMessage(s, false), override);
    }

    protected boolean out(String recip, String msg) {
        return out(new PlayerMessage(recip, msg), false);
    }

    private boolean cmd(String sender, String server, String cmd) {
        return out(new CommandMessage(sender, server, cmd), false);
    }

    private boolean out(String recip, String sender, String msg) {
        return out(new PrivateMessage(recip, sender, msg, getServer().getServerName()), false);
    }

    protected boolean out(Object obj, boolean override) {
        if ((connected && auth) || override) {
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

    private void status(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Connection status: " + ChatColor.BLUE + connected);
        sender.sendMessage(ChatColor.GREEN + "Authentication status: " + ChatColor.BLUE + auth);
        sender.sendMessage(ChatColor.GREEN + "Essentials status: " + ChatColor.BLUE + isEss);
    }

    private User getUser(String name) {
        for (User user : users) {
            if (user.name.equals(name)) {
                return user;
            }
        }
        User user = new User(name);
        users.add(user);
        return user;
    }

    private static class User {

        private String name;
        private boolean enabled = true;

        private User(String name) {
            this.name = name;
        }

        private boolean toggle() {
            return (enabled = !enabled);
        }
    }

    public boolean addChatListener(ChatListener listener) {
        return listeners.add(listener);
    }

    public interface ChatListener {

        public void onInput(String msg);
    }

    public boolean chat(String s) {
        return out(s, false);
    }

    private void processMessage(Message msg) {
        if (msg instanceof PrivateMessage) {
            PrivateMessage pm = (PrivateMessage) msg;
            lastPM.put(pm.RECIPIENT, pm.SENDER);
            if (pm.RECIPIENT.equalsIgnoreCase("console")) {
                getLogger().log(Level.INFO, "{0}[[{1}]{2}{3} -> me] {4}{5}",
                        new Object[]{ChatColor.GOLD, pm.SERVER, pm.SENDER,
                    ChatColor.GOLD, ChatColor.WHITE, pm.MSG});
                out(new PlayerMessage(ChatColor.GOLD
                        + "[me -> [" + getServer().getServerName()
                        + "]" + pm.RECIPIENT + ChatColor.GOLD
                        + "] " + ChatColor.WHITE
                        + pm.MSG, pm.SENDER), false);
            } else {
                Player player = getServer().getPlayerExact(pm.RECIPIENT);
                if (player != null) {
                    player.sendMessage(ChatColor.GOLD + "[["
                            + pm.SERVER + "]" + pm.SENDER
                            + ChatColor.GOLD + " -> me] "
                            + ChatColor.WHITE + pm.MSG);
                    out(new PlayerMessage(ChatColor.GOLD
                            + "[me -> [" + getServer().getServerName()
                            + "]" + pm.RECIPIENT + ChatColor.GOLD
                            + "] " + ChatColor.WHITE
                            + pm.MSG, pm.SENDER), false);
                }
            }
            return;
        }
        if (msg instanceof PlayerMessage) {
            PlayerMessage pm = (PlayerMessage) msg;
            if (pm.RECIPIENT.equals("CONSOLE")) {
                getLogger().info(pm.MSG);
            } else {
                Player player = getServer().getPlayerExact(pm.RECIPIENT);
                if (player != null) {
                    player.sendMessage(pm.MSG);
                }
            }
            return;
        }
        if (msg instanceof ChatMessage) {
            ChatMessage chatMsg = (ChatMessage) msg;
            for (ChatListener l : listeners) {
                l.onInput(chatMsg.MSG);
            }
            if (chatMsg.OVERRIDE) {
                getServer().broadcastMessage(chatMsg.MSG);
            } else {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (getUser(player.getName()).enabled) {
                        player.sendMessage(chatMsg.MSG);
                    }
                }
                getLogger().info(Main.format(chatMsg.MSG));
            }
            return;
        }
        if (msg instanceof CommandMessage) {
            CommandMessage cmd = (CommandMessage) msg;
            if (!getConfig().getBoolean("allow-cross-server-commands")) {
                out(new PlayerMessage(ChatColor.RED
                        + "This server doesn't allow cross-server commands.",
                        cmd.SENDER), false);
                return;
            }
            getLogger().log(Level.INFO, "Executing remote command {0}", cmd);
            try {
                getServer().dispatchCommand(new RemoteCommandSender(cmd.SENDER, ConvoSync.this), cmd.CMD);
            } catch (CommandException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
            return;
        }
        if (msg instanceof AuthenticationRequestResponse) {
            AuthenticationRequestResponse response = (AuthenticationRequestResponse) msg;
            auth = response.AUTH;
            getLogger().info(auth ? "Connection authenticated."
                    : "Failed to authenticate with server.");
            if (!Main.VERSION.equals(response.VERSION)) {
                getLogger().log(Level.WARNING, "Version mismatch:"
                        + "Local version {0}, ConvoSync server version {1}",
                        new Object[]{Main.VERSION, response.VERSION});
            }
            if (auth) {
                getServer().broadcastMessage(ChatColor.GREEN
                        + "Now connected to the ConvoSync server.");
            }
            return;
        }
        if (msg instanceof DisconnectMessage) {
            String toBroadcast;
            switch (((DisconnectMessage) msg).REASON) {
                case RESTARTING:
                    toBroadcast = "The ConvoSync server is restarting.";
                    break;
                case CLOSING:
                    toBroadcast = "The ConvoSync server has shut down.";
                    break;
                case KICKED:
                    toBroadcast = "This Minecraft server has been kicked from the ConvoSync server.";
                    break;
                case CRASHED:
                    toBroadcast = "Something went wrong, and we're now disconnected from the ConvoSync server.";
                    break;
                default:
                    toBroadcast = "This server has been disconnected from the ConvoSync server for an unknown reason.";
                    break;
            }
            getServer().broadcastMessage(ChatColor.RED + toBroadcast);
            connected = false;
            shouldBe = false;
            auth = false;
        }
    }

    private class InputTask implements Runnable {

        @Override
        public void run() {
            Object input;
            while (connected) {
                try {
                    input = in.readObject();
                    if (input instanceof Message) {
                        processMessage((Message) input);
                    } else {
                        getLogger().log(Level.WARNING, "{0} isn't a message!", input);
                    }

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
                }
            }
        }
    }

    private class AutoReconnectTask implements Runnable {

        private int pausetime;

        private AutoReconnectTask(int pausetime) {
            this.pausetime = pausetime;
        }

        @Override
        public void run() {
            int attempts = 1;
            try {
                do {
                    try {
                        Thread.sleep(pausetime);
                        connect();
                    } catch (IOException ex) {
                        attempts++;
                    }
                } while (!connected);
            } catch (InterruptedException ex) {
                return;
            }
            getLogger().log(Level.INFO, "Successfully reconnected after {0} attempts.", attempts);
        }
    }
}
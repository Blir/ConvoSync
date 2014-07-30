package com.github.blir.convosync.plugin;

import com.github.blir.convosync.Main;
import com.github.blir.convosync.net.*;

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

    private int port, players, maxPlayers = 25;
    private String ip, password;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    protected boolean connected, shouldBe = true, auth, isEss;
    private final List<ChatListener> listeners = new LinkedList<ChatListener>();
    private final Map<String, User> users = new HashMap<String, User>();
    private EssentialsTask essTask;
    private final Map<String, String> lastPM = new HashMap<String, String>();

    public String randomString(int len) {
        return Main.randomString(null, len);
    }

    /**
     *
     * @return the port the plugin is listening on
     */
    public int getPort() {
        return port;
    }

    /**
     *
     * @return the IP the plugin is connected to
     */
    public String getIP() {
        return ip;
    }

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
            maxPlayers = getConfig().getInt("max-players");
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
        if (getServer().getPluginManager().isPluginEnabled("Essentials")) {
            getServer().getPluginManager().registerEvents(new EssentialsListener(this), this);
        }
        players = getServer().getOnlinePlayers().length;
        if (players > maxPlayers) {
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
        getConfig().set("max-players", maxPlayers);
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
    public boolean onCommand(CommandSender sender, Command cmd, String label,
                             String[] args) {
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
                        maxPlayers = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(ChatColor.RED
                                           + "You did not enter a valid number.");
                        return true;
                    }
                    sender.sendMessage(ChatColor.GREEN
                                       + "Now using max player count of " + ChatColor.BLUE
                                       + maxPlayers + ChatColor.GREEN + ".");
                    return true;
                case USERS:
                    if (connected) {
                        if (auth) {
                            out(new UserListRequest(sender.getName()), false);
                            sender.sendMessage(ChatColor.GREEN + "User list request sent.");
                        } else {
                            sender.sendMessage(ChatColor.RED
                                               + "Cannot send message : Connection is not authenticated.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED
                                           + "Cannot send message : Disconnected from server.");
                    }
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
                    if (players < maxPlayers) {
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
            StringBuilder sb = new StringBuilder();
            for (int idx = 1; idx < args.length; idx++) {
                sb.append(" ").append(args[idx]);
            }
            if (sender instanceof Player) {
                pm(new MessageRecipient(args[0]),
                   new MessageRecipient(sender.getName(), MessageRecipient.SenderType.MINECRAFT_PLAYER),
                   sb.toString().substring(1));
            } else {
                pm(new MessageRecipient(args[0]),
                   new MessageRecipient(getServer().getServerName(), MessageRecipient.SenderType.MINECRAFT_CONSOLE),
                   sb.toString().substring(1));
            }
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
            for (String arg : args) {
                sb.append(" ").append(arg);
            }
            if (sender instanceof Player) {
                pm(new MessageRecipient(to),
                   new MessageRecipient(sender.getName(), MessageRecipient.SenderType.MINECRAFT_PLAYER),
                   sb.toString().substring(1));
            } else {
                pm(new MessageRecipient(to),
                   new MessageRecipient(getServer().getServerName(), MessageRecipient.SenderType.MINECRAFT_CONSOLE),
                   sb.toString().substring(1));
            }
            return true;
        } else if (cmd.getName().equals("ccmd") && args.length > 1) {
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
                        cmd(new MessageRecipient(
                                sender instanceof Player
                                ? sender.getName() : getServer().getServerName(),
                                sender instanceof Player
                                ? MessageRecipient.SenderType.MINECRAFT_PLAYER
                                : MessageRecipient.SenderType.MINECRAFT_CONSOLE),
                            server, sb.toString());
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
                cmd(new MessageRecipient(
                        sender instanceof Player
                        ? sender.getName() : getServer().getServerName(),
                        sender instanceof Player
                        ? MessageRecipient.SenderType.MINECRAFT_PLAYER
                        : MessageRecipient.SenderType.MINECRAFT_CONSOLE),
                    args[0], sb.toString());
                return true;
            }
            return false;
        } else if (cmd.getName().equals("togglecs")) {
            if (sender instanceof Player) {
                User user = users.get(sender.getName());
                if (user == null) {
                    user = new User(sender.getName());
                    users.put(user.name, user);
                }
                sender.sendMessage(ChatColor.GREEN + "Cross-server chat "
                                   + ChatColor.BLUE
                                   + (user.toggle() ? "enabled" : "disabled")
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
            StringBuilder sb = new StringBuilder(
                    new String[]{"poptarts", "fedora", "oops", "potato",
                                 "cabbage", "redhat", "pinkypie", "fluttershy",
                                 "badwolf", "tuesday", "tardis", "mycroft",
                                 "cafebabe", "steve", "herobrine", "creeper",
                                 "cthulhu", "zezima", "zelda", "hyrule",
                                 "gavin", "mogar"}[Main.RNG.nextInt(22)]);
            for (int idx = 0; idx < 4; idx++) {
                sb.append((char) (Main.RNG.nextInt(10) + 48));
            }
            String newPassword = sb.toString();
            out(new UserRegistration(((Player) sender).getUniqueId(), sender.getName(), newPassword, randomString(100)), false);
            sender.sendMessage(ChatColor.GREEN + "Attempting to register with password \""
                               + ChatColor.BLUE + newPassword + ChatColor.GREEN + "\".");
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent evt) {
        User user = users.get(evt.getPlayer().getName());
        if (user == null) {
            user = new User(evt.getPlayer().getName());
            users.put(user.name, user);
        }
        if (!evt.isCancelled() && user.enabled) {
            out(evt.getFormat().replace("%1$s", evt.getPlayer().getDisplayName())
                    .replace("%2$s", evt.getMessage()), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent evt) {
        players++;
        if (players == maxPlayers) {
            getServer().broadcastMessage(ChatColor.RED
                                         + "Cross-server chat is now disabled due to high player count.");
            out(new SetEnabledProperty(false), false);
        }
        User user = users.get(evt.getPlayer().getName());
        if (user == null) {
            user = new User(evt.getPlayer().getName());
            users.put(user.name, user);
        }
        if (user.enabled && ((isEss && essTask.canChat(evt.getPlayer())) || !isEss)) {
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
        if (players == maxPlayers - 1) {
            getServer().broadcastMessage(ChatColor.RED
                                         + "Cross-server chat is now enabled due to reduced player count.");
            out(new SetEnabledProperty(true), false);
        }
        out(new PlayerListMessage(evt.getPlayer().getName(), false), false);
        User user = users.get(evt.getPlayer().getName());
        if (user == null) {
            user = new User(evt.getPlayer().getName());
            users.put(user.name, user);
        }
        if (user.enabled && ((isEss && essTask.canChat(evt.getPlayer())) || !isEss)) {
            out(evt.getQuitMessage(), false);
        }
        if (essTask != null) {
            essTask.users.remove(evt.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent evt) {
        if (getConfig().getBoolean("notif.player-death")) {
            out(evt.getDeathMessage(), false);
        }
    }

    private void connect()
            throws IOException {
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
        }
        getLogger().info(socket.toString());
    }

    private void disconnect()
            throws IOException {
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
        if (!shouldBe || !isEnabled()) {
            return;
        }
        getLogger().log(Level.INFO, "Attempting to reconnect every {0} seconds...",
                        (int) Math.round(pausetime / 1000));
        new Thread(new AutoReconnectTask(pausetime)).start();
    }

    protected boolean out(String s, boolean override) {
        return out(new ChatMessage(s, false), override);
    }

    protected boolean out(String msg, MessageRecipient recip) {
        return out(new PlayerMessage(msg, recip), false);
    }

    private boolean cmd(MessageRecipient sender, String server, String cmd) {
        return out(new CommandMessage(sender, server, cmd), false);
    }

    private boolean pm(MessageRecipient recip, MessageRecipient sender,
                       String msg) {
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

    /**
     * Adds a ChatListener. ChatListeners listen to cross-server canChat.
     *
     * @param listener the ChatListener to add
     * @return true if the ChatListener was added
     */
    public boolean addChatListener(ChatListener listener) {
        return listeners.add(listener);
    }

    /**
     * Used to listen to cross-server canChat.
     */
    public static interface ChatListener {

        /**
         * Called when cross-server canChat occurs.
         *
         * @param msg the canChat that occurred
         */
        public void onInput(String msg);
    }

    /**
     * Sends cross-server canChat.
     *
     * @param s the canChat to send
     * @return true if the canChat was sent
     */
    public boolean chat(String s) {
        return out(s, false);
    }

    private void processMessage(Message msg) {
        if (msg instanceof PrivateMessage) {
            PrivateMessage pm = (PrivateMessage) msg;
            lastPM.put(pm.RECIPIENT.NAME, pm.SENDER.NAME);
            if (pm.RECIPIENT.NAME.equalsIgnoreCase("console")) {
                getLogger().log(Level.INFO, "{0}[[{1}]{2}{3} -> me] {4}{5}",
                                new Object[]{ChatColor.GOLD, pm.SERVER,
                                             pm.SENDER.NAME,
                                             ChatColor.GOLD, ChatColor.WHITE,
                                             pm.MSG});
                out(new PlayerMessage(ChatColor.GOLD
                                      + "[me -> [" + getServer().getServerName()
                                      + "]" + pm.RECIPIENT.NAME + ChatColor.GOLD
                                      + "] " + ChatColor.WHITE
                                      + pm.MSG, pm.SENDER), false);
            } else {
                Player player = getServer().getPlayerExact(pm.RECIPIENT.NAME);
                if (player != null) {
                    player.sendMessage(ChatColor.GOLD + "[["
                                       + pm.SERVER + "]" + pm.SENDER.NAME
                                       + ChatColor.GOLD + " -> me] "
                                       + ChatColor.WHITE + pm.MSG);
                    out(new PlayerMessage(ChatColor.GOLD
                                          + "[me -> [" + getServer().getServerName()
                                          + "]" + pm.RECIPIENT.NAME + ChatColor.GOLD
                                          + "] " + ChatColor.WHITE
                                          + pm.MSG, pm.SENDER), false);
                }
            }
            return;
        }
        if (msg instanceof PlayerMessage) {
            PlayerMessage pm = (PlayerMessage) msg;
            if (pm.RECIPIENT.TYPE == MessageRecipient.SenderType.MINECRAFT_CONSOLE) {
                getLogger().info(pm.MSG);
            } else {
                Player player = getServer().getPlayerExact(pm.RECIPIENT.NAME);
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
                    User user = users.get(player.getName());
                    if (user == null) {
                        user = new User(player.getName());
                        users.put(user.name, user);
                    }
                    if (user.enabled) {
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
                out(new CommandResponse(ChatColor.RED
                                        + "This server doesn't allow cross-server commands.",
                                        cmd.SENDER), false);
                return;
            }
            getLogger().log(Level.INFO, "Executing remote command {0}", cmd);
            try {
                getServer().dispatchCommand(new RemoteCommandSender(cmd.SENDER, this), cmd.CMD);
            } catch (CommandException ex) {
                getLogger().log(Level.SEVERE, "Error executing remote command:", ex);
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
                    if (!socket.isClosed()) {
                        getLogger().log(Level.WARNING, "Error reading from socket: {0}", ex.toString());
                    }
                    if (socket.isClosed() && getConfig().getBoolean("auto-reconnect.after-socket-close")) {
                        autoReconnect(getConfig().getInt("auto-reconnect.time-delay-ms"));
                    } else {
                        if (getConfig().getBoolean("auto-reconnect.after-socker-error")) {
                            autoReconnect(getConfig().getInt("auto-reconnect.time-delay-ms"));
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    getLogger().log(Level.SEVERE, "Error reading from socket:", ex);
                }
            }
        }
    }

    private class AutoReconnectTask implements Runnable {

        private final int pausetime;

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

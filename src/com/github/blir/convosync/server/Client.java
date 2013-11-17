package com.github.blir.convosync.server;

import com.github.blir.convosync.net.*;
import com.github.blir.convosync.Main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.logging.Level;

import static com.github.blir.convosync.Main.COLOR_CHAR;
import static com.github.blir.convosync.Main.format;
import static com.github.blir.convosync.server.ConvoSyncServer.LOGGER;

/**
 * Instances of this class are server side representations of connected clients.
 *
 * @author Blir
 */
public final class Client implements Runnable {

    protected Socket socket;
    /**
     * The type of client this client is. APPLICATION means it's a GUI
     * application client. PLUGIN means it's a plugin client.
     */
    protected ClientType type;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    /**
     * Whether the Client is alive. Once it's dead, it doesn't go back.
     */
    protected boolean alive = true;
    /**
     * Whether the Client is authenticated. The Client must be authenticated to
     * utilize cross-server chat, commands, private messages, and some other
     * features. If the client type is APPLICATION, then it must authenticate
     * with its own registered server side password. If the client type is
     * PLUGIN, it must authenticate with the server side plugin password.
     */
    protected boolean auth = false;
    /**
     * Whether cross-server chat is enabled for this Client.
     */
    protected boolean enabled = true;
    /**
     * The name of this Client for use in chat and private messages. May contain
     * special characters for color formatting.
     */
    protected String name;
    /**
     * Same as name, but without any special color characters. Use this for
     * displaying the name in the console, or anywhere you don't want the color
     * characters.
     */
    protected String localname;
    /**
     * The version of the ConvoSync suite that this Client connected with.
     */
    protected String version;
    private final ConvoSyncServer server;
    private final Messenger messenger;

    public static enum ClientType {

        APPLICATION, PLUGIN
    }

    protected Client(Socket socket, ConvoSyncServer server, Messenger messenger)
            throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
        this.server = server;
        this.messenger = messenger;
    }

    @Override
    public void run() {
        Object input;
        while (alive) {
            try {
                input = in.readObject();
                LOGGER.log(Level.FINER, "Input: {0}", input);
                if (input instanceof Message) {
                    processMessage((Message) input);
                } else {
                    LOGGER.log(Level.WARNING,
                               "Kicking {0}; unexpected input: {1}",
                               new Object[]{localname, input});
                    close(true, true, true, DisconnectMessage.Reason.KICKED);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Couldn't read from socket in {0}: {1}",
                           new Object[]{localname, ex});
                if (!socket.isClosed() && !messenger.deadClients.contains(this)) {
                    crash(ex);
                }
            } catch (ClassNotFoundException ex) {
                crash(ex);
            }
        }
    }

    private synchronized void processMessage(Message msg)
            throws IOException {
        if (msg instanceof PrivateMessage || msg instanceof PlayerMessage) {
            messenger.out(msg, this);
        } else if (msg instanceof ChatMessage) {
            if (server.usePrefix || type == ClientType.APPLICATION) {
                if (server.chatColor == '\u0000') {
                    messenger.out("[" + name + "] " + ((ChatMessage) msg).MSG, this);
                } else {
                    messenger.out("[" + COLOR_CHAR + server.chatColor
                                  + name + COLOR_CHAR + "f] " + ((ChatMessage) msg).MSG,
                                  this);
                }
            } else {
                messenger.out((ChatMessage) msg, this);
            }
        } else if (msg instanceof CommandMessage) {
            if (type == ClientType.APPLICATION) {
                if (server.users.get(name).op) {
                    messenger.out((CommandMessage) msg, this);
                } else {
                    sendMsg(new PlayerMessage(
                            "You don't have permission to use cross-server commands.",
                            name), false);
                }
            } else {
                messenger.out((CommandMessage) msg, this);
            }
        } else if (msg instanceof PlayerListMessage) {
            PlayerListMessage list = (PlayerListMessage) msg;
            if (list.JOIN) {
                for (String element : list.LIST) {
                    String value = server.userMap.get(element);
                    if (value != null) {
                        Client client = server.getClient(element);
                        if (client == null) {
                            LOGGER.log(Level.WARNING,
                                       "{0} is already logged on {1}.",
                                       new Object[]{element, value});
                        } else {
                            messenger.out(new PlayerMessage(
                                    "You cannot be logged into the client and the game simultaneously.",
                                    element), this);
                            client.close(true, true, true,
                                         new DisconnectMessage(DisconnectMessage.Reason.KICKED));
                            messenger.deadClients.add(client);
                        }
                    }
                    server.userMap.put(element, localname);
                }
            } else {
                for (String element : list.LIST) {
                    server.userMap.remove(element);
                }
            }
            messenger.sendPlayerListUpdate();
        } else if (msg instanceof PluginAuthenticationRequest) {
            PluginAuthenticationRequest authReq = (PluginAuthenticationRequest) msg;
            name = authReq.NAME;
            localname = format(name);
            type = ClientType.PLUGIN;
            version = authReq.VERSION;
            if (!Main.VERSION.equals(version)) {
                LOGGER.log(Level.WARNING,
                           "Version mismatch: Local version {0}, {1} version {2}",
                           new Object[]{Main.VERSION, localname, version});
            }
            auth = authReq.PASSWORD.equals(server.pluginPassword);
            sendMsg(new AuthenticationRequestResponse(auth,
                                                      auth
                                                      ? null
                                                      : AuthenticationRequestResponse.Reason.INVALID_PASSWORD,
                                                      Main.VERSION), true);
            for (String element : authReq.PLAYERS) {
                if (server.userMap.get(element) != null) {
                    Client client = server.getClient(element);
                    if (client == null) {
                        LOGGER.log(Level.WARNING,
                                   "{0} is already logged on {1}.",
                                   new Object[]{element,
                                                server.userMap.get(element)});
                    } else {
                        messenger.out(new PlayerMessage(
                                "You cannot be logged into the client and the game simultaneously.",
                                element), this);
                        client.close(true, true, true,
                                     new DisconnectMessage(DisconnectMessage.Reason.KICKED));
                    }
                }
                server.userMap.put(element, localname);
            }
            messenger.out(name + " has connected.", this);
            messenger.sendPlayerListUpdate();
        } else if (msg instanceof ApplicationAuthenticationRequest) {
            type = ClientType.APPLICATION;
            AuthenticationRequestResponse.Reason reason = null;
            ApplicationAuthenticationRequest authReq = (ApplicationAuthenticationRequest) msg;
            version = authReq.VERSION;
            if (!Main.VERSION.equals(version)) {
                LOGGER.log(Level.WARNING,
                           "Version mismatch: Local version {0}, {1} version {2}",
                           new Object[]{Main.VERSION, authReq.NAME, version});
            }
            User user = server.users.get(authReq.NAME);
            if (user == null) {
                reason = AuthenticationRequestResponse.Reason.INVALID_USER;
            } else {
                if (authReq.PASSWORD.equals(user.PASSWORD)) {
                    if (server.banlist.contains(authReq.NAME)) {
                        reason = AuthenticationRequestResponse.Reason.BANNED;
                    } else {
                        if (server.userMap.get(authReq.NAME) == null) {
                            auth = true;
                        } else {
                            reason = AuthenticationRequestResponse.Reason.LOGGED_IN;
                        }
                    }
                } else {
                    reason = AuthenticationRequestResponse.Reason.INVALID_PASSWORD;
                }
            }
            sendMsg(new AuthenticationRequestResponse(auth, reason, Main.VERSION), true);
            if (auth) {
                localname = (name = authReq.NAME);
                messenger.out(name + " has joined.", this);
                server.userMap.put(name, "CS-Client");
                messenger.sendPlayerListUpdate();
            }
        } else if (msg instanceof PlayerVanishMessage) {
            PlayerVanishMessage vmsg = (PlayerVanishMessage) msg;
            if (vmsg.VANISH) {
                messenger.vanishPlayer(vmsg.PLAYER);
            } else {
                messenger.sendPlayerListUpdate();
            }
            return;
        }
        if (msg instanceof SetEnabledProperty) {
            enabled = ((SetEnabledProperty) msg).ENABLED;
            messenger.out(name + " has " + (enabled ? "enabled" : "disabled")
                          + " cross-server chat due to " + (enabled ? "reduced" : "high")
                          + " player count.", this);
            if (!enabled) {
                messenger.out("This doesn't affect cross-server private messages or commands.",
                              this);
            }
        } else if (msg instanceof UserRegistration) {
            UserRegistration reg = (UserRegistration) msg;
            server.users.remove(reg.USER);
            server.users.put(reg.USER, new User(reg));
            sendMsg(new PlayerMessage(
                    COLOR_CHAR + "aYou've successfully registered.",
                    reg.USER), false);
        } else if (msg instanceof UserPropertyChange) {
            UserPropertyChange prop = (UserPropertyChange) msg;
            switch (prop.PROPERTY) {
                case PASSWORD:
                    server.users.remove(name);
                    server.users.put(name, new User(new UserRegistration(name, prop.VALUE)));
                    sendMsg(new PlayerMessage("Password changed.", name), false);
                    break;
            }
        } else if (msg instanceof UserListRequest) {
            String sender = ((UserListRequest) msg).SENDER;
            sendMsg(new PlayerMessage(COLOR_CHAR + "aAll known online users:", sender), false);
            for (String user : server.userMap.keySet()) {
                sendMsg(new PlayerMessage(
                        COLOR_CHAR + "a" + user + " on server " + server.userMap.get(
                                user), sender), false);
            }
        } else if (msg instanceof DisconnectMessage) {
            close(false, false, true, (DisconnectMessage) null);
            messenger.out(name + " has disconnected.", this);
        } else if (msg instanceof CustomMessage) {
            CustomMessage cmsg = (CustomMessage) msg;
            if (!messenger.fireMessageHandlers(this, cmsg)) {
                LOGGER.log(Level.WARNING, "Improperly supported message: {0}", cmsg);
                LOGGER.log(Level.WARNING, "Disabled {0} of {1}''s handlers.",
                           new Object[]{
                               messenger.disableMessageHandlers(cmsg.getName()),
                               cmsg.getName()});
            }
        }
    }

    /**
     * Writes a Message to the Socket.
     *
     * @param msg      the Message to be written
     * @param override if true, send the Message no matter the value of enabled.
     *                 Use false for chat, and true for important stuff.
     */
    protected void sendMsg(Message msg, boolean override) {
        if (enabled || override) {
            sendMsg(msg);
        }
    }

    public void sendMsg(CustomMessage msg) {
        sendMsg((Message) msg, msg.overrides());
    }

    private void sendMsg(Message msg) {
        if (!alive) {
            messenger.deadClients.add(this);
            return;
        }
        try {
            out.writeObject(msg);
            out.flush();
            LOGGER.log(Level.FINER, "{0} sent to {1}!",
                       new Object[]{msg, localname});
        } catch (IOException ex) {
            if (!socket.isClosed()) {
                LOGGER.log(Level.SEVERE, "Could not write {0} to client {1}: {2}",
                           new Object[]{msg, localname, ex});
                if (!messenger.deadClients.contains(this)) {
                    crash(ex);
                }
            }
        }
    }

    /**
     * Closes this Client. This in turn closes its socket.
     *
     * @param kick   whether the Client is being kicked
     * @param msg    whether to write a DisconnectMessage to the socket
     * @param update whether to send a player list update as a result of this
     *               client being closed
     * @param dmsg   the DisconectMessage to write
     * @throws IOException if the socket closes improperly
     */
    protected void close(boolean kick, boolean msg, boolean update,
                         DisconnectMessage dmsg)
            throws IOException {
        messenger.deadClients.add(this);
        if (msg && dmsg != null) {
            sendMsg(dmsg, true);
        }
        alive = false;
        socket.close();
        if (kick) {
            messenger.out(name + " has been kicked.", this);
        }
        switch (type) {
            case PLUGIN:
                server.userMap.values().removeAll(Collections.singleton(localname));
                break;
            case APPLICATION:
                server.userMap.remove(name);
                break;
        }
        if (update) {
            messenger.sendPlayerListUpdate();
        }
    }

    /**
     * Closes this Client. Equivalent to close(kick, msg, new
     * DisconnectMessage(reason)).
     *
     * @param kick   whether the Client is being kicked
     * @param msg    whether to write a DisconnectMessage to the socket
     * @param update whether to send a player list udpate as a result of this
     *               client being closed
     * @param reason the reason for which the Client is being disconnected
     * @throws IOException if the socket closes improperly
     */
    protected void close(boolean kick, boolean msg, boolean update,
                         DisconnectMessage.Reason reason)
            throws IOException {
        close(kick, msg, update, reason == null ? null : new DisconnectMessage(reason));
    }

    private void crash(Throwable t) {
        LOGGER.log(Level.WARNING, "Closing {0}", localname);
        try {
            close(false, false, true, DisconnectMessage.Reason.CRASHED);
        } catch (IOException ex) {
            // ignore
        }
        messenger.out(COLOR_CHAR + "c" + localname
                      + " has crashed or improperly disconnected.", null);
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAuthenticated() {
        return auth;
    }

    public String getName() {
        return localname;
    }

    public String getFormattedName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ClientType getClientType() {
        return type;
    }

    @Override
    public String toString() {
        return "Client[" + localname + "," + version + ","
               + (alive ? "ALIVE" : "DEAD") + "," + socket + "]";
    }
}

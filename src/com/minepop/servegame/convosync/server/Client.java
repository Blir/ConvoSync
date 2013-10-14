package com.minepop.servegame.convosync.server;

import com.minepop.servegame.convosync.Main;
import com.minepop.servegame.convosync.net.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.logging.Level;

import static com.minepop.servegame.convosync.Main.COLOR_CHAR;
import static com.minepop.servegame.convosync.Main.format;
import static com.minepop.servegame.convosync.server.ConvoSyncServer.LOGGER;

/**
 *
 * @author Blir
 */
public final class Client implements Runnable {

    protected Socket socket;
    protected ClientType type;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    protected boolean alive = true, auth = false, enabled = true;
    protected String name, localname, version;
    private ConvoSyncServer server;
    private Messenger messenger;

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
                               "Closing {0}; unexpected input: {1}",
                               new Object[]{localname, input});
                    close1(true, true, new DisconnectMessage(DisconnectMessage.Reason.KICKED));
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Couldn't read from socket in {0}: {1}",
                           new Object[]{localname, ex});
                if (!socket.isClosed()) {
                    boolean crash = !messenger.deadClients.contains(this);
                    try {
                        close2(false, false, DisconnectMessage.Reason.CRASHED);
                    } catch (IOException ex2) {
                        LOGGER.log(Level.FINE, "Couldn't close {0}: {1}",
                                   new Object[]{localname, ex2});
                    }
                    if (crash) {
                        messenger.out(COLOR_CHAR + "c" + localname + " has crashed or improperly disconnected.", null);
                    }
                }
            } catch (ClassNotFoundException ex) {
                LOGGER.log(Level.WARNING, "Closing {0}: {1}",
                           new Object[]{localname, ex});
                try {
                    close2(false, false, DisconnectMessage.Reason.CRASHED);
                } catch (IOException ex2) {
                    LOGGER.log(Level.FINE, "Couldn't close {0}: {1}",
                               new Object[]{localname, ex2});
                }
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
                if (server.getUser(name).op) {
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
                    if (server.userMap.get(element) != null) {
                        Client client = server.getClient(element);
                        if (client == null) {
                            LOGGER.log(Level.WARNING,
                                       "{0} is already logged on {1}.",
                                       new Object[]{element, server.userMap.get(
                                element)});
                        } else {
                            messenger.out(new PlayerMessage(
                                    "You cannot be logged into the client and the game simultaneously.",
                                    element), this);
                            client.close1(true, true,
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
                        client.close1(true, true,
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
            User user = server.getUser(authReq.NAME);
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
            if (server.isUserRegistered(reg.USER)) {
                server.users.remove(server.getUser(reg.USER));
            }
            server.users.add(new User(reg));
            sendMsg(new PlayerMessage(
                    COLOR_CHAR + "aYou've successfully registered.",
                    reg.USER), false);
        } else if (msg instanceof UserPropertyChange) {
            UserPropertyChange prop = (UserPropertyChange) msg;
            switch (prop.PROPERTY) {
                case PASSWORD:
                    server.users.remove(server.getUser(name));
                    server.users.add(new User(new UserRegistration(name, prop.VALUE)));
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
            close2(false, false, null);
            messenger.out(name + " has disconnected.", this);
        }
    }

    protected void sendMsg(Message msg, boolean override) {
        if (enabled || override) {
            sendMsg(msg);
        }
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
                boolean crash = !messenger.deadClients.contains(this);
                try {
                    close1(false, false, null);
                } catch (IOException ex2) {
                    LOGGER.log(Level.FINE, "Couldn't close {0}: {1}",
                               new Object[]{localname, ex2});
                }
                if (crash) {
                    messenger.out(COLOR_CHAR + "c" + localname + " has crashed or improperly disconnected.", null);
                }
            }
        }
    }

    protected void close1(boolean kick, boolean msg, DisconnectMessage dmsg)
            throws IOException {
        messenger.deadClients.add(this);
        if (msg) {
            sendMsg(dmsg, true);
        }
        alive = false;
        socket.close();
        if (kick) {
            messenger.out(name + " has been kicked.", this);
        }
        if (type == ClientType.PLUGIN) {
            server.userMap.values().removeAll(Collections.singleton(localname));
        } else {
            server.userMap.remove(name);
        }
        messenger.sendPlayerListUpdate();
    }

    protected void close2(boolean kick, boolean msg,
                          DisconnectMessage.Reason reason)
            throws IOException {
        close1(kick, msg, new DisconnectMessage(reason));
    }

    @Override
    public String toString() {
        return "Client[" + localname + "," + version + ","
               + (alive ? "ALIVE" : "DEAD") + "," + socket + "]";
    }
}
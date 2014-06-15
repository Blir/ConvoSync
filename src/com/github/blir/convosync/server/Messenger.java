package com.github.blir.convosync.server;

import com.github.blir.convosync.net.*;
import com.github.blir.convosync.server.Client.ClientType;

import java.util.*;
import java.util.logging.Level;

import static com.github.blir.convosync.Main.COLOR_CHAR;
import static com.github.blir.convosync.Main.format;
import static com.github.blir.convosync.server.ConvoSyncServer.LOGGER;

/**
 * Handles all operations that involve iterating through or modifying the list
 * of alive clients. This is in order to be more thread safe.
 *
 * @author Blir
 */
public final class Messenger {

    private final ConvoSyncServer server;
    private final Map<String, String> userMap;
    private final List<Client> aliveClients;
    /**
     * Clients whose alive variable is false; to be removed from aliveClients.
     */
    protected Set<Client> deadClients = new HashSet<Client>();
    /**
     * Clients that have just connected. To be added to aliveClients.
     */
    protected Set<Client> newClients = new HashSet<Client>();

    protected Messenger(ConvoSyncServer server) {
        this.server = server;
        this.aliveClients = server.clients;
        this.userMap = server.userMap;
    }

    /**
     * Used to send all messages that require iterating through the list of
     * clients. This is in order to be more thread safe.
     *
     * @param o      the message to be sent
     * @param sender the sender of the message, null if from no particular
     *               client
     */
    protected void out(Object o, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }

        LOGGER.log(Level.FINER, "Messenger processing {0} : {1}",
                   new Object[]{o.getClass().getName(), o});

        synchronized (aliveClients) {

            if (!newClients.isEmpty()) {
                LOGGER.log(Level.FINE, "Adding {0}", clientListToString(newClients));
                aliveClients.addAll(newClients);
                newClients.clear();
            }

            if (o instanceof String) {
                out(new ChatMessage((String) o, sender == null), sender);
            } else if (o instanceof PrivateMessage) {
                out((PrivateMessage) o, sender);
            } else if (o instanceof PlayerMessage) {
                out((PlayerMessage) o, sender);
            } else if (o instanceof ChatMessage) {
                out((ChatMessage) o, sender);
            } else if (o instanceof CommandMessage) {
                out((CommandMessage) o, sender);
            } else if (o instanceof PlayerListUpdate) {
                out((PlayerListUpdate) o);
            } else if (o instanceof ServerListUpdate) {
                out((ServerListUpdate) o);
            } else if (o instanceof UserPropertyChange) {
                out((UserPropertyChange) o);
            } else if (o instanceof DisconnectMessage) {
                close((DisconnectMessage) o);
            }

            if (!deadClients.isEmpty()) {
                LOGGER.log(Level.FINE, "Removing {0}", clientListToString(deadClients));
                aliveClients.removeAll(deadClients);
                deadClients.clear();
            }
        }
    }

    private static String clientListToString(Collection<Client> clients) {
        StringBuilder sb = new StringBuilder();
        Iterator<Client> it = clients.iterator();
        for (int idx = 0; it.hasNext(); idx++) {
            if (idx != 0) {
                sb.append(",");
            }
            sb.append(it.next().localname);
        }
        return sb.toString();
    }

    private void out(ChatMessage msg, Client sender) {
        for (Client client : aliveClients) {
            if (client != sender && client.auth) {
                client.sendMsg(msg, false);
            }
        }
        LOGGER.log(Level.INFO, "[{0}] {1} ",
                   new Object[]{sender == null ? "NA" : sender.socket.getPort(),
                                format(msg.MSG)});
    }

    /**
     * Updates the player list for the APPLICATION clients.
     */
    protected void sendPlayerListUpdate() {
        out(new PlayerListUpdate(userMap.keySet().toArray(
                new String[userMap.size()]), false), null);
    }

    /**
     * Used to vanish (as defined by Essentials) the player with the specified
     * name. The name of the specified player is removed from the player list on
     * the ConvoSyncClient GUI clients. This does not affect OPs.
     *
     * @param s the Minecraft user name of the player to vanish
     */
    protected void vanishPlayer(String s) {
        Set<String> userCopy = (new HashMap<String, String>(userMap)).keySet();
        userCopy.remove(s);
        PlayerListUpdate update = new PlayerListUpdate(userCopy.toArray(
                new String[userCopy.size()]), true);
        out(update, null);
    }

    protected void sendServerListUpdate() {
        List<String> serverList = new LinkedList<String>();
        for (Client client : aliveClients) {
            if (client.type == ClientType.PLUGIN) {
                serverList.add(client.localname);
            }
        }
        out(new ServerListUpdate(serverList.toArray(new String[serverList.size()])), null);
    }

    private void out(PrivateMessage msg, Client sender) {
        String clientName = userMap.get(msg.RECIPIENT.NAME);
        if (clientName != null && clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT.NAME;
        }
        if (clientName == null && sender != null) {
            sender.sendMsg(new PlayerMessage(
                    COLOR_CHAR + "cPlayer \"" + COLOR_CHAR + "9" + msg.RECIPIENT.NAME + COLOR_CHAR + "c\" not found.",
                    msg.SENDER), false);
            return;
        }
        for (Client client : aliveClients) {
            if (client.localname.equals(clientName)) {
                client.sendMsg(msg, false);
                return;
            }
        }
    }

    private void out(PlayerMessage msg, Client sender) {
        switch (msg.RECIPIENT.TYPE) {
            case CONVOSYNC_CONSOLE:
                LOGGER.log(Level.INFO, msg.MSG);
                break;
            case CONVOSYNC_CLIENT:
                for (Client client : aliveClients) {
                    if (client.type == ClientType.APPLICATION
                        && client.localname.equals(msg.RECIPIENT.NAME)) {

                        client.sendMsg(msg, false);
                        return;
                    }
                }
                break;
            case MINECRAFT_PLAYER:
                String clientName = userMap.get(msg.RECIPIENT.NAME);
                for (Client client : aliveClients) {
                    if (client.type == ClientType.PLUGIN
                        && client.localname.equals(clientName)) {

                        client.sendMsg(msg, false);
                        return;
                    }
                }
            case MINECRAFT_CONSOLE:
                for (Client client : aliveClients) {
                    if (client.type == ClientType.PLUGIN
                        && client.localname.equals(msg.RECIPIENT.NAME)) {

                        client.sendMsg(msg, false);
                        return;
                    }
                }
                break;
        }
    }

    private void out(CommandMessage msg, Client sender) {
        for (Client client : aliveClients) {
            if (client.type == ClientType.PLUGIN
                && client.name.equalsIgnoreCase(msg.TARGET)) {
                client.sendMsg(msg, false);
                if (sender != null) {
                    sender.sendMsg(new CommandResponse(
                            COLOR_CHAR + "aIssuing command " + COLOR_CHAR + "9"
                            + msg.CMD + COLOR_CHAR + "a on " + COLOR_CHAR + "9"
                            + msg.TARGET + COLOR_CHAR + "a.", msg.SENDER), false);
                }
                return;
            }
        }
        if (sender != null) {
            sender.sendMsg(new PlayerMessage(
                    COLOR_CHAR + "cServer " + COLOR_CHAR
                    + "9" + msg.TARGET + COLOR_CHAR + "c not found.", msg.SENDER), false);
        } else {
            LOGGER.log(Level.INFO, "No such Minecraft server {0}.", msg.TARGET);
        }
    }

    private void out(PlayerListUpdate update) {
        for (Client client : aliveClients) {
            if (client.type == ClientType.APPLICATION && client.auth
                && (!update.VANISH || !server.users.get(client.name).op)) {
                client.sendMsg(update, false);
            }
        }
    }

    private void out(ServerListUpdate update) {
        for (Client client : aliveClients) {
            if (client.type == ClientType.APPLICATION
                && client.auth) {
                client.sendMsg(update, false);
            }
        }
    }

    private void out(UserPropertyChange propChange) {
        for (Client client : aliveClients) {
            if (client.type == ClientType.APPLICATION && propChange.RECIP.NAME.equals(client.name)) {
                client.sendMsg(propChange, false);
            }
        }
    }

    private void close(DisconnectMessage dmsg) {
        for (Client client : aliveClients) {
            client.close(false, true, false, dmsg);
        }
    }
}

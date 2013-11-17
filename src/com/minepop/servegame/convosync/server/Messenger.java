package com.minepop.servegame.convosync.server;

import com.minepop.servegame.convosync.net.*;
import com.minepop.servegame.convosync.server.Client.ClientType;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static com.minepop.servegame.convosync.Main.COLOR_CHAR;
import static com.minepop.servegame.convosync.Main.format;
import static com.minepop.servegame.convosync.server.ConvoSyncServer.LOGGER;

/**
 * Handles all operations that involve iterating through or modifying the list
 * of alive clients. This is in order to be more thread safe.
 *
 * @author Blir
 */
public final class Messenger {

    private final ConvoSyncServer server;
    private final Map<String, String> userMap;
    private final Set<MessageHandler> handlers = new HashSet<MessageHandler>();
    private final Set<MessageFilter> filters = new HashSet<MessageFilter>();
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
     * @throws IllegalArgumentException if o is not a Message or a String
     */
    protected void out(Object o, Client sender)
            throws IllegalArgumentException {
        if (sender != null && !sender.auth) {
            return;
        }

        LOGGER.log(Level.FINER, "Messenger processing a(n) {0} : {1}",
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
            } else if (o instanceof DisconnectMessage) {
                close((DisconnectMessage) o);
            } else if (o instanceof CustomMessage) {
                if (!out((CustomMessage) o, sender)) {
                    LOGGER.log(Level.WARNING, "No filter found for custom message {0}", o);
                }
            } else {
                throw new IllegalArgumentException(String.format("Unsupported object: %s", o.toString()));
            }

            if (!deadClients.isEmpty()) {
                LOGGER.log(Level.FINE, "Removing {0}", clientListToString(deadClients));
                aliveClients.removeAll(deadClients);
                deadClients.clear();
            }
        }
    }

    private boolean out(CustomMessage msg, Client sender) {
        MessageFilter filter = getMessageFilter(msg.getName());
        if (filter == null) {
            return false;
        }
        if (!filter.isEnabled()) {
            return true;
        }
        for (Client client : aliveClients) {
            if (filter.filter(msg, client)) {
                client.sendMsg(msg, msg.overrides());
            }
        }
        return true;
    }

    private MessageFilter getMessageFilter(String pluginName) {
        for (MessageFilter filter : filters) {
            if (filter.getName().equals(pluginName)) {
                return filter;
            }
        }
        return null;
    }

    protected boolean fireMessageHandlers(Client client, Message msg) {
        for (MessageHandler handler : handlers) {
            if (handler.handle(client, msg)) {
                return true;
            }
        }
        return false;
    }

    public int disableMessageHandlers(String name) {
        int count = 0;
        for (MessageHandler handler : handlers) {
            if (handler.getName().equals(name)) {
                handler.disable();
                count++;
            }
        }
        return count;
    }

    public boolean disableMessageFilter(String name) {
        MessageFilter filter = getMessageFilter(name);
        if (filter == null) {
            return false;
        }
        filter.disable();
        return true;
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
        PlayerListUpdate update;
        update = new PlayerListUpdate(userMap.keySet().toArray(
                new String[userMap.size()]), false);
        out(update, null);
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

    private void out(PrivateMessage msg, Client sender) {
        String clientName = userMap.get(msg.RECIPIENT);
        if (clientName != null && clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT;
        }
        if (clientName == null && sender != null) {
            sender.sendMsg(new PlayerMessage(
                    COLOR_CHAR + "cPlayer \"" + COLOR_CHAR + "9" + msg.RECIPIENT + COLOR_CHAR + "c\" not found.",
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
        String clientName = userMap.get(msg.RECIPIENT);
        if (clientName == null) {
            LOGGER.log(Level.WARNING, "Is {0} a ghost?", msg.RECIPIENT);
            return;
        }
        if (clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT;
        }
        for (Client client : aliveClients) {
            if (client.localname.equals(clientName)) {
                client.sendMsg(msg, false);
                return;
            }
        }
    }

    private void out(CommandMessage msg, Client sender) {
        for (Client client : aliveClients) {
            if (client.type == ClientType.PLUGIN
                && client.name.equalsIgnoreCase(msg.TARGET)) {
                client.sendMsg(msg, false);
                if (sender != null) {
                    sender.sendMsg(new PlayerMessage(
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

    private void close(DisconnectMessage dmsg) {
        for (Client client : aliveClients) {
            try {
                client.close(false, true, false, dmsg);
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Error closing {0} : {1}",
                           new Object[]{client.localname, ex});
            }
        }
    }

    public boolean addMessageHandler(MessageHandler handler) {
        return handlers.add(handler);
    }

    public boolean addMessageFilter(MessageFilter filter) {
        return filters.add(filter);
    }
}

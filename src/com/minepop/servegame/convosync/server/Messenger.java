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
 *
 * @author Blir
 */
public final class Messenger {

    private ConvoSyncServer server;
    private Map<String, String> userMap;
    private List<Client> aliveClients;
    protected Set<Client> deadClients = new HashSet<Client>();
    protected Set<Client> newClients = new HashSet<Client>();

    protected Messenger(ConvoSyncServer server) {
        this.server = server;
        this.aliveClients = server.clients;
        this.userMap = server.userMap;
    }

    protected synchronized void out(Object o, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }

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
        }

        if (!deadClients.isEmpty()) {
            LOGGER.log(Level.FINE, "Removing {0}", clientListToString(deadClients));
            aliveClients.removeAll(deadClients);
            deadClients.clear();
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

    protected void sendPlayerListUpdate() {
        PlayerListUpdate update;
        update = new PlayerListUpdate(userMap.keySet().toArray(
                new String[userMap.size()]), false);
        out(update, null);
    }

    protected void vanishPlayer(String s) {
        Set<String> userCopy = (new HashMap<String, String>(userMap)).keySet();
        userCopy.remove(s);
        PlayerListUpdate update = new PlayerListUpdate(userCopy.toArray(
                new String[userCopy.size()]), true);
        out(update, null);
    }

    private void out(PrivateMessage msg, Client sender) {
        String clientName = userMap.get(msg.RECIPIENT);
        if (clientName.equals("CS-Client")) {
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
                && (!update.VANISH || !server.getUser(client.name).op)) {
                client.sendMsg(update, false);
            }
        }
    }

    private void close(DisconnectMessage dmsg) {
        for (Client client : aliveClients) {
            try {
                client.close1(false, true, dmsg);
            } catch (IOException ex) {
                // ignore
            }
        }
    }
}

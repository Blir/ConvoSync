package com.minepop.servegame.convosync;

import com.minepop.servegame.convosync.net.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.*;

/**
 *
 * @author Blir
 */
public class ConvoSyncServer {

    private enum Command {

        EXIT, STOP, RESTART, KICK, LIST, USERS, NAME, HELP, DEBUG
    }
    private int port;
    private ServerSocket socket;
    private Scanner in;
    private boolean open = true, debug = false;
    private ArrayList<Client> clients = new ArrayList<Client>();
    private String name = "ConvoSyncServer", pluginPassword, applicationPassword;
    private Map<String, String> userMap = new HashMap<String, String>();
    private static final Logger LOGGER = Logger.getLogger(ConvoSyncServer.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new ConvoSyncServer().run(args);
    }

    public void run(String[] startupArgs) throws IOException {
        Handler handler = new ConsoleHandler();
        Formatter formatter = new CustomFormatter();
        handler.setFormatter(formatter);
        handler.setLevel(Level.CONFIG);
        LOGGER.addHandler(handler);
        handler = new FileHandler("CS-Server.log", true);
        handler.setFormatter(formatter);
        handler.setLevel(Level.CONFIG);
        LOGGER.addHandler(handler);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.CONFIG);
        LOGGER.log(Level.INFO, java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG)
                .format(java.util.Calendar.getInstance().getTime()));
        LOGGER.log(Level.INFO, toString());
        LOGGER.log(Level.CONFIG, "Java Version: {0}", System.getProperty("java.version"));
        LOGGER.log(Level.CONFIG, "OS Architexture: {0}", System.getProperty("os.arch"));
        LOGGER.log(Level.CONFIG, "OS Name: {0}", System.getProperty("os.name"));
        LOGGER.log(Level.CONFIG, "OS Version: {0}", System.getProperty("os.version"));
        in = new Scanner(System.in);
        for (String arg : startupArgs) {
            try {
                if (arg.startsWith("Port:")) {
                    port = Integer.parseInt(arg.split(":")[1]);
                } else if (arg.startsWith("Name:")) {
                    name = arg.split(":")[1];
                } else if (arg.startsWith("ApplicationPassword:")) {
                    applicationPassword = arg.split(":")[1];
                } else if (arg.startsWith("PluginPassword:")) {
                    pluginPassword = arg.split(":")[1];
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            } catch (ArrayIndexOutOfBoundsException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            }
        }
        while (port == 0) {
            System.out.print("Enter port: ");
            try {
                port = Integer.parseInt(in.nextLine());
                if (port <= 0) {
                    System.out.println("You did not enter a valid number.");
                    port = 0;
                }
            } catch (NumberFormatException ex) {
                System.out.println("You did not enter a valid number.");
            }
        }
        while (pluginPassword == null || pluginPassword.equals("")) {
            System.out.print("Enter a password that the ConvoSync plugins will use to connect: ");
            pluginPassword = in.nextLine();
        }
        while (applicationPassword == null || applicationPassword.equals("")) {
            System.out.print("Enter a password that the ConvoSync application clients will use to connect: ");
            applicationPassword = in.nextLine();
        }
        open();
        final ConvoSyncServer server = this;
        new Thread() {
            @Override
            public void run() {
                Socket clientSocket;
                Client client;
                while (open) {
                    try {
                        clientSocket = socket.accept();
                        client = new Client(clientSocket);
                        clients.add(client);
                        client.setServer(server);
                        client.start();
                        LOGGER.log(Level.INFO, "Accepted a connection: {0}", client);
                    } catch (Exception ex) {
                        if (!socket.isClosed()) {
                            LOGGER.log(Level.SEVERE, "Error accepting a connection!", ex);
                        }
                    }
                }
            }
        }.start();

        String input;
        while (open || alive()) {
            try {
                input = in.nextLine();
                if (input != null && input.length() > 0) {
                    if (input.charAt(0) == '/') {
                        int delim = input.indexOf(" ");
                        Command cmd;
                        try {
                            cmd = Command.valueOf((delim > 0 ? input.substring(0, delim) : input).substring(1).toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            cmd = Command.HELP;
                        }
                        String[] args = delim > 0 ? input.substring(delim + 1).split(" ") : new String[0];
                        onCommand(cmd, args);
                    } else {
                        out("<" + COLOR_CHAR + "5" + name + COLOR_CHAR + "f> " + input, null);
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error in input Thread!", ex);
            }
        }
    }

    private boolean alive() {
        for (Client client : clients) {
            if (client.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private void restart() throws IOException {
        close(false);
        open();
    }

    private void open() throws IOException {
        socket = new ServerSocket(port);
        LOGGER.log(Level.INFO, socket.toString());
    }

    private void close(boolean force) throws IOException {
        LOGGER.log(Level.INFO, "Closing {0}", this);
        try {
            for (Client client : clients) {
                client.close();
            }
        } finally {
            userMap.clear();
            clients.clear();
            try {
                socket.close();
            } finally {
                if (force) {
                    System.exit(-1);
                }
            }
        }
    }

    private void out(String msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        for (Client client : clients) {
            if (client != sender && client.auth) {
                if (sender == null && client.enabled) {
                    client.sendMsg(new ChatMessage(msg, true));
                } else {
                    client.sendMsg(msg, false);
                }
            }
        }
        LOGGER.log(Level.INFO, "[{0}] {1} ",
                new Object[]{sender == null ? "NA" : sender.socket.getPort(), format(msg)});
    }

    private static void debug(String input) {
        LOGGER.log(Level.FINEST, "Debug info for message: {0}", input);
        for (int idx = 0; idx < input.length(); idx++) {
            LOGGER.log(Level.FINEST, "Character {0} : {1} : {2}",
                    new Object[]{idx, input.charAt(idx),
                Integer.toHexString(input.charAt(idx) | 0x10000).substring(1)});
        }
    }

    private void notify(PlayerListMessage notification, Client.ClientType type) {
        for (Client client : clients) {
            if (client.type == type && client.auth) {
                client.sendMsg(notification);
            }
        }
    }

    private void out(PrivateMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        String server = userMap.get(msg.RECIPIENT);
        if (server == null && sender != null) {
            sender.sendMsg(new PlayerMessage(COLOR_CHAR + "cPlayer \"" + COLOR_CHAR + "9" + msg.RECIPIENT + COLOR_CHAR + "c\"not found.", msg.SENDER));
            return;
        }
        for (Client client : clients) {
            if (client.type == Client.ClientType.PLUGIN && client.localname.equals(server)) {
                client.sendMsg(msg);
            }
        }
    }

    private void out(PlayerMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        for (Client client : clients) {
            if (client.type == Client.ClientType.PLUGIN) {
                client.sendMsg(msg);
            }
        }
    }

    private void out(CommandMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        for (Client client : clients) {
            if (client.type == Client.ClientType.PLUGIN && client.name.equalsIgnoreCase(msg.TARGET)) {
                client.sendMsg(msg);
                if (sender != null) {
                    sender.sendMsg(new PlayerMessage(COLOR_CHAR + "a" + msg + " sent!", msg.SENDER));
                }
                return;
            }
        }
        if (sender != null) {
            sender.sendMsg(new PlayerMessage(COLOR_CHAR + "cServer " + COLOR_CHAR
                    + "9" + msg.TARGET + COLOR_CHAR + "c not found.", msg.SENDER));
        }
    }

    private static class Client extends Thread {

        private enum ClientType {

            PLUGIN, APPLICATION
        }
        private ConvoSyncServer server;
        private Socket socket;
        private ClientType type;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private boolean alive = true, auth = false, enabled = true;
        private String name, localname;

        private Client(Socket socket) throws IOException {
            super();
            this.socket = socket;
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            Object input;
            while (alive) {
                try {
                    input = in.readObject();
                    LOGGER.log(Level.FINER, "Input: {0}", input);
                    if (!(input instanceof Message)) {
                        LOGGER.log(Level.WARNING, "{0} isn't a message!", input);
                        continue;
                    }
                    if (input instanceof PrivateMessage) {
                        server.out((PrivateMessage) input, this);
                        continue;
                    }
                    if (input instanceof PlayerMessage) {
                        server.out((PlayerMessage) input, this);
                        continue;
                    }
                    if (input instanceof ChatMessage) {
                        if (enabled) {
                            server.out("[" + name + "] " + ((ChatMessage) input).MSG, this);
                        }
                        continue;
                    }
                    if (input instanceof CommandMessage) {
                        server.out((CommandMessage) input, this);
                        continue;
                    }
                    if (input instanceof PlayerListMessage) {
                        PlayerListMessage msg = (PlayerListMessage) input;
                        server.notify(msg, ClientType.APPLICATION);
                        for (String element : msg.LIST) {
                            if (msg.JOIN) {
                                server.userMap.put(element, localname);
                            } else {
                                server.userMap.remove(element);
                            }
                        }
                        continue;
                    }
                    if (input instanceof PluginAuthenticationRequest) {
                        PluginAuthenticationRequest authReq = (PluginAuthenticationRequest) input;
                        name = authReq.NAME;
                        localname = format(name);
                        type = ClientType.PLUGIN;
                        auth = authReq.PASSWORD.equals(server.pluginPassword);
                        sendMsg(new AuthenticationRequestResponse(auth));
                        server.notify(new PlayerListMessage(authReq.PLAYERS, true), ClientType.APPLICATION);
                        for (String element : authReq.PLAYERS) {
                            server.userMap.put(element, localname);
                        }
                        server.out(name + " has connected.", this);
                        continue;
                    }
                    if (input instanceof ApplicationAuthenticationRequest) {
                        ApplicationAuthenticationRequest authReq = (ApplicationAuthenticationRequest) input;
                        name = authReq.NAME;
                        type = ClientType.APPLICATION;
                        auth = authReq.PASSWORD.equals(server.applicationPassword);
                        sendMsg(new AuthenticationRequestResponse(auth));
                        sendMsg(new PlayerListMessage(server.userMap.keySet().toArray(new String[server.userMap.keySet().size()]), true));
                        server.out(name + " has joined.", this);
                        continue;
                    }
                    if (input instanceof SetEnabledProperty) {
                        enabled = ((SetEnabledProperty) input).ENABLED;
                        server.out(name + " has " + (enabled ? "enabled" : "disabled")
                                + " cross-server chat due to " + (enabled ? "reduced" : "high")
                                + " player count.", this);
                        if (!enabled) {
                            server.out("This doesn't affect cross-server private messages or commands.", this);
                        }
                        continue;
                    }
                    if (input instanceof DisconnectMessage) {
                        server.out(name + " has disconnected.", this);
                        server.userMap.values().removeAll(Collections.singleton(localname));
                        alive = false;
                        server.clients.remove(this);
                    }
                } catch (IOException ex) {
                    alive = false;
                    if (server.open) {
                        server.clients.remove(this);
                    }
                    if (!socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException ex2) {
                            LOGGER.log(Level.WARNING, "Error disconnecting client " + this, ex2);
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    alive = false;
                    if (server.open) {
                        server.clients.remove(this);
                    }
                    LOGGER.log(Level.SEVERE, "Fatal error in client " + this, ex);
                    try {
                        socket.close();
                    } catch (IOException ex2) {
                        LOGGER.log(Level.WARNING, "Error disconnecting client " + this, ex2);
                    }
                }
            }
        }

        private void sendMsg(String s, boolean override) {
            if (enabled || override) {
                sendMsg(new ChatMessage(s, false));
            }
        }

        private void sendMsg(Object obj) {
            try {
                out.writeObject(obj);
                out.flush();
                LOGGER.log(Level.FINER, "{0} sent!", obj);
            } catch (IOException ex) {
                if (!socket.isClosed()) {
                    LOGGER.log(Level.SEVERE, "Could not write object " + obj, ex);
                }
            }
        }

        private void close() throws IOException {
            alive = false;
            sendMsg(new DisconnectMessage());
            socket.close();
        }

        @Override
        public String toString() {
            return "Client[" + localname + "," + socket + "," + super.toString() + "]";
        }

        private void setServer(ConvoSyncServer server) {
            this.server = server;
        }
    }

    @Override
    public String toString() {
        return "ConvoSyncServer 1.0.0";
    }

    private void onCommand(Command cmd, String[] args) {
        LOGGER.log(Level.INFO, "Executing command {0}", cmd);
        switch (cmd) {
            case EXIT:
            case STOP:
                open = false;
                try {
                    close(args.length > 0 && args[0].equalsIgnoreCase("force"));
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error closing!", ex);
                }
                break;
            case RESTART:
                try {
                    restart();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error restarting!", ex);
                }
                break;
            case KICK:
                if (args.length < 1) {
                    LOGGER.log(Level.INFO, "Usage: /kick <port>");
                    break;
                }
                int id;
                try {
                    id = Integer.parseInt(args[0]);
                } catch (NumberFormatException ex) {
                    LOGGER.log(Level.INFO, "You did not enter a valid number.");
                    break;
                }
                boolean found = false;
                for (Client client : clients) {
                    if (client.socket.getPort() == id) {
                        found = true;
                        LOGGER.log(Level.INFO, "Closing {0}", client);
                        try {
                            client.close();
                            LOGGER.log(Level.INFO, "Client closed.");
                            out(client.name + " has been kicked.", client);
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "Error closing " + client, ex);
                        }
                        break;
                    }
                }
                if (!found) {
                    LOGGER.log(Level.INFO, "Socket with port {0} not found.", id);
                }
                break;
            case LIST:
                if (clients.isEmpty()) {
                    LOGGER.log(Level.INFO, "There are currently no connected clients.");
                } else {
                    LOGGER.log(Level.INFO, "All connected clients:");
                    for (Client client : clients) {
                        LOGGER.log(Level.INFO, "{0}", client);
                    }
                }
                break;
            case USERS:
                if (userMap.isEmpty()) {
                    LOGGER.log(Level.INFO, "No known online users.");
                } else {
                    for (String key : userMap.keySet()) {
                        LOGGER.log(Level.INFO, "User {0} on server {1}", new String[]{key, userMap.get(key)});
                    }
                }
                break;
            case NAME:
                LOGGER.log(Level.INFO, "Name: {0}", (name = args.length > 0 ? args[0] : name));
                break;
            case HELP:
                LOGGER.log(Level.INFO, "Commands:\n"
                        + "/exit [force] - Closes the socket and exits the program.\n"
                        + "/stop [force] - Same as /exit.\n"
                        + "/restart - Closes the socket and then reopens it.\n"
                        + "/kick <port> - Closes the socket on the specified port.\n"
                        + "/list - Lists all connected clients.\n"
                        + "/users - Lists all known online users.\n"
                        + "/name [name] - Sets your name to the given name.\n"
                        + "/help - Prints all commands.\n"
                        + "/debug - Toggles debug mode.");
                break;
            case DEBUG:
                LOGGER.log(Level.INFO, "Debug mode {0}.", (debug = !debug) ? "enabled" : "disabled");
                for (Handler handler : LOGGER.getHandlers()) {
                    if (debug) {
                        handler.setLevel(Level.FINEST);
                    } else {
                        handler.setLevel(Level.CONFIG);
                    }
                }
                LOGGER.setLevel(debug ? Level.FINEST : Level.CONFIG);
                break;
        }
    }
    protected static final char COLOR_CHAR = '\u00A7';

    private static String format(String s) {
        return s.replaceAll(COLOR_CHAR + "\\w", "");
    }
}

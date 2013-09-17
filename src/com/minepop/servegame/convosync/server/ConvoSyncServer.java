package com.minepop.servegame.convosync.server;

import blir.util.logging.CompactFormatter;
import com.minepop.servegame.convosync.Main;
import com.minepop.servegame.convosync.net.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
import static com.minepop.servegame.convosync.Main.COLOR_CHAR;
import static com.minepop.servegame.convosync.Main.format;

/**
 *
 * @author Blir
 */
public class ConvoSyncServer {

    private enum Command {

        EXIT, STOP, RESTART, SETCOLOR, SETUSEPREFIX, KICK, LIST, USERS, NAME, HELP, DEBUG
    }

    private enum SubCommand {

        OP, LIST, UNREGISTER
    }
    private int port;
    private ServerSocket socket;
    private Scanner in;
    private boolean open = true, debug = false, prefix = true;
    private List<Client> clients = new ArrayList<Client>();
    private String name = "ConvoSyncServer", pluginPassword;
    private Map<String, String> userMap = new HashMap<String, String>();
    private List<User> users = new ArrayList<User>();
    private List<String> banlist = new ArrayList<String>();
    private static final Logger LOGGER = Logger.getLogger(ConvoSyncServer.class.getName());
    private char chatColor;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new ConvoSyncServer().run(args);
    }

    public void run(String[] startupArgs) throws IOException {
        Handler handler = new ConsoleHandler();
        java.util.logging.Formatter formatter = new CompactFormatter() {
            @Override
            public String format(LogRecord rec) {
                return Main.format(super.format(rec));
            }
        };
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
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("users.sav")));
            users.addAll(Arrays.asList((User[]) ois.readObject()));
            ois.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading user data.", ex);
        } catch (ClassNotFoundException ignore) {
        }
        try {
            Scanner scanner = new Scanner(new File("banlist.txt"));
            while (scanner.hasNext()) {
                banlist.add(scanner.nextLine());
            }
            scanner.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading ban list.", ex);
        }
        Properties p = null;
        try {
            p = new Properties();
            FileInputStream fis = new FileInputStream(new File("CS-Server.properties"));
            p.load(fis);
            String prop = p.getProperty("chat-color");
            chatColor = prop == null ? '\u0000' : prop.charAt(0);
            LOGGER.log(Level.CONFIG, "Using chat cholor code \"{0}\"", chatColor);
            prop = p.getProperty("use-prefixes");
            prefix = prop == null ? true : Boolean.parseBoolean(prop);
            LOGGER.log(Level.CONFIG, "Use prefixes set to {0}.", prefix);
            fis.close();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Couldn't load config:", ex);
        }
        in = new Scanner(System.in);
        for (String arg : startupArgs) {
            try {
                if (arg.startsWith("Port:")) {
                    port = Integer.parseInt(arg.split(":")[1]);
                } else if (arg.startsWith("Name:")) {
                    name = arg.split(":")[1];
                } else if (arg.startsWith("PluginPassword:")) {
                    pluginPassword = arg.split(":")[1];
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            } catch (ArrayIndexOutOfBoundsException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            }
        }
        if (p != null) {
            String prop;
            try {
                port = Integer.parseInt(p.getProperty("port"));
            } catch (NumberFormatException ignore) {
            }
            prop = p.getProperty("name");
            if (prop != null) {
                name = prop;
            }
            prop = p.getProperty("plugin-password");
            if (prop != null) {
                pluginPassword = prop;
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
                        client.server = server;
                        client.start();
                        LOGGER.log(Level.FINE, "Accepted a connection: {0}", client);
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
                        dispatchCommand(cmd, args);
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

    private void out(ChatMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        for (Client client : clients) {
            if (client != sender && client.auth) {
                client.sendMsg(msg, false);
            }
        }
        LOGGER.log(Level.INFO, "[{0}] {1} ",
                new Object[]{sender == null ? "NA" : sender.socket.getPort(), format(msg.MSG)});
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
        String clientName = userMap.get(msg.RECIPIENT);
        if (clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT;
        }
        if (clientName == null && sender != null) {
            sender.sendMsg(new PlayerMessage(COLOR_CHAR + "cPlayer \""
                    + COLOR_CHAR + "9" + msg.RECIPIENT + COLOR_CHAR
                    + "c\" not found.", msg.SENDER));
            return;
        }
        for (Client client : clients) {
            if (client.type == Client.ClientType.PLUGIN && client.localname.equals(clientName)) {
                client.sendMsg(msg);
            }
        }
    }

    private void out(PlayerMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        String clientName = userMap.get(msg.RECIPIENT);
        if (clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT;
        }
        for (Client client : clients) {
            if (client.localname.equals(clientName)) {
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

    private Client getClient(String name) {
        for (Client client : clients) {
            if (client.localname.equals(name)) {
                return client;
            }
        }
        return null;
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
                        if (server.prefix || type == ClientType.APPLICATION) {
                            if (server.chatColor == '\u0000') {
                                server.out("[" + name + "] " + ((ChatMessage) input).MSG, this);
                            } else {
                                server.out("[" + COLOR_CHAR + server.chatColor
                                        + name + COLOR_CHAR + "f] " + ((ChatMessage) input).MSG, this);
                            }
                        } else {
                            server.out((ChatMessage) input, this);
                        }
                        continue;
                    }
                    if (input instanceof CommandMessage) {
                        if (type == ClientType.APPLICATION) {
                            if (server.getUser(name).op) {
                                server.out((CommandMessage) input, this);
                            } else {
                                sendMsg(new PlayerMessage(
                                        "You don't have permission to use cross-server commands.", name));
                            }
                        } else {
                            server.out((CommandMessage) input, this);
                        }
                        continue;
                    }
                    if (input instanceof PlayerListMessage) {
                        PlayerListMessage msg = (PlayerListMessage) input;
                        server.notify(msg, ClientType.APPLICATION);
                        if (msg.JOIN) {
                            for (String element : msg.LIST) {
                                if (server.userMap.get(element) != null) {
                                    server.out(new PlayerMessage(
                                            "You cannot be logged into the client and the game simultaneously.", element), this);
                                    server.getClient(element).close();
                                }
                                server.userMap.put(element, localname);
                            }
                        } else {
                            for (String element : msg.LIST) {
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
                        sendMsg(new AuthenticationRequestResponse(auth, null));
                        server.notify(new PlayerListMessage(authReq.PLAYERS, true), ClientType.APPLICATION);
                        for (String element : authReq.PLAYERS) {
                            if (server.userMap.get(element) != null) {
                                server.out(new PlayerMessage(
                                        "You cannot be logged into the client and the game simultaneously.", element), this);
                                server.getClient(element).close();
                            }
                            server.userMap.put(element, localname);
                        }
                        server.out(name + " has connected.", this);
                        continue;
                    }
                    if (input instanceof ApplicationAuthenticationRequest) {
                        type = ClientType.APPLICATION;
                        AuthenticationRequestResponse.Reason reason = null;
                        ApplicationAuthenticationRequest authReq = (ApplicationAuthenticationRequest) input;
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
                        sendMsg(new AuthenticationRequestResponse(auth, reason));
                        if (auth) {
                            localname = (name = authReq.NAME);
                            sendMsg(new PlayerListMessage(
                                    server.userMap.keySet().toArray(new String[server.userMap.keySet().size()]), true));
                            server.notify(new PlayerListMessage(name, true), ClientType.APPLICATION);
                            server.out(name + " has joined.", this);
                            server.userMap.put(name, "CS-Client");
                        }
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
                    if (input instanceof UserRegistration) {
                        UserRegistration reg = (UserRegistration) input;
                        if (server.isUserRegistered(reg.USER)) {
                            sendMsg(new PlayerMessage(COLOR_CHAR + "cYou're already registered.", reg.USER), false);
                        } else {
                            server.users.add(new User(reg));
                            sendMsg(new PlayerMessage(COLOR_CHAR + "aYou've successfully registered.", reg.USER), false);
                        }
                        continue;
                    }
                    if (input instanceof DisconnectMessage) {
                        server.out(name + " has disconnected.", this);
                        if (type == ClientType.PLUGIN) {
                            server.userMap.values().removeAll(Collections.singleton(localname));
                        } else {
                            server.notify(new PlayerListMessage(name, false), ClientType.APPLICATION);
                            server.userMap.remove(name);
                        }
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

        private void sendMsg(Object obj, boolean override) {
            if (enabled || override) {
                sendMsg(obj);
            }
        }

        private void sendMsg(Object obj) {
            try {
                out.writeObject(obj);
                out.flush();
                LOGGER.log(Level.FINER, "{0} sent to {1}!", new Object[]{obj, this});
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
            server.out(name + " has been kicked.", this);
        }

        @Override
        public String toString() {
            return "Client[" + localname + "," + socket + "," + super.toString() + "]";
        }
    }

    @Override
    public String toString() {
        return "ConvoSyncServer " + Main.VERSION;
    }

    private void dispatchCommand(Command cmd, String[] args) {
        LOGGER.log(Level.INFO, "Executing command {0}", cmd);
        switch (cmd) {
            case EXIT:
            case STOP:
                open = false;
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("users.sav")));
                    oos.writeObject(users.toArray(new User[users.size()]));
                    oos.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error saving user data.", ex);
                }
                try {
                    PrintWriter pw = new PrintWriter("banlist.txt");
                    for (String elem : banlist) {
                        pw.println(elem);
                    }
                    pw.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error saving ban list.", ex);
                }
                try {
                    Properties p = new Properties();
                    FileOutputStream fos = new FileOutputStream(new File("CS-Server.properties"));
                    p.setProperty("chat-color", Character.toString(chatColor));
                    p.setProperty("use-prefixes", String.valueOf(prefix));
                    p.store(fos, null);
                    fos.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error saving config.", ex);
                }
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
            case SETCOLOR:
                LOGGER.log(Level.INFO, "Chat Color Code: {0}", args.length > 0
                        && args[0].length() > 0 ? chatColor = args[0].charAt(0) : chatColor);
                break;
            case SETUSEPREFIX:
                if (args.length > 0) {
                    prefix = Boolean.parseBoolean(args[0]);
                }
                LOGGER.log(Level.INFO, "Use Prefix: {0}", prefix);
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
                    LOGGER.log(Level.INFO, "All connected clients ({0}):", clients.size());
                    for (Client client : clients) {
                        LOGGER.log(Level.INFO, "{0}", client);
                    }
                }
                break;
            case USERS:
                if (args.length < 1) {
                    LOGGER.log(Level.INFO, "/users <list|op|unregister>");
                    break;
                }
                SubCommand subcmd;
                try {
                    subcmd = SubCommand.valueOf(args[0].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.INFO, "/users <list|op|unregister>");
                    break;
                }
                LOGGER.log(Level.INFO, "Executing sub-command {0}", subcmd);
                switch (subcmd) {
                    case LIST:
                        if (userMap.isEmpty()) {
                            LOGGER.log(Level.INFO, "No known online users.");
                        } else {
                            LOGGER.log(Level.INFO, "All known online users ({0}):",
                                    userMap.size());
                            for (String key : userMap.keySet()) {
                                LOGGER.log(Level.INFO, "User {0} on server {1}",
                                        new String[]{key, userMap.get(key)});
                            }
                        }
                        if (users.isEmpty()) {
                            LOGGER.log(Level.INFO, "No registered client users.");
                        } else {
                            LOGGER.log(Level.INFO, "All registered client users ({0}):",
                                    users.size());
                            for (User user : users) {
                                LOGGER.log(Level.INFO, "User: {0} OP: {1}",
                                        new Object[]{user.NAME, user.op});
                            }
                        }
                        break;
                    case OP:
                        if (args.length < 1) {
                            LOGGER.log(Level.INFO, "/users op <user name> [true|false]");
                            break;
                        }
                        if (!isUserRegistered(args[1])) {
                            LOGGER.log(Level.INFO, "Invalid user name.");
                            break;
                        }
                        User user = getUser(args[1]);
                        LOGGER.log(Level.INFO, (user.op = (args.length > 2
                                ? Boolean.parseBoolean(args[2]) : !user.op))
                                ? "{0} is now OP." : "{0} is no longer OP.", user.NAME);
                        break;
                    case UNREGISTER:
                        if (args.length < 1) {
                            LOGGER.log(Level.INFO, "/users unregister <user name>");
                            break;
                        }
                        Client client = getClient(args[1]);
                        if (client != null) {
                            try {
                                client.close();
                            } catch (IOException ex) {
                                LOGGER.log(Level.INFO, "Error closing client.", ex);
                            }
                        }
                        users.remove(getUser(args[1]));
                        LOGGER.log(Level.INFO, "{0} unregistered.", args[1]);
                        break;
                }
                break;
            case NAME:
                LOGGER.log(Level.INFO, "Name: {0}", args.length > 0 ? name = args[0] : name);
                break;
            case HELP:
                LOGGER.log(Level.INFO, "Commands:\n"
                        + "/exit [force]              - Closes the socket and exits the program.\n"
                        + "/stop [force]              - Same as /exit.\n"
                        + "/restart                   - Closes the socket and then reopens it.\n"
                        + "/setcolor [color code]     - Sets the color code used for server & client name prefixes.\n"
                        + "/setuseprefix [true|false] - Determines whether or not server name prefixes are included in chat.\n"
                        + "/kick <port>               - Closes the socket on the specified port.\n"
                        + "/list                      - Lists all connected clients.\n"
                        + "/users <list|op|register>  - Used to manage client users.\n"
                        + "/name [name]               - Sets your name to the given name.\n"
                        + "/help                      - Prints all commands.\n"
                        + "/debug                     - Toggles debug mode.");
                break;
            case DEBUG:
                LOGGER.log(Level.INFO, (debug = !debug) ? "Debug mode enabled." : "Debug mode disabled.");
                if (debug) {
                    for (Handler handler : LOGGER.getHandlers()) {
                        handler.setLevel(Level.FINEST);
                    }
                    LOGGER.setLevel(Level.FINEST);
                } else {
                    for (Handler handler : LOGGER.getHandlers()) {
                        handler.setLevel(Level.CONFIG);
                    }
                    LOGGER.setLevel(Level.CONFIG);
                }
                break;
        }
    }

    private static class User implements Serializable {

        private static final long serialVersionUID = 7526472295622776147L;
        private final String NAME, PASSWORD;
        private boolean op;

        public User(UserRegistration reg) {
            this.NAME = reg.USER;
            this.PASSWORD = reg.PASSWORD;
        }
    }

    private boolean isUserRegistered(String name) {
        for (User user : users) {
            if (user.NAME.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private User getUser(String name) {
        for (User user : users) {
            if (user.NAME.equals(name)) {
                return user;
            }
        }
        return null;
    }
}

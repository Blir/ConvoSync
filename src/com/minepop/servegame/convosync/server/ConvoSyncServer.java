package com.minepop.servegame.convosync.server;

import blir.crypto.QuickCipher;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author Blir
 */
public class ConvoSyncServer {

    private static enum Command {

        EXIT, STOP, RESTART, RECONNECT, SETCOLOR, SETUSEPREFIX, KICK, LIST,
        USERS, NAME, HELP, DEBUG
    }

    private static enum SubCommand {

        OP, LIST, UNREGISTER
    }
    private int port;
    private ServerSocket socket;
    private Scanner in;
    private boolean open = true, debug = false, prefix = true;
    private final List<Client> clients = new ArrayList<Client>();
    private String name = "ConvoSyncServer", pluginPassword;
    private Map<String, String> userMap = new HashMap<String, String>();
    private List<User> users = new ArrayList<User>();
    private List<String> banlist = new ArrayList<String>();
    private static final Logger LOGGER = Logger.getLogger(ConvoSyncServer.class.getName());
    private static Handler consoleHandler, fileHandler;
    private char chatColor;
    private QuickCipher cipher;
    private String[] startupArgs;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        consoleHandler = new ConsoleHandler();
        java.util.logging.Formatter formatter = new CompactFormatter() {
            @Override
            public String format(LogRecord rec) {
                return Main.format(super.format(rec));
            }
        };
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.CONFIG);
        LOGGER.addHandler(consoleHandler);
        fileHandler = new FileHandler("CS-Server.log", true);
        fileHandler.setFormatter(formatter);
        fileHandler.setLevel(Level.CONFIG);
        LOGGER.addHandler(fileHandler);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.CONFIG);
        new ConvoSyncServer().run(args);
    }

    public void run(String[] startupArgs) throws IOException {
        this.startupArgs = startupArgs;
        LOGGER.log(Level.INFO, java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG)
                .format(java.util.Calendar.getInstance().getTime()));
        LOGGER.log(Level.INFO, toString());
        LOGGER.log(Level.CONFIG, "Java Version: {0}", System.getProperty("java.version"));
        LOGGER.log(Level.CONFIG, "OS Architexture: {0}", System.getProperty("os.arch"));
        LOGGER.log(Level.CONFIG, "OS Name: {0}", System.getProperty("os.name"));
        LOGGER.log(Level.CONFIG, "OS Version: {0}", System.getProperty("os.version"));
        File decrypted = new File("users.sav");
        ObjectInputStream ois = null;
        try {
            cipher = new QuickCipher("DES/ECB/PKCS5Padding", "DES");
            cipher.decrypt(new File("users.sav.dat"), decrypted, new File("key.dat"));
            ois = new ObjectInputStream(new FileInputStream(decrypted));
            users.addAll(Arrays.asList((User[]) ois.readObject()));
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading user data.", ex);
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "Error loading user data.", ex);
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, "Error decrypting user data.", ex);
        } catch (NoSuchPaddingException ex) {
            LOGGER.log(Level.SEVERE, "Error decrypting user data.", ex);
        } catch (InvalidKeyException ex) {
            LOGGER.log(Level.SEVERE, "Error decrypting user data.", ex);
        } catch (IllegalBlockSizeException ex) {
            LOGGER.log(Level.SEVERE, "Error decrypting user data.", ex);
        } catch (BadPaddingException ex) {
            LOGGER.log(Level.SEVERE, "Error decrypting user data.", ex);
        } catch (InvalidKeySpecException ex) {
            LOGGER.log(Level.SEVERE, "Error decrypting user data.", ex);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } finally {
                decrypted.delete();
            }
        }
        try {
            Scanner scanner = new Scanner(new File("banlist.txt"));
            while (scanner.hasNext()) {
                banlist.add(scanner.nextLine());
            }
            scanner.close();
        } catch (FileNotFoundException ex) {
            // ignore
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
            LOGGER.log(Level.CONFIG, "Using chat color code \"{0}\"", chatColor);
            prop = p.getProperty("use-prefixes");
            prefix = prop == null ? true : Boolean.parseBoolean(prop);
            LOGGER.log(Level.CONFIG, "Use prefixes set to {0}.", prefix);
            fis.close();
        } catch (FileNotFoundException ex) {
            // ignore
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

        new Thread(new ConnectionAcceptionTask()).start();

        new Thread(new InputTask()).start();
    }

    private boolean alive() {
        for (Client client : clients) {
            if (client.alive) {
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

    private synchronized void close(boolean force) throws IOException {
        LOGGER.log(Level.INFO, "Closing {0}", this);
        try {
            socket.close();
        } finally {
            try {
                for (Client client : clients) {
                    client.close(false, true);
                }
            } finally {
                userMap.clear();
                clients.clear();
                if (force) {
                    System.exit(-1);
                }
            }
        }
    }

    private synchronized void out(String msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        for (Client client : clients) {
            if (client != sender && client.auth) {
                if (sender == null && client.enabled) {
                    client.sendMsg(new ChatMessage(msg, true), false);
                } else {
                    client.sendMsg(msg, false);
                }
            }
        }
        LOGGER.log(Level.INFO, "[{0}] {1} ",
                new Object[]{sender == null ? "NA" : sender.socket.getPort(),
            format(msg)});
    }

    private synchronized void out(ChatMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        for (Client client : clients) {
            if (client != sender && client.auth) {
                client.sendMsg(msg, false);
            }
        }
        LOGGER.log(Level.INFO, "[{0}] {1} ",
                new Object[]{sender == null ? "NA" : sender.socket.getPort(),
            format(msg.MSG)});
    }

    private synchronized void sendPlayerListUpdate() {
        String[] list = userMap.keySet().toArray(new String[userMap.size()]);
        PlayerListUpdate update = new PlayerListUpdate(list);
        for (Client client : clients) {
            if (client.type == ClientType.APPLICATION && client.auth) {
                client.sendMsg(update, false);
            }
        }
    }

    private synchronized void out(PrivateMessage msg, Client sender) {
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
                    + "c\" not found.", msg.SENDER), false);
            return;
        }
        for (Client client : clients) {
            if (client.localname.equals(clientName)) {
                client.sendMsg(msg, false);
            }
        }
    }

    private synchronized void out(PlayerMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        String clientName = userMap.get(msg.RECIPIENT);
        if (clientName == null) {
            LOGGER.log(Level.WARNING, "Is {0} a ghost?", msg.RECIPIENT);
            return;
        }
        if (clientName.equals("CS-Client")) {
            clientName = msg.RECIPIENT;
        }
        for (Client client : clients) {
            if (client.localname.equals(clientName)) {
                client.sendMsg(msg, false);
            }
        }
    }

    private synchronized void out(CommandMessage msg, Client sender) {
        if (sender != null && !sender.auth) {
            return;
        }
        for (Client client : clients) {
            if (client.type == ClientType.PLUGIN
                    && client.name.equalsIgnoreCase(msg.TARGET)) {
                client.sendMsg(msg, false);
                if (sender != null) {
                    sender.sendMsg(new PlayerMessage(COLOR_CHAR + "a" + msg
                            + " sent!", msg.SENDER), false);
                }
                return;
            }
        }
        if (sender != null) {
            sender.sendMsg(new PlayerMessage(COLOR_CHAR + "cServer " + COLOR_CHAR
                    + "9" + msg.TARGET + COLOR_CHAR + "c not found.", msg.SENDER),
                    false);
        }
    }

    private synchronized Client getClient(String name) {
        for (Client client : clients) {
            if (client.localname.equals(name)) {
                return client;
            }
        }
        return null;
    }

    private enum ClientType {

        PLUGIN, APPLICATION
    }

    private class Client implements Runnable {

        private Socket socket;
        private ClientType type;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private boolean alive = true, auth = false, enabled = true;
        private String name, localname, version;

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
                    if (input instanceof Message) {
                        processMessage((Message) input);
                    } else {
                        LOGGER.log(Level.WARNING, "{0} isn't a message!", input);
                    }
                } catch (IOException ex) {
                    if (!socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException ex2) {
                            LOGGER.log(Level.WARNING, "Error disconnecting client " + this, ex2);
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    LOGGER.log(Level.SEVERE, "Fatal error in client " + this, ex);
                    try {
                        socket.close();
                    } catch (IOException ex2) {
                        LOGGER.log(Level.WARNING, "Error disconnecting client " + this, ex2);
                    }
                }
            }
        }

        private synchronized void processMessage(Message msg) throws IOException {
            if (msg instanceof PrivateMessage) {
                out((PrivateMessage) msg, this);
                return;
            }
            if (msg instanceof PlayerMessage) {
                out((PlayerMessage) msg, this);
                return;
            }
            if (msg instanceof ChatMessage) {
                if (prefix || type == ClientType.APPLICATION) {
                    if (chatColor == '\u0000') {
                        out("[" + name + "] " + ((ChatMessage) msg).MSG, this);
                    } else {
                        out("[" + COLOR_CHAR + chatColor
                                + name + COLOR_CHAR + "f] " + ((ChatMessage) msg).MSG, this);
                    }
                } else {
                    out((ChatMessage) msg, this);
                }
                return;
            }
            if (msg instanceof CommandMessage) {
                if (type == ClientType.APPLICATION) {
                    if (getUser(name).op) {
                        out((CommandMessage) msg, this);
                    } else {
                        sendMsg(new PlayerMessage(
                                "You don't have permission to use cross-server commands.",
                                name), false);
                    }
                } else {
                    out((CommandMessage) msg, this);
                }
                return;
            }
            if (msg instanceof PlayerListMessage) {
                PlayerListMessage list = (PlayerListMessage) msg;
                if (list.JOIN) {
                    for (String element : list.LIST) {
                        if (userMap.get(element) != null) {
                            out(new PlayerMessage(
                                    "You cannot be logged into the client and the game simultaneously.",
                                    element), this);
                            Client client = getClient(element);
                            if (client == null) {
                                LOGGER.log(Level.WARNING, "{0} is already logged on, but their client cannot be found."
                                        + "\nAre they logged onto two Minecraft servers connected to this ConvoSync Server?",
                                        element);
                            } else {
                                getClient(element).close(true, true);
                            }
                        }
                        userMap.put(element, localname);
                    }
                } else {
                    for (String element : list.LIST) {
                        userMap.remove(element);
                    }
                }
                sendPlayerListUpdate();
                return;
            }
            if (msg instanceof PluginAuthenticationRequest) {
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
                auth = authReq.PASSWORD.equals(pluginPassword);
                sendMsg(new AuthenticationRequestResponse(auth,
                        AuthenticationRequestResponse.Reason.INVALID_PASSWORD,
                        Main.VERSION), true);
                for (String element : authReq.PLAYERS) {
                    if (userMap.get(element) != null) {
                        out(new PlayerMessage(
                                "You cannot be logged into the client and the game simultaneously.",
                                element), this);
                        getClient(element).close(true, true);
                    }
                    userMap.put(element, localname);
                }
                out(name + " has connected.", this);
                sendPlayerListUpdate();
                return;
            }
            if (msg instanceof ApplicationAuthenticationRequest) {
                type = ClientType.APPLICATION;
                AuthenticationRequestResponse.Reason reason = null;
                ApplicationAuthenticationRequest authReq = (ApplicationAuthenticationRequest) msg;
                version = authReq.VERSION;
                if (!Main.VERSION.equals(version)) {
                    LOGGER.log(Level.WARNING, "Version mismatch: Local version {0}, {1} version {2}", new Object[]{Main.VERSION, authReq.NAME, version});
                }
                User user = getUser(authReq.NAME);
                if (user == null) {
                    reason = AuthenticationRequestResponse.Reason.INVALID_USER;
                } else {
                    if (authReq.PASSWORD.equals(user.PASSWORD)) {
                        if (banlist.contains(authReq.NAME)) {
                            reason = AuthenticationRequestResponse.Reason.BANNED;
                        } else {
                            if (userMap.get(authReq.NAME) == null) {
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
                    sendMsg(new PlayerListUpdate(
                            userMap.keySet().toArray(
                            new String[userMap.keySet().size()])), false);
                    out(name + " has joined.", this);
                    userMap.put(name, "CS-Client");
                    sendPlayerListUpdate();
                }
                return;
            }
            if (msg instanceof SetEnabledProperty) {
                enabled = ((SetEnabledProperty) msg).ENABLED;
                out(name + " has " + (enabled ? "enabled" : "disabled")
                        + " cross-server chat due to " + (enabled ? "reduced" : "high")
                        + " player count.", this);
                if (!enabled) {
                    out("This doesn't affect cross-server private messages or commands.", this);
                }
                return;
            }
            if (msg instanceof UserRegistration) {
                UserRegistration reg = (UserRegistration) msg;
                if (isUserRegistered(reg.USER)) {
                    users.remove(getUser(reg.USER));
                }
                users.add(new User(reg));
                sendMsg(new PlayerMessage(COLOR_CHAR + "aYou've successfully registered.", reg.USER), false);
                return;
            }
            if (msg instanceof UserPropertyChange) {
                UserPropertyChange prop = (UserPropertyChange) msg;
                switch (prop.PROPERTY) {
                    case PASSWORD:
                        users.remove(getUser(name));
                        users.add(new User(new UserRegistration(name, prop.VALUE)));
                        sendMsg(new PlayerMessage("Password changed.", name), false);
                        break;
                }
                return;
            }
            if (msg instanceof UserListRequest) {
                String sender = ((UserListRequest) msg).SENDER;
                sendMsg(new PlayerMessage("All known online users:", sender), false);
                for (String user : userMap.keySet()) {
                    sendMsg(new PlayerMessage(user + " on server " + userMap.get(user), sender), false);
                }
                return;
            }
            if (msg instanceof DisconnectMessage) {
                try {
                    completelyClose(false);
                } catch (ConcurrentModificationException ex) {
                    LOGGER.log(Level.SEVERE, "Uh-oh! This is bad! : {0}", ex.toString());
                }
                try {
                    out(name + " has disconnected.", this);
                } catch (ConcurrentModificationException ex) {
                    LOGGER.log(Level.SEVERE, "Uh-oh! This is bad! : {0}", ex.toString());
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
            if (!alive) {
                LOGGER.log(Level.WARNING, "Tried to write to a dead client!");
                return;
            }
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

        private void close(boolean kick, boolean msg) throws IOException {
            if (msg) {
                sendMsg(new DisconnectMessage(), true);
            }
            alive = false;
            socket.close();
            if (kick) {
                out(name + " has been kicked.", this);
            }
            if (type == ClientType.PLUGIN) {
                userMap.values().removeAll(Collections.singleton(localname));
            } else {
                userMap.remove(name);
            }
            sendPlayerListUpdate();
        }

        private synchronized void completelyClose(boolean msg) throws IOException {
            clients.remove(this);
            close(false, msg);
        }

        @Override
        public String toString() {
            return "Client[" + localname + "," + version + "," + socket + "]";
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
                File decrypted = null;
                try {
                    ObjectOutputStream oos = null;
                    try {
                        decrypted = new File("users.sav");
                        oos = new ObjectOutputStream(new FileOutputStream(decrypted));
                        oos.writeObject(users.toArray(new User[users.size()]));
                    } finally {
                        if (oos != null) {
                            oos.close();
                        }
                    }
                    if (cipher != null) {
                        cipher.encrypt(decrypted, new File("users.sav.dat"),
                                new File("key.dat"));
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error saving user data.", ex);
                } catch (InvalidKeyException ex) {
                    LOGGER.log(Level.SEVERE, "Error encrypting user data.", ex);
                } catch (IllegalBlockSizeException ex) {
                    LOGGER.log(Level.SEVERE, "Error encrypting user data.", ex);
                } catch (BadPaddingException ex) {
                    LOGGER.log(Level.SEVERE, "Error encrypting user data.", ex);
                } finally {
                    if (decrypted != null) {
                        decrypted.delete();
                    }
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
                    close(args != null && args.length > 0
                            && args[0].equalsIgnoreCase("force"));
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error closing!", ex);
                }
                break;
            case RESTART:
                dispatchCommand(Command.EXIT, null);
                try {
                    new ConvoSyncServer().run(args.length == 0 ? startupArgs : args);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error restarting server.", ex);
                }
                break;
            case RECONNECT:
                try {
                    restart();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error restarting!", ex);
                }
                break;
            case SETCOLOR:
                LOGGER.log(Level.INFO, "Chat Color Code: {0}", args.length > 0
                        && args[0].length() > 0 ? chatColor = args[0].charAt(0)
                        : chatColor);
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
                            client.completelyClose(true);
                            LOGGER.log(Level.INFO, "Client closed.");
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
                    LOGGER.log(Level.INFO, "All connected clients ({0}):",
                            clients.size());
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
                                ? "{0} is now OP." : "{0} is no longer OP.",
                                user.NAME);
                        break;
                    case UNREGISTER:
                        if (args.length < 1) {
                            LOGGER.log(Level.INFO, "/users unregister <user name>");
                            break;
                        }
                        Client client = getClient(args[1]);
                        if (client != null) {
                            try {
                                client.close(true, true);
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
                LOGGER.log(Level.INFO, "Name: {0}", args.length > 0
                        ? name = args[0] : name);
                break;
            case HELP:
                LOGGER.log(Level.INFO, "Commands:\n"
                        + "/exit [force]              - Closes the socket and exits the program.\n"
                        + "/stop [force]              - Same as /exit.\n"
                        + "/restart                   - Completely restarts the server.\n"
                        + "/reconnect                 - Closes the socket and then reopens it.\n"
                        + "/setcolor [color code]     - Sets the color code used for server & client name prefixes.\n"
                        + "/setuseprefix [true|false] - Determines whether or not server name prefixes are included in chat.\n"
                        + "/kick <port>               - Closes the socket on the specified port.\n"
                        + "/list                      - Lists all connected clients.\n"
                        + "/users <list|op|unregister>- Used to manage client users.\n"
                        + "/name [name]               - Sets your name to the given name.\n"
                        + "/help                      - Prints all commands.\n"
                        + "/debug                     - Toggles debug mode.");
                break;
            case DEBUG:
                LOGGER.log(Level.INFO, (debug = !debug) ? "Debug mode enabled."
                        : "Debug mode disabled.");
                if (debug) {
                    consoleHandler.setLevel(Level.FINEST);
                    fileHandler.setLevel(Level.FINEST);
                    LOGGER.setLevel(Level.FINEST);
                } else {
                    consoleHandler.setLevel(Level.CONFIG);
                    fileHandler.setLevel(Level.CONFIG);
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

    private class InputTask implements Runnable {

        @Override
        public void run() {
            String input;
            while (open || alive()) {
                try {
                    input = in.nextLine();
                    if (input != null && input.length() > 0) {
                        if (input.charAt(0) == '/') {
                            int delim = input.indexOf(" ");
                            Command cmd;
                            try {
                                cmd = Command.valueOf((delim > 0
                                        ? input.substring(0, delim)
                                        : input).substring(1).toUpperCase());
                            } catch (IllegalArgumentException ex) {
                                cmd = Command.HELP;
                            }
                            String[] args = delim > 0
                                    ? input.substring(delim + 1).split(" ")
                                    : new String[0];
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
    }

    private class ConnectionAcceptionTask implements Runnable {

        @Override
        public void run() {
            Socket clientSocket;
            Client client;
            while (open) {
                try {
                    clientSocket = socket.accept();
                    client = new Client(clientSocket);
                    synchronized (clients) {
                        clients.add(client);
                    }
                    new Thread(client).start();
                    LOGGER.log(Level.FINE, "Accepted a connection: {0}", client);
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }
}

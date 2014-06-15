package com.github.blir.convosync.server;

import com.github.blir.convosync.Main;
import com.github.blir.convosync.application.ConvoSyncClient;
import com.github.blir.convosync.net.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import static com.github.blir.convosync.Main.COLOR_CHAR;

/**
 *
 * @author Blir
 */
public final class ConvoSyncServer {

    public static final Logger LOGGER = Logger.getLogger(ConvoSyncServer.class.getName());
    private static Handler fileHandler;
    private static Handler consoleHandler;
    private static Scanner in;

    private int port;
    private ServerSocket socket;
    private User superUser;
    private Thread connectionAcceptionThread, inputThread;
    protected boolean open = true, debug = false;
    /**
     * Controls whether prefixes are used for plugin clients in chat.
     */
    protected boolean usePrefix = true;
    protected final List<Client> clients = Collections.synchronizedList(new ArrayList<Client>());
    protected String name = "ConvoSyncServer", pluginPassword, superPassword;
    protected Map<String, String> userMap = new HashMap<String, String>();
    /**
     * List of registered ConvoSyncClient users.
     */
    protected Map<String, User> users = new HashMap<String, User>();
    /**
     * List of the Minecraft user names of banned ConvoSyncClient users.
     */
    protected List<String> banlist = new LinkedList<String>();
    /**
     * Used to send all messages that require iterating through the clients in
     * order to be more thread safe.
     */
    protected Messenger messenger;
    /**
     * The color that client names are in chat.
     */
    protected char chatColor;
    private String[] startupArgs;

    /**
     * Used to represent all commands.
     */
    public static enum Command {

        EXIT, STOP, RESTART, RECONNECT, SETCOLOR, SETUSEPREFIX, KICK, LIST,
        USERS, NAME, HELP, DEBUG, VERSION, CONFIG, CLIENT, CCMD
    }

    /**
     * Used to represent all sub-commands.
     */
    public static enum SubCommand {

        OP, LIST, UNREGISTER
    }

    public static String randomString(int len) {
        return Main.randomString(LOGGER, len);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String format = null;
        for (String arg : args) {
            try {
                if (arg.startsWith("DateFormat:")) {
                    format = arg.split(":")[1];
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                // ignore, probably an argument for something else
            } catch (NullPointerException ex) {
                // ignore, probably an argument for something else
            }
        }
        in = new Scanner(System.in);
        java.util.logging.Formatter formatter;
        try {
            formatter = new ServerFormatter(format == null ? "yyyy/MM/dd HH:mm:ss" : format) {
                @Override
                public String format(LogRecord rec) {
                    return Main.format(super.format(rec));
                }
            };
            format = null;
        } catch (IllegalArgumentException ex) {
            format = ex.getMessage();
            formatter = new ServerFormatter("yyyy/MM/dd HH:mm:ss") {
                @Override
                public String format(LogRecord rec) {
                    return Main.format(super.format(rec));
                }
            };
        }
        consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.CONFIG);
        LOGGER.addHandler(consoleHandler);
        File log = new File("CS-Server0.log");
        for (int idx = 1; log.length() > 5242880; idx++) {
            log = new File("CS-Server" + idx + ".log");
        }
        LOGGER.setUseParentHandlers(false);
        try {
            fileHandler = new FileHandler(log.getName(), true);
            fileHandler.setFormatter(formatter);
            fileHandler.setLevel(Level.CONFIG);
            LOGGER.addHandler(fileHandler);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Couldn't initialize the file handler.", ex);
        }
        LOGGER.setLevel(Level.CONFIG);
        if (format != null) {
            LOGGER.log(Level.WARNING, "Invalid date format: {0}", format);
        }
        LOGGER.log(Level.CONFIG, "Logging to {0}", log.getName());
        new ConvoSyncServer().run(args);
    }

    /**
     * Runs the ConvoSyncServer.
     *
     * @param startupArgs command line arguments
     */
    public void run(String[] startupArgs) {
        this.startupArgs = startupArgs;
        LOGGER.log(Level.INFO, toString());
        LOGGER.log(Level.CONFIG, "Java Version: {0}", System.getProperty("java.version"));
        LOGGER.log(Level.CONFIG, "OS Architexture: {0}", System.getProperty("os.arch"));
        LOGGER.log(Level.CONFIG, "OS Name: {0}", System.getProperty("os.name"));
        LOGGER.log(Level.CONFIG, "OS Version: {0}", System.getProperty("os.version"));
        DataInputStream dis = null;
        try {
            try {
                dis = new DataInputStream(new FileInputStream(new File("users.dat")));
                while (dis.available() > 0) {
                    User user = new User(dis.readUTF(), dis.readInt(), dis.readUTF());
                    users.put(user.NAME, user);
                }
            } finally {
                if (dis != null) {
                    dis.close();
                }
            }
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading user data.", ex);
        }
        try {
            Scanner scanner = new Scanner(new File("banlist.txt"));
            while (scanner.hasNext()) {
                banlist.add(scanner.nextLine());
            }
            scanner.close();
        } catch (FileNotFoundException ex) {
            // ignore
        }
        try {
            Properties p = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File("config.properties"));
                p.load(fis);
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
            String prop = p.getProperty("chat-color");
            chatColor = prop.length() < 1 ? '\u0000' : prop.charAt(0);
            if (chatColor != '\u0000') {
                LOGGER.log(Level.CONFIG, "Using chat color code \"{0}\"",
                           chatColor);
            }
            prop = p.getProperty("use-prefixes");
            usePrefix = prop == null ? true : Boolean.parseBoolean(prop);
            LOGGER.log(Level.CONFIG, "Use prefixes set to {0}.", usePrefix);
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Couldn't load config:", ex);
        }
        for (String arg : startupArgs) {
            try {
                if (arg.startsWith("Port:")) {
                    port = Integer.parseInt(arg.split(":")[1]);
                } else if (arg.startsWith("Name:")) {
                    name = arg.split(":")[1];
                } else if (arg.startsWith("PluginPassword:")) {
                    pluginPassword = arg.split(":")[1];
                } else if (arg.startsWith("SuperPassword:")) {
                    superPassword = arg.split(":")[1];
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            } catch (ArrayIndexOutOfBoundsException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            }
        }
        try {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File("connection.properties"));
                Properties p = new Properties();
                p.load(fis);
                String prop = p.getProperty("port");
                try {
                    port = Integer.parseInt(prop);
                } catch (NumberFormatException ex) {
                    if (prop != null) {
                        LOGGER.log(Level.SEVERE, "Invalid config: {0}", prop);
                    }
                }
                prop = p.getProperty("name");
                if (prop != null) {
                    name = prop;
                }
                prop = p.getProperty("plugin-password");
                if (prop != null) {
                    pluginPassword = prop;
                }
                prop = p.getProperty("super-password");
                if (prop != null) {
                    superPassword = prop;
                }
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Couldn't load connection config: ", ex);
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

        while (superPassword == null || superPassword.equals("")) {
            System.out.println("Enter a password that the Super User will use to log in: ");
            superPassword = in.nextLine();
        }

        superUser = new User("~", superPassword, randomString(25));
        superUser.op = true;
        users.put(superUser.NAME, superUser);

        try {
            open();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error opening socket.", ex);
            return;
        }

        messenger = new Messenger(this);

        (connectionAcceptionThread = new Thread(new ConnectionAcceptionTask())).start();

        (inputThread = new Thread(new InputTask())).start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                LOGGER.log(Level.FINE, "Running Shutdown hook.");
                if (open) {
                    LOGGER.log(Level.WARNING, "Server still open; attempting to force close.");
                    close(DisconnectMessage.Reason.CRASHED);
                }
            }
        }));
    }

    /**
     * Restarts (calls close() and then open()) the ConvoSyncServer.
     *
     * @throws IOException if either close() or open() throw it
     */
    public void restart()
            throws IOException {
        close(DisconnectMessage.Reason.RESTARTING);
        open();
    }

    /**
     * Opens the ConvoSyncServer.
     *
     * @throws IOException if the socket couldn't be initialized
     */
    public void open()
            throws IOException {
        socket = new ServerSocket(port);
        socket.setSoTimeout(2500);
        LOGGER.log(Level.INFO, socket.toString());
    }

    /**
     * Closes the ConvoSyncServer.
     */
    public void close() {
        close(DisconnectMessage.Reason.CLOSING);
    }

    private void close(DisconnectMessage.Reason reason) {
        LOGGER.log(Level.INFO, "Closing {0}", this);
        try {
            socket.close();
        } catch (IOException ex) {
            // ignore
        }
        messenger.out(new DisconnectMessage(reason), null);
        userMap.clear();
        clients.clear();
        if (reason != DisconnectMessage.Reason.RESTARTING) {
            System.exit(0);
        }
    }

    protected Client getClient(String name) {
        for (Client client : clients) {
            if (client.localname.equals(name)) {
                return client;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ConvoSyncServer " + Main.VERSION;
    }

    /**
     * Used to dispatch commands. dispatchCommand(Command.USERS, new
     * String[]{"list"}) would be equivalent to dispatchSubComand(Command.LIST,
     * new String[0]). Currently, only the USERS command has sub-commands.
     *
     * @param cmd  the Command to dispatch
     * @param args the Command arguments
     */
    public void dispatchCommand(Command cmd, String[] args) {
        LOGGER.log(Level.INFO, "Executing command {0}", cmd);
        switch (cmd) {
            case EXIT:
            case STOP:
                open = false;
                users.remove(superUser.NAME);
                // save user data
                try {
                    DataOutputStream dos = null;
                    try {
                        dos = new DataOutputStream(new FileOutputStream(new File("users.dat")));
                        for (User user : users.values()) {
                            dos.writeUTF(user.NAME);
                            dos.writeInt(user.SALTED_HASH);
                            dos.writeUTF(user.SALT);
                        }
                    } finally {
                        if (dos != null) {
                            dos.close();
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error saving user data.", ex);
                }
                // save ban list
                try {
                    PrintWriter pw = null;
                    try {
                        pw = new PrintWriter("banlist.txt");
                        for (String elem : banlist) {
                            pw.println(elem);
                        }
                    } finally {
                        if (pw != null) {
                            pw.close();
                        }
                    }
                } catch (FileNotFoundException ex) {
                    // ignore
                }
                // save config
                try {
                    FileOutputStream fos = null;
                    try {
                        Properties p = new Properties();
                        fos = new FileOutputStream(new File("config.properties"));
                        p.setProperty("chat-color",
                                      chatColor == '\u0000' ? "" : String.valueOf(chatColor));
                        p.setProperty("use-prefixes", String.valueOf(usePrefix));
                        p.store(fos, null);
                    } finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error saving config.", ex);
                }

                /*
                 * Close the server. If the restart argument is included,
                 * indicate that the server will be back soon.
                 */
                close(args != null && args.length > 0
                      && args[0].equalsIgnoreCase("restart")
                      ? DisconnectMessage.Reason.RESTARTING
                      : DisconnectMessage.Reason.CLOSING);
                break;
            case RESTART:
                // exit the current server and indicate that it is restarting
                dispatchCommand(Command.EXIT, new String[]{"restart"});
                ConvoSyncServer server = new ConvoSyncServer();
                // preserve debug mode from last server
                server.debug = debug;
                // re-use the server arguments, if there were any
                server.run(args.length == 0 ? startupArgs : args);
                break;
            case RECONNECT:
                try {
                    restart();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error restarting!", ex);
                }
                break;
            case SETCOLOR:
                LOGGER.log(Level.INFO,
                           "Chat Color Code: {0}", args.length > 0 && args[0].length() > 0
                                                   ? chatColor = args[0].charAt(0)
                                                   : chatColor);
                break;
            case SETUSEPREFIX:
                LOGGER.log(Level.INFO, "Use Prefix: {0}", args.length > 0
                                                          ? usePrefix = Boolean.parseBoolean(args[0])
                                                          : usePrefix);
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
                    LOGGER.log(Level.INFO, "{0} is not a valid number.", args[0]);
                    break;
                }
                boolean found = false;
                for (Client client : clients) {
                    if (client.socket.getPort() == id) {
                        found = true;
                        LOGGER.log(Level.INFO, "Closing {0}...", client.localname);
                        client.close(true, true, true,
                                     DisconnectMessage.Reason.KICKED);
                        LOGGER.log(Level.INFO, "Client {0} closed.", client.localname);
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
                        LOGGER.log(Level.INFO, client.toString());
                    }
                }
                break;
            case USERS:
                if (args.length < 1) {
                    LOGGER.log(Level.INFO, "/users <list|op|unregister>");
                    break;
                }
                SubCommand subCmd;
                try {
                    subCmd = SubCommand.valueOf(args[0].toUpperCase());
                } catch (IllegalArgumentException ex) {
                    LOGGER.log(Level.INFO, "/users <list|op|unregister>");
                    break;
                }
                // drop the first argument (it was the sub-command)
                dispatchSubCommand(subCmd, Arrays.copyOfRange(args, 1, args.length));
                break;
            case NAME:
                LOGGER.log(Level.INFO, "Name: {0}", args.length > 0
                                                    ? name = args[0] : name);
                break;
            case HELP:
                LOGGER.log(Level.INFO, "Commands:\n"
                                       + "/exit                                - Closes the socket and exits the program.\n"
                                       + "/stop                                - Same as /exit.\n"
                                       + "/restart                             - Completely restarts the server.\n"
                                       + "/reconnect                           - Closes the socket and then reopens it.\n"
                                       + "/setcolor [color code]               - Sets the color code used for server & client name prefixes.\n"
                                       + "/setuseprefix [true|false]           - Determines whether or not server name prefixes are included in chat.\n"
                                       + "/kick <port>                         - Closes the socket on the specified port.\n"
                                       + "/list                                - Lists all connected clients.\n"
                                       + "/users <list|op|unregister>          - Used to manage client users.\n"
                                       + "/name [name]                         - Sets your name to the given name.\n"
                                       + "/help                                - Prints all commands.\n"
                                       + "/debug                               - Toggles debug mode.\n"
                                       + "/version                             - Displays version info.\n"
                                       + "/config                              - Generates the server config properties.\n"
                                       + "/ccmd <server name> <command> [args] - Executes the specified command on the specified server.\n"
                                       + "/client                              - Launches the ConvoSync client.");
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
            case VERSION:
                LOGGER.log(Level.INFO, "v{0}", Main.VERSION);
                break;
            case CONFIG:
                Properties p = new Properties();
                p.setProperty("plugin-password", pluginPassword);
                p.setProperty("super-password", superPassword);
                p.setProperty("name", name);
                p.setProperty("port", String.valueOf(port));
                FileOutputStream fos = null;
                try {
                    try {
                        fos = new FileOutputStream("connection.properties");
                        p.store(fos, null);
                        LOGGER.log(Level.INFO, "Config generated.");
                    } finally {
                        if (fos != null) {
                            fos.close();
                        }
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Could not generate config: {0}",
                               ex.toString());
                }
                break;
            case CLIENT:
                String[] clientArgs = new String[args.length + 2];
                System.arraycopy(args, 0, clientArgs, 2, args.length);
                clientArgs[0] = "DefaultCloseOperation:" + javax.swing.JFrame.DISPOSE_ON_CLOSE;
                clientArgs[1] = "Level:" + Level.INFO;
                LOGGER.log(Level.INFO, "Launching client with args: {0}", Arrays.toString(clientArgs));
                new ConvoSyncClient().run(clientArgs);
                break;
            case CCMD:
                if (args.length < 1) {
                    LOGGER.log(Level.INFO, "/ccmd <server name> <command> [arg] [arg] [...]");
                    LOGGER.log(Level.INFO, "Use quotes if the server name has spaces.");
                    break;
                }
                StringBuilder sb;
                String target = null;
                if (args[0].startsWith("\"") && !args[0].endsWith("\"")) {
                    sb = new StringBuilder(args[0].substring(1));
                    for (int idx = 1; idx < args.length; idx++) {
                        if (args[idx].endsWith("\"")) {
                            target = sb.append(" ")
                                    .append(args[idx].substring(0, args[idx].length() - 1))
                                    .toString();
                            if (args.length >= idx + 2) {
                                sb = new StringBuilder(args[idx + 1]);
                                for (int idx2 = idx + 2; idx2 < args.length; idx2++) {
                                    sb.append(" ").append(args[idx2]);
                                }
                            }
                        } else {
                            sb.append(" ").append(args[idx]);
                        }
                    }
                } else {
                    sb = new StringBuilder(args[1]);
                    for (int idx = 2; idx < args.length; idx++) {
                        sb.append(" ").append(args[idx]);
                    }
                    target = args[0];
                }
                if (target != null) {
                    messenger.out(new CommandMessage(new MessageRecipient("ConvoSync_Console", MessageRecipient.SenderType.CONVOSYNC_CONSOLE), target, sb.toString()), null);
                    LOGGER.log(Level.INFO, String.format("Issuing command %s on %s.", args[1], args[0]));
                } else {
                    LOGGER.log(Level.INFO, "Missing closing quote.");
                }
                break;
        }
    }

    /**
     * Used to dispatch sub-commands. dispatchCommand(Command.USERS, new
     * String[]{"list"}) would be equivalent to dispatchSubCommand(Command.LIST,
     * new String[0]). Currently, only the USERS command has sub-commands.
     *
     * @param subCmd the SubCommand to dispatch
     * @param args   the SubCommand arguments
     */
    public void dispatchSubCommand(SubCommand subCmd, String[] args) {
        LOGGER.log(Level.INFO, "Executing sub-command {0}", subCmd);
        switch (subCmd) {
            case LIST:
                if (userMap.isEmpty()) {
                    LOGGER.log(Level.INFO, "No known online users.");
                } else {
                    LOGGER.log(Level.INFO,
                               "All known online users ({0}):",
                               userMap.size());
                    for (Map.Entry<String, String> entry : userMap.entrySet()) {
                        LOGGER.log(Level.INFO, "User {0} on server {1}",
                                   new String[]{entry.getKey(), entry.getValue()});
                    }
                }
                if (users.isEmpty()) {
                    LOGGER.log(Level.INFO, "No registered client users.");
                } else {
                    LOGGER.log(Level.INFO,
                               "All registered client users ({0}):",
                               users.size());
                    for (User user : users.values()) {
                        LOGGER.log(Level.INFO, "User: {0} OP: {1}",
                                   new Object[]{user.NAME, user.op});
                    }
                }
                break;
            case OP:
                if (args.length < 0) {
                    LOGGER.log(Level.INFO, "/users op <user name> [true|false]");
                    break;
                }
                if (!users.containsKey(args[0])) {
                    LOGGER.log(Level.INFO, "No such user: {0}", args[0]);
                    break;
                }
                User user = users.get(args[0]);
                LOGGER.log(Level.INFO, (user.op = (args.length > 1
                                                   ? Boolean.parseBoolean(args[1])
                                                   : !user.op))
                                       ? "{0} is now OP." : "{0} is no longer OP.",
                           user.NAME);
                break;
            case UNREGISTER:
                if (args.length < 0) {
                    LOGGER.log(Level.INFO, "/users unregister <user name>");
                    break;
                }
                user = users.get(args[0]);
                if (user != null) {
                    Client client = getClient(args[0]);
                    if (client != null) {
                        client.close(true, true, true,
                                     new DisconnectMessage(DisconnectMessage.Reason.KICKED));
                    }
                    users.remove(user.NAME);
                    LOGGER.log(Level.INFO, "{0} unregistered.", args[0]);
                } else {
                    LOGGER.log(Level.INFO, "No such user: {0}", args[0]);
                }
                break;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Public API">
    public boolean isOpen() {
        return open;
    }

    public char getChatColor() {
        return chatColor;
    }

    public boolean isUsingPrefixes() {
        return usePrefix;
    }

    /**
     *
     * @return true if debug mode is enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     *
     * @return the port the server is listening on
     */
    public int getPort() {
        return port;
    } // </editor-fold>

    private class InputTask implements Runnable {

        @Override
        public void run() {
            String input;
            while (open || !clients.isEmpty() || connectionAcceptionThread.isAlive()) {
                try {
                    input = in.nextLine();
                    if (input != null && input.length() > 0) {
                        if (input.charAt(0) == '/') {
                            // command
                            input = input.substring(1);
                            int delim = input.indexOf(" ");
                            Command cmd;
                            String value = (delim > 0
                                            ? input.substring(0, delim)
                                            : input).toUpperCase();
                            try {
                                cmd = Command.valueOf(value);
                            } catch (IllegalArgumentException ex) {
                                LOGGER.log(Level.INFO, "No such command {0}; enter /help for help.", value);
                                continue;
                            }
                            String[] args = delim > 0
                                            ? input.substring(delim + 1).split(" ")
                                            : new String[0];
                            dispatchCommand(cmd, args);
                        } else {
                            // chat
                            messenger.out("<" + COLOR_CHAR + "5" + name + COLOR_CHAR + "f> " + input, null);
                        }
                    }
                } catch (NoSuchElementException ex) {
                    LOGGER.log(Level.WARNING, "Input terminated.");
                    in = new Scanner(System.in);
                }
            }
            LOGGER.log(Level.INFO, "Input Task ended.");
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
                    client = new Client(clientSocket, ConvoSyncServer.this, messenger);
                    messenger.newClients.add(client);
                    new Thread(client).start();
                    LOGGER.log(Level.FINE, "Accepted a connection: {0}", clientSocket);
                } catch (SocketTimeoutException ex) {
                    // ignore
                } catch (IOException ex) {
                    LOGGER.log(Level.FINER, "Error accepting a connection: {0}", ex.toString());
                }
            }
            LOGGER.log(Level.FINE, "Connection Acception Task ended.");
        }
    }
}

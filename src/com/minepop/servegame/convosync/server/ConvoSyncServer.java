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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.*;

import static com.minepop.servegame.convosync.Main.*;

/**
 *
 * @author Blir
 */
public final class ConvoSyncServer {

    private int port;
    private ServerSocket socket;
    private Scanner in;
    protected boolean open = true, debug = false, usePrefix = true;
    protected List<Client> clients = new ArrayList<Client>();
    protected String name = "ConvoSyncServer", pluginPassword;
    protected Map<String, String> userMap = new HashMap<String, String>();
    protected List<User> users = new ArrayList<User>();
    protected List<String> banlist = new ArrayList<String>();
    protected static final Logger LOGGER = Logger.getLogger(
            ConvoSyncServer.class.getName());
    private static Handler consoleHandler, fileHandler;
    private Messenger messenger;
    protected char chatColor;
    private QuickCipher cipher;
    private String[] startupArgs;

    private static enum Command {

        EXIT, STOP, RESTART, RECONNECT, SETCOLOR, SETUSEPREFIX, KICK, LIST,
        USERS, NAME, HELP, DEBUG, VERSION, CONFIG
    }

    private static enum SubCommand {

        OP, LIST, UNREGISTER
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
            throws IOException {
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
        File log = new File("CS-Server0.log");
        for (int idx = 1; log.length() > 5242880; idx++) {
            log = new File("CS-Server" + idx + ".log");
        }
        fileHandler = new FileHandler(log.getName(), true);
        fileHandler.setFormatter(formatter);
        fileHandler.setLevel(Level.CONFIG);
        LOGGER.addHandler(fileHandler);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.CONFIG);
        LOGGER.log(Level.CONFIG, "Logging to {0}", log.getName());
        new ConvoSyncServer().run(args);
    }

    public void run(String[] startupArgs)
            throws IOException {
        this.startupArgs = startupArgs;
        LOGGER.log(Level.INFO, java.text.DateFormat.getDateInstance(
                java.text.DateFormat.LONG)
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
        open();

        messenger = new Messenger(this);

        new Thread(new ConnectionAcceptionTask()).start();

        new Thread(new InputTask()).start();
    }

    private void restart()
            throws IOException {
        close(false, DisconnectMessage.Reason.RESTARTING);
        open();
    }

    private void open()
            throws IOException {
        socket = new ServerSocket(port);
        LOGGER.log(Level.INFO, socket.toString());
    }

    private void close(boolean force, DisconnectMessage.Reason reason)
            throws IOException {
        LOGGER.log(Level.INFO, "Closing {0}", this);
        try {
            socket.close();
        } finally {
            try {
                messenger.out(new DisconnectMessage(reason), null);
            } finally {
                userMap.clear();
                clients.clear();
                if (force) {
                    System.exit(-1);
                }
            }
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
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error saving ban list.", ex);
                }
                try {
                    Properties p = new Properties();
                    FileOutputStream fos = new FileOutputStream(new File("config.properties"));
                    p.setProperty("chat-color",
                                  chatColor == '\u0000' ? "" : String.valueOf(chatColor));
                    p.setProperty("use-prefixes", String.valueOf(usePrefix));
                    p.store(fos, null);
                    fos.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Error saving config.", ex);
                }
                try {
                    close(args != null && args.length > 0
                          && args[0].equalsIgnoreCase("force"),
                          args != null && args.length > 0
                          && args[0].equals("restart")
                          ? DisconnectMessage.Reason.RESTARTING
                          : DisconnectMessage.Reason.CLOSING);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error closing!", ex);
                }
                break;
            case RESTART:
                dispatchCommand(Command.EXIT, new String[]{"restart"});
                try {
                    ConvoSyncServer server = new ConvoSyncServer();
                    server.debug = debug;
                    server.run(args.length == 0 ? startupArgs : args);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Error restarting server!", ex);
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
                LOGGER.log(Level.INFO,
                           "Chat Color Code: {0}", args.length > 0 && args[0].length() > 0
                                                   ? chatColor = args[0].charAt(0)
                                                   : chatColor);
                break;
            case SETUSEPREFIX:
                if (args.length > 0) {
                    usePrefix = Boolean.parseBoolean(args[0]);
                }
                LOGGER.log(Level.INFO, "Use Prefix: {0}", usePrefix);
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
                            client.close2(true, true,
                                          DisconnectMessage.Reason.KICKED);
                            LOGGER.log(Level.INFO, "Client closed.");
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "Error closing {0}: {1}",
                                       new Object[]{client.localname, ex});
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
                dispatchSubCommand(subCmd, Arrays.copyOfRange(args, 1, args.length));
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
                                       + "/debug                     - Toggles debug mode.\n"
                                       + "/version                   - Displays version info.\n"
                                       + "/config                    - Generates the server config properties.");
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
        }
    }

    private void dispatchSubCommand(SubCommand subCmd, String[] args) {
        LOGGER.log(Level.INFO, "Executing sub-command {0}", subCmd);
        switch (subCmd) {
            case LIST:
                if (userMap.isEmpty()) {
                    LOGGER.log(Level.INFO, "No known online users.");
                } else {
                    LOGGER.log(Level.INFO,
                               "All known online users ({0}):",
                               userMap.size());
                    for (String key : userMap.keySet()) {
                        LOGGER.log(Level.INFO, "User {0} on server {1}",
                                   new String[]{key, userMap.get(key)});
                    }
                }
                if (users.isEmpty()) {
                    LOGGER.log(Level.INFO, "No registered client users.");
                } else {
                    LOGGER.log(Level.INFO,
                               "All registered client users ({0}):",
                               users.size());
                    for (User user : users) {
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
                if (!isUserRegistered(args[0])) {
                    LOGGER.log(Level.INFO, "Invalid user name.");
                    break;
                }
                User user = getUser(args[0]);
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
                Client client = getClient(args[0]);
                if (client != null) {
                    try {
                        client.close1(true, true,
                                      new DisconnectMessage(DisconnectMessage.Reason.KICKED));
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, "Error closing {0} : {1}",
                                   new Object[]{client.localname, ex});
                    }
                }
                users.remove(getUser(args[0]));
                LOGGER.log(Level.INFO, "{0} unregistered.", args[0]);
                break;
        }
    }

    protected boolean isUserRegistered(String name) {
        for (User user : users) {
            if (user.NAME.equals(name)) {
                return true;
            }
        }
        return false;
    }

    protected User getUser(String name) {
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
            while (open || !clients.isEmpty()) {
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
                            messenger.out("<" + COLOR_CHAR + "5" + name + COLOR_CHAR + "f> " + input, null);
                        }
                    }
                } catch (NoSuchElementException ex) {
                    LOGGER.log(Level.WARNING, "Input terminated.");
                    in = new Scanner(System.in);
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
                    client = new Client(clientSocket, ConvoSyncServer.this, messenger);
                    messenger.newClients.add(client);
                    new Thread(client).start();
                    LOGGER.log(Level.FINE, "Accepted a connection: {0}", clientSocket);
                } catch (IOException ex) {
                    LOGGER.log(Level.FINEST, "Error accepting a connection: {0}", ex.toString());
                }
            }
        }
    }
}

package com.minepop.servegame.convosync;

import blir.net.AuthenticationRequest;
import blir.net.ChatMessage;
import blir.net.CommandMessage;
import blir.net.Message;
import blir.net.PlayerMessage;
import blir.net.PrivateMessage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Blir
 */
public class ConvoSyncServer {

    private enum Command {

        EXIT, STOP, RESTART, KICK, LIST, NAME, HELP
    }
    private int port;
    private ServerSocket socket;
    private Scanner in;
    private boolean open = true;
    private final ArrayList<Client> clients = new ArrayList<Client>();
    private String name = "ConvoSyncServer", pluginPassword, applicationPassword;
    private final Map<String, String> userMap = new HashMap<String, String>();
    private static final Logger LOGGER = Logger.getLogger(ConvoSyncServer.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new ConvoSyncServer().run(args);
    }

    public void run(String[] startupArgs) throws IOException {
        LOGGER.setLevel(Level.CONFIG);
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
        Client.setServer(this);
        open();
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
        try {
            while (alive() || open) {
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
                        out("c<" + COLOR_CHAR + "5" + name + COLOR_CHAR + "f> " + input, null);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error in input Thread!", ex);
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
        if (sender != null && !sender.verified) {
            return;
        }
        for (Client client : clients) {
            if (client != sender && client.verified) {
                client.sendMsg(msg);
            }
        }
        LOGGER.log(Level.INFO, "[{0}] {1} ",
                new Object[]{sender == null ? "N/A" : sender.socket.getPort(), format(msg.substring(1))});
    }

    private static void debug(String input) {
        LOGGER.log(Level.FINEST, "Debug info for message: {0}", input);
        for (int idx = 0; idx < input.length(); idx++) {
            LOGGER.log(Level.FINEST, "Character {0} : {1} : {2}",
                    new Object[]{idx, input.charAt(idx),
                Integer.toHexString(input.charAt(idx) | 0x10000).substring(1)});
        }
    }

    private void notify(String notification, Client.ClientType type) {
        for (Client client : clients) {
            if (client.type == type && client.verified) {
                client.sendMsg(notification);
            }
        }
    }

    private void out(PrivateMessage msg, Client sender) {
        if (sender != null && !sender.verified) {
            return;
        }
        String server = userMap.get(msg.recip);
        if (server == null && sender != null) {
            sender.sendMsg(new PlayerMessage(COLOR_CHAR + "cPlayer \"" + COLOR_CHAR + "9" + msg.recip + COLOR_CHAR + "c\"not found.", msg.sender));
            return;
        }
        for (Client client : clients) {
            if (client.type == Client.ClientType.PLUGIN && client.localname.equals(server)) {
                client.sendMsg(msg);
                LOGGER.log(Level.FINER, "{0} sent!", msg);
            }
        }
    }

    private void out(PlayerMessage msg, Client sender) {
        if (sender != null && !sender.verified) {
            return;
        }
        for (Client client : clients) {
            if (client.type == Client.ClientType.PLUGIN) {
                client.sendMsg(msg);
                LOGGER.log(Level.FINER, "{0} sent!", msg);
            }
        }
    }

    private void out(CommandMessage msg, Client sender) {
        if (sender != null && !sender.verified) {
            return;
        }
        for (Client client : clients) {
            if (client.type == Client.ClientType.PLUGIN && client.name.equalsIgnoreCase(msg.target)) {
                client.sendMsg(msg);
                if (sender != null) {
                    sender.sendMsg(new PlayerMessage(COLOR_CHAR + "a" + msg + " sent!", msg.sender));
                }
                LOGGER.log(Level.FINER, "{0} sent!", msg);
                return;
            }
        }
        if (sender != null) {
            sender.sendMsg(new PlayerMessage(COLOR_CHAR + "cServer " + COLOR_CHAR
                    + "9" + msg.target + COLOR_CHAR + "c not found.", msg.sender));
        }
    }

    private static class Client extends Thread {

        private enum ClientType {

            PLUGIN, APPLICATION
        }
        private static ConvoSyncServer server;
        private Socket socket;
        private ClientType type;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private boolean alive = true, verified = false;
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
            String msg;
            while (alive) {
                try {
                    input = in.readObject();
                    if (!(input instanceof Message)) {
                        LOGGER.log(Level.WARNING, "{0} isn't a message!", input);
                    }
                    if (input != null && input instanceof Message) {
                        if (input instanceof PrivateMessage) {
                            server.out((PrivateMessage) input, this);
                            continue;
                        }
                        if (input instanceof CommandMessage) {
                            server.out((CommandMessage) input, this);
                            continue;
                        }
                        if (input instanceof AuthenticationRequest) {
                            AuthenticationRequest authReq = (AuthenticationRequest) input;
                            name = authReq.name;
                            type = ClientType.valueOf(authReq.type.toUpperCase());
                            if (type == ClientType.APPLICATION) {
                                server.notify("l", ClientType.PLUGIN);
                            }
                            if (type != null) {
                                switch (type) {
                                    case PLUGIN:
                                        if (authReq.password.equals(server.pluginPassword)) {
                                            verified = true;
                                            sendMsg("v");
                                        } else {
                                            sendMsg("w");
                                        }
                                        break;
                                    case APPLICATION:
                                        if (authReq.password.equals(server.applicationPassword)) {
                                            verified = true;
                                            sendMsg("v");
                                        } else {
                                            sendMsg("w");
                                        }
                                        break;
                                }
                            }
                            continue;
                        }
                        if (input instanceof PlayerMessage) {
                            server.out((PlayerMessage) input, this);
                            continue;
                        }
                        msg = ((ChatMessage) input).msg;
                        debug(msg);
                        switch (msg.charAt(0)) {
                            case 'c':
                                server.out("c[" + name + "]" + msg.substring(1), this);
                                break;
                            case 'l':
                            case 'r':
                                server.notify(msg, ClientType.APPLICATION);
                                break;
                            case 'j':
                                server.userMap.put(msg.substring(1), localname);
                                server.notify(msg, ClientType.APPLICATION);
                                break;
                            case 'q':
                                server.userMap.remove(msg.substring(1));
                                server.notify(msg, ClientType.APPLICATION);
                                break;
                            case 'd':
                                alive = false;
                                server.clients.remove(this);
                                break;
                            default:
                                LOGGER.log(Level.WARNING, "Unidentified chat message from {0}: {1}",
                                        new Object[]{this, input});
                                break;
                        }
                    }
                } catch (IOException ex) {
                    alive = false;
                    server.clients.remove(this);
                    if (!socket.isClosed()) {
                        try {
                            socket.close();
                        } catch (IOException ex2) {
                            LOGGER.log(Level.WARNING, "Error disconnecting client " + this, ex2);
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    alive = false;
                    server.clients.remove(this);
                    LOGGER.log(Level.SEVERE, "Fatal error in client " + this, ex);
                    try {
                        socket.close();
                    } catch (IOException ex2) {
                        LOGGER.log(Level.WARNING, "Error disconnecting client " + this, ex2);
                    }
                }
            }
        }

        private void sendMsg(String s) {
            sendMsg(new ChatMessage(s));
        }

        private void sendMsg(Object obj) {
            try {
                out.writeObject(obj);
                out.flush();
            } catch (IOException ex) {
                if (!socket.isClosed()) {
                    LOGGER.log(Level.SEVERE, "Could not write object " + obj, ex);
                }
            }
        }

        private void close() throws IOException {
            alive = false;
            sendMsg("d");
            socket.close();
        }

        @Override
        public String toString() {
            return "Client[" + localname + "," + socket + "," + super.toString() + "]";
        }

        private static void setServer(ConvoSyncServer server) {
            Client.server = server;
        }
    }

    @Override
    public String toString() {
        return "ConvoSyncServer Beta 1.2";
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
                            out("c[" + client.name + "]" + COLOR_CHAR + "c has been kicked.", client);
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
                        + "/name [name] - Sets your name to the given name.\n"
                        + "/help - Prints all commands.");
                break;
        }
    }
    private static final char COLOR_CHAR = '\u00A7';

    private static String format(String s) {
        return s.replaceAll(COLOR_CHAR + "\\w", "");
    }
}

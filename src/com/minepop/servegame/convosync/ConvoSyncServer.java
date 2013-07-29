package com.minepop.servegame.convosync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new ConvoSyncServer().run(args);
    }

    public void run(String[] args) throws IOException {

        System.out.println(this);
        in = new Scanner(System.in);
        for (String arg : args) {
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
                System.out.println("Invalid argument: " + arg);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Invalid argument: " + arg);
            }
        }
        while (port == 0) {
            System.out.print("Enter port: ");
            try {
                port = Integer.parseInt(in.nextLine());
                if (port <= 0) {
                    System.out.println("You did not enter a positive number.");
                    port = 0;
                }
            } catch (NumberFormatException ex) {
                System.out.println("You did not enter an integer.");
            }
        }
        while (pluginPassword == null || pluginPassword.equals("")) {
            System.out.print("Enter a password that the ConvoSync plugins will use to connect: ");
            pluginPassword = in.nextLine();
        }
        while (applicationPassword == null || applicationPassword.equals("")) {
            System.out.print("Enter a password that the ConcoSync application clients will use to connect: ");
            applicationPassword = in.nextLine();
        }
        Client.setServer(this);
        connect();
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
                    } catch (IOException ex) {
                        System.out.println(ex);
                        open = false;
                        client = null;
                    }
                    if (client != null) {
                        System.out.println("Accepted a connection: " + client);
                    }
                    if (open) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }.start();

        String input;
        try {
            while (open) {
                input = in.nextLine();
                if (input != null && input.length() > 0) {
                    if (input.charAt(0) == '/') {
                        String cmd;
                        if (input.contains(" ")) {
                            cmd = input.substring(1, input.indexOf(" "));
                        } else {
                            cmd = input.substring(1);
                        }
                        switch (Command.valueOf(cmd.toUpperCase())) {
                            case EXIT:
                            case STOP:
                                open = false;
                                try {
                                    close();
                                    System.out.println();
                                    System.exit(0);
                                } catch (IOException ex) {
                                    System.out.println("Error closing: " + ex);
                                }
                                break;
                            case RESTART:
                                try {
                                    restart();
                                } catch (IOException ex) {
                                    System.out.println("Error restarting: " + ex);
                                }
                                break;
                            case KICK:
                                int id;
                                boolean found = false;
                                try {
                                    id = Integer.parseInt(input.split(" ")[1]);
                                } catch (NumberFormatException ex) {
                                    System.out.println("Invalid parameters.");
                                    break;
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    System.out.println("Invalid parameters.");
                                    break;
                                } catch (StringIndexOutOfBoundsException ex) {
                                    System.out.println("Invalid parameters.");
                                    break;
                                }
                                for (Client client : clients) {
                                    if (client.socket.getPort() == id) {
                                        found = true;
                                        System.out.println("Closing " + client);
                                        try {
                                            client.close();
                                            System.out.println("Client closed.");
                                        } catch (IOException ex) {
                                            System.out.println("Error closing socket: " + ex);
                                        }
                                        break;
                                    }
                                }
                                if (!found) {
                                    System.out.println("Socket with port " + id + " not found.");
                                }
                                break;
                            case LIST:
                                if (clients.isEmpty()) {
                                    System.out.println("There are currently no connected clients.");
                                } else {
                                    System.out.println("All connected clients:");
                                    for (Client client : clients) {
                                        System.out.println(client);
                                    }
                                }
                                break;
                            case NAME:
                                try {
                                    name = input.split(" ")[1];
                                } catch (NumberFormatException ex) {
                                    System.out.println("Invalid parameters.");
                                } catch (ArrayIndexOutOfBoundsException ex) {
                                    System.out.println("Invalid parameters.");
                                }
                                System.out.println("Name: " + name);
                                break;
                            case HELP:
                                System.out.println("Commands:\n"
                                        + "/exit - Closes the socket and exits the program.\n"
                                        + "/stop - Same as /exit.\n"
                                        + "/restart - Closes the socket and then reopens it.\n"
                                        + "/kick <port> - Closes the socket on the specified port.\n"
                                        + "/list - Lists all connected clients.\n"
                                        + "/name <name> - Sets your name to the given name.\n"
                                        + "/help - Prints all commands.");
                                break;
                            default:
                                System.out.println("Unknown command. Enter /help for a list of commands.");
                                break;
                        }
                    } else {
                        chat("c<§5" + name + "§f> " + input, null);
                    }
                }
                Thread.sleep(50);
            }
        } catch (Exception ex) {
            System.out.println("Fatal error! Closing server.");
            ex.printStackTrace();
            try {
                close();
                System.out.println();
                System.exit(-1);
            } catch (IOException ex2) {
                System.err.println("Error closing server: " + ex2);
            }
        }
    }

    private void restart() throws IOException {
        close();
        connect();
    }

    private void connect() throws IOException {
        socket = new ServerSocket(port);
        System.out.println(socket);
        System.out.println();
    }

    private void close() throws IOException {
        int[] clientPorts = new int[clients.size()];
        for (int idx = 0; idx < clients.size(); idx++) {
            clientPorts[idx] = clients.get(idx).socket.getPort();
        }
        Client client;
        for (int clientPort : clientPorts) {
            client = getClient(clientPort);
            client.close();
        }
        socket.close();
    }

    private Client getClient(int port) {
        for (Client client : clients) {
            if (client.socket.getPort() == port) {
                return client;
            }
        }
        return null;
    }

    private void chat(String s, Client sender) {
        if (sender != null && !sender.verified) {
            return;
        }
        for (Client client : clients) {
            if (client != sender && client.verified) {
                client.sendMsg(s);
            }
        }
        if (sender != null) {
            System.out.println("[" + sender.socket.getPort() + "]" + s.substring(1).replaceAll("Â§[a-zA-Z0-9]", "").replaceAll("§[a-zA-Z0-9]", ""));
        }
    }

    private void notify(String notification, Client.ClientType type) {
        for (Client client : clients) {
            if (client.type == type && client.verified) {
                client.sendMsg(notification);
            }
        }
    }

    private static class Client extends Thread {

        private enum ClientType {

            PLUGIN, APPLICATION
        }
        private static ConvoSyncServer server;
        private Socket socket;
        private ClientType type;
        private PrintWriter out;
        private BufferedReader in;
        private boolean alive = true, verified = false;
        private String name, localname;

        private Client(Socket socket) throws IOException {
            super();
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            while (alive) {
                try {
                    String input = in.readLine();
                    if (input != null && input.length() > 0) {
                        switch (input.charAt(0)) {
                            case 'n':
                                name = input.substring(1).replaceAll("\\?", "");
                                localname = name.replaceAll("Â§[a-zA-Z0-9]", "").replaceAll("§[a-zA-Z0-9]", "");
                                break;
                            case 'c':
                                server.chat("c[" + name + "]" + input.substring(1), this);
                                break;
                            case 't':
                                type = ClientType.valueOf(input.substring(1).toUpperCase());
                                if (type == ClientType.APPLICATION) {
                                    server.notify("l", ClientType.PLUGIN);
                                }
                                break;
                            case 'l':
                            case 'j':
                            case 'q':
                            case 'r':
                                server.notify(input, ClientType.APPLICATION);
                                break;
                            case 'd':
                                alive = false;
                                server.clients.remove(this);
                                break;
                            case 'v':
                                if (type == null) {
                                    break;
                                }
                                switch (type) {
                                    case PLUGIN:
                                        if (input.substring(1).equals(server.pluginPassword)) {
                                            verified = true;
                                            sendMsg("v");
                                        } else {
                                            sendMsg("w");
                                        }
                                        break;
                                    case APPLICATION:
                                        if (input.substring(1).equals(server.applicationPassword)) {
                                            verified = true;
                                            sendMsg("v");
                                        } else {
                                            sendMsg("w");
                                        }
                                        break;
                                }
                                break;
                            default:
                                System.out.println("Unidentified message from " + this + ": " + input);
                                break;
                        }
                    }
                } catch (IOException ex) {
                    if (socket.isClosed()) {
                    } else {
                        try {
                            socket.close();
                        } catch (IOException ex2) {
                            System.out.println("Error disconnecting client " + this + ": " + ex2);
                        }
                    }
                    server.clients.remove(this);
                    alive = false;
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException ex) {
                }
            }
        }

        private void sendMsg(String s) {
            out.println(s);
            out.flush();
        }

        private void close() throws IOException {
            alive = false;
            server.clients.remove(this);
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
        return "ConvoSyncServer Beta 1.1";
    }
}

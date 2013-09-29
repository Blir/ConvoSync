package com.minepop.servegame.convosync.application;

import blir.swing.QuickGUI;
import blir.util.logging.CompactFormatter;
import com.minepop.servegame.convosync.Main;
import com.minepop.servegame.convosync.net.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.*;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Blir
 */
public final class ConvoSyncClient {

    private ConvoSyncGUI gui;
    private LoginGUI login;
    protected String name;
    private String ip, password;
    private int port;
    protected int timeout = 20000;
    private Socket socket;
    protected boolean pm, connected, auth;
    private boolean remember;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private static final Logger LOGGER = Logger.getLogger(ConvoSyncClient.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        try {
            QuickGUI.setLookAndFeel("Windows");
        } catch (ClassNotFoundException ex) {
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (UnsupportedLookAndFeelException ex) {
        }
        // ignore all of those; just use the default look and feel

        new ConvoSyncClient().run(args);
    }

    public ConvoSyncClient() throws IOException {
        Handler handler = new ConsoleHandler();
        Formatter formatter = new CompactFormatter() {
            @Override
            public String format(LogRecord rec) {
                return Main.format(super.format(rec));
            }
        };
        handler.setFormatter(formatter);
        handler.setLevel(Level.FINEST);
        LOGGER.addHandler(handler);
        handler = new FileHandler("CS-Client.log", true);
        handler.setFormatter(formatter);
        handler.setLevel(Level.FINEST);
        LOGGER.addHandler(handler);
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.FINEST);
        LOGGER.log(Level.INFO, java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG)
                .format(java.util.Calendar.getInstance().getTime()));
        LOGGER.log(Level.INFO, toString());
        LOGGER.log(Level.CONFIG, "Java Version: {0}", System.getProperty("java.version"));
        LOGGER.log(Level.CONFIG, "OS Architexture: {0}", System.getProperty("os.arch"));
        LOGGER.log(Level.CONFIG, "OS Name: {0}", System.getProperty("os.name"));
        LOGGER.log(Level.CONFIG, "OS Version: {0}", System.getProperty("os.version"));
    }

    public void run(String[] args) {
        for (String arg : args) {
            try {
                if (arg.startsWith("IP:")) {
                    ip = arg.split(":")[1];
                } else if (arg.startsWith("Port:")) {
                    port = Integer.parseInt(arg.split(":")[1]);
                } else if (arg.startsWith("Name:")) {
                    name = arg.split(":")[1];
                } else if (arg.startsWith("Password:")) {
                    password = arg.split(":")[1];
                }
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            } catch (ArrayIndexOutOfBoundsException ex) {
                LOGGER.log(Level.WARNING, "Invalid argument: {0}", arg);
            }
        }
        gui = new ConvoSyncGUI(this);
        try {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File("login.properties"));
                Properties p = new Properties();
                p.load(fis);
                if (Boolean.parseBoolean(p.getProperty("remember"))) {
                    remember = true;
                    ip = p.getProperty("ip");
                    port = Integer.parseInt(p.getProperty("port"));
                    name = p.getProperty("name");
                }
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading login info.", ex);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.WARNING, "Invalid port in saved login info.");
        }
        login = new LoginGUI(this, ip, port, name, password, remember);
        login.setVisible(true);
    }

    protected String reconnect() {
        disconnect(true);
        return connect();
    }

    protected String connect(String ip, int port, String password, boolean remember) {
        this.ip = ip;
        this.port = port;
        this.password = password;
        Properties p = new Properties();
        p.setProperty("remember", String.valueOf(remember));
        p.setProperty("ip", ip);
        p.setProperty("port", String.valueOf(port));
        p.setProperty("name", name);
        FileOutputStream fos = null;
        try {
            try {
                fos = new FileOutputStream(new File("login.properties"));
                p.store(fos, null);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error saving login info.", ex);
        }
        return connect();
    }

    private String connect() {
        try {
            gui.clearUserList();
            LOGGER.log(Level.INFO, "Connecting to {0}:{1}...", new Object[]{ip, port});
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            connected = true;
            out(new ApplicationAuthenticationRequest(name, password, Main.VERSION));
            new Thread(new InputTask()).start();
            LOGGER.log(Level.INFO, "{0}", socket);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString());
            return ex.getMessage();
        }
        return null;
    }

    protected void disconnect(boolean sendMsg) {
        auth = false;
        connected = false;
        try {
            if (sendMsg) {
                out(new DisconnectMessage());
            }
            socket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    protected void out(String s, boolean override) {
        if (auth || override) {
            out(new ChatMessage(s, override));
        }
    }

    protected void out(Object obj) {
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

    @Override
    public String toString() {
        return "ConvoSyncClient " + Main.VERSION;
    }

    private class InputTask implements Runnable {

        @Override
        public void run() {
            Object input;
            while (connected) {
                try {
                    input = in.readObject();
                    LOGGER.log(Level.FINER, "Input: {0}", input);
                    if (input instanceof Message) {
                        processMessage((Message) input);
                    } else {
                        LOGGER.log(Level.WARNING, "{0} isn't a message!", input);
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    connected = false;
                    gui.log("Connection lost.");
                } catch (ClassNotFoundException ex) {
                    LOGGER.log(Level.SEVERE, "Fatal error.", ex);
                    System.exit(-1);
                }
            }
        }
    }

    private void processMessage(Message msg) {
        if (msg instanceof PrivateMessage) {
            PrivateMessage pmsg = (PrivateMessage) msg;
            gui.log("[[" + pmsg.SERVER + "] " + pmsg.SENDER + "] -> me] " + pmsg.MSG);
            out(new PlayerMessage("[me -> [CS-Client] " + name + "]] " + pmsg.MSG, pmsg.SENDER));
            return;
        }
        if (msg instanceof PlayerMessage) {
            gui.log(((PlayerMessage) msg).MSG);
            return;
        }
        if (msg instanceof ChatMessage) {
            gui.log(((ChatMessage) msg).MSG);
            return;
        }
        if (msg instanceof PlayerListUpdate) {
            PlayerListUpdate update = (PlayerListUpdate) msg;
            gui.clearUserList();
            for (String elem : update.LIST) {
                if (!elem.equals(name)) {
                    gui.addToUserList(elem);
                }
            }
            return;
        }
        if (msg instanceof AuthenticationRequestResponse) {
            AuthenticationRequestResponse response = (AuthenticationRequestResponse) msg;
            auth = response.AUTH;
            if (!Main.VERSION.equals(response.VERSION)) {
                gui.log("Version mismatch: Local version " + Main.VERSION
                        + ", ConvoSync server version " + response.VERSION);
            }
            if (auth) {
                if (login != null) {
                    login.setVisible(false);
                    login = null;
                }
                gui.setVisible(true);
                gui.log("Connected.");
            } else {
                switch (((AuthenticationRequestResponse) msg).REASON) {
                    case INVALID_USER:
                        login.setLabel("Invalid user name.");
                        break;
                    case INVALID_PASSWORD:
                        login.setLabel("Invalid password.");
                        password = null;
                        break;
                    case LOGGED_IN:
                        login.setLabel("You're already logged in.");
                        break;
                }
                disconnect(true);
                return;
            }
            if (msg instanceof DisconnectMessage) {
                switch (((DisconnectMessage) msg).REASON) {
                    case RESTARTING:
                        gui.log("The ConvoSync server is restarting.");
                        break;
                    case CLOSING:
                        gui.log("The ConvoSync server has shut down.");
                        break;
                    case KICKED:
                        gui.log("You have been kicked.");
                        break;
                    case CRASHED:
                        gui.log("Something went wrong, and your server-side thread crashed.\n"
                                + "Contact a server administrator.");
                        break;
                    default:
                        gui.log("You've been disconnected and I don't know why.");
                        break;
                }
                disconnect(false);
            }
        }
    }
}

package com.minepop.servegame.convosync.application;

import blir.swing.QuickGUI;
import blir.swing.listener.*;
import blir.swing.quickgui.InputBox;
import blir.swing.quickgui.MsgBox;
import blir.swing.quickgui.PasswordBox;
import blir.util.logging.CompactFormatter;
import com.minepop.servegame.convosync.Main;
import com.minepop.servegame.convosync.net.*;
import java.io.*;
import java.net.Socket;
import java.util.logging.*;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Blir
 */
public final class ConvoSyncClient {

    private ConvoSyncGUI gui;
    protected String name;
    private String ip, password;
    private int port;
    private Socket socket;
    protected boolean pm, connected, auth;
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
        new LoginGUI(this).setVisible(true);
    }

    protected boolean reconnect() {
        disconnect();
        return connect();
    }

    protected boolean connect(String ip, int port, String password) {
        this.ip = ip;
        this.port = port;
        this.password = password;
        return connect();
    }
    
    private boolean connect() {
        try {
            gui.clearUserList();
            //model.addElement("SERVER");
            LOGGER.log(Level.INFO, "Connecting to {0}:{1}...", new Object[]{ip, port});
            socket = new Socket(ip, port);
            System.out.println(socket);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            connected = true;
            out(new ApplicationAuthenticationRequest(name, password, Main.VERSION));
            final ConvoSyncClient client = this;
            new Thread() {
                @Override
                public void run() {
                    Object input;
                    while (connected) {
                        try {
                            input = in.readObject();
                            LOGGER.log(Level.FINER, "Input: {0}", input);
                            if (!(input instanceof Message)) {
                                LOGGER.log(Level.WARNING, "{0} isn't a message!", input);
                                continue;
                            }
                            if (input instanceof Message) {
                                if (input instanceof PrivateMessage) {
                                    PrivateMessage pm = (PrivateMessage) input;
                                    gui.log("[[" + pm.SERVER + "] " + pm.SENDER + "] -> me] " + pm.MSG);
                                    out(new PlayerMessage("[me -> [CS-Client] " + name + "]] " + pm.MSG, pm.SENDER));
                                    continue;
                                }
                                if (input instanceof PlayerMessage) {
                                    gui.log(((PlayerMessage) input).MSG);
                                    continue;
                                }
                                if (input instanceof ChatMessage) {
                                    gui.log(((ChatMessage) input).MSG);
                                    continue;
                                }
                                if (input instanceof PlayerListMessage) {
                                    PlayerListMessage list = (PlayerListMessage) input;
                                    if (list.JOIN) {
                                        for (String elem : list.LIST) {
                                            if (!elem.equals(name)) {
                                                gui.addToUserList(elem);
                                            }
                                        }
                                    } else {
                                        for (String elem : list.LIST) {
                                            gui.removeFromUserList(elem);
                                        }
                                    }
                                    continue;
                                }
                                if (input instanceof AuthenticationRequestResponse) {
                                    AuthenticationRequestResponse response = (AuthenticationRequestResponse) input;
                                    auth = response.AUTH;
                                    if (!Main.VERSION.equals(response.VERSION)) {
                                        gui.log("Version mismatch: Local version " + Main.VERSION + ", Convosync server version " + response.VERSION);
                                    }
                                    if (auth) {
                                        gui.log("Connected.");
                                    } else {
                                        switch (((AuthenticationRequestResponse) input).REASON) {
                                            case INVALID_USER:
                                                gui.log("Invalid user name.");
                                                break;
                                            case INVALID_PASSWORD:
                                                gui.log("Invalid password.");
                                                break;
                                            case LOGGED_IN:
                                                gui.log("You're already logged in.");
                                                break;
                                        }
                                        name = null;
                                        password = null;
                                        disconnect();
                                        new LoginGUI(client).setVisible(true);
                                    }
                                    continue;
                                }
                                if (input instanceof DisconnectMessage) {
                                    gui.log("The server has disconnected you.");
                                    disconnect();
                                    continue;
                                }
                            }
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            connected = false;
                            gui.log("Connection lost.");
                            continue;
                        } catch (ClassNotFoundException ex) {
                            LOGGER.log(Level.SEVERE, "Fatal error.", ex);
                            System.exit(-1);
                        }
                    }
                }
            }.start();
            LOGGER.log(Level.INFO, "{0}", socket);
            gui.cls();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    protected void disconnect() {
        try {
            out(new DisconnectMessage());
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
}

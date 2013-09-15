package com.minepop.servegame.convosync.application;

import blir.swing.QuickGUI;
import blir.swing.listener.*;
import blir.swing.quickgui.InputBox;
import blir.swing.quickgui.MsgBox;
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
    protected String ip, name, password;
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
        setup();
        task1();
    }

    private void task1() {
        if (ip == null) {
            new InputBox("ConvoSyncClient - Enter IP", "Enter the IP of the server:", "127.0.0.1", new InputListener() {
                @Override
                public void onInput(String input) {
                    ip = input;
                    task2();
                }
            }, true).setVisible(true);
        } else {
            task2();
        }
    }

    private void task2() {
        if (port == 0) {
            new InputBox("ConvoSyncClient - Enter Port", "Enter the port the server listens to:", "25000", new InputListener() {
                @Override
                public void onInput(String input) {
                    try {
                        port = Integer.parseInt(input);
                        task3();
                    } catch (NumberFormatException ex) {
                        new MsgBox("ConvoSyncClient - Error", "\"" + input + "\" is not a valid port.", new Runnable() {
                            @Override
                            public void run() {
                                task2();
                            }
                        }, true).setVisible(true);
                    }
                }
            }, true).setVisible(true);
        } else {
            task3();
        }
    }

    private void task3() {
        if (name == null) {
            new InputBox("ConvoSyncClient - Enter User Name", "Enter your MC user name:", new InputListener() {
                @Override
                public void onInput(final String input) {
                    name = input;
                    task4();
                }
            }, true).setVisible(true);
        } else {
            task4();
        }
    }

    private void task4() {
        if (password == null) {
            new InputBox("ConvoSyncClient - Enter CS Password", "Enter your ConvoSync password:", new InputListener() {
                @Override
                public void onInput(String input) {
                    password = input;
                    task5();
                }
            }, true).setVisible(true);
        } else {
            task5();
        }
    }

    private void task5() {
        if (connect()) {
            gui.setVisible(true);
        } else {
            new MsgBox("ConvoSyncClient - Warning", "Can't reach server. Press OK to retry.", new Runnable() {
                @Override
                public void run() {
                    ip = null;
                    port = 0;
                    task1();
                }
            }, true).setVisible(true);
        }
    }

    private void setup() {
        gui = new ConvoSyncGUI(this);
    }

    protected boolean reconnect() {
        disconnect();
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
            out(new ApplicationAuthenticationRequest(name, password));
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
                                    auth = ((AuthenticationRequestResponse) input).AUTH;
                                    if (!auth) {
                                        gui.log("Invalid login. Make sure you're logged out of Minecraft, registered on the CS server, and check your user name and password.");
                                        name = null;
                                        password = null;
                                        disconnect();
                                        task3();
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
                            gui.log("Connection lost. Try to reconnect.");
                            continue;
                        } catch (ClassNotFoundException ex) {
                            System.exit(-1);
                            connected = false;
                        }
                    }
                }
            }.start();
            LOGGER.log(Level.INFO, null, socket);
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

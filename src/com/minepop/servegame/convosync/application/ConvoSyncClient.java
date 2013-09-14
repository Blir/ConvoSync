package com.minepop.servegame.convosync.application;

import blir.swing.QuickGUI;
import blir.swing.listener.*;
import blir.util.logging.CompactFormatter;
import com.minepop.servegame.convosync.Main;
import com.minepop.servegame.convosync.net.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.DefaultCaret;

/**
 *
 * @author Blir
 */
public final class ConvoSyncClient {

    private String ip, name, password;
    private int port;
    private Socket socket;
    private boolean pm, connected, auth, debug;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private JFrame frame;
    private JTextArea console;
    private JTextField input;
    private JMenuBar mb;
    private JMenu options;
    private JMenuItem reconnect, cls, refresh, about, toggleDebug;
    private JScrollPane cpane, lpane;
    private JList<String> list;
    private DefaultListModel<String> model;
    private DefaultListSelectionModel selection;
    private static final Logger LOGGER = Logger.getLogger(ConvoSyncClient.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        try {
            QuickGUI.setLookAndFeel("Windows");
        } catch (ClassNotFoundException ex) {
            QuickGUI.msgBox("ConvoSyncClient - Warning", "Unable to use Windows look and feel. Using default look and feel.");
        } catch (InstantiationException ex) {
            QuickGUI.msgBox("ConvoSyncClient - Warning", "Unable to use Windows look and feel. Using default look and feel.");
        } catch (IllegalAccessException ex) {
            QuickGUI.msgBox("ConvoSyncClient - Warning", "Unable to use Windows look and feel. Using default look and feel.");
        } catch (UnsupportedLookAndFeelException ex) {
            QuickGUI.msgBox("ConvoSyncClient - Warning", "Unable to use Windows look and feel. Using default look and feel.");
        }

        new ConvoSyncClient().run(args);
    }

    public ConvoSyncClient() throws IOException {
        Handler handler = new ConsoleHandler();
        Formatter formatter = new CompactFormatter() {
            @Override
            public String format(LogRecord rec) {
                return super.format(rec).replaceAll(COLOR_CHAR + "\\w", "");
            }
        };
        handler.setFormatter(formatter);
        handler.setLevel(Level.CONFIG);
        LOGGER.addHandler(handler);
        handler = new FileHandler("CS-Client.log", true);
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
        task1();
    }

    private void task1() {
        if (ip == null) {
            QuickGUI.inputBox("ConvoSyncClient - Enter IP", "Enter the IP of the server:", "127.0.0.1", new InputListener() {
                @Override
                public void onInput(String input) {
                    ip = input;
                    task2();
                }
            }, true);
        } else {
            task2();
        }
    }

    private void task2() {
        if (port == 0) {
            QuickGUI.inputBox("ConvoSyncClient - Enter Port", "Enter the port the server is hosted on:", "25000", new InputListener() {
                @Override
                public void onInput(String input) {
                    try {
                        port = Integer.parseInt(input);
                        task3();
                    } catch (NumberFormatException ex) {
                        QuickGUI.msgBox("ConvoSyncClient - Error", "\"" + input + "\" is not a valid port.", new Runnable() {
                            @Override
                            public void run() {
                                task2();
                            }
                        });
                    }
                }
            }, true);
        } else {
            task3();
        }
    }

    private void task3() {
        if (name == null) {
            QuickGUI.inputBox("ConvoSyncClient - Enter Name", "Enter your MC user name:", new InputListener() {
                @Override
                public void onInput(final String input) {
                    if (input.toLowerCase().contains("server")) {
                        QuickGUI.msgBox("ConvoSyncClient - Invalid Name", "Your name cannot contain any variant of \"server\".", new Runnable() {
                            @Override
                            public void run() {
                                task3();
                            }
                        });
                        return;
                    } else if (input.contains(" ") || input.contains("_")) {
                        QuickGUI.msgBox("ConvoSyncClient - Invalid Name", "Your name cannot contain spaces or underscores.", new Runnable() {
                            @Override
                            public void run() {
                                task3();
                            }
                        });
                        return;
                    }
                    name = input;
                    task4();
                }
            }, true);
        } else {
            task4();
        }
    }

    private void task4() {
        if (password == null) {
            QuickGUI.inputBox("ConvoSyncClient - Enter Password", "Enter your password:", new InputListener() {
                @Override
                public void onInput(String input) {
                    password = input;
                    task5();
                }
            }, true);
        } else {
            task5();
        }
    }

    private void task5() {
        if (connect()) {
            setup();
        } else {
            QuickGUI.yesOrNoBox("ConvoSyncClient - Error", "Could not connect to server. Retry?", new YesOrNoListener() {
                @Override
                public void onYes() {
                    ip = null;
                    port = 0;
                    task1();
                }

                @Override
                public void onNo() {
                }
            });
        }
    }

    private void setup() {
        frame = new JFrame("ConvoSyncClient " + Main.VERSION);
        console = new JTextArea();
        input = new JTextField();
        mb = new JMenuBar();
        options = new JMenu("Options");
        reconnect = new JMenuItem("Reconnect");
        cls = new JMenuItem("Clear Console");
        refresh = new JMenuItem("Refresh");
        about = new JMenuItem("About");
        toggleDebug = new JMenuItem("Debug");
        JButton bugfix = new JButton();
        cpane = new JScrollPane(console);
        model = new DefaultListModel<String>();
        list = new JList<String>(model);
        lpane = new JScrollPane(list);
        selection = new DefaultListSelectionModel();

        frame.setSize(330, 405);
        frame.setMinimumSize(new Dimension(330, 405));
        cpane.setBounds(5, 5, 200, 300);
        lpane.setBounds(210, 5, 95, 300);
        input.setBounds(5, 310, 300, 30);
        list.setBounds(5, 5, 230, 150);

        mb.add(options);
        options.add(reconnect);
        options.add(cls);
        options.add(toggleDebug);
        options.add(about);
        options.add(refresh);
        cpane.setAutoscrolls(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().addHierarchyBoundsListener(new HierarchyBoundsListener() {
            @Override
            public void ancestorMoved(HierarchyEvent he) {
            }

            @Override
            public void ancestorResized(HierarchyEvent he) {
                cpane.setSize(frame.getWidth() - 130, frame.getHeight() - 105);
                lpane.setBounds(frame.getWidth() - 120, lpane.getY(), lpane.getWidth(), frame.getHeight() - 105);
                input.setBounds(5, frame.getHeight() - 95, frame.getWidth() - 30, 30);
            }
        });
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });
        console.setEditable(false);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        ((DefaultCaret) console.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (input.getText().equals("")) {
                    return;
                }
                if (!auth) {
                    log1("You must authenticate before you can send any messages.");
                    return;
                }
                if (input.getText().charAt(0) == '/') {
                    int delim = input.getText().indexOf(" ");
                    if (delim > 0) {
                        String server = input.getText().substring(1, delim);
                        String cmd = input.getText().substring(delim);
                        out(new CommandMessage(name, server, cmd));
                    } else {
                        log1("Usage: /<server> <command>");
                    }
                } else {
                    out(input.getText(), false);
                    log2(input.getText());
                }
                input.setText("");
            }
        });
        reconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                reconnect();
            }
        });
        cls.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                console.setText("");
            }
        });
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                list.clearSelection();
            }
        });
        about.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                QuickGUI.msgBox("ConvoSyncClient - About", "Compatible with versions: CS-1.0.0,CS-1.0.1");
            }
        });
        toggleDebug.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                log1((debug = !debug) ? "Debugging enabled." : "Debugging disabled.");
                if (debug) {
                    for (Handler h : LOGGER.getHandlers()) {
                        h.setLevel(Level.FINEST);
                    }
                    LOGGER.setLevel(Level.FINEST);
                } else {
                    for (Handler h : LOGGER.getHandlers()) {
                        h.setLevel(Level.CONFIG);
                    }
                    LOGGER.setLevel(Level.CONFIG);
                }
            }
        });
        bugfix.setVisible(false);
        selection.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectionModel(selection);
        list.setSelectionMode(JList.VERTICAL_WRAP);
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                if (list.getSelectedValue() == null || pm) {
                    return;
                }
                final String recip = list.getSelectedValue();
                list.clearSelection();
                pm = true;

                QuickGUI.inputBox("ConvoSyncClient - Send PM", "Enter what you would like to send " + recip + ":", new InputOrCancelListener() {
                    @Override
                    public void onCancel() {
                        pm = false;
                    }

                    @Override
                    public void onInput(String input) {
                        out(new PrivateMessage(recip, name, input, "CS-Client"));
                        pm = false;
                    }
                }, false);
            }
        });
        //model.addElement("SERVER");

        frame.add(cpane);
        frame.add(lpane);
        frame.add(input);
        frame.add(bugfix);
        frame.setJMenuBar(mb);

        frame.setVisible(true);
    }

    private void log2(String s) {
        console.setText(console.getText() + "\n[" + name + "] " + s);
    }

    private void log1(String s) {
        console.setText(console.getText() + "\n" + s);
    }

    private boolean reconnect() {
        disconnect();
        return connect();
    }

    private boolean connect() {
        try {
            if (model != null) {
                model.clear();
            }
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
                            if (console != null && input instanceof Message) {
                                if (input instanceof ChatMessage) {
                                    log1(format(((ChatMessage) input).MSG));
                                    continue;
                                }
                                if (input instanceof PlayerListMessage) {
                                    PlayerListMessage list = (PlayerListMessage) input;
                                    if (list.JOIN) {
                                        for (String elem : list.LIST) {
                                            model.addElement(elem);
                                        }
                                    } else {
                                        for (String elem : list.LIST) {
                                            model.removeElement(elem);
                                        }
                                    }
                                    continue;
                                }
                                if (input instanceof AuthenticationRequestResponse) {
                                    auth = ((AuthenticationRequestResponse) input).AUTH;
                                    if (!auth) {
                                        console.setText("Invalid user name or password.");
                                        name = null;
                                        password = null;
                                        task3();
                                    }
                                    continue;
                                }
                                if (input instanceof DisconnectMessage) {
                                    log1("The server has disconnected you.");
                                    continue;
                                }
                            }
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            connected = false;
                            console.setText("Connection lost. Try to reconnect.");
                            continue;
                        } catch (ClassNotFoundException ex) {
                            QuickGUI.errorBox("ConvoSyncClient - Error", "Fatal error: " + ex, ex.hashCode());
                            connected = false;
                        }
                    }
                }
            }.start();
            LOGGER.log(Level.INFO, null, socket);
            if (console != null) {
                console.setText("");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private void disconnect() {
        try {
            out(new DisconnectMessage());
            socket.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void out(String s, boolean override) {
        if (auth || override) {
            out(new ChatMessage(s, override));
        }
    }

    private void out(Object obj) {
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
    private static final char COLOR_CHAR = '\u00A7';

    private static String format(String s) {
        return s.replaceAll(COLOR_CHAR + "\\w", "");
    }
}

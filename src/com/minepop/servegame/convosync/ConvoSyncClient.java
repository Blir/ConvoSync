package com.minepop.servegame.convosync;

import blir.swing.QuickGUI;
import blir.swing.listener.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.event.*;

/**
 *
 * @author Blir
 */
public class ConvoSyncClient {

    private String ip, name, password;
    private int port;
    private Socket socket;
    private boolean pm = false, connected, verified;
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame;
    private JTextArea console;
    private JTextField input;
    private JMenuBar mb;
    private JMenu options;
    private JMenuItem reconnect, cls, refresh;
    private JScrollPane cpane, lpane;
    private JList<String> list;
    private DefaultListModel<String> model;
    private DefaultListSelectionModel selection;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            QuickGUI.setLookAndFeel("Windows");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            QuickGUI.infoBox("ConvoSyncClient - Warning", "Unable to use Windows look and feel. Using default look and feel.");
        }

        new ConvoSyncClient().run(args);
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
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            }
        }
        task1();
    }

    private void task1() {
        if (ip == null) {
            QuickGUI.inputBox("ConvoSyncClient - Enter IP", "Enter the IP of the server:", new InputOrCancelListener() {
                @Override
                public void onCancel() {
                }

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
            QuickGUI.inputBox("ConvoSyncClient - Enter Port", "Enter the port the server is hosted on:", new InputOrCancelListener() {
                @Override
                public void onCancel() {
                }

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
        QuickGUI.inputBox("ConvoSyncClient - Enter Name", "Enter your name:", new InputOrCancelListener() {
            @Override
            public void onCancel() {
            }

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
    }

    private void task4() {
        if (password == null) {
            QuickGUI.inputBox("ConvoSyncClient - Enter Password", "Enter the password to connect to the server:", new InputOrCancelListener() {
                @Override
                public void onCancel() {
                }

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
        frame = new JFrame("ConvoSyncClient Beta 1.1");
        console = new JTextArea();
        input = new JTextField();
        mb = new JMenuBar();
        options = new JMenu("Options");
        reconnect = new JMenuItem("Reconnect");
        cls = new JMenuItem("Clear Console");
        refresh = new JMenuItem("Refresh");
        JButton bugfix = new JButton();
        cpane = new JScrollPane(console);
        model = new DefaultListModel<>();
        list = new JList<>(model);
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
        //options.add(refresh);
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
        input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (input.getText().equals("")) {
                    return;
                }
                if (!verified) {
                    log1("You must authenticate before you can send any messages.");
                    return;
                }
                out("c " + input.getText());
                log2(input.getText());
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
                        //out("p" + recip + " " + input.replaceAll(" ", "_"));
                        //log3(input, recip);
                        QuickGUI.infoBox("ConvoSyncClient - Warning", "PMs are not currently supported.");
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

    private void log3(String s, String recip) {
        console.setText(console.getText() + "\n[PM][To: " + recip + "] " + s);
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
            System.out.println("Connecting to " + ip + ":" + port + "...");
            socket = new Socket(ip, port);
            System.out.println(socket);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            connected = true;
            verify();
            new Thread() {
                @Override
                public void run() {
                    while (connected) {
                        String s;
                        try {
                            s = in.readLine();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            connected = false;
                            console.setText("Connection lost. Try to reconnect.");
                            continue;
                        }
                        if (s != null) {
                            System.out.println("Input: " + s);
                            if (console != null) {
                                switch (s.charAt(0)) {
                                    case 'c':
                                        log1(s.substring(1).replaceAll("Â§[a-zA-Z0-9]", "").replaceAll("§[a-zA-Z0-9]", ""));
                                        break;
                                    case 'i':
                                        QuickGUI.infoBox("ConvoSyncClient - Warning", "You seem to have send a private message with invalid parameters."
                                                + " Check to make sure you entered a valid name.");
                                        break;
                                    case 'u':
                                        QuickGUI.infoBox("ConvoSyncClient - Warning", "The server seems to have disconnected for unknown reasons."
                                                + " Try reconnecting.");
                                        break;
                                    case 'd':
                                        log1("The server has disconnected you.");
                                        break;
                                    case 'n':
                                        //verification = true;
                                        break;
                                    case 't':
                                        //verifyName();
                                        break;
                                    case 'l':
                                        String[] list = s.substring(1).split("`");
                                        for (String element : list) {
                                            element = element.replaceAll("Â§[a-zA-Z0-9]", "").replaceAll("§[a-zA-Z0-9]", "");
                                            if (!model.contains(element) && !element.equals("")) {
                                                model.addElement(element);
                                            }
                                        }
                                        break;
                                    case 'j':
                                        model.addElement(s.substring(1).replaceAll("Â§[a-zA-Z0-9]", "").replaceAll("§[a-zA-Z0-9]", ""));
                                        break;
                                    case 'q':
                                        model.removeElement(s.substring(1).replaceAll("Â§[a-zA-Z0-9]", "").replaceAll("§[a-zA-Z0-9]", ""));
                                        break;
                                    case 'r':
                                        list = s.substring(1).split("`");
                                        for (String element : list) {
                                            element = element.replaceAll("Â§[a-zA-Z0-9]", "").replaceAll("§[a-zA-Z0-9]", "");
                                            model.removeElement(element);
                                        }
                                        break;
                                    case 'v':
                                        verified = true;
                                        out("c has joined.");
                                        break;
                                    case 'w':
                                        verified = false;
                                        password = null;
                                        console.setText("Failed to authenticate with server.");
                                        QuickGUI.inputBox("ConvoSyncClient - Enter Password", "Enter the password to connect to the server:", new InputOrCancelListener() {
                                            @Override
                                            public void onCancel() {
                                            }

                                            @Override
                                            public void onInput(String input) {
                                                password = input;
                                                reconnect();
                                            }
                                        }, true);
                                        break;
                                }
                            }
                        }
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }.start();
            if (console != null) {
                console.setText("");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private void disconnect() {
        try {
            out("c has left.");
            out.println("d");
            out.flush();
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void out(String s) {
        if (verified) {
            out.println(s);
            out.flush();
        }
    }

    private void verify() {
        out.println("tapplication");
        out.flush();
        out.println("v" + password);
        out.flush();
        out.println("nÂ§dConvoSyncClientÂ§f-Â§4" + name + "Â§f");
        out.flush();
    }
}

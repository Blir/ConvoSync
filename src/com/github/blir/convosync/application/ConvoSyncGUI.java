package com.github.blir.convosync.application;

import com.github.blir.convosync.net.*;

import blir.swing.listener.NewPasswordListener;
import blir.swing.quickgui.MsgBox;
import blir.swing.quickgui.NewPasswordBox;

import com.github.blir.convosync.Main;

import java.awt.Cursor;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.logging.Level;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author Blir
 */
public class ConvoSyncGUI extends javax.swing.JFrame {

    private final ConvoSyncClient client;
    private final javax.swing.DefaultListModel<String> model;
    private final Calendar CAL;
    protected final AdminConsoleGUI adminConsole;
    private boolean busy;

    /**
     * Creates new form ConvoSyncGUI
     *
     * @param client
     */
    protected ConvoSyncGUI(ConvoSyncClient client) {
        super(client.toString());
        this.client = client;
        initComponents();
        model = new javax.swing.DefaultListModel<String>();
        userList.setModel(model);
        if (output.getCaret() instanceof javax.swing.text.DefaultCaret) {
            ((javax.swing.text.DefaultCaret) output.getCaret())
                    .setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE);
        }
        CAL = Calendar.getInstance();
        onToggleWordWrap(null);
        setLocationRelativeTo(null);
        adminConsole = new AdminConsoleGUI(client, this);
        if (client.defaultCloseOperation != JFrame.EXIT_ON_CLOSE) {
            setDefaultCloseOperation(client.defaultCloseOperation);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        output = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        userList = new javax.swing.JList<String>();
        input = new javax.swing.JTextField();
        infoLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        connectionMenu = new javax.swing.JMenu();
        reconnectMenuItem = new javax.swing.JMenuItem();
        logOutMenuItem = new javax.swing.JMenuItem();
        accountMenu = new javax.swing.JMenu();
        changePasswordMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        clsMenuItem = new javax.swing.JMenuItem();
        timeStampsMenuItem = new javax.swing.JCheckBoxMenuItem();
        wordWrapMenuItem = new javax.swing.JCheckBoxMenuItem();
        adminConsoleMenuItem = new javax.swing.JMenuItem();
        optionsMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                ConvoSyncGUI.this.windowClosing(evt);
            }
        });

        jSplitPane1.setDividerLocation(350);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setAlignmentY(131.0F);

        output.setEditable(false);
        output.setColumns(20);
        output.setRows(5);
        jScrollPane1.setViewportView(output);

        jSplitPane1.setLeftComponent(jScrollPane1);

        userList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                onUserListSelection(evt);
            }
        });
        jScrollPane2.setViewportView(userList);

        jSplitPane1.setRightComponent(jScrollPane2);

        input.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onInput(evt);
            }
        });

        infoLabel.setText("Press enter to send.");

        connectionMenu.setText("Connection");

        reconnectMenuItem.setText("Reconnect");
        reconnectMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onReconnect(evt);
            }
        });
        connectionMenu.add(reconnectMenuItem);

        logOutMenuItem.setText("Log Out");
        logOutMenuItem.setToolTipText("");
        logOutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onLogOut(evt);
            }
        });
        connectionMenu.add(logOutMenuItem);

        jMenuBar1.add(connectionMenu);

        accountMenu.setText("Account");

        changePasswordMenuItem.setText("Change Password");
        changePasswordMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onNewPasswordRequest(evt);
            }
        });
        accountMenu.add(changePasswordMenuItem);

        jMenuBar1.add(accountMenu);

        viewMenu.setText("View");

        clsMenuItem.setText("Clear Output");
        clsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onClearOutput(evt);
            }
        });
        viewMenu.add(clsMenuItem);

        timeStampsMenuItem.setText("Time Stamps");
        viewMenu.add(timeStampsMenuItem);

        wordWrapMenuItem.setSelected(true);
        wordWrapMenuItem.setText("Word Wrap");
        wordWrapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onToggleWordWrap(evt);
            }
        });
        viewMenu.add(wordWrapMenuItem);

        adminConsoleMenuItem.setText("Admin Console");
        adminConsoleMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onAdminConsoleOpened(evt);
            }
        });
        viewMenu.add(adminConsoleMenuItem);

        jMenuBar1.add(viewMenu);

        optionsMenu.setText("Options");
        optionsMenu.setToolTipText("");

        helpMenuItem.setText("Help: https://github.com/Blir/ConvoSync/wiki");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onHelp(evt);
            }
        });
        optionsMenu.add(helpMenuItem);

        jMenuBar1.add(optionsMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(input)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(infoLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onInput(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onInput
        if (client.pm) {
            infoLabel.setText("Press enter to send.");
            client.pm = false;
            if (client.auth) {
                if (input.getText().equals("")) {
                    return;
                }
                client.out(new PrivateMessage(new MessageRecipient(userList.getSelectedValue(), MessageRecipient.SenderType.UNKNOWN),
                                              new MessageRecipient(client.name, MessageRecipient.SenderType.CONVOSYNC_CLIENT),
                                              input.getText(), "CS-Client"));
            }
            userList.clearSelection();
        } else if (client.auth && !input.getText().equals("")) {
            client.out(input.getText(), false);
            logChat(input.getText());
        }
        if (!client.auth) {
            output.setText("You are not authenticated to send messages.");
        }
        input.setText("");
    }//GEN-LAST:event_onInput

    private void onUserListSelection(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_onUserListSelection
        if (userList.getSelectedValue() == null || client.pm) {
            return;
        }
        infoLabel.setText("Press enter to send. [PM: " + userList.getSelectedValue() + "]");
        client.pm = true;
        input.grabFocus();
    }//GEN-LAST:event_onUserListSelection

    private void windowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_windowClosing
        ConvoSyncClient.LOGGER.log(Level.FINE, "Main GUI closing.");
        client.disconnect(true);
        if (client.defaultCloseOperation != JFrame.EXIT_ON_CLOSE) {
            ConvoSyncClient.LOGGER.log(Level.FINE, "Not exiting on close; disposing of GUIs.");
            if (client.login != null) {
                client.login.dispose();
            }
            adminConsole.dispose();
        }
    }//GEN-LAST:event_windowClosing

    private void onReconnect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onReconnect
        if (busy) {
            return;
        }
        if (!client.auth) {
            log("You are not authenticated.");
            return;
        }
        busy = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.reconnect();
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                busy = false;
            }
        }).start();
    }//GEN-LAST:event_onReconnect

    private void onClearOutput(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onClearOutput
        output.setText("");
    }//GEN-LAST:event_onClearOutput

    private void onToggleWordWrap(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onToggleWordWrap
        output.setWrapStyleWord(wordWrapMenuItem.getState());
        output.setLineWrap(wordWrapMenuItem.getState());
    }//GEN-LAST:event_onToggleWordWrap

    private void onNewPasswordRequest(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onNewPasswordRequest
        new NewPasswordBox("CS - New Password", "", new NewPasswordListener() {
            @Override
            public void onInput(String input) {
                client.out(new UserPropertyChange(UserPropertyChange.Property.PASSWORD, input));
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onPasswordMismatch() {
                new MsgBox("ConvoSyncClient - Warning", "The passwords you entered did not match.", false).setVisible(true);
            }
        }, false).setVisible(true);
    }//GEN-LAST:event_onNewPasswordRequest

    private void onLogOut(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onLogOut
        client.disconnect(true);
        setVisible(false);
        adminConsole.setVisible(false);
        client.openLoginGUI();
        cls();
    }//GEN-LAST:event_onLogOut

    private void onHelp(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onHelp
        URI help = null;
        try {
            help = new URI("https://github.com/Blir/ConvoSync/wiki");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(help);
            } else {
                JOptionPane.showMessageDialog(rootPane, "Your desktop is not supported.", "ConvosyncClient - Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (URISyntaxException ex) {
            ConvoSyncClient.LOGGER.log(Level.WARNING, "Has GitHub ceased to exist? : ", ex);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(rootPane, "Couldn't reach " + help + ": " + ex, "ConvoSyncClient - Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_onHelp

    private void onAdminConsoleOpened(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAdminConsoleOpened
        if (client.op) {
            adminConsole.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(rootPane, "You must be opped to issue cross-server commands.", "ConvoSyncClient - You're not OP", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_onAdminConsoleOpened

    protected void log(String s) {
        s = Main.format(s);
        if (timeStampsMenuItem.getState()) {
            CAL.setTimeInMillis(System.currentTimeMillis());
            StringBuilder sb = new StringBuilder();
            String hour = String.valueOf(CAL.get(Calendar.HOUR_OF_DAY));
            String minute = String.valueOf(CAL.get(Calendar.MINUTE));
            String second = String.valueOf(CAL.get(Calendar.SECOND));
            sb
                    .append(hour.length() == 1 ? "0" : "")
                    .append(hour)
                    .append(minute.length() == 1 ? ":0" : ":")
                    .append(minute)
                    .append(second.length() == 1 ? ":0" : ":")
                    .append(second)
                    .append(" ");
            s = sb.append(s).toString();
        }
        output.setText(output.getText() + "\n" + s);
    }

    protected void log(String s, Object... objects) {
        for (int idx = 0; idx < objects.length; idx++) {
            s = s.replace("{" + idx + "}", String.valueOf(objects[idx]));
        }
        log(s);
    }

    protected void logChat(String s) {
        log("[" + client.name + "] " + s);
    }

    protected void setText(String s) {
        output.setText(s);
    }

    protected void addToUserList(String elem) {
        model.addElement(elem);
    }

    protected boolean removeFromUserList(String elem) {
        return model.removeElement(elem);
    }

    protected void clearUserList() {
        model.clear();
    }

    protected void cls() {
        output.setText("");
    }

    protected boolean useTimeStamps() {
        return timeStampsMenuItem.getState();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu accountMenu;
    private javax.swing.JMenuItem adminConsoleMenuItem;
    private javax.swing.JMenuItem changePasswordMenuItem;
    private javax.swing.JMenuItem clsMenuItem;
    private javax.swing.JMenu connectionMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JTextField input;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuItem logOutMenuItem;
    private javax.swing.JMenu optionsMenu;
    private javax.swing.JTextArea output;
    private javax.swing.JMenuItem reconnectMenuItem;
    private javax.swing.JCheckBoxMenuItem timeStampsMenuItem;
    private javax.swing.JList<String> userList;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JCheckBoxMenuItem wordWrapMenuItem;
    // End of variables declaration//GEN-END:variables
}

package com.minepop.servegame.convosync.application;

import blir.swing.listener.NewPasswordListener;
import blir.swing.quickgui.MsgBox;
import blir.swing.quickgui.NewPasswordBox;
import com.minepop.servegame.convosync.Main;
import com.minepop.servegame.convosync.net.*;
import java.awt.Cursor;
import java.util.Calendar;

/**
 *
 * @author Blir
 */
public class ConvoSyncGUI extends javax.swing.JFrame {

    private ConvoSyncClient client;
    private javax.swing.DefaultListModel<String> model;
    private final Calendar CAL;

    /**
     * Creates new form ConvoSyncGUI
     */
    public ConvoSyncGUI(ConvoSyncClient client) {
        super(client.toString());
        this.client = client;
        initComponents();
        model = new javax.swing.DefaultListModel<String>();
        userList.setModel(model);
        if (output.getCaret() instanceof javax.swing.text.DefaultCaret) {
            ((javax.swing.text.DefaultCaret) output.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE);
        }
        CAL = Calendar.getInstance();
        onToggleWordWrap(null);
        setLocationRelativeTo(null);
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
        jLabel1 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jCheckBoxMenuItem2 = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();

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

        jLabel1.setText("Press enter to send.");

        jMenu1.setText("Options");

        jMenuItem1.setText("Reconnect");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onReconnect(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setText("Clear Output");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onClearOutput(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jCheckBoxMenuItem2.setSelected(true);
        jCheckBoxMenuItem2.setText("Word Wrap");
        jCheckBoxMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onToggleWordWrap(evt);
            }
        });
        jMenu1.add(jCheckBoxMenuItem2);

        jCheckBoxMenuItem1.setText("Time Stamps");
        jMenu1.add(jCheckBoxMenuItem1);

        jMenuItem3.setText("Refresh");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OnRefresh(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Account");

        jMenuItem4.setText("Change Password");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onPasswordChangeRequest(evt);
            }
        });
        jMenu2.add(jMenuItem4);

        jMenuBar1.add(jMenu2);

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
                        .addComponent(jLabel1)
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
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(input, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onInput(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onInput
        if (client.pm) {
            jLabel1.setText("Press enter to send.");
            client.pm = false;
            if (client.auth) {
                if (input.getText().equals("")) {
                    return;
                }
                client.out(new PrivateMessage(userList.getSelectedValue(),
                        client.name, input.getText(), "CS-Client"));
            }
            userList.clearSelection();
        } else if (client.auth) {
            if (input.getText().equals("")) {
                return;
            }
            if (input.getText().charAt(0) == '/') {
                int delim = input.getText().indexOf(" ");
                if (delim > 0) {
                    String server = input.getText().substring(1, delim);
                    String cmd = input.getText().substring(delim + 1);
                    client.out(new CommandMessage(client.name, server, cmd));
                } else {
                    logChat("Usage: /<server> <command>");
                }
            } else {
                client.out(input.getText(), false);
                logChat(input.getText());
            }
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
        jLabel1.setText("Press enter to send. [PM: " + userList.getSelectedValue() + "]");
        client.pm = true;
        input.grabFocus();
    }//GEN-LAST:event_onUserListSelection

    private void windowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_windowClosing
        client.disconnect(true);
    }//GEN-LAST:event_windowClosing

    private void onReconnect(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onReconnect
        jMenuItem1.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.reconnect();
            }
        }).start();
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        jMenuItem1.setEnabled(true);
    }//GEN-LAST:event_onReconnect

    private void onClearOutput(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onClearOutput
        output.setText("");
    }//GEN-LAST:event_onClearOutput

    private void OnRefresh(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OnRefresh
        userList.clearSelection();
        client.pm = false;
        jLabel1.setText("Press enter to send.");
    }//GEN-LAST:event_OnRefresh

    private void onToggleWordWrap(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onToggleWordWrap
        output.setWrapStyleWord(jCheckBoxMenuItem2.getState());
        output.setLineWrap(jCheckBoxMenuItem2.getState());
    }//GEN-LAST:event_onToggleWordWrap

    private void onPasswordChangeRequest(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onPasswordChangeRequest
        new NewPasswordBox("ConvoSyncClient - New Password", "", new NewPasswordListener() {
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
    }//GEN-LAST:event_onPasswordChangeRequest

    public void log(String s) {
        s = Main.format(s);
        if (jCheckBoxMenuItem1.getState()) {
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

    public void log(String s, Object... objects) {
        for (int idx = 0; idx < objects.length; idx++) {
            s = s.replace("{" + idx + "}", String.valueOf(objects[idx]));
        }
    }

    public void logChat(String s) {
        log("[" + client.name + "] " + s);
    }

    public void setText(String s) {
        output.setText(s);
    }

    public void addToUserList(String elem) {
        model.addElement(elem);
    }

    public boolean removeFromUserList(String elem) {
        return model.removeElement(elem);
    }

    public void clearUserList() {
        model.clear();
    }

    public void cls() {
        input.setText("");
    }

    public boolean useTimeStamps() {
        return jCheckBoxMenuItem1.getState();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField input;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextArea output;
    private javax.swing.JList<String> userList;
    // End of variables declaration//GEN-END:variables
}

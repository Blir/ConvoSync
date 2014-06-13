package com.github.blir.convosync.application;

import com.github.blir.convosync.Main;
import com.github.blir.convosync.net.CommandMessage;
import com.github.blir.convosync.net.MessageRecipient;

/**
 *
 * @author Blir
 */
public class AdminConsoleGUI extends javax.swing.JFrame {

    private final ConvoSyncClient client;
    private final javax.swing.DefaultListModel<String> model;

    /**
     * Creates new form AdminConsoleGUI
     *
     * @param client
     * @param gui
     */
    protected AdminConsoleGUI(ConvoSyncClient client, ConvoSyncGUI gui) {
        super("CS - Admin Console");
        this.client = client;
        initComponents();
        model = new javax.swing.DefaultListModel<String>();
        serverList.setModel(model);
        if (console.getCaret() instanceof javax.swing.text.DefaultCaret) {
            ((javax.swing.text.DefaultCaret) console.getCaret())
                    .setUpdatePolicy(javax.swing.text.DefaultCaret.ALWAYS_UPDATE);
        }
        setLocationRelativeTo(gui);
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
        console = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        serverList = new javax.swing.JList();
        commandLine = new javax.swing.JTextField();
        label = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jSplitPane1.setDividerLocation(275);
        jSplitPane1.setResizeWeight(1.0);

        console.setEditable(false);
        console.setColumns(20);
        console.setRows(5);
        jScrollPane1.setViewportView(console);

        jSplitPane1.setLeftComponent(jScrollPane1);

        serverList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                onServerSelection(evt);
            }
        });
        jScrollPane2.setViewportView(serverList);

        jSplitPane1.setRightComponent(jScrollPane2);

        commandLine.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCommand(evt);
            }
        });

        label.setText("Select a server from the list on the right.");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(label)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(commandLine))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(commandLine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onCommand(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onCommand
        if (!commandLine.getText().equals("") && serverList.getSelectedValue() != null) {
            client.out(new CommandMessage(new MessageRecipient(client.name), String.valueOf(serverList.getSelectedValue()), commandLine.getText()));
            commandLine.setText("");
        }
    }//GEN-LAST:event_onCommand

    private void onServerSelection(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_onServerSelection
        if (serverList.getSelectedValue() == null) {
            return;
        }
        label.setText("Press enter to issue a command on " + serverList.getSelectedValue() + ".");
        console.grabFocus();
    }//GEN-LAST:event_onServerSelection

    protected void addToServerList(String elem) {
        model.addElement(elem);
    }

    protected void clearServerList() {
        model.clear();
    }

    protected void log(String s) {
        console.setText(console.getText() + "\n" + Main.format(s));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField commandLine;
    private javax.swing.JTextArea console;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel label;
    private javax.swing.JList serverList;
    // End of variables declaration//GEN-END:variables
}

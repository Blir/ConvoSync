package com.github.blir.convosync.application;

import blir.swing.listener.InputListener;
import blir.swing.quickgui.InputBox;
import com.github.blir.convosync.Main;
import java.awt.Cursor;
import javax.swing.JFrame;

/**
 *
 * @author Blir
 */
public class LoginGUI extends javax.swing.JFrame {

    private final ConvoSyncClient client;

    /**
     * Creates new form LoginGUI
     * @param client
     * @param ip
     * @param port
     * @param user
     * @param password
     * @param remember
     */
    protected LoginGUI(ConvoSyncClient client, String ip, int port, String user,
                       String password, boolean remember) {
        super("CS" + Main.VERSION);
        this.client = client;
        initComponents();
        if (ip != null && port != 0) {
            jTextField1.setText(ip + ":" + port);
        }
        if (user != null) {
            jTextField2.setText(user);
        }
        if (password != null) {
            jPasswordField1.setText(password);
        }
        if (client.defaultCloseOperation != JFrame.EXIT_ON_CLOSE) {
            setDefaultCloseOperation(client.defaultCloseOperation);
        }
        jTextField1.grabFocus();
        jTextField1.setSelectionStart(0);
        jTextField1.setSelectionEnd(jTextField1.getText().length());
        jCheckBox1.setSelected(remember);
        setLocationRelativeTo(null);
    }

    protected void setLabel(String s) {
        jLabel4.setText(s);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jPasswordField1 = new javax.swing.JPasswordField();
        jButton1 = new javax.swing.JButton();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel4 = new javax.swing.JLabel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel1.setText("IP & Port:");

        jLabel2.setText("MC User Name:");

        jLabel3.setText("CS Password:");

        jTextField1.setText("127.0.0.1:25000");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onIPAndPortEntered(evt);
            }
        });

        jTextField2.setText("xTrollx1337");
        jTextField2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onUserNameEntered(evt);
            }
        });

        jPasswordField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onLogin(evt);
            }
        });

        jButton1.setText("Login");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onLogin(evt);
            }
        });

        jLabel4.setText("Enter user credentials.");

        jCheckBox1.setText("Remember Login Info");

        jMenu1.setText("Advanced");

        jMenuItem1.setText("Change Timeout");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onChangeConnectionTimeout(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextField1)
                            .addComponent(jPasswordField1)
                            .addComponent(jTextField2)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1))
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jCheckBox1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onIPAndPortEntered(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onIPAndPortEntered
        jTextField2.grabFocus();
        jTextField2.setSelectionStart(0);
        jTextField2.setSelectionEnd(jTextField2.getText().length());
    }//GEN-LAST:event_onIPAndPortEntered

    private void onUserNameEntered(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onUserNameEntered
        jPasswordField1.grabFocus();
        jPasswordField1.setSelectionStart(0);
        jPasswordField1.setSelectionEnd(jPasswordField1.getPassword().length);
    }//GEN-LAST:event_onUserNameEntered

    private void onLogin(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onLogin
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        jProgressBar1.setIndeterminate(true);
        jLabel4.setText("Connecting...");
        new Thread(new ConnectTask()).start();
    }//GEN-LAST:event_onLogin

    private void onChangeConnectionTimeout(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onChangeConnectionTimeout
        new InputBox("ConvoSyncClient - Change Connection Timeout",
                "Enter a connection timeout value in milliseconds: ",
                new InputListener() {
            @Override
            public void onInput(String input) {
                try {
                    int timeout = Integer.parseInt(input);
                    if (timeout < 5000) {
                        timeout = 5000;
                    }
                    if (timeout > 60000) {
                        timeout = 60000;
                    }
                    client.timeout = timeout;
                    jLabel4.setText("Now using timeout value of " + timeout + " ms");
                } catch (NumberFormatException ex) {
                    jLabel4.setText("Invalid timeout value: " + input);
                }
            }
        }, false).setVisible(true);
    }//GEN-LAST:event_onChangeConnectionTimeout
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    // End of variables declaration//GEN-END:variables

    private class ConnectTask implements Runnable {

        @Override
        public void run() {
            client.name = jTextField2.getText();
            String ipAndPort = jTextField1.getText();
            String ip;
            int port = 25000;
            if (ipAndPort.contains(":")) {
                String[] ipAndPortPieces = ipAndPort.split(":");
                if (ipAndPortPieces.length > 1) {
                    ip = ipAndPortPieces[0];
                    try {
                        port = Integer.parseInt(ipAndPortPieces[1]);
                    } catch (NumberFormatException ex) {
                        jLabel4.setText("Invalid port.");
                        return;
                    }
                } else {
                    jLabel4.setText("Invalid IP format.");
                    return;
                }
            } else {
                ip = ipAndPort;
            }
            String msg = client.connect(ip, port, String.valueOf(jPasswordField1.getPassword()), jCheckBox1.isSelected());
            if (msg != null) {
                jLabel4.setText(msg);
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            jProgressBar1.setIndeterminate(false);
        }
    }
}

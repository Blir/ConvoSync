package com.minepop.servegame.convosync;

import blir.swing.QuickGUI;
import blir.swing.listener.InputListener;
import blir.swing.quickgui.InputBox;
import blir.swing.quickgui.MsgBox;
import com.minepop.servegame.convosync.application.ConvoSyncClient;
import com.minepop.servegame.convosync.server.ConvoSyncServer;
import java.io.IOException;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Blir
 */
public class Main {

    public static final String VERSION = "1.0.3 Dev 3.3";

    public static enum Action {

        SERVER, APPLICATION
    }

    public static void main(final String[] args) throws IOException {
        try {
            QuickGUI.setLookAndFeel("Windows");
        } catch (ClassNotFoundException ex) {
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (UnsupportedLookAndFeelException ex) {
        }
        // ignore all these; just use the default look and feel
        for (String arg : args) {
            try {
                switch (Action.valueOf(arg.toUpperCase())) {
                    case SERVER:
                        new ConvoSyncServer().run(args);
                        return;
                    case APPLICATION:
                        new ConvoSyncClient().run(args);
                        return;
                }
            } catch (IllegalArgumentException ignore) {
            } // ignore - probably an argument used by the server or gui client
        }
        prompt(args);
    }

    private static void prompt(final String[] args) {
        new InputBox("ConvoSync - What do you wish to do?", "Enter \"server\""
                + "to run the server. Enter \"application\" to run the application.",
                new InputListener() {
            @Override
            public void onInput(final String input) {
                try {
                    switch (Action.valueOf(input.toUpperCase())) {
                        case SERVER:
                            try {
                                new ConvoSyncServer().run(args);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                System.exit(-291);
                            }
                            break;
                        case APPLICATION:
                            try {
                                new ConvoSyncClient().run(args);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                System.exit(-291);
                            }
                            break;
                    }
                } catch (IllegalArgumentException ex) {
                    new MsgBox("ConvoSync - Warning", "\"" + input + "\" was an invalid selection. Please type \"server\" or \"application\".", new Runnable() {
                        @Override
                        public void run() {
                            prompt(args);
                        }
                    }, true).setVisible(true);
                }
            }
        }, true).setVisible(true);
    }
    public static final char COLOR_CHAR = '\u00A7';

    public static String format(String s) {
        return s.replaceAll(COLOR_CHAR + "\\w", "");
    }
}

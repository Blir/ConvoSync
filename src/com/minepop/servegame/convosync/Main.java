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

    public static final String VERSION = "1.0.3 Dev 3.8";

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
        new SelectionGUI(args).setVisible(true);
    }
    public static final char COLOR_CHAR = '\u00A7';

    public static String format(String s) {
        return s.replaceAll(COLOR_CHAR + "\\w", "");
    }
}

package com.minepop.servegame.convosync;

import blir.swing.QuickGUI;

import com.minepop.servegame.convosync.application.ConvoSyncClient;
import com.minepop.servegame.convosync.server.ConvoSyncServer;

import java.util.logging.Level;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author Blir
 */
public class Main {

    /**
     * The version of this ConvoSync suite.
     */
    public static final String VERSION = "1.0.5-dev4.1";

    public static void main(final String[] args) {
        try {
            QuickGUI.setLookAndFeel("Windows");
        } catch (ClassNotFoundException ex) {
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (UnsupportedLookAndFeelException ex) {
        }
        // ignore all these; just use the default look and feel
        for (String arg : args) {
            if (arg.equals("server")) {
                int instances = ConvoSyncServer.instances();
                if (instances == 0) {
                    ConvoSyncServer.main(args);
                } else {
                    ConvoSyncServer.LOGGER.log(Level.INFO, "{0} instances of the ConvoSync server are now running.", instances + 1);
                    new ConvoSyncServer().run(args);
                }
                return;
            }
        }
        new ConvoSyncClient().run(args);
    }
    /**
     * The section (§) symbol Minecraft uses to format chat.
     */
    public static final char COLOR_CHAR = '\u00A7';

    /**
     * Removes any Minecraft formatting codes from the given String.
     *
     * @param s the String to strip
     * @return the stripped String
     */
    public static String format(String s) {
        return s.replaceAll(COLOR_CHAR + "\\w", "");
    }
}

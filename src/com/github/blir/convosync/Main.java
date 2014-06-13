package com.github.blir.convosync;

import com.github.blir.convosync.application.ConvoSyncClient;
import com.github.blir.convosync.server.ConvoSyncServer;

import java.util.Random;

/**
 *
 * @author Blir
 */
public class Main {

    /**
     * The version of this ConvoSync suite.
     */
    public static final String VERSION = "1.1.1";

    public static final Random RNG = new Random();

    public static void main(final String[] args) {
        for (String arg : args) {
            if (arg.equals("server")) {
                ConvoSyncServer.main(args);
                return;
            }
        }
        new ConvoSyncClient().run(args);
    }

    public static String randomString(java.util.logging.Logger l, int len) {
        StringBuilder string = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            string.append((char) RNG.nextInt(25) + 97);
        }
        String gen = string.toString();
        if (l != null) {
            l.log(java.util.logging.Level.FINER, "Generated random string {0}", gen);
        }
        return gen;
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

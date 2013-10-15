package com.minepop.servegame.convosync.server;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import scala.tools.jline.console.ConsoleReader;

/**
 *
 * @author Blir
 */
public class TerminalConsoleHandler extends ConsoleHandler {

    private ConsoleReader reader;

    public TerminalConsoleHandler(ConsoleReader reader) {
        this.reader = reader;
    }

    @Override
    public synchronized void flush() {
        try {
            if (ConvoSyncServer.jline) {
                reader.print(String.valueOf(ConsoleReader.RESET_LINE));
                reader.flush();
                super.flush();
                try {
                    reader.drawLine();
                } catch (Throwable ex) {
                    reader.getCursorBuffer().clear();
                }
                reader.flush();
            } else {
                super.flush();
            }
        } catch (IOException ex) {
            ConvoSyncServer.LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}

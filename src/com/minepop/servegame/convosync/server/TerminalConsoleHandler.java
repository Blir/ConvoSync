package com.minepop.servegame.convosync.server;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import scala.tools.jline.console.ConsoleReader;

/**
 * Used to format console I/O similar to how Bukkit's console works.
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
            ConvoSyncServer.LOGGER.log(Level.SEVERE, "Error flushing the console handler.", ex);
        }
    }
}

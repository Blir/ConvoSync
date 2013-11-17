package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class CommandMessage extends Message {

    public final String SENDER, TARGET, CMD;

    public CommandMessage(String sender, String target, String cmd) {
        this.SENDER = sender;
        this.TARGET = target;
        this.CMD = cmd;
    }

    @Override
    public String toString() {
        int delim = CMD.indexOf(" ");
        return "CommandMessage[" + TARGET + "," + SENDER + "," + (delim > 0 ? CMD.substring(0, delim) : CMD) + "]";
    }
}

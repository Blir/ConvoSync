package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class CommandMessage extends Message {

    public final MessageRecipient SENDER;
    public final String TARGET, CMD;

    public CommandMessage(MessageRecipient sender, String target, String cmd) {
        this.SENDER = sender;
        this.TARGET = target;
        this.CMD = cmd;
    }

    @Override
    public String toString() {
        int delim = CMD.indexOf(" ");
        return String.format("CommandMessage[%s,%s,%s]", TARGET, SENDER, (delim > 0 ? CMD.substring(0, delim) : CMD));
    }
}

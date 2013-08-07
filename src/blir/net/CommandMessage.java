package blir.net;

/**
 *
 * @author Blir
 */
public class CommandMessage extends Message {

    public final String sender, target, cmd;

    public CommandMessage(final String sender, final String target, final String cmd) {
        this.sender = sender;
        this.target = target;
        this.cmd = cmd;
    }

    @Override
    public String toString() {
        int delim = cmd.indexOf(" ");
        return "CommandMessage[" + target + "," + sender + "," + (delim > 0 ? cmd.substring(0, delim) : cmd) + "]";
    }
}

package blir.net;

/**
 *
 * @author Blir
 */
public class PrivateMessage extends PlayerMessage {
    
    public final String sender, server;
    
    public PrivateMessage(final String recip, final String sender, final String msg, final String server) {
        super(msg, recip);
        this.sender = sender;
        this.server = server;
    }
    
    @Override
    public String toString() {
        return "PrivateMessage[" + sender + "," + recip + "]";
    }
}

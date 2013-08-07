package blir.net;

/**
 *
 * @author Blir
 */
public class PlayerMessage extends Message {

    public final String msg;
    public final String recip;

    public PlayerMessage(final String msg, final String recip) {
        this.msg = msg;
        this.recip = recip;
    }
    
    @Override
    public String toString() {
        return "PlayerMessage[" + recip + "," + msg + "]";
    }
}

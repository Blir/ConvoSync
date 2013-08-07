package blir.net;

/**
 *
 * @author Blir
 */
public class ChatMessage extends Message {

    public final String msg;

    public ChatMessage(final String msg) {
        this.msg = msg;
    }
    
    @Override
    public String toString() {
        return "ChatMessage[" + msg + "]";
    }
}

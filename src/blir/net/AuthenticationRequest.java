package blir.net;

/**
 *
 * @author Blir
 */
public class AuthenticationRequest extends Message {

    public final String name, password, type;

    public AuthenticationRequest(final String name, final String password, final String type) {
        this.name = name;
        this.password = password;
        this.type = type;
    }
    
    @Override
    public String toString() {
        return "AuthenticationRequest[" + name + "," + type + "]";
    }
}

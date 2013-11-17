package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class ApplicationAuthenticationRequest extends AuthenticationRequest {
    
    public ApplicationAuthenticationRequest(String name, String password, String version) {
        super(name, password, version);
    }
    
    @Override
    public String toString() {
        return "ApplicationAuthenticationRequest[" + NAME + "," + VERSION +  "]";
    }
}

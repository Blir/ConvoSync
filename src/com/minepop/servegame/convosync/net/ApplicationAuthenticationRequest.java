package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class ApplicationAuthenticationRequest extends AuthenticationRequest {
    
    public ApplicationAuthenticationRequest(String name, String password) {
        super(name, password);
    }
    
    @Override
    public String toString() {
        return "ApplicationAuthenticationRequest[" + NAME + "]";
    }
}

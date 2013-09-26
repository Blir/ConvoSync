package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class UserRegistration extends Message {

    public final String USER, PASSWORD;

    public UserRegistration(String user, String password) {
        this.USER = user;
        this.PASSWORD = password;
    }
    
    @Override
    public String toString() {
        return "UserRegistration[" + USER + "]";
    }
}

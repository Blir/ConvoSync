package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class UserRegistration extends Message {

    public final String USER, SALT;
    public final int SALTED_HASH;

    public UserRegistration(String user, int saltedHash, String salt) {
        this.USER = user;
        this.SALT = salt;
        this.SALTED_HASH = saltedHash;
    }
    
    public UserRegistration(String user, String password, String salt) {
        this.USER = user;
        this.SALT = salt;
        this.SALTED_HASH = (password + SALT).hashCode();
    }
    
    @Override
    public String toString() {
        return "UserRegistration[" + USER + "]";
    }
}

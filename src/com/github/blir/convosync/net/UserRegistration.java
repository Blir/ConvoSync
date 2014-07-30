package com.github.blir.convosync.net;

import java.util.UUID;

/**
 *
 * @author Blir
 */
public class UserRegistration extends Message {

    public final UUID UUID;
    public final String USER, SALT;
    public final int SALTED_HASH;

    public UserRegistration(UUID uuid, String user, int saltedHash, String salt) {
        this.UUID = uuid;
        this.USER = user;
        this.SALT = salt;
        this.SALTED_HASH = saltedHash;
    }
    
    public UserRegistration(UUID uuid, String user, String password, String salt) {
        this.UUID = uuid;
        this.USER = user;
        this.SALT = salt;
        this.SALTED_HASH = (password + SALT).hashCode();
    }
    
    @Override
    public String toString() {
        return "UserRegistration[" + USER + "]";
    }
}

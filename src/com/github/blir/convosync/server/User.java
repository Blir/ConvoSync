package com.github.blir.convosync.server;

import com.github.blir.convosync.net.AuthenticationRequest;
import com.github.blir.convosync.net.UserRegistration;

/**
 * Instances of this class are created to represent GUI application client
 * users.
 * 
 * @author Blir
 */
public final class User {

    public final String NAME, SALT;
    public final int SALTED_HASH;
    
    /**
     * Similar to Bukkit's OP. A ConvoSync GUI client user must be OP to send
     * cross-server commands. An OP User can also see vanished players.
     */
    protected boolean op;

    protected User(UserRegistration reg) {
        this.NAME = reg.USER;
        this.SALT = reg.SALT;
        this.SALTED_HASH = reg.SALTED_HASH;
    }
    
    protected User(String name, int saltedHash, String salt) {
        this.NAME = name;
        this.SALTED_HASH = saltedHash;
        this.SALT = salt;
    }
    
    protected User(String name, String password, String salt) {
        this.NAME = name;
        this.SALT = salt;
        this.SALTED_HASH = (password + SALT).hashCode();
    }
    
    public boolean isOp() {
        return op;
    }
    
    public boolean validate(String password) {
        return (password + SALT).hashCode() == SALTED_HASH;
    }
    
    public boolean validate(AuthenticationRequest authReq) {
        return (authReq.PASSWORD + SALT).hashCode() == SALTED_HASH;
    }
}
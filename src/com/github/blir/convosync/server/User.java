package com.github.blir.convosync.server;

import com.github.blir.convosync.net.AuthenticationRequest;
import com.github.blir.convosync.net.UserRegistration;
import java.util.UUID;

/**
 * Instances of this class are created to represent GUI application client
 * users.
 * 
 * @author Blir
 */
public final class User {

    public final UUID uuid;
    public final String SALT;
    public final int SALTED_HASH;
    
    /**
     * Similar to Bukkit's OP. A ConvoSync GUI client user must be OP to send
     * cross-server commands. An OP User can also see vanished players.
     */
    protected boolean op;
    protected String name;

    protected User(UserRegistration reg) {
        this.uuid = reg.UUID;
        this.name = reg.USER;
        this.SALT = reg.SALT;
        this.SALTED_HASH = reg.SALTED_HASH;
    }
    
    protected User(UUID uuid, String name, int saltedHash, String salt) {
        this.uuid = uuid;
        this.name = name;
        this.SALTED_HASH = saltedHash;
        this.SALT = salt;
    }
    
    protected User(UUID uuid, String name, String password, String salt) {
        this(uuid, name, (password + salt).hashCode(), salt);
    }
    
    protected User(String uuid, String name, int saltedHash, String salt) {
        this(UUID.fromString(uuid), name, saltedHash, salt);
    }
    
    protected User(String uuid, String name, String password, String salt) {
        this(UUID.fromString(uuid), name, (password + salt).hashCode(), salt);
    }
    
    public boolean isOp() {
        return op;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean validate(String password) {
        return (password + SALT).hashCode() == SALTED_HASH;
    }
    
    public boolean validate(AuthenticationRequest authReq) {
        return (authReq.PASSWORD + SALT).hashCode() == SALTED_HASH;
    }
}
package com.github.blir.convosync.server;

import com.github.blir.convosync.net.UserRegistration;

/**
 * Instances of this class are created to represent GUI application client
 * users.
 * 
 * @author Blir
 */
public final class User implements java.io.Serializable {

    private static final long serialVersionUID = 7526472295622776147L;
    public final String NAME, PASSWORD;
    /**
     * Similar to Bukkit's OP. A ConvoSync GUI client user must be OP to send
     * cross-server commands. An OP User can also see vanished players.
     */
    protected boolean op;

    protected User(UserRegistration reg) {
        this.NAME = reg.USER;
        this.PASSWORD = reg.PASSWORD;
    }
    
    public boolean isOp() {
        return op;
    }
}
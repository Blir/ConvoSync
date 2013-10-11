package com.minepop.servegame.convosync.server;

import com.minepop.servegame.convosync.net.UserRegistration;
import java.io.Serializable;

/**
 *
 * @author Blir
 */
public final class User implements Serializable {

    private static final long serialVersionUID = 7526472295622776147L;
    public final String NAME, PASSWORD;
    protected boolean op;

    protected User(UserRegistration reg) {
        this.NAME = reg.USER;
        this.PASSWORD = reg.PASSWORD;
    }
}
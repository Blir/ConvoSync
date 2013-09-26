package com.minepop.servegame.convosync.net;

/**
 *
 * @author Blir
 */
public class UserPropertyChange extends Message {

    public final Property PROPERTY;
    public final String VALUE;

    public static enum Property {

        PASSWORD
    }

    public UserPropertyChange(Property property, String value) {
        this.PROPERTY = property;
        this.VALUE = value;
    }

    @Override
    public String toString() {
        return "UserPropertyChange[" + PROPERTY + "]";
    }
}

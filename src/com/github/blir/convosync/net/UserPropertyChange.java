package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class UserPropertyChange extends Message {

    public final Property PROPERTY;
    public final String VALUE;
    public final MessageRecipient RECIP;

    public static enum Property {

        PASSWORD, OP
    }

    public UserPropertyChange(Property property, String value,
                              MessageRecipient recip) {
        this.PROPERTY = property;
        this.VALUE = value;
        this.RECIP = recip;
    }

    public UserPropertyChange(Property property, String value) {
        this.PROPERTY = property;
        this.VALUE = value;
        this.RECIP = null;
    }

    public boolean booleanValue() {
        return Boolean.parseBoolean(VALUE);
    }

    @Override
    public String toString() {
        return "UserPropertyChange[" + PROPERTY + "]";
    }
}

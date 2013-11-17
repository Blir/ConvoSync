package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class SetEnabledProperty extends Message {

    public final boolean ENABLED;

    public SetEnabledProperty(boolean enabled) {
        this.ENABLED = enabled;
    }
    
    @Override
    public String toString() {
        return "SetEnabledProperty[" + ENABLED + "]";
    }
}

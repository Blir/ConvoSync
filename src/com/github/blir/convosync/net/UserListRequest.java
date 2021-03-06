package com.github.blir.convosync.net;

/**
 *
 * @author Blir
 */
public class UserListRequest extends Message {

    public final String SENDER;

    public UserListRequest(String sender) {
        this.SENDER = sender;
    }

    @Override
    public String toString() {
        return "UserListRequest[" + SENDER + "]";
    }
}

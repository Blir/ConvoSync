/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.minepop.servegame.convosync.net;

/**
 *
 * @author Travis
 */
public class PluginAuthenticationRequest extends AuthenticationRequest {
    
    public final String[] PLAYERS;
    
    public PluginAuthenticationRequest(String name, String password, String version, String[] players) {
        super(name, password, version);
        this.PLAYERS = players;
    }
    
    @Override
    public String toString() {
        return "PluginAuthenticationRequest[" + NAME + "," + VERSION + "]";
    }
}

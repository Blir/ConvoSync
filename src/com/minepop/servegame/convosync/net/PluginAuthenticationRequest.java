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
    
    public PluginAuthenticationRequest(String name, String password, String[] players) {
        super(name, password);
        this.PLAYERS = players;
    }
    
    @Override
    public String toString() {
        return "PluginAuthenticationRequest[" + NAME + "]";
    }
}

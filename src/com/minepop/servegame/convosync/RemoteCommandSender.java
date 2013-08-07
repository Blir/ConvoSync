package com.minepop.servegame.convosync;

import java.util.Set;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Blir
 */
public class RemoteCommandSender implements CommandSender {

    private final ConvoSync plugin;
    private final String name;

    public RemoteCommandSender(final String name, final ConvoSync plugin) {
        this.name = name;
        this.plugin = plugin;
    }

    @Override
    public void sendMessage(String msg) {
        plugin.pm(msg, name);
    }

    @Override
    public void sendMessage(String[] msgs) {
        for (String msg : msgs) {
            sendMessage(msg);
        }
    }

    @Override
    public Server getServer() {
        return plugin.getServer();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPermissionSet(String string) {
        return true;
    }

    @Override
    public boolean isPermissionSet(Permission prmsn) {
        return true;
    }

    @Override
    public boolean hasPermission(String string) {
        return true;
    }

    @Override
    public boolean hasPermission(Permission prmsn) {
        return true;
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln, int i) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeAttachment(PermissionAttachment pa) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void recalculatePermissions() {
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        throw new UnsupportedOperationException("This CommandSender has no permissions."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean bln) {
        throw new UnsupportedOperationException("This CommandSender must be OP."); //To change body of generated methods, choose Tools | Templates.
    }
}

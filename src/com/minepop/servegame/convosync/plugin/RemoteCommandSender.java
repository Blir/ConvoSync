package com.minepop.servegame.convosync.plugin;

import java.util.Set;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Blir
 */
public class RemoteCommandSender implements CommandSender {

    private final ConvoSync PLUGIN;
    private final String NAME;

    public RemoteCommandSender(String name, ConvoSync plugin) {
        this.NAME = name;
        this.PLUGIN = plugin;
    }

    @Override
    public void sendMessage(String msg) {
        PLUGIN.out(msg, NAME);
    }

    @Override
    public void sendMessage(String... msgs) {
        for (String msg : msgs) {
            sendMessage(msg);
        }
    }

    @Override
    public Server getServer() {
        return PLUGIN.getServer();
    }

    @Override
    public String getName() {
        return NAME;
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
    public PermissionAttachment addAttachment(Plugin plugin, String string,
                                              boolean bln) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, String string,
                                              boolean bln, int i) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed.");
    }

    @Override
    public PermissionAttachment addAttachment(Plugin plugin, int i) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed.");
    }

    @Override
    public void removeAttachment(PermissionAttachment pa) {
        throw new UnsupportedOperationException("This CommandSender's permissions cannot be changed.");
    }

    @Override
    public void recalculatePermissions() {
    }

    @Override
    public Set<PermissionAttachmentInfo> getEffectivePermissions() {
        throw new UnsupportedOperationException("This CommandSender has no permissions.");
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean bln) {
        throw new UnsupportedOperationException("This CommandSender must be OP.");
    }
}

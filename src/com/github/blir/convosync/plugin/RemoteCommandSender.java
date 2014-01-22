package com.github.blir.convosync.plugin;

import com.github.blir.convosync.net.CommandResponse;
import com.github.blir.convosync.net.MessageRecipient;

import java.util.Set;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

/**
 * Used to represent any sender of a cross-server command.
 * If an exception thrown by this class breaks your plugin, I am sorry.
 * 
 * @author Blir
 */
public class RemoteCommandSender implements CommandSender {

    private final ConvoSync PLUGIN;
    private final MessageRecipient SENDER;

    public RemoteCommandSender(MessageRecipient sender, ConvoSync plugin) {
        this.PLUGIN = plugin;
        this.SENDER = sender;
    }

    @Override
    public void sendMessage(String msg) {
        PLUGIN.out(new CommandResponse(msg, SENDER), false);
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
        return SENDER.NAME;
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

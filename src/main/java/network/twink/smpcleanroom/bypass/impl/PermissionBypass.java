package network.twink.smpcleanroom.bypass.impl;

import network.twink.smpcleanroom.bypass.AbstractBypass;
import network.twink.smpcleanroom.bypass.IPlayerBypass;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PermissionBypass extends AbstractBypass implements IPlayerBypass {

    private final String bypassPermission;

    public PermissionBypass(String bypassPermission) {
        super();
        this.bypassPermission = bypassPermission;
    }

    @Override
    public boolean isCriteriaMet(Plugin plugin, Player player) {
        if (player == null) return false;
        return (player.hasPermission(bypassPermission) || player.isOp());
    }
}

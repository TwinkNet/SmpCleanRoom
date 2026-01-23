package network.twink.smpcleanroom.bypass.impl;

import network.twink.smpcleanroom.bypass.AbstractBypass;
import network.twink.smpcleanroom.bypass.IPlayerBypass;
import network.twink.smpcleanroom.util.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class JoinDateBypass extends AbstractBypass implements IPlayerBypass {

    private final long criteriaTimestamp;

    public JoinDateBypass(long timestamp) {
        super();
        this.criteriaTimestamp = timestamp;
    }

    @Override
    public boolean isCriteriaMet(Plugin plugin, Player player) {
        return PlayerUtil.didPlayerJoinBefore(criteriaTimestamp, player);
    }
}

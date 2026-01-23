package network.twink.smpcleanroom.bypass.impl;

import network.twink.smpcleanroom.bypass.AbstractBypass;
import network.twink.smpcleanroom.bypass.IPlayerBypass;
import network.twink.smpcleanroom.util.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlaytimeBypass extends AbstractBypass implements IPlayerBypass {

    private final long criteriaTicks;

    public PlaytimeBypass(long criteriaTicks) {
        super();
        this.criteriaTicks = criteriaTicks;
    }

    @Override
    public boolean isCriteriaMet(Plugin plugin, Player player) {
        return PlayerUtil.hasPlayerPlayedFor(criteriaTicks, player);
    }
}

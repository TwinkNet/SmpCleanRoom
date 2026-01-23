package network.twink.smpcleanroom.bypass;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface IPlayerBypass extends IBypass {

    boolean isCriteriaMet(Plugin plugin, Player player);
}

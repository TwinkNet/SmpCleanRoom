package network.twink.smpcleanroom.bypass;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface IBypass {

    boolean isImmune();

    default boolean isCriteriaMet(Plugin plugin, Location location) {
        return false;
    }

    default boolean isCriteriaMet(Plugin plugin, Player player) {
        return false;
    }
}

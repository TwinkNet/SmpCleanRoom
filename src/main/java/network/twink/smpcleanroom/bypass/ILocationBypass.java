package network.twink.smpcleanroom.bypass;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public interface ILocationBypass extends IBypass {

    boolean isCriteriaMet(Plugin plugin, Location location);
}

package network.twink.smpcleanroom.bypass;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public interface IBypassManager {

    boolean isCriteriaMet(Player player);

    boolean isCriteriaMet(Location location);

    List<IBypass> getBypassRegistryCopy();

    void registerBypass(IBypass bypass);

    int getTotalBypassCount();

    Mode getMode();

    Plugin getPlugin();
}

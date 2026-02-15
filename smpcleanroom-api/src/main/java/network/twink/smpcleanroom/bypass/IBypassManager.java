package network.twink.smpcleanroom.bypass;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface IBypassManager {

    boolean isCriteriaMet(Player player);

    boolean isCriteriaMet(Location location);

    List<IBypass> getBypassRegistryCopy();

    void registerBypass(IBypass bypass);

    int getTotalBypassCount();

    Mode getMode();
}

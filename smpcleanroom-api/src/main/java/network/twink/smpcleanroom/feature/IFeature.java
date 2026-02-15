package network.twink.smpcleanroom.feature;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public interface IFeature extends Listener {

    void onPreStartup();

    void onStartup();

    void onShutdown();

    Plugin getPlugin();
}

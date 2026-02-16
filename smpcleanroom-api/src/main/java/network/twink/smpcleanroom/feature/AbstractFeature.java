package network.twink.smpcleanroom.feature;

import org.bukkit.plugin.Plugin;

public abstract class AbstractFeature implements IFeature {

    private final String configName;
    private final Plugin plugin;

    public AbstractFeature(Plugin plugin, String configName) {
        this.configName = configName;
        this.plugin = plugin;
    }

    public final String getConfigName() {
        return configName;
    }

    @Override
    public final Plugin getPlugin() {
        return plugin;
    }
}

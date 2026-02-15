package network.twink.smpcleanroom.feature;

import org.bukkit.plugin.Plugin;

public abstract class AbstractFeature implements IFeature {

    private final FeatureManager featureManager;
    private final String configName;
    private final Plugin plugin;

    public AbstractFeature(FeatureManager manager, Plugin plugin, String configName) {
        this.configName = configName;
        this.plugin = plugin;
        this.featureManager = manager;
    }

    public final String getConfigName() {
        return configName;
    }

    @Override
    public final Plugin getPlugin() {
        return plugin;
    }

    @Override
    public FeatureManager getFeatureManager() {
        return featureManager;
    }
}

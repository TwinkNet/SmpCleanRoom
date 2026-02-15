package network.twink.smpcleanroom;

import network.twink.smpcleanroom.bypass.BypassManager;
import network.twink.smpcleanroom.feature.FeatureManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CompliantCleanRoom extends JavaPlugin {

    private CleanRoomConfiguration configuration;
    private static FeatureManager featureManager;

    @Override
    public void onLoad() {
        this.getLogger().info("Initialising...");
        this.configuration = new CleanRoomConfiguration(this);
        BypassManager bypassManager = new BypassManager(this, this.configuration);
        featureManager = new FeatureManager(this, bypassManager, configuration);
        this.getLogger()
                .info("loaded " + FeatureManager.getBypassManager().getTotalBypassCount()
                        + " registered bypasses.");
        this.getLogger().info("loaded " + featureManager.getTotalFeatureCount() + " registered features.");
        featureManager.onPreStartup();
    }

    @Override
    public void onEnable() {
        featureManager.onStartup();
    }

    @Override
    public void onDisable() {
        featureManager.onShutdown();
    }

    public static FeatureManager getFeatureManager() {
        return featureManager;
    }

    public CleanRoomConfiguration getConfiguration() {
        return configuration;
    }
}

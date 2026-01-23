package network.twink.smpcleanroom;

import network.twink.smpcleanroom.bypass.BypassManager;
import network.twink.smpcleanroom.feature.FeatureManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmpCleanRoom extends JavaPlugin {

    private CleanRoomConfiguration configuration;
    private BypassManager bypassManager;
    private FeatureManager featureManager;
    private final String worldDir = "world";
    private final String worldNetherDir = "world_nether";
    private final String worldEndDir = "world_the_end";

    @Override
    public void onLoad() {
        this.getLogger().info("Initialising...");
        this.configuration = new CleanRoomConfiguration(this);
        this.featureManager = new FeatureManager(this, configuration);
        this.bypassManager = new BypassManager(this.configuration);
        this.getLogger().info("loaded " + this.bypassManager.getTotalBypassCount() + " registered bypass.");
        this.getLogger().info("loaded " + this.featureManager.getTotalFeatureCount() + " registered bypass.");
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
}

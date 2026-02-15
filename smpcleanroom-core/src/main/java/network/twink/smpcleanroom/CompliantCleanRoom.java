package network.twink.smpcleanroom;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;
import network.twink.smpcleanroom.bypass.BypassManager;
import network.twink.smpcleanroom.bypass.IBypass;
import network.twink.smpcleanroom.feature.FeatureManager;
import network.twink.smpcleanroom.feature.IFeature;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class CompliantCleanRoom extends JavaPlugin {

    private CleanRoomConfiguration configuration;
    private static FeatureManager featureManager;
    private static final LinkedBlockingDeque<Supplier<IFeature>> thirdPartyFeatures = new LinkedBlockingDeque<>();
    private static final LinkedBlockingDeque<Supplier<IBypass>> thirdPartyBypasses = new LinkedBlockingDeque<>();
    private static boolean maybeTooLateToRegister = false;

    @Override
    public void onLoad() {
        this.getLogger().info("Initialising...");
        this.configuration = new CleanRoomConfiguration(this);
        BypassManager bypassManager = new BypassManager(this, this.configuration);
        featureManager = new FeatureManager(this, bypassManager, configuration);
    }

    @Override
    public void onEnable() {
        while (!thirdPartyFeatures.isEmpty()) {
            IFeature feature = thirdPartyFeatures.pop().get();
            if (feature != null) getFeatureManager().registerFeature(feature);
        }
        while (!thirdPartyBypasses.isEmpty()) {
            IBypass bypass = thirdPartyBypasses.pop().get();
            if (bypass != null) getFeatureManager().getBypassManager().registerBypass(bypass);
        }
        maybeTooLateToRegister = true;
        this.getLogger()
                .info("loaded " + getFeatureManager().getBypassManager().getTotalBypassCount()
                        + " registered bypasses.");
        this.getLogger().info("loaded " + featureManager.getTotalFeatureCount() + " registered features.");
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

    public static void queueRegisterFeature(Plugin plugin, Supplier<IFeature> featureSupplier) {
        if (maybeTooLateToRegister) {
            plugin.getLogger()
                    .severe(
                            plugin.getName()
                                    + "!! It may be too late to register your feature. Make sure you're registering your features in onLoad(), not onEnable()");
        }
        thirdPartyFeatures.addLast(featureSupplier);
    }

    public static void queueRegisterBypass(Plugin plugin, Supplier<IBypass> bypassSupplier) {
        if (maybeTooLateToRegister) {
            plugin.getLogger()
                    .severe(
                            plugin.getName()
                                    + "!! It may be too late to register your bypass. Make sure you're registering your bypass in onLoad(), not onEnable()");
        }
        thirdPartyBypasses.addLast(bypassSupplier);
    }
}

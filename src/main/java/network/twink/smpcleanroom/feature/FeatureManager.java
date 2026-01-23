package network.twink.smpcleanroom.feature;

import java.util.ArrayList;
import java.util.List;
import network.twink.smpcleanroom.CleanRoomConfiguration;
import network.twink.smpcleanroom.bypass.BypassManager;
import network.twink.smpcleanroom.feature.impl.FilterSignFeature;
import network.twink.smpcleanroom.feature.impl.WithholdMapFeature;
import network.twink.smpcleanroom.util.yml.YMLParser;
import org.bukkit.plugin.Plugin;

public class FeatureManager {

    private static final String FEATURES_ = "features.";
    private static final String VALUES_ = "values.";
    private final List<IFeature> featureRegistry;
    private final BypassManager bypassManager;

    public FeatureManager(Plugin plugin, BypassManager bypassManager, CleanRoomConfiguration config) {
        if (!config.isLoaded()) throw new IllegalStateException("CleanRoomConfiguration must be loaded.");
        YMLParser parser = config.getParser();
        this.bypassManager = bypassManager;
        int radius = parser.getInt(VALUES_ + "spawn_radius", 5000);
        featureRegistry = new ArrayList<>();
        if (parser.getBoolean(FEATURES_ + "withhold_map_feature.enabled", true)) {
            List<Integer> defaultMapIdBanList = new ArrayList<>();
            final String key = FEATURES_ + "withhold_map_feature.withheld_maps";
            if (parser.exists(key)) {
                defaultMapIdBanList = parser.getIntegerList(key);
            }
            boolean withHoldAll = parser.getBoolean(FEATURES_ + "withhold_map_feature.withhold_all_maps", true);
            String worldName = parser.getString(VALUES_ + "overworld_dir_name", "world");
            featureRegistry.add(new WithholdMapFeature(this, plugin, worldName, defaultMapIdBanList, withHoldAll));
        }
        if (parser.getBoolean(FEATURES_ + "filter_sign_feature.enabled", true)) {
            List<String> defaultBannedWords = new ArrayList<>();
            defaultBannedWords.add("error");
            final String key = VALUES_ + "banned_words";
            if (parser.exists(key)) {
                defaultBannedWords = parser.getStringList(key);
            }
            featureRegistry.add(new FilterSignFeature(this, plugin, defaultBannedWords, radius));
        }
    }

    public void onPreStartup() {
        for (IFeature iFeature : this.featureRegistry) {
            iFeature.onPreStartup();
        }
    }

    public void onStartup() {
        for (IFeature iFeature : this.featureRegistry) {
            iFeature.onStartup();
        }
    }

    public void onShutdown() {
        for (IFeature iFeature : this.featureRegistry) {
            iFeature.onShutdown();
        }
    }

    public int getTotalFeatureCount() {
        return featureRegistry.size();
    }

    public BypassManager getBypassManager() {
        return bypassManager;
    }
}

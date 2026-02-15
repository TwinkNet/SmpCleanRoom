package network.twink.smpcleanroom.feature;

import java.util.ArrayList;
import java.util.List;
import network.twink.smpcleanroom.CleanRoomConfiguration;
import network.twink.smpcleanroom.bypass.BypassManager;
import network.twink.smpcleanroom.event.CleanroomRegistrationEvent;
import network.twink.smpcleanroom.feature.impl.FilterSignFeature;
import network.twink.smpcleanroom.feature.impl.WithholdMapFeature;
import network.twink.smpcleanroom.util.yml.YMLParser;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class FeatureManager implements IFeatureManager {

    private static final String FEATURES_ = "features.";
    public static final String VALUES_ = "values.";
    private final List<IFeature> featureRegistry;
    private static BypassManager BYPASS_MANAGER;

    public FeatureManager(Plugin plugin, BypassManager bypassManager, CleanRoomConfiguration config) {
        if (!config.isLoaded()) throw new IllegalStateException("CleanRoomConfiguration must be loaded.");
        YMLParser parser = config.getParser();
        BYPASS_MANAGER = bypassManager;
        featureRegistry = new ArrayList<>();
        if (parser.getBoolean(FEATURES_ + "withhold_map_feature.enabled", true)) {
            List<String> defaultMapIdBanList = new ArrayList<>();
            final String key = FEATURES_ + "withhold_map_feature.withheld_maps";
            if (parser.exists(key)) {
                defaultMapIdBanList = parser.getStringList(key);
            }
            boolean withHoldAll = parser.getBoolean(FEATURES_ + "withhold_map_feature.withhold_all_maps", true);
            boolean useAlternateMethod =
                    parser.getBoolean(FEATURES_ + "withhold_map_feature.use_alternate_method", false);
            boolean replaceWithId = false;
            int replaceId = 0;
            if (parser.isInt(FEATURES_ + "withhold_map_feature.obfuscation.replace_with_id_when_possible")) {
                replaceWithId = true;
                replaceId = parser.getInt(FEATURES_ + "withhold_map_feature.obfuscation.replace_with_id_when_possible");
            }
            boolean useNoise =
                    parser.getBoolean(FEATURES_ + "withhold_map_feature.obfuscation.obfuscate_with_noise", true);
            registerFeature(new WithholdMapFeature(
                    plugin, defaultMapIdBanList, withHoldAll, useAlternateMethod, replaceWithId, useNoise, replaceId));
        }
        if (parser.getBoolean(FEATURES_ + "filter_sign_feature.enabled", true)) {
            List<String> defaultBannedWords = new ArrayList<>();
            defaultBannedWords.add("error");
            final String key = VALUES_ + "banned_words";
            if (parser.exists(key)) {
                defaultBannedWords = parser.getStringList(key);
            }
            registerFeature(new FilterSignFeature(plugin, defaultBannedWords));
        }
        Bukkit.getPluginManager().callEvent(new CleanroomRegistrationEvent(this, getBypassManager()));
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

    @Override
    public void registerFeature(IFeature feature) {
        featureRegistry.add(feature);
    }

    public static BypassManager getBypassManager() {
        return BYPASS_MANAGER;
    }
}

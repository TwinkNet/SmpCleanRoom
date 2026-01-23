package network.twink.smpcleanroom;

import java.io.File;
import network.twink.smpcleanroom.util.yml.YMLParser;
import org.bukkit.plugin.Plugin;

public class CleanRoomConfiguration {

    private static final int CONFIG_LATEST_VER = 0;
    private static final String VALUES_ = "values.";
    private static final String FEATURES_ = "features.";

    private File file;
    private YMLParser parser;
    private final Plugin plugin;
    private boolean loaded = false;
    // private Values values;

    public CleanRoomConfiguration(Plugin plugin) {
        this.plugin = plugin;
        plugin.saveResource("config.yml", false);
        this.file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            throw new IllegalStateException(
                    "this shouldn't have been possible, but config.yml isn't where SmpCleanRoom just saved it.");
        }
        this.parser = new YMLParser(file);
        int version = parser.getInt("version");
        boolean success = true;
        if (version > CONFIG_LATEST_VER) {
            plugin.getLogger()
                    .warning("the version of config.yml detected is from the future, unexpected behaviour may occur.");
        } else if (version < CONFIG_LATEST_VER) {
            plugin.getLogger()
                    .warning("detected config.yml version " + version + ". it will be converted to version"
                            + CONFIG_LATEST_VER);
            success = upgradeConfiguration(parser, version);
            if (success) {
                plugin.getLogger().info("upgraded config.yml " + version + " -> " + CONFIG_LATEST_VER);
            } else {
                plugin.getLogger()
                        .warning("a failure occurred while upgrading config.yml, unexpected behaviour may occur.");
            }
        } else {
            plugin.getLogger()
                    .info("detected config.yml version " + CONFIG_LATEST_VER + ". this is the latest version.");
        }
        plugin.getLogger().info("loaded configuration");
        loaded = success;
    }

    private boolean upgradeConfiguration(YMLParser parser, int version) {
        plugin.getLogger().warning("upgradeConfiguration() is not implemented, you shouldn't be seeing this message.");
        return true; // to be written when the config gets new values and a version bump is needed.
    }

    public YMLParser getParser() {
        return parser;
    }

    public boolean isLoaded() {
        return loaded;
    }

    //    public void populateConfiguration(YMLParser parser) {
    //        List<String> defaultBannedWords = new ArrayList<>();
    //        defaultBannedWords.add("error");
    //        if (parser.isList(VALUES_ + "banned_words")) {
    //            defaultBannedWords = parser.getStringList(VALUES_ + "banned_words");
    //        }
    //        List<Integer> defaultWithheldMaps = new ArrayList<>();
    //        if (parser.isList(FEATURES_ + "withhold_map_feature.withheld_maps")) {
    //            defaultWithheldMaps = parser.getIntegerList(FEATURES_ + "withhold_map_feature.withheld_maps");
    //        }
    //        final String worldDirName = parser.getString(VALUES_ + "world_dir_name");
    //        final boolean enableWithholdMapFeature = parser.getBoolean(FEATURES_ + "withhold_map_feature.enabled");
    //        final boolean enableFilterSignFeature = parser.getBoolean(FEATURES_ + "filter_sign_feature.enabled");
    //        final boolean withholdAllMaps = parser.getBoolean(FEATURES_ + "withhold_map_feature.withhold_all_maps");
    //        this.values = new Values(defaultBannedWords, defaultWithheldMaps, worldDirName, enableWithholdMapFeature,
    // enableFilterSignFeature, withholdAllMaps);
    //    }

    //    public static class Values {
    //        public final String worldDirName;
    //        public final List<String> bannedWords;
    //        public final List<Integer> withheldMaps;
    //        public final boolean enableWithholdMapFeature;
    //        public final boolean withholdAllMaps;
    //        public final boolean enableFilterSignFeature;
    //
    //        protected Values(
    //                List<String> bannedWords,
    //                List<Integer> withheldMaps,
    //                String worldDirName,
    //                boolean enableWithholdMapFeature,
    //                boolean enableFilterSignFeature,
    //                boolean withholdAllMaps) {
    //            this.bannedWords = bannedWords;
    //            this.withheldMaps = withheldMaps;
    //            this.worldDirName = worldDirName;
    //            this.enableWithholdMapFeature = enableWithholdMapFeature;
    //            this.enableFilterSignFeature = enableFilterSignFeature;
    //            this.withholdAllMaps = withholdAllMaps;
    //        }
    //    }
}

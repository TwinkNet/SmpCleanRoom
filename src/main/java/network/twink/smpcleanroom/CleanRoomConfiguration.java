package network.twink.smpcleanroom;

import java.io.File;
import network.twink.smpcleanroom.util.yml.YMLParser;
import org.bukkit.plugin.Plugin;

public class CleanRoomConfiguration {

    private static final int CONFIG_LATEST_VER = 0;

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
}

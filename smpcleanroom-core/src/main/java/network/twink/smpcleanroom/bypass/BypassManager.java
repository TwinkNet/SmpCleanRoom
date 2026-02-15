package network.twink.smpcleanroom.bypass;

import network.twink.smpcleanroom.CleanRoomConfiguration;
import network.twink.smpcleanroom.bypass.impl.JoinDateBypass;
import network.twink.smpcleanroom.bypass.impl.PermissionBypass;
import network.twink.smpcleanroom.bypass.impl.PlaytimeBypass;
import network.twink.smpcleanroom.bypass.impl.SpawnRadiusBypass;
import network.twink.smpcleanroom.feature.FeatureManager;
import network.twink.smpcleanroom.util.yml.YMLParser;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class BypassManager implements IBypassManager {

    private static final String BYPASSES_ = "bypasses.";
    private final List<IBypass> bypassRegistry;
    private final Plugin plugin;
    private final Mode mode;

    public BypassManager(Plugin plugin, CleanRoomConfiguration config) {
        if (!config.isLoaded()) throw new IllegalStateException("CleanRoomConfiguration must be loaded.");
        YMLParser parser = config.getParser();
        this.plugin = plugin;
        bypassRegistry = new ArrayList<>();
        if (parser.getBoolean(BYPASSES_ + "first_join.enabled", false)) {
            long criteriaMs = parser.getLong(BYPASSES_ + "first_join.joindate_timestamp_ms");
            registerBypass(new JoinDateBypass(criteriaMs));
        }
        if (parser.getBoolean(BYPASSES_ + "total_playtime.enabled", false)) {
            long criteriaTicks = parser.getLong(BYPASSES_ + "total_playtime.playtime_ticks");
            registerBypass(new PlaytimeBypass(criteriaTicks));
        }
        if (parser.getBoolean(BYPASSES_ + "permission.enabled", false)) {
            String permission = parser.getString(BYPASSES_ + "permission.perm");
            registerBypass(new PermissionBypass(permission));
        }
        /* location */ {
            int radius = parser.getInt(FeatureManager.VALUES_ + "spawn_radius", 5000);
            registerBypass(new SpawnRadiusBypass(radius));
        }
        mode = Mode.valueOf(parser.getString(BYPASSES_ + "mode").toUpperCase());
    }

    public List<IBypass> getBypassRegistryCopy() {
        return new ArrayList<>(bypassRegistry);
    }

    @Override
    public void registerBypass(IBypass bypass) {
        bypassRegistry.add(bypass);
    }

    public boolean isCriteriaMet(Player player) {
        if (mode == Mode.ALL) {
            PermanentBool bool = new PermanentBool();
            for (IBypass iBypass : bypassRegistry) {
                boolean flag = iBypass.isCriteriaMet(getPlugin(), player);
                bool.setFlag(flag, iBypass.isImmune());
            }
            return bool.getFlag();
        } else {
            boolean flag = false;
            for (IBypass iBypass : bypassRegistry) {
                flag = iBypass.isCriteriaMet(getPlugin(), player);
            }
            return flag;
        }
    }

    public boolean isCriteriaMet(Location loc) {
        if (mode == Mode.ALL) {
            PermanentBool bool = new PermanentBool();
            for (IBypass iBypass : bypassRegistry) {
                boolean flag = iBypass.isCriteriaMet(getPlugin(), loc);
                bool.setFlag(flag, iBypass.isImmune());
            }
            return bool.getFlag();
        } else {
            boolean flag = false;
            for (IBypass iBypass : bypassRegistry) {
                flag = iBypass.isCriteriaMet(getPlugin(), loc);
            }
            return flag;
        }
    }

    public int getTotalBypassCount() {
        return bypassRegistry.size();
    }

    public Mode getMode() {
        return mode;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    private static class PermanentBool {
        private boolean flag;
        private boolean locked;

        public PermanentBool() {
            this.flag = false;
            this.locked = false;
        }

        public void setFlag(boolean flag, boolean bypass) {
            if (locked && !bypass) return;
            this.flag = flag;
            if (!flag && !bypass) {
                locked = true;
            }
        }

        public void setFlag(boolean flag) {
            setFlag(flag, false);
        }

        public boolean getFlag() {
            return flag;
        }
    }
}

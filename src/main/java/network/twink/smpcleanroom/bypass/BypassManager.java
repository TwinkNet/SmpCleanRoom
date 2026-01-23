package network.twink.smpcleanroom.bypass;

import java.util.ArrayList;
import java.util.List;
import network.twink.smpcleanroom.CleanRoomConfiguration;
import network.twink.smpcleanroom.bypass.impl.JoinDateBypass;
import network.twink.smpcleanroom.bypass.impl.PermissionBypass;
import network.twink.smpcleanroom.bypass.impl.PlaytimeBypass;
import network.twink.smpcleanroom.util.yml.YMLParser;

public class BypassManager {

    private static final String BYPASSES_ = "bypasses.";
    private final List<IPlayerBypass> bypassRegistry;
    private final Mode mode;

    public BypassManager(CleanRoomConfiguration config) {
        if (!config.isLoaded()) throw new IllegalStateException("CleanRoomConfiguration must be loaded.");
        YMLParser parser = config.getParser();
        bypassRegistry = new ArrayList<>();
        if (parser.getBoolean(BYPASSES_ + "first_join.enabled", false)) {
            long criteriaMs = parser.getLong(BYPASSES_ + "first_join.joindate_timestamp_ms");
            bypassRegistry.add(new JoinDateBypass(criteriaMs));
        }
        if (parser.getBoolean(BYPASSES_ + "total_playtime.enabled", false)) {
            long criteriaTicks = parser.getLong(BYPASSES_ + "total_playtime.playtime_ticks");
            bypassRegistry.add(new PlaytimeBypass(criteriaTicks));
        }
        if (parser.getBoolean(BYPASSES_ + "permission.enabled", false)) {
            String permission = parser.getString(BYPASSES_ + "permission.perm");
            bypassRegistry.add(new PermissionBypass(permission));
        }
        mode = Mode.valueOf(parser.getString(BYPASSES_ + "mode").toUpperCase());
    }

    public List<IPlayerBypass> getBypassRegistryCopy() {
        return new ArrayList<>(bypassRegistry);
    }

    public int getTotalBypassCount() {
        return bypassRegistry.size();
    }

    public Mode getMode() {
        return mode;
    }
}

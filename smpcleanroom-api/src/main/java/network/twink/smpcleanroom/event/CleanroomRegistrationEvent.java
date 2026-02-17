package network.twink.smpcleanroom.event;

import network.twink.smpcleanroom.bypass.IBypass;
import network.twink.smpcleanroom.bypass.IBypassManager;
import network.twink.smpcleanroom.feature.IFeature;
import network.twink.smpcleanroom.feature.IFeatureManager;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is fired AFTER CompliantCleanRoom has finished
 * setting itself up. You are now free to register your own
 * features and bypasses. I would also recommend storing
 * the FeatureManager and BypassManager that is passed
 * by this event into a field, because this is your only opportunity
 * to obtain these objects without using {@link network.twink.smpcleanroom.CompliantCleanRoomAdapter},
 * which relies on Reflection.
 *
 * Deprecated: It doesn't work how I intended it to.
 */
@Deprecated
public class CleanroomRegistrationEvent extends org.bukkit.event.Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final IBypassManager bypassManager;
    private final IFeatureManager featureManager;

    public CleanroomRegistrationEvent(IFeatureManager featureManager, IBypassManager bypassManager) {
        this.featureManager = featureManager;
        this.bypassManager = bypassManager;
    }

    public IBypassManager getBypassManager() {
        return bypassManager;
    }

    public IFeatureManager getFeatureManager() {
        return featureManager;
    }

    public void register(IBypass... bypasses) {
        for (IBypass bypass : bypasses) {
            getBypassManager().registerBypass(bypass);
        }
    }

    public void register(IFeature... features) {
        for (IFeature feature : features) {
            getFeatureManager().registerFeature(feature);
        }
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}

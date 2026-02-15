package network.twink.smpcleanroom;

import network.twink.smpcleanroom.bypass.IBypassManager;
import network.twink.smpcleanroom.feature.IFeatureManager;

@SuppressWarnings("CallToPrintStackTrace")
public class CompliantCleanRoomAdapter {

    private static final String BASE_PKG = "network.twink.smpcleanroom";
    private static final String PLUGIN_CLASS = "CompliantCleanRoom";
    private static final String FEATURE_MANAGER_CLASS = "CompliantCleanRoom";

    private IBypassManager cachedBypassManager = null;
    private IFeatureManager cachedFeatureManager = null;

    /**
     * You shouldn't actually need to use this, you can obtain the
     * BypassManager from CleanroomPostRegistrationEvent
     *
     * @return The BypassManager in the core plugin.
     */
    @Deprecated
    public IBypassManager getBypassManager() {
        try {
            if (cachedBypassManager != null) return cachedBypassManager;
            Class<?> clazz = Class.forName(BASE_PKG + ".feature." + FEATURE_MANAGER_CLASS);
            Object object = clazz.getDeclaredMethod("getBypassManager").invoke(null);
            if (object instanceof IBypassManager) {
                cachedBypassManager = (IBypassManager) object;
                return cachedBypassManager;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * You shouldn't actually need to use this, you can obtain the
     * FeatureManager from CleanroomPostRegistrationEvent
     *
     * @return The FeatureManager in the core plugin.
     */
    @Deprecated
    public IFeatureManager getFeatureManager() {
        try {
            if (cachedFeatureManager != null) return cachedFeatureManager;
            Class<?> clazz = Class.forName(BASE_PKG + "." + PLUGIN_CLASS);
            Object object = clazz.getDeclaredMethod("getFeatureManager").invoke(null);
            if (object instanceof IFeatureManager) {
                cachedFeatureManager = (IFeatureManager) object;
                return cachedFeatureManager;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

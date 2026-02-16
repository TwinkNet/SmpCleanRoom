package network.twink.smpcleanroom;

import java.util.function.Supplier;
import network.twink.smpcleanroom.bypass.IBypass;
import network.twink.smpcleanroom.bypass.IBypassManager;
import network.twink.smpcleanroom.feature.IFeature;
import network.twink.smpcleanroom.feature.IFeatureManager;
import org.bukkit.plugin.Plugin;

@SuppressWarnings({"CallToPrintStackTrace"})
public class CompliantCleanRoomAdapter {

    private static final String BASE_PKG = "network.twink.smpcleanroom";
    private static final String PLUGIN_CLASS = "CompliantCleanRoom";
    private static final String FEATURE_MANAGER_CLASS = "FeatureManager";

    private IBypassManager cachedBypassManager = null;
    private IFeatureManager cachedFeatureManager = null;
    private Class<?> cachedPluginClass = null;

    /**
     * @return The BypassManager in the core plugin.
     */
    public IBypassManager getBypassManager() {
        try {
            if (cachedBypassManager != null) return cachedBypassManager;
            Class<?> clazz = Class.forName(BASE_PKG + ".feature." + FEATURE_MANAGER_CLASS);
            Object object = clazz.getDeclaredMethod("getBypassManager").invoke(getFeatureManager());
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
     * @return The FeatureManager in the core plugin.
     */
    public IFeatureManager getFeatureManager() {
        try {
            if (cachedFeatureManager != null) return cachedFeatureManager;
            Class<?> clazz = getPluginClass();
            if (clazz == null) {
                throw new ClassNotFoundException(BASE_PKG + "." + PLUGIN_CLASS);
            }
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

    private Class<?> getPluginClass() {
        if (cachedPluginClass != null) return cachedPluginClass;
        try {
            cachedPluginClass = Class.forName(BASE_PKG + "." + PLUGIN_CLASS);
            return cachedPluginClass;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerFeature(Plugin plugin, IFeature... features) {
        try {
            for (IFeature feature : features) {
                Supplier<IFeature> supplier = () -> feature;
                Class<?> clazz = getPluginClass();
                if (clazz == null) {
                    throw new ClassNotFoundException(BASE_PKG + "." + PLUGIN_CLASS);
                }
                clazz.getDeclaredMethod("queueRegisterFeature", Plugin.class, Supplier.class)
                        .invoke(null, (Plugin) plugin, (Supplier<IFeature>) supplier);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerBypass(Plugin plugin, IBypass... bypasses) {
        try {
            for (IBypass bypass : bypasses) {
                Supplier<IBypass> supplier = () -> bypass;
                Class<?> clazz = getPluginClass();
                if (clazz == null) {
                    throw new ClassNotFoundException(BASE_PKG + "." + PLUGIN_CLASS);
                }
                clazz.getMethod("queueRegisterBypass", Plugin.class, Supplier.class)
                        .invoke(null, (Plugin) plugin, (Supplier<IBypass>) supplier);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

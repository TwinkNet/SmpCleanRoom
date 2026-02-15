package network.twink.smpcleanroom.feature;

import network.twink.smpcleanroom.bypass.IBypassManager;

public interface IFeatureManager {

    void onStartup();

    void onShutdown();

    int getTotalFeatureCount();

    void registerFeature(IFeature feature);

    IBypassManager getBypassManager();
}

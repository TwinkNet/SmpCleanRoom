package network.twink.smpcleanroom.feature;

public interface IFeatureManager {

    void onPreStartup();

    void onStartup();

    void onShutdown();

    int getTotalFeatureCount();

    void registerFeature(IFeature feature);
}

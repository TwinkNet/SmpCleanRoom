package network.twink.smpcleanroom.feature.impl;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import network.twink.smpcleanroom.feature.AbstractFeature;
import network.twink.smpcleanroom.feature.FeatureManager;
import org.bukkit.plugin.Plugin;

public class WithholdMapFeature extends AbstractFeature {

    private boolean ready = false;
    private final Executor executor;
    private boolean withholdAll;
    private List<Integer> mapIds;
    private String worldName;

    public WithholdMapFeature(FeatureManager featureManager, Plugin plugin, List<Integer> mapIds, boolean withholdAll) {
        super(featureManager, plugin, "withhold_map_feature");
        this.mapIds = mapIds;
        this.withholdAll = withholdAll;
        if (withholdAll) {
            executor = null;
            return;
        }
        int logicalProcessors = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(logicalProcessors);
    }

    @Override
    public void onPreStartup() {}

    @Override
    public void onStartup() {
        this.getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    @Override
    public void onShutdown() {}
}

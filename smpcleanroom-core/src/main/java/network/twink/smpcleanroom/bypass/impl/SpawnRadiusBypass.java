package network.twink.smpcleanroom.bypass.impl;

import network.twink.smpcleanroom.bypass.AbstractBypass;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpawnRadiusBypass extends AbstractBypass {

    private final int globalSpawnRadius;

    public SpawnRadiusBypass(int radius) {
        super(true);
        this.globalSpawnRadius = radius;
    }

    @Override
    public boolean isCriteriaMet(Plugin plugin, Location location) {
        return location.getBlockX() > globalSpawnRadius || location.getBlockZ() > globalSpawnRadius;
    }
}

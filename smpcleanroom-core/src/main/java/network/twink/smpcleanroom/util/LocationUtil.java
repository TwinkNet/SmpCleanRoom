package network.twink.smpcleanroom.util;

import org.bukkit.Location;

public class LocationUtil {

    /**
     * Check if a specified location is within the specified spawn radius.
     *
     * @param location    The specified location to check
     * @param spawnRadius Radius from the centre of the world (blocks from 0, 0)
     * @return True if the location is inside the specified spawn radius
     */
    public static boolean isLocationInsideSpawnRadius(Location location, int spawnRadius) {
        return Math.abs(location.getBlockX()) <= Math.abs(spawnRadius)
                && Math.abs(location.getBlockZ()) <= Math.abs(spawnRadius);
    }
}

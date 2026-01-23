package network.twink.smpcleanroom.util;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class PlayerUtil {

    /**
     * Returns true if the player has joined the server before the specified timestamp in milliseconds since 1 Jan 1970
     *
     * @param criteria The timestamp that the playerfile must be older than (in ms since 00:00 1 Jan 1970)
     * @param player   The player to check
     * @return True if the player first joined before the specified timestamp.
     */
    public static boolean didPlayerJoinBefore(long criteria, Player player) {
        if (player == null) return false;
        return (player.getFirstPlayed() < criteria);
    }

    /**
     * Returns true if the player has played for longer than the specified criteria (in ticks)
     *
     * @param criteria The specified length of time required for this to return true (in ticks, so, 20 = 1 sec)
     * @param player   The player to test
     * @return True if the player has played for the specified amount of time in ticks or longer.
     */
    public static boolean hasPlayerPlayedFor(long criteria, Player player) {
        if (player == null) return false;
        int playTimeTicks = player.getStatistic(Statistic.TOTAL_WORLD_TIME);
        if (playTimeTicks <= 0) {
            playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        }
        return playTimeTicks >= criteria;
    }
}

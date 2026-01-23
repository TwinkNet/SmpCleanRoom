# SMP Clean Room (Anarchy Clean Room?)

## Download (for Folia 1.21.x)
It's not done yet. Give me a few days.

I do not intend to target non-Folia Paper at this time unless there is a demand. You should be using Folia over Paper if you run a server like this.

---

You operate a Minecraft server. It has no rules. Players are putting stuff up at spawn and it's catching Mojang's attention because it doesn't follow their commerical use guidelines or community standards.

You probably don't want to add in-game moderators and you're probably tired of deleting .dat files and banning people.

This plugin attempts to find a balance between being presentable to Mojang's enforcement team, tourists (players who log in once or twice for a few miutes, then never again), and not screwing over your old players who are upset about the changes to your server's moderation tactics.

## How?
Your server probably has a spawn point at 0, 0 with a radius that goes out a few thousand blocks. All of your old players who just want to mind their own business and do whatever are way far away from spawn, so let's not mess with them. Instead, we can scrub the area around spawn squeaky clean and free of the most problematic junk without destroying the world and it's data on your disk with some packet trickery.

### Sign Filtering
This plugin employs a feature that can filter configurable words out of signs within a configurable proximity to spawn both newly placed and existing without deleting the sign data. Actual sign data is **not modified**, only the packets that are sent to the client are modified. You can optionally set up Bypassing so that certain players are not affected by this filtering at spawn. Currently, you can add a permission to a player to bypass the filtering, allow players who joined before a certain date to bypass the filtering, and/or allow players who have a certain amount of playtime on the server to bypass the filtering.

### Mapdata Withholding
This plugin employs a feature that can withhold map data packets for players within a configurable proximity to spawn. Affected players will not see the contents of maps whether they're in an Item Frame or in their hands. You can choose to withhold only certain map IDs (and their duplicates, automatically obtained by the plugin), or you can withhold all maps. Actual map data is **not modified**, only the packets that are sent to the client are modified, or rather not sent at all in this case. Yout can optionally set up Bypassing so that certain players are not affected by the withholding of Mapdata at spawn. Currently, you can add a permission to bypass the withholding, allow players who joined before a certain date to bypass the withholding, and/or allow players who have a certain amount of playtime on the server to bypass the withholding.
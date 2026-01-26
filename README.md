# CompliantCleanRoom

## Download (for Folia 1.21.x)
[Release Candidate RC-2026.01.25 is out!](https://github.com/TwinkNet/SmpCleanRoom/releases/tag/RC-2026.01.25)
This release candidate is ready to go. It has been tested on a small server, but it hasn't been tested on a larger server. The good news is that this plugin doesn't change with any of your world's data. If an issue is noticed, make an issue on this page and remove the plugin from your server if it is causing critical playability issues or not being effective.

I do not intend to target non-Folia Paper at this time unless there is a demand. You should be using Folia over Paper if you run a server that needs a plugin like this.

[Default config.yml and documentation](https://github.com/TwinkNet/SmpCleanRoom/blob/master/src/main/resources/config.yml)

---
## What is this?
You may be interested in this plugin if you operate an "anarchy" Minecraft server has little or no rules. If players are putting stuff up at spawn that is catching Mojang's attention due to their commercial use guidelines or community standards, you will definitely want this plugin.

Adding in-game moderators, deleting .dat files, and banning people go against the concept of the kind of server I'm referencing. Instead of destroying data and destroying player experiences, we can attempt to find a balance between being presentable to Mojang's enforcement team, tourists (players who log in once or twice for a few minutes, then never again), and not screwing over your old players who are upset about the changes to your server's moderation tactics. 

This plugin's core principal is to be **non-destructive** to your world's data.

## How?
Your real playerbase, the people who just want to mind their own business and do whatever, are way far away from spawn, so let's not mess with them. Instead, we can scrub the area that encapsulates spawn and its surroundings squeaky clean and free of the most problematic junk without destroying the world and it's data on your disk with some packet trickery. This area will be your "Clean Room" that provides a player experience suitable for all-ages.

### Sign Filtering
This plugin employs a feature that can filter configurable words out of signs within a configurable proximity to spawn, both newly placed and existing, without deleting the sign's real data. Actual sign data is **not modified**, only the packets that are sent to the client are modified. 

### Mapdata Withholding
This plugin employs a feature that can withhold map data packets for players within a configurable proximity to spawn. Affected players will not see the contents of maps whether they're in an Item Frame or in their hands. You can choose to withhold only certain map IDs (and their duplicates, automatically obtained by the plugin), or you can withhold all maps. Actual map data is **not modified**, only the packets that are sent to the client are modified.

## Configurable Bypassing
You can optionally set up Bypassing so that certain players are not affected by these features in the spawn radius. You can:

- Allow players who have a certain amount of playtime on the server to bypass the features.
- Allow players who joined before a certain date to bypass the filtering.

You can configure the plugin to either require all of the above conditions to be met, or only one of them. You can also

- Add a permission (by default: "cleanroom.bypass") to a player to bypass the filtering.

### Map Hashing and Banning
To hash and ban maps, log onto your server under an account that is either opped or has the following permissions:

- cleanroom.hash
- cleanroom.banhash

Once you're logged in, hold the map that you'd like to ban and run `/hash`, when the notification appears the chatbox, click on the "click to ban" message to automatically run the `/banhash <hash>` command. The map will now be redacted to players who do not meet the requirements to see the map.
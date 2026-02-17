# CompliantCleanRoom

## Download (for Folia 1.21.x)
[Release Candidate R-2026.02.17 is out!](https://github.com/TwinkNet/SmpCleanRoom/releases/tag/R-2026.02.17)
This release candidate is ready to go. It includes a lot of behind the scenes changes that incorporates a new 
API that will be used to expand what features are available.

I do not intend to target non-Folia Paper at this time unless there is a demand. You should be using Folia over
Paper if you run a server that needs a plugin like this.

To install, download the `smpcleanroom-core-RC-XXXX.XX.XX-all.jar` jarfile and place in your `plugins/`
directory. **CompliantCleanRoom depends on ProtocolLib**, so please install it if you don't have it already.

[Default config.yml and documentation](https://github.com/TwinkNet/SmpCleanRoom/blob/master/src/main/resources/config.yml)

## Compiling
`./gradlew build -x spotlessJavaCheck`

---
## What is this?
You may be interested in this plugin if you operate an "anarchy" Minecraft server has little or no rules. 
If players are putting stuff up at spawn that is catching Mojang's attention due to their commercial use 
guidelines or community standards, you will definitely want this plugin.

Adding in-game moderators, deleting .dat files, and banning people go against the concept of the kind of 
server I'm referencing. Instead of destroying data and destroying player experiences, we can attempt to find 
a balance between being presentable to Mojang's enforcement team, tourists (players who log in once or twice 
for a few minutes, then never again), and not screwing over your old players who are upset about the changes 
to your server's moderation tactics. 

This plugin's core principal is to be **non-destructive** to your world's data.

## How?
Your real playerbase, the people who just want to mind their own business and do whatever, are way far away 
from spawn, so let's not mess with them. Instead, we can scrub the area that encapsulates spawn and its 
surroundings squeaky clean and free of the most problematic junk without destroying the world and it's data 
on your disk with some packet trickery. This area will be your "Clean Room" that provides a player experience 
suitable for all-ages.

### Sign Filtering
This plugin employs a feature that can filter configurable words out of signs within a configurable proximity 
to spawn, both newly placed and existing, without deleting the sign's real data. Actual sign data is 
**not modified**, only the packets that are sent to the client are modified. 

### Mapdata Withholding
This plugin employs a feature that can withhold map data packets for players within a configurable proximity 
to spawn. Affected players will not see the contents of maps whether they're in an Item Frame or in their 
hands. You can choose to withhold only certain maps (and their duplicates, automatically obtained by 
the plugin), or you can withhold all maps. Actual map data is **not modified**, only the packets that are 
sent to the client are modified.

## Configurable Bypassing
You can optionally set up Bypassing so that certain players are not affected by these features in the spawn 
radius. You can:

- Allow players who have a certain amount of playtime on the server to bypass the features.
- Allow players who joined before a certain date to bypass the filtering.

You can configure the plugin to either require all of the above conditions to be met, or only one of them. 
You can also

- Add a permission (by default: "cleanroom.bypass") to a player to bypass the filtering.

### Map Hashing and Banning
To hash and ban maps, log onto your server under an account that is either opped or has the following 
permissions:

- cleanroom.hash
- cleanroom.banhash

Once you're logged in, hold the map that you'd like to ban and run `/hash`, when the notification appears 
the chatbox, click on the "click to ban" message to automatically run the `/banhash <hash>` command. The map 
will now be redacted to players who do not meet the requirements to see the map.

# API

[See AIChatPlugin](https://github.com/TwinkNet/CompliantCleanRoom-AIChatPlugin) for a good real-world example of
a plugin that uses CompliantCleanRoom's API.

### Importing with Gradle
```groovy
repositories {
    // ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    // ...
    compileOnly 'com.github.TwinkNet.SmpCleanRoom:smpcleanroom-api:R-2026.02.17'
}
```

### In your plugin Main class...

```java
public class MyPlugin extends JavaPlugin {

    public static IFeatureManager featureManager;
    public static IBypassManager bypassManager;
    
    @Override
    public void onLoad() {
        // all bypasses and features must be registered during onLoad() !!!
        CompliantCleanRoomAdapter adapter = new CompliantCleanRoomAdapter();
        adapter.registerFeature(this, new MyCustomFeature(this));
        adapter.registerBypass(this, new MyCustomBypass(this));
        featureManager = adapter.getFeatureManager();
        bypassManager = adapter.getBypassManager();
    }
}
```

### Checking if a player or a location meets any of the criteria for Bypassing
```java
public class MyFeature implements IFeature /* IFeature extends bukkit's Listener by default */ {
    public MyFeature(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onStartup() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // you can set up ProtocolLib here if you would like to do packetry
        // see FilterSignFeature.java for more info on this.
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @EventHandler
    public void onPlayer(PlayerAttemptPickupItemEvent event) {
        if (MyPlugin.bypassManager.isCriteriaMet(event.getPlayer())) {
            // if a bypass does not override isCriteriaMet(Plugin plugin, Player player),
            // it will return isCriteriaMet(Plugin plugin, Location location) by default,
            // using player.getLocation() for that parameter.
            // See IBypass.java and SpawnRadiusBypass.java for more info on this.
            event.setCancelled(true);
        }
    }
}
```
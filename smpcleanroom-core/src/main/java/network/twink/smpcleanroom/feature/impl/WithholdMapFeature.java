package network.twink.smpcleanroom.feature.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import network.twink.smpcleanroom.CompliantCleanRoom;
import network.twink.smpcleanroom.feature.AbstractFeature;
import network.twink.smpcleanroom.feature.FeatureManager;
import network.twink.smpcleanroom.util.LocationUtil;
import network.twink.smpcleanroom.util.SerializableRedactionData;
import network.twink.smpcleanroom.util.yml.YMLParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WithholdMapFeature extends AbstractFeature {

    private ProtocolManager protocolManager;
    private final int radius;
    private final boolean withholdAll;
    private final boolean useAlternateMethod;
    private final boolean replaceWithIdWheneverPossible;
    private final boolean replaceWithNoise;
    private int replacementId = 0;
    private final List<String> bannedHashes;
    private SpoofMapRenderer spoofMapRenderer;
    private SerializableRedactionData savedRedactionData;

    public WithholdMapFeature(
            Plugin plugin,
            int radius,
            List<String> bannedHashes,
            boolean withholdAll,
            boolean useAlternateMethod,
            boolean replaceWithIdWheneverPossible,
            boolean replaceWithNoise,
            int replacementId) {
        super(plugin, "withhold_map_feature");
        this.bannedHashes = bannedHashes;
        this.withholdAll = withholdAll;
        this.radius = radius;
        this.replaceWithNoise = replaceWithNoise;
        this.useAlternateMethod = useAlternateMethod;
        this.replaceWithIdWheneverPossible = replaceWithIdWheneverPossible;
        if (replaceWithIdWheneverPossible) {
            this.replacementId = replacementId;
            getPlugin().getLogger().info("Will try to render map_" + replacementId + ".dat on banned maps.");
        }
    }

    @Override
    public void onPreStartup() {}

    @Override
    public void onStartup() {
        this.getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
        if (replaceWithIdWheneverPossible) {
            boolean flag = this.loadSavedRedactionData();
            if (flag) {
                getPlugin()
                        .getLogger()
                        .info("Restored Map ID " + savedRedactionData.getMapId()
                                + " from disk. It will be used as the redaction method.");
            } else {
                String method = replaceWithNoise ? "random noise" : "solid colour";
                getPlugin()
                        .getLogger()
                        .warning("Redaction method \"" + method + "\" will be used until a player discovers Map ID "
                                + replacementId);
            }
        }
        if (!useAlternateMethod) {
            protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager
                    .getAsynchronousManager()
                    .registerAsyncHandler(
                            new PacketAdapter(getPlugin(), ListenerPriority.NORMAL, PacketType.Play.Server.MAP) {
                                @Override
                                public void onPacketSending(PacketEvent pck) {
                                    int mapId = pck.getPacket()
                                            .getStructures()
                                            .read(0)
                                            .getIntegers()
                                            .read(0);
                                    MapView view = Bukkit.getMap(mapId);
                                    if (view == null) return;
                                    pck.getAsyncMarker().incrementProcessingDelay();
                                    pck.getPlayer()
                                            .getScheduler()
                                            .run(getPlugin(), getMapAnalyseSyncTask(pck, view), null);
                                }
                            })
                    .start();
        }
        ArrayList<String> aliases = new ArrayList<>();
        aliases.add("hash");
        getPlugin().getServer().getCommandMap().register("smpcleanroom", new HashCommand(this, aliases));
        ArrayList<String> aliaseses = new ArrayList<>();
        aliases.add("banhash");
        getPlugin().getServer().getCommandMap().register("smpcleanroom", new BanHashCommand(this, aliaseses));
    }

    private Consumer<ScheduledTask> getMapAnalyseSyncTask(@Nullable PacketEvent event, MapView mapView) {
        return task -> {
            boolean flag = event != null;
            ItemStack stack = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) stack.getItemMeta();
            meta.setMapView(mapView);
            if (!meta.hasMapView() || meta.getMapView() == null) {
                if (flag) protocolManager.getAsynchronousManager().signalPacketTransmission(event);
                return;
            }
            for (MapRenderer renderer : meta.getMapView().getRenderers()) {
                if (renderer instanceof RedactionMapRenderer) {
                    if (flag) protocolManager.getAsynchronousManager().signalPacketTransmission(event);
                    return;
                }
            }
            RedactionMapRenderer renderer = new RedactionMapRenderer(mapView.getId(), replaceWithNoise);
            meta.getMapView().addRenderer(renderer);
            stack.setItemMeta(meta);
            if (flag) protocolManager.getAsynchronousManager().signalPacketTransmission(event);
        };
    }

    @Override
    public void onShutdown() {}

    public boolean loadSavedRedactionData() {
        File file = new File(getPlugin().getDataFolder(), "CleanRoomSavedMapData.arr");
        if (!file.exists()) return false;
        try (FileInputStream stream = new FileInputStream(file)) {
            ObjectInputStream objStream = new ObjectInputStream(stream);
            SerializableRedactionData data = (SerializableRedactionData) objStream.readObject();
            if (data.getMapId() == this.replacementId) {
                this.savedRedactionData = data;
                return true;
            }
            return false;
        } catch (Exception e) {
            getPlugin().getLogger().severe(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return false;
    }

    public boolean saveRedactionData(int mapId, int[] arr) {
        File file = new File(getPlugin().getDataFolder(), "CleanRoomSavedMapData.arr");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
                getPlugin().getLogger().severe("IOException while creating CleanRoomSavedMapData.arr");
                return false;
            }
        }
        try (FileOutputStream stream = new FileOutputStream(file)) {
            ObjectOutputStream objStream = new ObjectOutputStream(stream);
            this.savedRedactionData = new SerializableRedactionData(mapId, arr);
            objStream.writeObject(this.savedRedactionData);
            return true;
        } catch (Exception e) {
            getPlugin().getLogger().severe(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (replaceWithIdWheneverPossible && spoofMapRenderer == null) {
            spoofMapRenderer = new SpoofMapRenderer(event.getPlayer(), replacementId);
        }
        if (useAlternateMethod) {
            for (ItemStack stack : event.getPlayer().getInventory()) {
                if (stack != null && stack.getType() == Material.FILLED_MAP) {
                    MapMeta meta = (MapMeta) stack.getItemMeta();
                    if (meta.hasMapView() && meta.getMapView() != null) {
                        int id = meta.getMapId();
                        MapView view = Bukkit.getMap(id);
                        if (view == null) return;
                        event.getPlayer().getScheduler().run(getPlugin(), getMapAnalyseSyncTask(null, view), null);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (useAlternateMethod) {
            ItemStack stack = event.getCurrentItem();
            if (stack != null && stack.getType() == Material.FILLED_MAP) {
                MapMeta meta = (MapMeta) stack.getItemMeta();
                if (meta.hasMapView() && meta.getMapView() != null) {
                    int id = meta.getMapId();
                    MapView view = Bukkit.getMap(id);
                    if (view == null) return;
                    event.getWhoClicked().getScheduler().run(getPlugin(), getMapAnalyseSyncTask(null, view), null);
                }
            }
            stack = event.getCursor();
            if (stack.getType() == Material.FILLED_MAP) {
                MapMeta meta = (MapMeta) stack.getItemMeta();
                if (meta.hasMapView() && meta.getMapView() != null) {
                    int id = meta.getMapId();
                    MapView view = Bukkit.getMap(id);
                    if (view == null) return;
                    event.getWhoClicked().getScheduler().run(getPlugin(), getMapAnalyseSyncTask(null, view), null);
                }
            }
        }
    }

    @EventHandler
    public void onEntityLoad(EntityAddToWorldEvent e) {
        if (!useAlternateMethod) return;
        if (e.getEntity() instanceof ItemFrame frame) {
            if (frame.getItem().getType() == Material.FILLED_MAP) {
                MapMeta meta = (MapMeta) frame.getItem().getItemMeta();
                if (meta.hasMapView() && meta.getMapView() != null) {
                    int id = meta.getMapId();
                    MapView view = Bukkit.getMap(id);
                    if (view == null) return;
                    e.getEntity().getScheduler().run(getPlugin(), getMapAnalyseSyncTask(null, view), null);
                }
            }
        }
    }

    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent e) {
        ItemStack stack = e.getItem().getItemStack();
        if (stack != null && stack.getType() == Material.FILLED_MAP) {
            MapMeta meta = (MapMeta) stack.getItemMeta();
            if (meta.hasMapView() && meta.getMapView() != null) {
                int id = meta.getMapId();
                MapView view = Bukkit.getMap(id);
                if (view == null) return;
                e.getPlayer().getScheduler().run(getPlugin(), getMapAnalyseSyncTask(null, view), null);
            }
        }
    }

    private String generateHash(int[] pixels) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        ByteBuffer buf = ByteBuffer.allocate(pixels.length * 4);
        for (int i : pixels) {
            buf.putInt(i);
        }
        byte[] arr = buf.array();
        return toHex(md.digest(arr));
    }

    public static String toHex(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    public class HashCommand extends org.bukkit.command.Command {

        private final WithholdMapFeature feature;

        protected HashCommand(WithholdMapFeature feature, ArrayList<String> als) {
            super("hash", "Hash the currently held map", "/hash", als);
            this.feature = feature;
        }

        @Override
        public boolean execute(
                @NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
            if (sender instanceof Player p) {
                if (p.hasPermission("cleanroom.hash") || p.isOp()) {
                    ItemStack stack = null;
                    if (p.getInventory().getItemInMainHand().getType() == Material.FILLED_MAP) {
                        stack = p.getInventory().getItemInMainHand();
                    } else if (p.getInventory().getItemInOffHand().getType() == Material.FILLED_MAP) {
                        stack = p.getInventory().getItemInOffHand();
                    }
                    if (stack != null) {
                        MapMeta meta = (MapMeta) stack.getItemMeta();
                        if (meta.hasMapView() && meta.getMapView() != null) {
                            RedactionMapRenderer renderer = null;
                            for (MapRenderer rander : meta.getMapView().getRenderers()) {
                                if (rander instanceof RedactionMapRenderer) {
                                    renderer = (RedactionMapRenderer) rander;
                                    break;
                                }
                            }
                            if (renderer == null) {
                                renderer = new RedactionMapRenderer(meta.getMapId(), replaceWithNoise);
                                meta.getMapView().addRenderer(renderer);
                            }
                            RedactionMapRenderer finalRenderer = renderer;
                            p.getScheduler()
                                    .runDelayed(
                                            feature.getPlugin(),
                                            delayed -> {
                                                String hash = finalRenderer.getHash(); // Get MD5 Hash
                                                HoverEvent<Component> copyNotif = Component.text("Click to copy")
                                                        .asHoverEvent();
                                                Component comp = Component.text("(click to copy) " + hash + " (map_"
                                                                + meta.getMapId() + ".dat)")
                                                        .hoverEvent(copyNotif)
                                                        .color(TextColor.color(255, 177, 105))
                                                        .clickEvent(ClickEvent.copyToClipboard(hash));
                                                p.sendMessage(comp);
                                                if (feature.bannedHashes.contains(hash)) {
                                                    HoverEvent<Component> unbanNotif = Component.text("Click to unban.")
                                                            .asHoverEvent();
                                                    Component unbanComp = Component.text(
                                                                    "This map is banned. Click here to unban it.")
                                                            .color(TextColor.color(201, 60, 0))
                                                            .hoverEvent(unbanNotif)
                                                            .clickEvent(ClickEvent.runCommand("/banhash " + hash));
                                                    p.sendMessage(unbanComp);
                                                } else {
                                                    HoverEvent<Component> banNotif = Component.text("Click to ban.")
                                                            .asHoverEvent();
                                                    Component banComp = Component.text(
                                                                    "This map is not banned. Click here to ban it.")
                                                            .color(TextColor.color(79, 196, 145))
                                                            .hoverEvent(banNotif)
                                                            .clickEvent(ClickEvent.runCommand("/banhash " + hash));
                                                    p.sendMessage(banComp);
                                                }
                                            },
                                            null,
                                            10L);
                            p.sendMessage("\2477Processing...");
                            return true;
                        }
                        sender.sendMessage("\2464The filled map contains no valid data.");
                        return false;
                    }
                    sender.sendMessage("\2474You are not holding a Filled Map.");
                    return false;
                } else {
                    sender.sendMessage("\2474You do not have sufficient permissions to run this command.");
                    return false;
                }
            }
            sender.sendMessage("\2474Only a player can use this command.");
            return false;
        }

        @Override
        public @NotNull List<String> tabComplete(
                @NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args)
                throws IllegalArgumentException {
            // empty
            return new ArrayList<>();
        }
    }

    public static class BanHashCommand extends org.bukkit.command.Command {

        private final WithholdMapFeature feature;

        protected BanHashCommand(WithholdMapFeature feature, ArrayList<String> als) {
            super("banhash", "Ban the specified hash", "/banhash <hash>", als);
            this.feature = feature;
        }

        @Override
        public boolean execute(
                @NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
            if (sender.hasPermission("cleanroom.banhash")
                    || sender.isOp()
                    || (sender instanceof ConsoleCommandSender)) {
                if (args.length != 1) {
                    sender.sendMessage("\2474Improper Usage");
                    sender.sendMessage("\2477Usage: /banhash <hash>");
                    sender.sendMessage("\2477Example: /banhash 20D0AAE318649F10AC5860230CA0DA5F");
                    sender.sendMessage("\2474Run /hash while holding a map to get its hash");
                    return false;
                }
                final String hash = args[0].trim();
                if (this.feature.getPlugin() instanceof CompliantCleanRoom cleanRoomPlugin) {
                    if (feature.bannedHashes.contains(hash)) {
                        synchronized (feature.bannedHashes) {
                            YMLParser parser =
                                    cleanRoomPlugin.getConfiguration().getParser();
                            feature.bannedHashes.remove(hash);
                            parser.set("features.withhold_map_feature.withheld_maps", feature.bannedHashes);
                            parser.save();
                        }
                        sender.sendMessage("\2477" + hash + " is now \247aallowed\2477.");
                        return true;
                    }
                    synchronized (feature.bannedHashes) {
                        YMLParser parser = cleanRoomPlugin.getConfiguration().getParser();
                        feature.bannedHashes.add(hash);
                        parser.set("features.withhold_map_feature.withheld_maps", feature.bannedHashes);
                        parser.save();
                    }
                    sender.sendMessage("\2477" + hash + " is now \2474banned\2477.");
                    return true;
                } else {
                    sender.sendMessage("\2474This command isn't registered to SmpCleanRoom. You are a skid.");
                    return false;
                }
            } else {
                sender.sendMessage("\2474You do not have sufficient permissions to run this command.");
            }
            return false;
        }

        @Override
        public @NotNull List<String> tabComplete(
                @NotNull CommandSender sender, @NotNull String alias, @NotNull String @NotNull [] args)
                throws IllegalArgumentException {
            // empty
            return new ArrayList<>();
        }
    }

    private class RedactionMapRenderer extends MapRenderer {

        private final int mapId;
        private String hash = null;
        private final int[] pixelArr = new int[16384];
        private final int[] noisePixelArr = new int[16384];
        private Random rand = null;

        private RedactionMapRenderer(int mapId, boolean useNoise) {
            this.mapId = mapId;
            if (useNoise) {
                rand = new Random();
            }
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            if (hash == null) {
                int cursor = 0;
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        Color pixel = canvas.getPixelColor(x, y);
                        if (pixel == null) {
                            pixel = canvas.getBasePixelColor(x, y);
                        }
                        pixelArr[cursor++] = pixel.getRGB();
                        if (rand != null) {
                            noisePixelArr[rand.nextInt(16384)] = pixel.getRGB();
                        }
                    }
                }
                try {
                    this.hash = WithholdMapFeature.this.generateHash(pixelArr);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
            if (WithholdMapFeature.this.withholdAll || WithholdMapFeature.this.bannedHashes.contains(this.hash)) {
                if (replaceWithIdWheneverPossible && replacementId == mapId) {
                    getPlugin()
                            .getLogger()
                            .severe("Banned Map ID " + mapId
                                    + " is the same as the Map ID you have set to replace banned maps with.");
                    getPlugin()
                            .getLogger()
                            .severe(
                                    "Unexpected behaviour WILL occur, and this message won't go away until you do something about it.");
                }
                banThisMap(map, canvas, player);
            } else {
                unbanThisMap(canvas);
            }
        }

        public int[] getPixelArr() {
            synchronized (pixelArr) {
                return pixelArr;
            }
        }

        public String getHash() {
            return hash;
        }

        public int getMapId() {
            return mapId;
        }

        public void banThisMap(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            if (!LocationUtil.isLocationInsideSpawnRadius(player.getLocation(), WithholdMapFeature.this.radius)) {
                unbanThisMap(canvas);
                return;
            }
            if (CompliantCleanRoom.getFeatureManager().getBypassManager().isCriteriaMet(player)) {
                unbanThisMap(canvas);
                return;
            }
            if (CompliantCleanRoom.getFeatureManager().getBypassManager().isCriteriaMet(player.getLocation())) {
                unbanThisMap(canvas);
                return;
            }
            if (WithholdMapFeature.this.replaceWithIdWheneverPossible
                    && WithholdMapFeature.this.spoofMapRenderer != null
                    && WithholdMapFeature.this.spoofMapRenderer.isCaptured()) {
                int cursor = 0;
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        int rgb = spoofMapRenderer.getPixelArr()[cursor++];
                        canvas.setPixelColor(x, y, new Color(rgb));
                    }
                }
            } else {
                if (rand != null) {
                    int cursor = 0;
                    for (int x = 0; x < 128; x++) {
                        for (int y = 0; y < 128; y++) {
                            canvas.setPixelColor(x, y, new Color(noisePixelArr[cursor++]));
                        }
                    }
                } else {
                    Color color = canvas.getBasePixelColor(6, 7);
                    for (int x = 0; x < 128; x++) {
                        for (int y = 0; y < 128; y++) {
                            canvas.setPixelColor(x, y, color);
                        }
                    }
                }
            }
        }

        public void unbanThisMap(@NotNull MapCanvas canvas) {
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    canvas.setPixelColor(x, y, null);
                }
            }
        }
    }

    private class SpoofMapRenderer extends MapRenderer {

        private boolean error = false;
        private int[] pixelArr = new int[16384];

        {
            Arrays.fill(pixelArr, Integer.MIN_VALUE);
        }

        public SpoofMapRenderer(int[] pixelArr) {
            this.pixelArr = pixelArr;
        }

        public SpoofMapRenderer(Player randomPlayer, int mapId) {
            ItemStack stack = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) stack.getItemMeta();
            MapView view = Bukkit.getMap(mapId);
            meta.setMapView(view);
            stack.setItemMeta(meta);
            if (meta.hasMapView() && meta.getMapView() != null) {
                randomPlayer.sendMap(meta.getMapView());
                meta.getMapView().addRenderer(this);
                return;
            }
            getPlugin()
                    .getLogger()
                    .warning(
                            "Couldn't get mapview. The configured map ID might not exist. Switching default strategy (solid colour)");
            error = true;
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            if (!isCaptured() && !isErrored()) {
                int cursor = 0;
                for (int x = 0; x < 128; x++) {
                    for (int y = 0; y < 128; y++) {
                        Color pixel = canvas.getPixelColor(x, y);
                        if (pixel == null) {
                            pixel = canvas.getBasePixelColor(x, y);
                        }
                        pixelArr[cursor++] = pixel.getRGB();
                    }
                }
                if (isCaptured()) {
                    if (WithholdMapFeature.this.saveRedactionData(map.getId(), getPixelArr())) {
                        WithholdMapFeature.this
                                .getPlugin()
                                .getLogger()
                                .info("Map ID " + map.getId()
                                        + " will be restored across reboots without need for re-rendering.");
                    }
                }
            }
        }

        public int[] getPixelArr() {
            synchronized (pixelArr) {
                return pixelArr;
            }
        }

        public boolean isCaptured() {
            return pixelArr[0] != Integer.MIN_VALUE;
        }

        public boolean isErrored() {
            return error;
        }
    }
}

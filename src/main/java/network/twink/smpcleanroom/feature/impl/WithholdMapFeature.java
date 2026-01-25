package network.twink.smpcleanroom.feature.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import network.twink.smpcleanroom.SmpCleanRoom;
import network.twink.smpcleanroom.feature.AbstractFeature;
import network.twink.smpcleanroom.feature.FeatureManager;
import network.twink.smpcleanroom.util.LocationUtil;
import network.twink.smpcleanroom.util.TwinkMapRenderer;
import network.twink.smpcleanroom.util.yml.YMLParser;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class WithholdMapFeature extends AbstractFeature {

    private ProtocolManager protocolManager;

    private final int radius;
    private final boolean withholdAll;
    private final List<String> bannedHashes;
    private final Map<Integer, Boolean> bannedMapIdCache = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> lockedPassedIdCache = new ConcurrentHashMap<>();

    public WithholdMapFeature(
            FeatureManager featureManager, Plugin plugin, int radius, List<String> bannedHashes, boolean withholdAll) {
        super(featureManager, plugin, "withhold_map_feature");
        this.bannedHashes = bannedHashes;
        this.withholdAll = withholdAll;
        this.radius = radius;
    }

    @Override
    public void onPreStartup() {}

    @Override
    public void onStartup() {
        this.getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager
                .getAsynchronousManager()
                .registerAsyncHandler(
                        new PacketAdapter(getPlugin(), ListenerPriority.NORMAL, PacketType.Play.Server.MAP) {
                            @Override
                            public void onPacketSending(PacketEvent pck) {
                                if (!LocationUtil.isLocationInsideSpawnRadius(
                                        pck.getPlayer().getLocation(), radius)) {
                                    return; // Don't redact the map for this player if they're not within from the
                                    // configured spawn radius
                                }
                                if (getFeatureManager().getBypassManager().isCriteriaMet(pck.getPlayer())) {
                                    return; // Don't redact the map for this player if they meet any of the criteria of
                                    // the configurable Bypass rules.
                                }
                                if (getFeatureManager()
                                        .getBypassManager()
                                        .isCriteriaMet(pck.getPlayer().getLocation())) {
                                    return; // Don't redact the map for this player if their location meets any of the
                                    // criteria of the configurable Bypass rules.
                                }
                                if (withholdAll) {
                                    pck.setCancelled(true); // Very strict. Just don't allow anything.
                                    return;
                                }
                                int mapId = pck.getPacket()
                                        .getStructures()
                                        .read(0)
                                        .getIntegers()
                                        .read(0);
                                if (lockedPassedIdCache.get(mapId) != null && lockedPassedIdCache.get(mapId)) {
                                    return; // Map has already been checked, it is not banned, and it can't change
                                    // because it is locked.
                                }
                                if (bannedMapIdCache.get(mapId) != null && bannedMapIdCache.get(mapId)) {
                                    pck.setCancelled(true);
                                    return; // The map has already been checked and it's banned.
                                }
                                pck.getAsyncMarker().incrementProcessingDelay();
                                // The packet will be delayed at minimum 21 ticks for further inspection.
                                pck.getPlayer()
                                        .getScheduler()
                                        .run(getPlugin(), getMapAnalyseSyncTask(pck, mapId), null);
                            }
                        })
                .start();
        ArrayList<String> aliases = new ArrayList<>();
        aliases.add("hash");
        getPlugin().getServer().getCommandMap().register("smpcleanroom", new HashCommand(this, aliases));
        ArrayList<String> aliaseses = new ArrayList<>();
        aliases.add("banhash");
        getPlugin().getServer().getCommandMap().register("smpcleanroom", new BanHashCommand(this, aliaseses));
    }

    private @NonNull Consumer<ScheduledTask> getMapAnalyseSyncTask(PacketEvent event, int mapId) {
        return task -> {
            ItemStack stack = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) stack.getItemMeta();
            //noinspection deprecation
            meta.setMapId(mapId);
            TwinkMapRenderer renderer = new TwinkMapRenderer();
            if (!meta.hasMapView() || meta.getMapView() == null) {
                // it's a nothingburger
                protocolManager.getAsynchronousManager().signalPacketTransmission(event);
                return;
            }
            meta.getMapView().addRenderer(renderer);
            stack.setItemMeta(meta);

            event.getPlayer()
                    .getScheduler()
                    .runDelayed(
                            getPlugin(),
                            delayed -> {
                                try {
                                    int[] arr = renderer.getPixelArr();
                                    String hash = generateHash(arr); // Gen MD5 Hash
                                    meta.getMapView().removeRenderer(renderer);
                                    if (bannedHashes.contains(hash)) {
                                        bannedMapIdCache.put(meta.getMapId(), true);
                                        event.setCancelled(true);
                                    } else if (meta.getMapView().isLocked()) {
                                        lockedPassedIdCache.put(meta.getMapId(), true);
                                    }
                                } catch (NoSuchAlgorithmException ignored) {
                                }
                                protocolManager.getAsynchronousManager().signalPacketTransmission(event);
                            },
                            null,
                            20L);
        };
    }

    @Override
    public void onShutdown() {}

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

    public static class HashCommand extends org.bukkit.command.Command {

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
                            TwinkMapRenderer renderer = new TwinkMapRenderer();
                            meta.getMapView().addRenderer(renderer);
                            p.getScheduler()
                                    .runDelayed(
                                            feature.getPlugin(),
                                            delayed -> {
                                                try {
                                                    int[] arr = renderer.getPixelArr();
                                                    String hash = feature.generateHash(arr); // Gen MD5 Hash
                                                    meta.getMapView().removeRenderer(renderer);
                                                    HoverEvent<Component> copyNotif = Component.text("Click to copy")
                                                            .asHoverEvent();
                                                    Component comp = Component.text("(click to copy) " + hash + " (map_"
                                                                    + meta.getMapId() + ".dat)")
                                                            .hoverEvent(copyNotif)
                                                            .color(TextColor.color(255, 177, 105))
                                                            .clickEvent(ClickEvent.copyToClipboard(hash));
                                                    p.sendMessage(comp);
                                                    if (feature.bannedHashes.contains(hash)) {
                                                        HoverEvent<Component> unbanNotif = Component.text(
                                                                        "Click to unban.")
                                                                .asHoverEvent();
                                                        Component unbanComp = Component.text(
                                                                        "This map is banned. Click to unban it.")
                                                                .color(TextColor.color(201, 60, 0))
                                                                .hoverEvent(unbanNotif)
                                                                .clickEvent(ClickEvent.runCommand("/banhash " + hash));
                                                        p.sendMessage(unbanComp);
                                                    } else {
                                                        HoverEvent<Component> banNotif = Component.text("Click to ban.")
                                                                .asHoverEvent();
                                                        Component banComp = Component.text(
                                                                        "This map is not banned. Click to ban it.")
                                                                .color(TextColor.color(79, 196, 145))
                                                                .hoverEvent(banNotif)
                                                                .clickEvent(ClickEvent.runCommand("/banhash " + hash));
                                                        p.sendMessage(banComp);
                                                    }
                                                } catch (NoSuchAlgorithmException e) {
                                                    sender.sendMessage(
                                                            "\2474MD5 is not a valid algorithm in this Runtime.");
                                                }
                                            },
                                            null,
                                            20L);
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
                if (this.feature.getPlugin() instanceof SmpCleanRoom cleanRoomPlugin) {
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
}

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import network.twink.smpcleanroom.feature.AbstractFeature;
import network.twink.smpcleanroom.feature.FeatureManager;
import network.twink.smpcleanroom.util.LocationUtil;
import network.twink.smpcleanroom.util.TwinkMapRenderer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

public class WithholdMapFeature extends AbstractFeature {

    private ProtocolManager protocolManager;

    private int radius;
    private final boolean withholdAll;
    private final List<String> bannedHashes;
    //              md5 hash, list of map ids
    private final Map<Integer, Boolean> bannedMapIdCache = new ConcurrentHashMap<>();

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
                                    return;
                                }
                                if (getFeatureManager().getBypassManager().isCriteriaMet(pck.getPlayer())) {
                                    return;
                                }
                                if (getFeatureManager()
                                        .getBypassManager()
                                        .isCriteriaMet(pck.getPlayer().getLocation())) {
                                    return;
                                }
                                if (withholdAll) {
                                    pck.setCancelled(true);
                                    return;
                                }
                                int mapId = pck.getPacket()
                                        .getStructures()
                                        .read(0)
                                        .getIntegers()
                                        .read(0);
                                if (bannedMapIdCache.get(mapId) != null && bannedMapIdCache.get(mapId)) {
                                    pck.setCancelled(true);
                                    return;
                                }
                                pck.getAsyncMarker().incrementProcessingDelay();
                                pck.getPlayer()
                                        .getScheduler()
                                        .run(getPlugin(), getMapAnalyseSyncTask(pck, mapId), null);
                            }
                        })
                .start();
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
}

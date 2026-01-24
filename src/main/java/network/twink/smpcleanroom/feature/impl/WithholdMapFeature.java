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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import network.twink.smpcleanroom.feature.AbstractFeature;
import network.twink.smpcleanroom.feature.FeatureManager;
import network.twink.smpcleanroom.util.TwinkMapRenderer;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

public class WithholdMapFeature extends AbstractFeature {

    private ProtocolManager protocolManager;

    private boolean ready = false;
    private final ExecutorService executor;
    private boolean withholdAll;
    private List<String> bannedHashes;
    private String worldName;
    //              md5 hash, list of map ids
    private final Map<String, List<Integer>> mapDataHashes = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> bannedMapIdCache = new ConcurrentHashMap<>();

    public WithholdMapFeature(
            FeatureManager featureManager,
            Plugin plugin,
            String worldName,
            List<String> bannedHashes,
            boolean withholdAll) {
        super(featureManager, plugin, "withhold_map_feature");
        this.bannedHashes = bannedHashes;
        this.worldName = worldName;
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
                                if (bannedMapIdCache.get(mapId) != null && bannedMapIdCache.get(mapId)) {
                                    pck.setCancelled(true); // don't render
                                    return;
                                }
                                pck.getAsyncMarker().incrementProcessingDelay();
                                pck.getPlayer()
                                        .getScheduler()
                                        .run(getPlugin(), getMapAnalyseTaskSync(pck, mapId), null);
                            }
                        })
                .start();
    }

    private void processFrame(ItemFrame frame) {
        if (frame.getItem().getType() == Material.FILLED_MAP) {
            MapMeta meta = (MapMeta) frame.getItem().getItemMeta();
            if (meta == null) return;
            if (!meta.hasMapId()) return;
            // Check if we've already dealt with this map before, we can remove it without rendering and hashing it.
            if (bannedMapIdCache.get(meta.getMapId()) != null && bannedMapIdCache.get(meta.getMapId())) {
                ItemStack stack = frame.getItem();
                frame.setItem(null, true);
                frame.getWorld().dropItemNaturally(frame.getLocation(), stack);
                // pop the item out of the frame
            }
            if (meta.hasMapView() && meta.getMapView() != null) {
                TwinkMapRenderer renderer = new TwinkMapRenderer();
                meta.getMapView().addRenderer(renderer);
                frame.getScheduler()
                        .runDelayed(
                                getPlugin(),
                                removalTask -> {
                                    try {
                                        int[] arr = renderer.getPixelArr();
                                        String hash = generateHashSync(arr); // Gen MD5 Hash
                                        mapDataHashes
                                                .computeIfAbsent(hash, k -> new ArrayList<>())
                                                .add(meta.getMapId());
                                        mapDataHashes.computeIfPresent(hash, (k, li) -> {
                                            li.add(meta.getMapId());
                                            return li;
                                        });
                                        meta.getMapView().removeRenderer(renderer);
                                        if (bannedHashes.contains(hash)) {
                                            bannedMapIdCache.put(meta.getMapId(), true);
                                            ItemStack stack = frame.getItem();
                                            frame.setItem(null, true);
                                            frame.getWorld().dropItemNaturally(frame.getLocation(), stack);
                                        }
                                    } catch (NoSuchAlgorithmException ignored) {
                                    }
                                },
                                null,
                                10L);
            }
        }
    }

    private @NonNull Consumer<ScheduledTask> getMapAnalyseTaskSync(PacketEvent event, int mapId) {
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
                                    String hash = generateHashSync(arr); // Gen MD5 Hash
                                    System.out.println(hash);
                                    mapDataHashes
                                            .computeIfAbsent(hash, k -> new ArrayList<>())
                                            .add(meta.getMapId());
                                    mapDataHashes.computeIfPresent(hash, (k, li) -> {
                                        li.add(meta.getMapId());
                                        return li;
                                    });
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

    private String generateHashSync(int[] pixels) throws NoSuchAlgorithmException {
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

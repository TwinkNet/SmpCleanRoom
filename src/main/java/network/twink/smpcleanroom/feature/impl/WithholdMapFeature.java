package network.twink.smpcleanroom.feature.impl;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import network.twink.smpcleanroom.feature.AbstractFeature;
import network.twink.smpcleanroom.feature.FeatureManager;
import network.twink.smpcleanroom.util.TwinkMapRenderer;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;

public class WithholdMapFeature extends AbstractFeature {

    private boolean ready = false;
    private final ExecutorService executor;
    private boolean withholdAll;
    private List<Integer> mapIds;
    private String worldName;
    private final Map<byte[], List<Integer>> mapDataHashes = new ConcurrentHashMap<>();

    public WithholdMapFeature(FeatureManager featureManager, Plugin plugin, String worldName, List<Integer> mapIds, boolean withholdAll) {
        super(featureManager, plugin, "withhold_map_feature");
        this.mapIds = mapIds;
        this.worldName = worldName;
        this.withholdAll = withholdAll;
        if (withholdAll) {
            executor = null;
            return;
        }
        int logicalProcessors = Runtime.getRuntime().availableProcessors();
        executor = Executors.newFixedThreadPool(logicalProcessors);
    }

    @SuppressWarnings({"DataFlowIssue", "CallToPrintStackTrace"})
    @Override
    public void onPreStartup() {
        if (this.withholdAll) return;
        File file = new File(worldName);
        if (!file.exists()) {
            getPlugin().getLogger().severe("Directory " + file.getAbsoluteFile() + " doesn't exist. WithholdMapFeature will be disabled.");
            return;
        }
        if (!file.isDirectory()) {
            getPlugin().getLogger().severe("Directory " + file.getAbsoluteFile() + " exists, but it's not a directory. WithholdMapFeature will be disabled.");
            return;
        }
        File dataFile = new File(worldName, "data");
        if (!dataFile.exists()) {
            getPlugin().getLogger().severe("Directory " + file.getAbsoluteFile() + " doesn't exist. WithholdMapFeature will be disabled.");
            return;
        }
        if (!dataFile.isDirectory()) {
            getPlugin().getLogger().severe("Directory " + file.getAbsoluteFile() + " exists, but it's not a directory. WithholdMapFeature will be disabled.");
            return;
        }
        getPlugin().getLogger().warning("Hashing all Map IDs. This may take a while.");
        for (File mapDat : dataFile.listFiles()) {
            executor.execute(() -> {
                String fn = mapDat.getName();
                if (!fn.matches("^map_\\d+\\.dat$")) {
                    getPlugin().getLogger().warning(fn + " is not a map data file. it will be skipped.");
                    return;
                }
                int mapId = Integer.parseInt(fn.substring(4, fn.indexOf('.')));
                try {
                    byte[] hash = generateHashSync(mapDat);
                    synchronized (WithholdMapFeature.this) {
                        mapDataHashes.computeIfAbsent(hash, k -> new ArrayList<>()).add(mapId);
                        mapDataHashes.computeIfPresent(hash, (k, li) -> {
                            li.add(mapId);
                            return li;
                        });
                    }
                } catch (NoSuchAlgorithmException | IOException e) {
                    e.printStackTrace();
                }
            });
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        ready = true;
        getPlugin().getLogger().info("Got " + mapDataHashes.size() + " unique hashes out of " + dataFile.listFiles().length + " files.");
        // TODO this doens't work because of things...
    }

    @Override
    public void onStartup() {
        this.getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
    }

    @Override
    public void onShutdown() {
    }

    private byte[] generateHashSync(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = new FileInputStream(file)) {
            try (DigestInputStream dis = new DigestInputStream(is, md)) {
                md.update(dis.readAllBytes());
            }
        }
        return md.digest();
    }

    private byte[] generateHashSync(int[] pixels) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        ByteBuffer buf = ByteBuffer.allocate(pixels.length * 4);
        for (int i : pixels) {
            buf.putInt(i);
        }
        byte[] arr = buf.array();
        return md.digest(arr);
    }

    @EventHandler
    public void add(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof ItemFrame frame) {
            if (frame.getItem().getType() == Material.FILLED_MAP) {
                MapMeta meta = (MapMeta) frame.getItem().getItemMeta();
                if (meta == null) return;
                if (!meta.hasMapId()) return;
                if (meta.hasMapView() && meta.getMapView() != null) {
                    TwinkMapRenderer renderer = new TwinkMapRenderer();
                    meta.getMapView().addRenderer(renderer);
                    event.getEntity().getScheduler().runDelayed(getPlugin(), task -> {
                        try {
                            int[] arr = renderer.getPixelArr();
                            byte[] hash = generateHashSync(arr);
                            mapDataHashes.computeIfAbsent(hash, k -> new ArrayList<>()).add(meta.getMapId());
                            mapDataHashes.computeIfPresent(hash, (k, li) -> {
                                li.add(meta.getMapId());
                                return li;
                            });
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }
                    }, null, 80L);
                }
            }
        }
    }
}

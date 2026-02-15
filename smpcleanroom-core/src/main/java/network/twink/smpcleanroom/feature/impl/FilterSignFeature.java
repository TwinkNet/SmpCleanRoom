package network.twink.smpcleanroom.feature.impl;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedRegistrable;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import network.twink.smpcleanroom.feature.AbstractFeature;
import network.twink.smpcleanroom.feature.FeatureManager;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class FilterSignFeature extends AbstractFeature {

    private ProtocolManager protocolManager;
    private final List<String> bannedWords;

    public FilterSignFeature(Plugin plugin, List<String> bannedWords) {
        super(plugin, "filter_sign_feature");
        this.bannedWords = bannedWords;
    }

    @Override
    public void onPreStartup() {
    }

    @Override
    public void onStartup() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        this.getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
        protocolManager.addPacketListener(
                new PacketAdapter(getPlugin(), ListenerPriority.NORMAL, PacketType.Play.Server.TILE_ENTITY_DATA) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (event.isPlayerTemporary()) return;
                        Optional<String> str = event.getPacket().getMeta("Mr.Clean");
                        if (str.isPresent()) {
                            if (str.get().equals("Scrubbed")) {
                                return;
                            }
                        }
                        if (FeatureManager.getBypassManager().isCriteriaMet(event.getPlayer())) return;
                        if (FeatureManager
                                .getBypassManager()
                                .isCriteriaMet(event.getPlayer().getLocation())) return;
                        PacketContainer packet = event.getPacket();
                        boolean flag = packet.getBlockEntityTypeModifier()
                                .read(0)
                                .equals(WrappedRegistrable.blockEntityType("sign"));
                        if (!flag) return;
                        NbtCompound nbt = (NbtCompound) packet.getNbtModifier().read(0);
                        NbtCompound compoundTagFront = null;
                        List<String> newJsonsFront = new ArrayList<>();
                        List<String> newJsonsBack = new ArrayList<>();
                        NbtCompound compoundTagBack = null;
                        if (nbt.containsKey("front_text")) {
                            compoundTagFront = nbt.getCompound("front_text");
                        }
                        if (nbt.containsKey("back_text")) {
                            compoundTagBack = nbt.getCompound("back_text");
                        }
                        if (compoundTagFront != null) {
                            NbtList<String> jsonMessages = compoundTagFront.getList("messages");
                            for (String json : jsonMessages) {
                                Component component =
                                        JSONComponentSerializer.json().deserialize(json);
                                for (String bannedWord : bannedWords) {
                                    component = component.replaceText(builder -> {
                                        builder.match(Pattern.compile("(?i)\\b" + Pattern.quote(bannedWord) + "\\b"));
                                        builder.replacement("?");
                                    });
                                }
                                newJsonsFront.add(JSONComponentSerializer.json().serialize(component));
                            }
                            NbtList<String> newComponentsFront = NbtFactory.ofList("messages", newJsonsFront);
                            compoundTagFront.put(newComponentsFront);
                            nbt.put(compoundTagFront.getName(), compoundTagFront);
                        }
                        if (compoundTagBack != null) {
                            NbtList<String> jsonMessages = compoundTagBack.getList("messages");
                            for (String json : jsonMessages) {
                                Component component =
                                        JSONComponentSerializer.json().deserialize(json);
                                for (String bannedWord : bannedWords) {
                                    component = component.replaceText(builder -> {
                                        builder.match(Pattern.compile("(?i)\\b" + Pattern.quote(bannedWord) + "\\b"));
                                        builder.replacement("?");
                                    });
                                }
                                newJsonsBack.add(JSONComponentSerializer.json().serialize(component));
                            }
                            NbtList<String> newComponentsBack = NbtFactory.ofList("messages", newJsonsBack);
                            compoundTagBack.put(newComponentsBack);
                            nbt.put(compoundTagBack.getName(), compoundTagBack);
                        }
                        packet.getNbtModifier().write(0, nbt);
                    }
                });
    }

    @Override
    public void onShutdown() {
    }

    @EventHandler
    public void onLoad(PlayerChunkLoadEvent event) {
        if (!FeatureManager.getBypassManager().isCriteriaMet(event.getChunk().getBlock(0, event.getWorld().getMinHeight(), 0).getLocation()))
            for (BlockState tileEntity : event.getChunk().getTileEntities()) {
                if (tileEntity instanceof Sign sign) {
                    List<String> frontSide = new ArrayList<>();
                    List<String> backSide = new ArrayList<>();
                    @NotNull SignSide signSide = sign.getSide(Side.FRONT);
                    for (int i = 0; i < signSide.lines().size(); i++) {
                        Component component = signSide.lines().get(i);
                        if (component instanceof TextComponent textComponent) {
                            for (String bannedWord : bannedWords) {
                                component = textComponent.replaceText(builder -> {
                                    builder.match("\\b" + bannedWord + "\\b");
                                    builder.replacement("?");
                                });
                            }
                            frontSide.add(JSONComponentSerializer.json().serialize(component));
                        }
                    }
                    signSide = sign.getSide(Side.BACK);
                    for (int i = 0; i < signSide.lines().size(); i++) {
                        Component component = signSide.lines().get(i);
                        for (String bannedWord : bannedWords) {
                            component = component.replaceText(builder -> {
                                builder.match(Pattern.compile("(?i)\\b" + Pattern.quote(bannedWord) + "\\b"));
                                builder.replacement("?");
                            });
                        }
                        backSide.add(JSONComponentSerializer.json().serialize(component));
                    }
                    PacketContainer container = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);
                    BlockPosition pos = new BlockPosition(sign.getX(), sign.getY(), sign.getZ());
                    container.getBlockPositionModifier().write(0, pos);
                    container.getBlockEntityTypeModifier().write(0, WrappedRegistrable.blockEntityType("sign"));
                    NbtCompound rootCompound =
                            (NbtCompound) container.getNbtModifier().read(0);
                    NbtCompound frontCompound = NbtFactory.ofCompound("front_text");
                    NbtCompound backCompound = NbtFactory.ofCompound("back_text");
                    NbtList<String> frontMessages = NbtFactory.ofList("messages", frontSide);
                    NbtList<String> backMessages = NbtFactory.ofList("messages", backSide);
                    frontCompound.put(frontMessages);
                    backCompound.put(backMessages);
                    rootCompound.put(frontCompound);
                    rootCompound.put(backCompound);
                    container.getNbtModifier().write(0, rootCompound);
                    container.setMeta("Mr. Clean", "Scrubbed");
                    protocolManager.sendServerPacket(event.getPlayer(), container);
                }
            }
    }
}

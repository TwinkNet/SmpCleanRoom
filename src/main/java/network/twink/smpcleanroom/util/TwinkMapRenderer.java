package network.twink.smpcleanroom.util;

import java.awt.Color;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

public class TwinkMapRenderer extends MapRenderer {

    private final int[] pixelArr = new int[16384];

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        synchronized (pixelArr) {
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
        }
    }

    public int[] getPixelArr() {
        synchronized (pixelArr) {
            return pixelArr;
        }
    }
}

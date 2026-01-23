package network.twink.smpcleanroom.util;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public class TwinkMapRenderer extends MapRenderer {

    private int[] pixelArr;

    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
        pixelArr = new int[16129];
        int cursor = 0;
        for (int x = 0; x < 127; x++) {
            for (int y = 0; y < 127; y++) {
                Color pixel = canvas.getPixelColor(x, y);
                if (pixel == null) continue;
                pixelArr[cursor++] = pixel.getRGB();
            }
        }
    }

    public int[] getPixelArr() {
        return pixelArr;
    }
}

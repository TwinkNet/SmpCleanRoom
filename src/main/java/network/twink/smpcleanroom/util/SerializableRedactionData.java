package network.twink.smpcleanroom.util;

import java.io.Serial;
import java.io.Serializable;

public class SerializableRedactionData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private int mapId;
    private int[] rgbArr;

    public SerializableRedactionData(int mapId, int[] rgbArr) {
        this.mapId = mapId;
        this.rgbArr = rgbArr;
    }

    public int getMapId() {
        return mapId;
    }

    public int[] getRgbArr() {
        return rgbArr;
    }
}

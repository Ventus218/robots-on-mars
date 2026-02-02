package src.model;

import java.util.Date;

public sealed interface TerrainView permits TerrainView.Known, TerrainView.Unknown {

    record Known(Terrain terrain, Date timestamp) implements TerrainView {
    }

    record Unknown() implements TerrainView {
    }
}

package src.model;

import java.util.Date;

sealed interface TerrainView permits Known, Unknown {
}

record Known(Terrain terrain, Date timestamp) implements TerrainView {
}

record Unknown() implements TerrainView {
}

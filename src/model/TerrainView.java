package src.model;

public sealed interface TerrainView permits TerrainView.Known, TerrainView.Unknown {

    record Known(Terrain terrain) implements TerrainView {
    }

    record Unknown() implements TerrainView {
    }
}

package src.model;

import java.util.*;
import java.util.stream.Collectors;
import src.utils.MapWithDefault;

public class MarsView {

    private final Map<Coordinates, TerrainView> view = new MapWithDefault<>(new TerrainView.Unknown());

    public Map<Coordinates, TerrainView.Known> knownTerrain() {
        return view.entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof TerrainView.Known)
                .collect(Collectors.toMap(e -> e.getKey(), e -> (TerrainView.Known) e.getValue()));
    }

    public void updateView(Map<Coordinates, TerrainView.Known> update) {
        update.entrySet().stream().forEach(e -> view.put(e.getKey(), e.getValue()));
    }

    public void merge(MarsView other) {
        other.knownTerrain().forEach((coord, t) -> {
            switch (this.view.get(coord)) {
                case TerrainView.Unknown():
                    this.view.put(coord, t);
                    break;
                case TerrainView.Known(var myTerrain, var timestamp):
                    // Updating my view if other has fresher information
                    if (timestamp.before(t.timestamp())) {
                        this.view.put(coord, t);
                    }
                    break;
            }
        });
    }
}

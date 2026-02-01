package src.model;

import java.util.*;
import java.util.stream.Collectors;
import src.utils.MapWithDefault;

public class MarsView {

    private final Map<Coordinates, TerrainView> view = new MapWithDefault<>(new Unknown());

    public Map<Coordinates, Known> knownTerrain() {
        return view.entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof Known)
                .collect(Collectors.toMap(e -> e.getKey(), e -> (Known) e.getValue()));
    }

    public void merge(MarsView other) {
        other.knownTerrain().forEach((coord, t) -> {
            switch (this.view.get(coord)) {
                case Unknown():
                    this.view.put(coord, t);
                    break;
                case Known(var myTerrain, var timestamp):
                    // Updating my view if other has fresher information
                    if (timestamp.before(t.timestamp())) {
                        this.view.put(coord, t);
                    }
                    break;
            }
        });
    }
}

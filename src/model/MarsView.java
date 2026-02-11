package src.model;

import java.util.*;
import java.util.stream.Collectors;
import src.utils.MapWithDefault;

public class MarsView {

    private final Map<Coordinates, TerrainView> view = new MapWithDefault<>(new TerrainView.Unknown());

    synchronized public Map<Coordinates, TerrainView.Known> knownTerrain() {
        return view.entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof TerrainView.Known)
                .collect(Collectors.toMap(e -> e.getKey(), e -> (TerrainView.Known) e.getValue()));
    }

    synchronized public void updateView(Map<Coordinates, TerrainView.Known> updates) {
        updates.forEach((c, t) -> view.put(c, t));
    }

    synchronized public void updateView(Coordinates coord, TerrainView.Known terrain) {
        view.put(coord, terrain);
    }
}

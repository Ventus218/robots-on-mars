package src.env;

import java.util.*;
import java.util.stream.Collectors;

public class MarsView {

    private final Map<Coordinates, CellView> view = new MapWithDefault<>(new Unknown());

    public Map<Coordinates, Known> knownCells() {
        return view.entrySet()
                .stream()
                .filter(entry -> entry.getValue() instanceof Known)
                .collect(Collectors.toMap(e -> e.getKey(), e -> (Known) e.getValue()));
    }

    public void merge(MarsView other) {
        other.knownCells().forEach((coord, cell) -> {
            switch (this.view.get(coord)) {
                case Unknown():
                    this.view.put(coord, cell);
                    break;
                case Known(var myCell, var timestamp):
                    // Updating my view if other has fresher information
                    if (timestamp.before(cell.timestamp())) {
                        this.view.put(coord, cell);
                    }
                    break;
            }
        });
    }
}

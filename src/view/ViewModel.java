package src.view;

import java.util.*;
import java.util.stream.Collectors;

import src.model.*;

public class ViewModel implements Mars.Listener {
    public static interface Listener {
        public void viewModelChanged();
    }

    private final Mars mars;
    private Optional<Rover> selectedRover;
    private final List<Listener> listeners = new ArrayList<>();

    public ViewModel(Mars mars) {
        this.mars = mars;
        mars.addListener(this);
        this.selectedRover = Optional.empty();
    }

    public Mars mars() {
        return this.mars;
    }

    public Optional<Rover> selectedRover() {
        return selectedRover;
    }

    /**
     * Selects a specific rover view by name.
     * If the rover is re-selected it will be unselected.
     * Not quite intuitive but actually what we want.
     */
    public void selectRoverNamed(String name) {
        if (selectedRover.map(r -> r.name().equals(name)).orElse(false)) {
            this.selectedRover = Optional.empty();
        } else {
            this.selectedRover = mars.rover(name);
        }
        informListeners();
    }

    public Map<Coordinates, Terrain> knownArea() {
        return selectedRover()
                .map(r -> r.marsView().knownTerrain().entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().terrain())))
                .orElse(mars().knownArea().stream()
                        .collect(Collectors.toMap(c -> c, c -> mars().terrainAt(c))));
    }

    private void informListeners() {
        listeners.stream().forEach(Listener::viewModelChanged);
    }

    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public void removeListener(Listener l) {
        this.listeners.remove(l);
    }

    @Override
    public void marsUpdated() {
        informListeners();
    }
}

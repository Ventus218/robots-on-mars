package src.model;

import java.util.*;
import java.util.stream.Collectors;

import src.utils.MapWithDefault;

public class Mars {

    public interface Listener {
        void marsUpdated();
    }

    private Random random;
    private final int bound;
    private final Base base;
    private final Coordinates baseCenter;
    private final Map<Coordinates, Terrain> terrain = new MapWithDefault<>(new Terrain.Empty());
    private final Map<Rover, Coordinates> roverCoordinates = new HashMap<>();
    private final List<Listener> listeners = new ArrayList<>();

    public Mars(int squareSide, double obstaclesDensity, double samplesDensity, double miningSpotsDensity,
            int baseSquareSide, int baseAntennaRange) {
        this(squareSide, obstaclesDensity, samplesDensity, miningSpotsDensity, baseSquareSide, baseAntennaRange,
                System.currentTimeMillis());
    }

    public Mars(int squareSide, double obstaclesDensity, double samplesDensity, double miningSpotsDensity,
            int baseSquareSide, int baseAntennaRange, long seed) {
        this.random = new Random(seed);
        this.bound = Math.abs(squareSide) / 2;
        this.base = new Base(baseSquareSide, baseAntennaRange);
        this.baseCenter = new Coordinates(0, 0);

        for (var x = base.negativeBound(); x <= base.positiveBound(); x++) {
            for (var y = base.negativeBound(); y <= base.positiveBound(); y++) {
                terrain.put(new Coordinates(x, y), new Terrain.Base());
            }
        }

        placeWithDensity(new Terrain.Obstacle(), obstaclesDensity);
        placeWithDensity(new Terrain.MiningSpot(false), miningSpotsDensity);
        placeWithDensity(new Terrain.Sample(), samplesDensity);
    }

    public void spawn(Rover r) {
        var placed = false;

        // TODO: warning, infinite loop if there is no space left in base
        while (!placed) {
            final var coordinates = new Coordinates(randomBaseBounds(), randomBaseBounds());
            if (!roverAtCoordinates(coordinates).isPresent()) {
                roverCoordinates.put(r, coordinates);
                placed = true;
                updateRoverView(r);
                informListeners();
            }
        }
    }

    private void placeWithDensity(Terrain t, double density) {
        var toPlace = area() * density;

        while (toPlace > 0) {
            final var coordinates = new Coordinates(randomInBounds(), randomInBounds());
            if (terrain.get(coordinates).equals(new Terrain.Empty())) {
                terrain.put(coordinates, t);
                toPlace -= 1;
            }
        }

    }

    private int randomInBounds() {
        return random.nextInt(negativeBound(), positiveBound() + 1);
    }

    private int randomBaseBounds() {
        return random.nextInt(base.negativeBound(), base.positiveBound() + 1);
    }

    private int area() {
        return side() * side();
    }

    public int negativeBound() {
        return -bound;
    }

    public int positiveBound() {
        return bound;
    }

    public int side() {
        return bound * 2 + 1;
    }

    public Terrain terrainAt(Coordinates coordinates) {
        assert Math.abs(coordinates.x()) <= positiveBound();
        assert Math.abs(coordinates.y()) <= positiveBound();
        return terrain.get(coordinates);
    }

    public Set<Rover> rovers() {
        return Collections.unmodifiableSet(roverCoordinates.keySet());
    }

    public Optional<Rover> roverAtCoordinates(Coordinates coordinates) {
        return roverCoordinates.entrySet().stream().filter(e -> e.getValue().equals(coordinates)).map(e -> e.getKey())
                .findFirst();
    }

    public Optional<Rover> rover(String name) {
        return rovers().stream().filter(r -> r.name().equals(name)).findFirst();
    }

    private void moveRover(Rover rover, Direction motion) {
        final var coordinates = roverCoordinates.get(rover);
        final var newCoordinates = switch (motion) {
            case Direction.Up() -> new Coordinates(coordinates.x(), coordinates.y() + 1);
            case Direction.Down() -> new Coordinates(coordinates.x(), coordinates.y() - 1);
            case Direction.Left() -> new Coordinates(coordinates.x() - 1, coordinates.y());
            case Direction.Right() -> new Coordinates(coordinates.x() + 1, coordinates.y());
        };
        if (Math.abs(newCoordinates.x()) <= positiveBound() && Math.abs(coordinates.y()) <= positiveBound()) {
            roverCoordinates.put(rover, newCoordinates);
            updateRoverView(rover);
        }
    }

    private void updateRoverView(Rover rover) {
        final var view = cameraRangeOf(rover).stream()
                .collect(Collectors.toMap(c -> c, c -> new TerrainView.Known(terrainAt(c), new Date())));
        rover.marsView().updateView(view);
    }

    public void performAction(Action action) {
        switch (action) {
            case Action.Move(var r, var dir) -> moveRover(r, dir);
            case Action.MapMars(var r) -> updateRoverView(r);
            case Action.ExchangeKnowledge(var r, var other) -> r.exchangeMarsView(other);
            default -> {
            }
        }
        informListeners();
    }

    private void informListeners() {
        listeners.stream().forEach(Listener::marsUpdated);
    }

    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public void removeListener(Listener l) {
        this.listeners.remove(l);
    }

    private Set<Coordinates> radiusOver(Coordinates coordinates, int radius) {
        final Set<Coordinates> result = new HashSet<>();
        final var r2 = radius * radius;

        for (int x = coordinates.x() - radius; x <= coordinates.x() + radius; x++) {
            for (int y = coordinates.y() - radius; y <= coordinates.y() + radius; y++) {
                int dx = x - coordinates.x();
                int dy = y - coordinates.y();
                if (dx * dx + dy * dy <= r2) {
                    result.add(new Coordinates(x, y));
                }
            }
        }
        return result;
    }

    public Set<Coordinates> cameraRangeOf(Rover r) {
        return radiusOver(roverCoordinates.get(r), r.cameraRange());
    }

    public Set<Coordinates> antennaRangeOf(Rover r) {
        return radiusOver(roverCoordinates.get(r), r.antennaRange());
    }

    public Set<Coordinates> antennaRangeOfBase() {
        return radiusOver(baseCenter, base.antennaRange());
    }

    public Set<Coordinates> knownArea() {
        return rovers().stream().map(r -> r.marsView().knownTerrain().keySet()).flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        final var builder = new StringBuilder();
        for (var y = positiveBound(); y >= negativeBound(); y--) {
            for (var x = negativeBound(); x <= positiveBound(); x++) {
                final var coordinates = new Coordinates(x, y);
                var str = switch (terrain.get(coordinates)) {
                    case Terrain.Empty() -> "-";
                    case Terrain.Obstacle() -> "O";
                    case Terrain.Sample() -> "S";
                    case Terrain.MiningSpot(var mined) -> "X";
                    case Terrain.Base() -> "B";
                };
                if (roverAtCoordinates(coordinates).isPresent()) {
                    str = "R";
                }
                builder.append(str);
            }
            builder.append("\n");
        }
        return builder.toString();
    }

}

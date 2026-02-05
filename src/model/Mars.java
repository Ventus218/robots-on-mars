package src.model;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import src.utils.MapWithDefault;
import src.utils.Tuple;
import src.utils.V2D;

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

    synchronized public void spawn(Rover r) {
        var placed = false;

        // TODO: warning, infinite loop if there is no space left in base
        while (!placed) {
            final var coordinates = new Coordinates(randomInBaseBounds(), randomInBaseBounds());
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

    private int randomInBaseBounds() {
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
        assert isInsideBounds(coordinates);
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
        final var newCoordinates = coordinates.apply(motion);
        if (canBeMovedOn(newCoordinates)) {
            roverCoordinates.put(rover, newCoordinates);
            updateRoverView(rover); // TODO: remove from here
        }
    }

    public List<Rover> reachableRovers(Rover rover) {
        final var roverCoord = roverCoordinates.get(rover);
        return roverCoordinates.entrySet().stream()
                .filter(e -> e.getKey() != rover)
                .filter(e -> e.getValue().distanceTo(roverCoord) <= rover.antennaRange())
                .map(e -> e.getKey())
                .toList();

    }

    public boolean canReachBase(Rover rover) {
        final var roverCoord = roverCoordinates.get(rover);
        return roverCoord.distanceTo(baseCenter) <= rover.antennaRange();
    }

    public List<Direction> bestExploreDirections(Rover rover) {
        final var roverCoord = roverCoordinates.get(rover);
        final var knownCoord = rover.marsView().knownTerrain().keySet();
        final Function<Coordinates, Boolean> isBorder = coord -> coord.neighbours().stream()
                .filter(n -> isInsideBounds(n))
                .map(c -> knownCoord.contains(c))
                .anyMatch(hasKnownNeighbour -> hasKnownNeighbour);

        final var unknownCoord = allCoordinates().stream()
                .filter(c -> !knownCoord.contains(c) && isBorder.apply(c))
                .toList();

        final var vector = unknownCoord.stream()
                .map(c -> {
                    final double nearToRover = 1 / (c.distanceTo(roverCoord) - rover.cameraRange());
                    // final double nearToBase = 1 / c.distanceTo(baseCenter);
                    // final double k = 0.4;
                    // final double weight = (1 - k) * nearToRover + k * nearToBase;
                    final double weight = nearToRover;
                    final var versor = c.minus(roverCoord).toVector().versor();
                    return versor.mult(weight);
                })
                .collect(Collectors.reducing(new V2D(0, 0), (v1, v2) -> v1.plus(v2)));

        return availableDirections(rover).stream()
                .map(d -> Tuple.of(d, d.toVector().dot(vector)))
                .sorted(Comparator.<Tuple<Direction, Double>>comparingDouble(t -> t._2()).reversed())
                .map(t -> t._1())
                .toList();
    }

    private Set<Coordinates> allCoordinates() {
        final var result = new HashSet<Coordinates>();
        for (var x = negativeBound(); x <= positiveBound(); x++) {
            for (var y = negativeBound(); y <= positiveBound(); y++) {
                result.add(new Coordinates(x, y));
            }
        }
        return result;
    }

    private void updateRoverView(Rover rover) {
        final var view = cameraRangeOf(rover).stream()
                .collect(Collectors.toMap(c -> c, c -> new TerrainView.Known(terrainAt(c), new Date())));
        rover.marsView().updateView(view);
    }

    synchronized public void performAction(Action action) {
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
                final var c = new Coordinates(x, y);
                if (dx * dx + dy * dy <= r2 && isInsideBounds(c)) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    public Set<Direction> availableDirections(Rover r) {
        return Direction.all().stream()
                .filter(d -> canBeMovedOn(roverCoordinates.get(r).apply(d)))
                .collect(Collectors.toSet());
    }

    private boolean canBeMovedOn(Coordinates coordinates) {
        return switch (terrainAt(coordinates)) {
            case Terrain.Obstacle() -> false;
            case Terrain.MiningSpot(var mined) -> false;
            default -> true;
        } && roverAtCoordinates(coordinates).isEmpty() && isInsideBounds(coordinates);
    }

    public Set<Coordinates> cameraRangeOf(Rover r) {
        return radiusOver(roverCoordinates.get(r), r.cameraRange());
    }

    public boolean isInsideBounds(Coordinates coordinates) {
        return Math.abs(coordinates.x()) <= positiveBound() && Math.abs(coordinates.y()) <= positiveBound();
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

    public Base base() {
        return base;
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

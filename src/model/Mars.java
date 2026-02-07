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
        placeWithDensity(new Terrain.MiningSpot(), miningSpotsDensity);
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
                informListeners();
            }
        }
    }

    synchronized private void placeWithDensity(Terrain t, double density) {
        var toPlace = area() * density;

        while (toPlace > 0) {
            final var coordinates = new Coordinates(randomInBounds(), randomInBounds());
            if (terrain.get(coordinates).equals(new Terrain.Empty())) {
                terrain.put(coordinates, t);
                toPlace -= 1;
            }
        }

    }

    synchronized private int randomInBounds() {
        return random.nextInt(negativeBound(), positiveBound() + 1);
    }

    synchronized private int randomInBaseBounds() {
        return random.nextInt(base.negativeBound(), base.positiveBound() + 1);
    }

    synchronized private int area() {
        return side() * side();
    }

    synchronized public int negativeBound() {
        return -bound;
    }

    synchronized public int positiveBound() {
        return bound;
    }

    synchronized public int side() {
        return bound * 2 + 1;
    }

    synchronized public Terrain terrainAt(Coordinates coordinates) {
        assert isInsideBounds(coordinates);
        return terrain.get(coordinates);
    }

    synchronized public Set<Rover> rovers() {
        return roverCoordinates().keySet();
    }

    synchronized public Optional<Rover> roverAtCoordinates(Coordinates coordinates) {
        return roverCoordinates().entrySet().stream().filter(e -> e.getValue().equals(coordinates)).map(e -> e.getKey())
                .findFirst();
    }

    synchronized public Optional<Rover> rover(String name) {
        return rovers().stream().filter(r -> r.name().equals(name)).findFirst();
    }

    synchronized private void moveRover(Rover rover, Direction motion) {
        final var coordinates = roverCoordinates.get(rover);
        final var newCoordinates = coordinates.apply(motion);
        if (canBeMovedOn(newCoordinates)) {
            roverCoordinates.put(rover, newCoordinates);
            rover.updateBatteryWith(-1);
        }
    }

    synchronized public List<Rover> reachableRovers(HasViewOfMars h) {
        final var coordAndRange = switch (h) {
            case Base b -> Tuple.of(baseCenter, b.antennaRange());
            case Rover r -> Tuple.of(roverCoordinates().get(h), r.antennaRange());
            default -> throw new IllegalArgumentException();
        };
        final var myCoord = coordAndRange._1();
        final var myRange = coordAndRange._2();
        final var reachableRovers = roverCoordinates().entrySet().stream()
                .filter(e -> e.getKey() != h)
                .filter(e -> e.getValue().distanceTo(myCoord) <= myRange)
                .map(e -> e.getKey())
                .toList();
        return reachableRovers;
    }

    synchronized public boolean canReachBase(Rover rover) {
        final var roverCoord = roverCoordinates().get(rover);
        return roverCoord.distanceTo(baseCenter) <= rover.antennaRange();
    }

    synchronized public List<Direction> bestExploreDirections(Rover rover) {
        final var roverCoord = roverCoordinates().get(rover);
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

    synchronized private Set<Coordinates> allCoordinates() {
        final var result = new HashSet<Coordinates>();
        for (var x = negativeBound(); x <= positiveBound(); x++) {
            for (var y = negativeBound(); y <= positiveBound(); y++) {
                result.add(new Coordinates(x, y));
            }
        }
        return result;
    }

    synchronized public void performAction(Action action) {
        switch (action) {
            case Action.Move(var r, var dir) -> moveRover(r, dir);
            default -> {
            }
        }
        informListeners();
    }

    synchronized private void informListeners() {
        listeners.stream().forEach(Listener::marsUpdated);
    }

    synchronized public void addListener(Listener l) {
        this.listeners.add(l);
    }

    synchronized public void removeListener(Listener l) {
        this.listeners.remove(l);
    }

    synchronized private Set<Coordinates> radiusOver(Coordinates coordinates, int radius) {
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

    synchronized public Set<Direction> availableDirections(Rover r) {
        return Direction.all().stream()
                .filter(d -> canBeMovedOn(roverCoordinates().get(r).apply(d)))
                .collect(Collectors.toSet());
    }

    synchronized private boolean canBeMovedOn(Coordinates coordinates) {
        return switch (terrainAt(coordinates)) {
            case Terrain.Obstacle() -> false;
            case Terrain.MiningSpot() -> false;
            default -> true;
        } && roverAtCoordinates(coordinates).isEmpty() && isInsideBounds(coordinates);
    }

    synchronized public Set<Coordinates> cameraRangeOf(Rover r) {
        return radiusOver(roverCoordinates().get(r), r.cameraRange());
    }

    synchronized public boolean isInsideBounds(Coordinates coordinates) {
        return Math.abs(coordinates.x()) <= positiveBound() && Math.abs(coordinates.y()) <= positiveBound();
    }

    synchronized public Set<Coordinates> antennaRangeOf(Rover r) {
        return radiusOver(roverCoordinates().get(r), r.antennaRange());
    }

    synchronized public Set<Coordinates> antennaRangeOfBase() {
        return radiusOver(baseCenter, base.antennaRange());
    }

    synchronized public Set<Coordinates> knownArea() {
        return rovers().stream().map(r -> r.marsView().knownTerrain().keySet()).flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    synchronized public void updateMarsViewOf(HasViewOfMars h, Coordinates c, Terrain t) {
        h.marsView().updateView(Map.of(c, new TerrainView.Known(t)));
        informListeners();
    }

    synchronized public Map<Rover, Coordinates> roverCoordinates() {
        return Map.copyOf(roverCoordinates);
    }

    synchronized public Base base() {
        return base;
    }

    synchronized public Coordinates baseCenterCoordinates() {
        return baseCenter;
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
                    case Terrain.MiningSpot() -> "X";
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

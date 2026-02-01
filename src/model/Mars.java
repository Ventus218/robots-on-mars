package src.model;

import java.util.*;
import src.utils.MapWithDefault;

public class Mars {

    public interface Listener {
        void marsUpdated();
    }

    private Random random;
    private final int bound;
    private final int baseBound;
    private final Map<Coordinates, Cell> grid = new MapWithDefault<>(new Cell.Empty());
    private final List<Listener> listeners = new ArrayList<>();

    public Mars(int squareSide, double obstaclesDensity, double samplesDensity, double miningSpotsDensity,
            int baseSquareSide) {
        this(squareSide, obstaclesDensity, samplesDensity, miningSpotsDensity, baseSquareSide,
                System.currentTimeMillis());
    }

    public Mars(int squareSide, double obstaclesDensity, double samplesDensity, double miningSpotsDensity,
            int baseSquareSide, long seed) {
        this.random = new Random(seed);
        this.bound = Math.abs(squareSide) / 2;
        this.baseBound = Math.abs(baseSquareSide) / 2;

        for (var x = -baseBound; x <= baseBound; x++) {
            for (var y = -baseBound; y <= baseBound; y++) {
                grid.put(new Coordinates(x, y), new Cell.Base());
            }
        }

        placeWithDensity(new Cell.Obstacle(), obstaclesDensity);
        placeWithDensity(new Cell.MiningSpot(false), miningSpotsDensity);
        placeWithDensity(new Cell.Sample(), samplesDensity);
    }

    private void placeWithDensity(Cell cell, double density) {
        var toPlace = area() * density;

        while (toPlace > 0) {
            final var coordinates = new Coordinates(randomInBounds(), randomInBounds());
            if (grid.get(coordinates).equals(new Cell.Empty())) {
                grid.put(coordinates, cell);
                toPlace -= 1;
            }
        }

    }

    private int randomInBounds() {
        return random.nextInt(negativeBound(), positiveBound() + 1);
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

    public Cell cellAt(Coordinates coordinates) {
        assert Math.abs(coordinates.x()) <= positiveBound();
        assert Math.abs(coordinates.y()) <= positiveBound();
        return grid.get(coordinates);
    }

    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public void removeListener(Listener l) {
        this.listeners.remove(l);
    }

    @Override
    public String toString() {
        final var builder = new StringBuilder();
        for (var y = positiveBound(); y >= negativeBound(); y--) {
            for (var x = negativeBound(); x <= positiveBound(); x++) {
                final var coordinates = new Coordinates(x, y);
                final var str = switch (grid.get(coordinates)) {
                    case Cell.Empty() -> "-";
                    case Cell.Obstacle() -> "O";
                    case Cell.Sample() -> "S";
                    case Cell.MiningSpot(var mined) -> "X";
                    case Cell.Rover(var name) -> "R";
                    case Cell.Base() -> "B";
                };
                builder.append(str);
            }
            builder.append("\n");
        }
        return builder.toString();
    }

}

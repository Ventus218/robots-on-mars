package src.env;

import java.util.*;

public class Mars {

    private Random random;
    private final int size;
    private final Map<Coordinates, Cell> grid = new MapWithDefault<>(new Cell.Empty());

    public Mars(int fullSize, double obstaclesDensity, double samplesDensity, double miningSpotsDensity,
            int baseSize) {
        this(fullSize, obstaclesDensity, samplesDensity, miningSpotsDensity, baseSize, System.currentTimeMillis());
    }

    public Mars(int fullSize, double obstaclesDensity, double samplesDensity, double miningSpotsDensity, int baseSize,
            long seed) {
        this.random = new Random(seed);
        this.size = fullSize / 2;

        final var halfBaseSize = baseSize / 2;
        for (var x = -halfBaseSize; x < halfBaseSize; x++) {
            for (var y = -halfBaseSize; y < halfBaseSize; y++) {
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
            final var coordinates = new Coordinates(randomInSize(), randomInSize());
            if (grid.get(coordinates).equals(new Cell.Empty())) {
                grid.put(coordinates, cell);
                toPlace -= 1;
            }
        }

    }

    private int randomInSize() {
        return random.nextInt(-size, size + 1);
    }

    private int area() {
        return size * size;
    }

    @Override
    public String toString() {
        final var builder = new StringBuilder();
        for (var y = size; y >= -size; y--) {
            for (var x = -size; x <= size; x++) {
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

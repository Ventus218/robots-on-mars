package src.env;

import java.util.*;

public class EnvMap {

    private Random random;
    private final int size;
    private final Map<Integer, Map<Integer, Cell>> grid = new HashMap<>();

    public EnvMap(int fullSize, float obstaclesDensity, float samplesDensity, float miningSpotsDensity, int baseSize,
            int seed) {
        this.random = new Random(seed);
        this.size = fullSize / 2;
        for (var x = -size; x < size; x++) {
            final var column = new HashMap<Integer, Cell>();
            grid.put(x, column);
            for (var y = -size; y < size; y++) {
                column.put(y, new Empty());
            }
        }

        final var halfBaseSize = baseSize / 2;
        for (var x = -halfBaseSize; x < halfBaseSize; x++) {
            for (var y = -halfBaseSize; y < halfBaseSize; y++) {
                setCellAt(x, y, new Base());
            }
        }

        placeWithDensity(new Obstacle(), obstaclesDensity);
        placeWithDensity(new MiningSpot(), miningSpotsDensity);
        placeWithDensity(new Sample(), samplesDensity);
    }

    private void setCellAt(int x, int y, Cell cell) {
        grid.get(x).put(y, cell);
    }

    private Cell getCellAt(int x, int y) {
        return grid.get(x).get(y);
    }

    private void placeWithDensity(Cell cell, float density) {
        var toPlace = area() * density;

        while (toPlace > 0) {
            final var x = randomInSize();
            final var y = randomInSize();
            if (getCellAt(x, y).equals(new Empty())) {
                setCellAt(x, y, cell);
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

}

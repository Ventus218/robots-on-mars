package src.model;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public record Coordinates(int x, int y) {
    public double distanceTo(Coordinates coordinates) {
        return Math.sqrt(Math.pow(x - coordinates.x(), 2) + Math.pow(y - coordinates.y(), 2));
    }

    public Direction directionTowards(Coordinates coordinates) {
        return directionsTowards(coordinates).getFirst();
    }

    public List<Direction> directionsTowards(Coordinates coordinates) {
        return Direction.all().stream()
                .sorted(Comparator.comparingDouble(d -> d.applyTo(this).distanceTo(coordinates)))
                .toList();
    }

    public Set<Coordinates> neighbours() {
        return Set.of(
                new Coordinates(x + 1, y + 1),
                new Coordinates(x + 1, y),
                new Coordinates(x + 1, y - 1),
                new Coordinates(x, y + 1),
                new Coordinates(x, y - 1),
                new Coordinates(x - 1, y + 1),
                new Coordinates(x - 1, y),
                new Coordinates(x - 1, y - 1));
    }
}

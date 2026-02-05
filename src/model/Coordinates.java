package src.model;

import java.util.Set;
import src.utils.V2D;

public record Coordinates(int x, int y) {
    public double distanceTo(Coordinates coordinates) {
        return toVector().distanceTo(coordinates.toVector());
    }

    public Coordinates plus(Coordinates coordinates) {
        return new Coordinates(x + coordinates.x(), y + coordinates.y());
    }

    public Coordinates minus(Coordinates coordinates) {
        return new Coordinates(x - coordinates.x(), y - coordinates.y());
    }

    public Coordinates apply(Direction dir) {
        return switch (dir) {
            case Direction.Up() -> plus(new Coordinates(0, 1));
            case Direction.Down() -> plus(new Coordinates(0, -1));
            case Direction.Right() -> plus(new Coordinates(1, 0));
            case Direction.Left() -> plus(new Coordinates(-1, 0));
        };
    }

    public V2D toVector() {
        return new V2D(x, y);
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

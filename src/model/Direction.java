package src.model;

import java.util.Random;
import java.util.Set;

public sealed interface Direction
        permits Direction.Up, Direction.Down, Direction.Left, Direction.Right {

    record Up() implements Direction {
        public Coordinates applyTo(Coordinates coordinates) {
            return new Coordinates(coordinates.x(), coordinates.y() + 1);
        }
    }

    record Down() implements Direction {
        public Coordinates applyTo(Coordinates coordinates) {
            return new Coordinates(coordinates.x(), coordinates.y() - 1);
        }
    }

    record Left() implements Direction {
        public Coordinates applyTo(Coordinates coordinates) {
            return new Coordinates(coordinates.x() - 1, coordinates.y());
        }
    }

    record Right() implements Direction {
        public Coordinates applyTo(Coordinates coordinates) {
            return new Coordinates(coordinates.x() + 1, coordinates.y());
        }
    }

    public Coordinates applyTo(Coordinates coordinates);

    public static Set<Direction> all() {
        return Set.of(new Up(), new Down(), new Left(), new Right());
    }

    public static Direction random() {
        return switch (new Random().nextInt(0, 4)) {
            case 0 -> new Up();
            case 1 -> new Down();
            case 2 -> new Left();
            default -> new Right();
        };
    }
}

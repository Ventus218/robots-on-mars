package src.model;

import java.util.Random;
import java.util.Set;

import src.utils.V2D;

public sealed interface Direction
        permits Direction.Up, Direction.Down, Direction.Left, Direction.Right {

    public V2D toVector();

    record Up() implements Direction {
        @Override
        public V2D toVector() {
            return new V2D(0, 1);
        }
    }

    record Down() implements Direction {
        @Override
        public V2D toVector() {
            return new V2D(0, -1);
        }
    }

    record Left() implements Direction {
        @Override
        public V2D toVector() {
            return new V2D(-1, 0);
        }
    }

    record Right() implements Direction {
        @Override
        public V2D toVector() {
            return new V2D(1, 0);
        }
    }

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

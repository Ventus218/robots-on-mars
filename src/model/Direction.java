package src.model;

import java.util.Random;

public sealed interface Direction
        permits Direction.Up, Direction.Down, Direction.Left, Direction.Right {

    record Up() implements Direction {
    }

    record Down() implements Direction {
    }

    record Left() implements Direction {
    }

    record Right() implements Direction {
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

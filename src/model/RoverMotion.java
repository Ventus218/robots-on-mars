package src.model;

import java.util.Random;

public sealed interface RoverMotion
        permits RoverMotion.Up, RoverMotion.Down, RoverMotion.Left, RoverMotion.Right {

    record Up() implements RoverMotion {
    }

    record Down() implements RoverMotion {
    }

    record Left() implements RoverMotion {
    }

    record Right() implements RoverMotion {
    }

    public static RoverMotion random() {
        return switch (new Random().nextInt(0, 4)) {
            case 0 -> new Up();
            case 1 -> new Down();
            case 2 -> new Left();
            default -> new Right();
        };
    }
}

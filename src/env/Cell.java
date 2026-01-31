package src.env;

sealed interface Cell permits Cell.Rover, Cell.Empty, Cell.Obstacle, Cell.MiningSpot, Cell.Sample, Cell.Base {
    record Rover(String name) implements Cell {
    }

    record Empty() implements Cell {
    }

    record Obstacle() implements Cell {
    }

    record MiningSpot(Boolean mined) implements Cell {
    }

    record Sample() implements Cell {
    }

    record Base() implements Cell {
    }
}

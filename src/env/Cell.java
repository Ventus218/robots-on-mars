package src.env;

sealed interface Cell permits Rover, Empty, Obstacle, MiningSpot, Sample, Base {
}

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

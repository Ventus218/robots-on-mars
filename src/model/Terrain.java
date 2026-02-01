package src.model;

public sealed interface Terrain
        permits Terrain.Empty, Terrain.Obstacle, Terrain.MiningSpot, Terrain.Sample, Terrain.Base {

    record Empty() implements Terrain {
    }

    record Obstacle() implements Terrain {
    }

    record MiningSpot(Boolean mined) implements Terrain {
    }

    record Sample() implements Terrain {
    }

    record Base() implements Terrain {
    }
}

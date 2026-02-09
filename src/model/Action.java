package src.model;

public sealed interface Action
        permits Action.Move, Action.MineSample, Action.CollectSample, Action.DepositSamples, Action.Recharge {

    public record Move(Rover rover, Direction direction) implements Action {
    }

    public record MineSample(ScientistRover rover, Coordinates coordinates) implements Action {
    }

    public record CollectSample(ScientistRover rover, Coordinates coordinates) implements Action {
    }

    public record DepositSamples(ScientistRover rover) implements Action {
    }

    public record Recharge(Rover rover) implements Action {
    }

    public Rover rover();
}

package src.model;

public sealed interface Action
        permits Action.Move, Action.StartMining, Action.PickupSample, Action.DepositSamples {

    public record Move(Rover rover, Direction direction) implements Action {
    }

    public record StartMining(Rover rover, Direction direction) implements Action {
    }

    public record PickupSample(Rover rover, Direction direction) implements Action {
    }

    public record DepositSamples(Rover rover) implements Action {
    }

    public Rover rover();
}

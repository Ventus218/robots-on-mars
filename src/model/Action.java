package src.model;

public sealed interface Action
        permits Action.Move, Action.StartMining, Action.PickupSample, Action.DepositSamples, Action.Recharge {

    public record Move(Rover rover, Direction direction) implements Action {
    }

    public record StartMining(Rover rover, Direction direction) implements Action {
    }

    public record PickupSample(Rover rover, Direction direction) implements Action {
    }

    public record DepositSamples(Rover rover) implements Action {
    }

    public record Recharge(Rover rover) implements Action {
    }

    public Rover rover();
}

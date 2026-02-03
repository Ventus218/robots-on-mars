package src.model;

public sealed interface Action
        permits Action.MapMars, Action.Move, Action.StartMining, Action.ExchangeKnowledge, Action.PickupSample,
        Action.DepositSamples {

    public record MapMars(Rover rover) implements Action {
    }

    public record Move(Rover rover, Direction direction) implements Action {
    }

    public record StartMining(Rover rover, Direction direction) implements Action {
    }

    public record ExchangeKnowledge(Rover rover, MarsViewExchanger exchanger) implements Action {
    }

    public record PickupSample(Rover rover, Direction direction) implements Action {
    }

    public record DepositSamples(Rover rover) implements Action {
    }

    public Rover rover();
}

package src.model;

public class ScientistRover implements Rover {

    private final SimpleRover rover;

    public ScientistRover(String name, int battery, int cameraRange, int antennaRange) {
        this.rover = new SimpleRover(name, battery, cameraRange, antennaRange);
    }

    @Override
    public String name() {
        return rover.name();
    }

    @Override
    public int battery() {
        return rover.battery();
    }

    @Override
    public int cameraRange() {
        return rover.cameraRange();
    }

    @Override
    public int antennaRange() {
        return rover.antennaRange();
    }

    @Override
    public MarsView marsView() {
        return this.rover.marsView();
    }

    @Override
    public void exchangeMarsView(MarsViewExchanger other) {
        this.rover.exchangeMarsView(other);
    }
}

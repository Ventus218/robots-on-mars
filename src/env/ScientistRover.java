package src.env;

public class ScientistRover implements Rover {

    private final SimpleRover rover;

    public ScientistRover(String name, int battery, int cameraRange, int antennaRange) {
        this.rover = new SimpleRover(name, battery, cameraRange, antennaRange);
    }

    @Override
    public String getName() {
        return rover.getName();
    }

    @Override
    public int getBattery() {
        return rover.getBattery();
    }

    @Override
    public int getCameraRange() {
        return rover.getCameraRange();
    }

    @Override
    public int getAntennaRange() {
        return rover.getAntennaRange();
    }
}

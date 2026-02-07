package src.model;

public class ScientistRover implements Rover {

    private final SimpleRover rover;
    private final int carriedSamples = 0;
    private final int samplesCapacity;

    public ScientistRover(String name, int battery, int batteryCapacity, int cameraRange, int antennaRange,
            int samplesCapacity) {
        this.rover = new SimpleRover(name, battery, batteryCapacity, cameraRange, antennaRange);
        this.samplesCapacity = samplesCapacity;
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
    public int batteryCapacity() {
        return rover.batteryCapacity();
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
    public void updateBatteryWith(int update) {
        rover.updateBatteryWith(update);
    }

    public int carriedSamples() {
        return carriedSamples;
    }

    public int samplesCapacity() {
        return samplesCapacity;
    }

}

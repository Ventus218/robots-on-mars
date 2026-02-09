package src.model;

public class ScientistRover implements Rover {

    private final SimpleRover rover;
    private int carriedSamples = 0;
    private final int samplesCapacity;
    private final int miningSampleEnergyCost;

    public ScientistRover(String name, int battery, int batteryCapacity, int cameraRange, int antennaRange,
            int samplesCapacity, int miningSampleEnergyCost) {
        this.rover = new SimpleRover(name, battery, batteryCapacity, cameraRange, antennaRange);
        this.samplesCapacity = samplesCapacity;
        this.miningSampleEnergyCost = miningSampleEnergyCost;
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

    public boolean collectSample() {
        if (carriedSamples >= samplesCapacity) {
            return false;
        } else {
            carriedSamples += 1;
            return true;
        }
    }

    public int samplesCapacity() {
        return samplesCapacity;
    }

    public boolean mineSample() {
        if (battery() - miningSampleEnergyCost >= 0) {
            rover.updateBatteryWith(-miningSampleEnergyCost);
            return true;
        } else {
            return false;
        }
    }

    public void depositSamples() {
        carriedSamples = 0;
    }

    public int miningSampleEnergyCost() {
        return miningSampleEnergyCost;
    }

}

package src.model;

public class ScientistRover implements Rover {

    private final SimpleRover rover;
    private int carriedSamples = 0;
    private final int samplesCapacity;
    private final int miningSampleEnergyCost;

    public ScientistRover(String name, int battery, int batteryCapacity, int cameraRange, int antennaRange,
            int movementEnergyCost, int rechargeEnergyAmount,
            int samplesCapacity, int miningSampleEnergyCost) {
        this.rover = new SimpleRover(name, battery, batteryCapacity, cameraRange, antennaRange, movementEnergyCost,
                rechargeEnergyAmount);
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
    public boolean move() {
        return rover.move();
    }

    @Override
    public void recharge() {
        rover.recharge();
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
        return rover.updateBatteryWith(-miningSampleEnergyCost);
    }

    public void depositSamples() {
        carriedSamples = 0;
    }

    public int miningSampleEnergyCost() {
        return miningSampleEnergyCost;
    }

}

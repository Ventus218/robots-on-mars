package src.model;

public class SimpleRover implements Rover {
    private final String name;
    private int battery;
    private final int batteryCapacity;
    private final int cameraRange;
    private final int antennaRange;
    private final int movementEnergyCost;
    private final int rechargeEnergyAmount;
    private final MarsView marsView = new MarsView();

    public SimpleRover(String name, int battery, int batteryCapacity, int cameraRange, int antennaRange,
            int movementEnergyCost, int rechargeEnergyAmount) {
        this.name = name;
        this.battery = battery;
        this.batteryCapacity = batteryCapacity;
        this.cameraRange = cameraRange;
        this.antennaRange = antennaRange;
        this.movementEnergyCost = movementEnergyCost;
        this.rechargeEnergyAmount = rechargeEnergyAmount;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int battery() {
        return battery;
    }

    @Override
    public int batteryCapacity() {
        return batteryCapacity;
    }

    @Override
    public int cameraRange() {
        return cameraRange;
    }

    @Override
    public int antennaRange() {
        return antennaRange;
    }

    @Override
    public MarsView marsView() {
        return this.marsView;
    }

    @Override
    public boolean move() {
        if (battery() - movementEnergyCost < 0) {
            return false;
        } else {
            updateBatteryWith(-movementEnergyCost);
            return true;
        }
    }

    @Override
    public void recharge() {
        updateBatteryWith(rechargeEnergyAmount);
    }

    public void updateBatteryWith(int update) {
        battery = battery + update;
        battery = Math.min(batteryCapacity, battery);
        battery = Math.max(0, battery);
    }
}

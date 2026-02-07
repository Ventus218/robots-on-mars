package src.model;

public class SimpleRover implements Rover {
    private final String name;
    private int battery;
    private final int batteryCapacity;
    private final int cameraRange;
    private final int antennaRange;
    private final MarsView marsView = new MarsView();

    public SimpleRover(String name, int battery, int batteryCapacity, int cameraRange, int antennaRange) {
        this.name = name;
        this.battery = battery;
        this.batteryCapacity = batteryCapacity;
        this.cameraRange = cameraRange;
        this.antennaRange = antennaRange;
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
    public void updateBatteryWith(int update) {
        battery = battery + update;
        battery = Math.min(batteryCapacity, battery);
        battery = Math.max(0, battery);
    }
}

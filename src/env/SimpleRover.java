package src.env;

public class SimpleRover implements Rover {
    private final String name;
    private final int battery;
    private final int cameraRange;
    private final int antennaRange;
    private final MarsView view = new MarsView();

    public SimpleRover(String name, int battery, int cameraRange, int antennaRange) {
        this.name = name;
        this.battery = battery;
        this.cameraRange = cameraRange;
        this.antennaRange = antennaRange;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getBattery() {
        return battery;
    }

    @Override
    public int getCameraRange() {
        return cameraRange;
    }

    @Override
    public int getAntennaRange() {
        return antennaRange;
    }
}

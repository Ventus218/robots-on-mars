package src.model;

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
    public String name() {
        return name;
    }

    @Override
    public int battery() {
        return battery;
    }

    @Override
    public int cameraRange() {
        return cameraRange;
    }

    @Override
    public int antennaRange() {
        return antennaRange;
    }
}

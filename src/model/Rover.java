package src.model;

public interface Rover {
    public String name();

    public int battery();

    public int cameraRange();

    public int antennaRange();

    public MarsView marsView();
}

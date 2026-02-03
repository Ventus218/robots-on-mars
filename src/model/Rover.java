package src.model;

public interface Rover extends MarsViewExchanger {
    public String name();

    public int battery();

    public int cameraRange();

    public int antennaRange();
}

package src.model;

public interface Rover extends HasViewOfMars {
    public String name();

    public int battery();

    public int cameraRange();

    public int antennaRange();
}

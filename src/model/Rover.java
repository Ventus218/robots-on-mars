package src.model;

public interface Rover extends HasViewOfMars {
    public String name();

    public int battery();

    public int batteryCapacity();

    public int cameraRange();

    public int antennaRange();

    public boolean move();

    public void recharge();
}

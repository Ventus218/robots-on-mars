package src.model;

public class Base implements HasViewOfMars {

    private final MarsView marsView = new MarsView();
    private final int bound;
    private final int antennaRange;

    public Base(int squareSide, int antennaRange) {
        this.bound = Math.abs(squareSide) / 2;
        this.antennaRange = antennaRange;
    }

    public int antennaRange() {
        return antennaRange;
    }

    public int negativeBound() {
        return -bound;
    }

    public int positiveBound() {
        return bound;
    }

    public int side() {
        return bound * 2 + 1;
    }

    @Override
    public MarsView marsView() {
        return this.marsView;
    }
}

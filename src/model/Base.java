package src.model;

public class Base implements MarsViewExchanger {

    private final MarsViewExchangerImpl marsViewExchanger;
    private final int bound;
    private final int antennaRange;

    public Base(int squareSide, int antennaRange) {
        this.bound = Math.abs(squareSide) / 2;
        this.antennaRange = antennaRange;
        this.marsViewExchanger = new MarsViewExchangerImpl(new MarsView());
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
        return this.marsViewExchanger.marsView();
    }

    @Override
    public void exchangeMarsView(MarsViewExchanger other) {
        this.marsViewExchanger.exchangeMarsView(other);
    }
}

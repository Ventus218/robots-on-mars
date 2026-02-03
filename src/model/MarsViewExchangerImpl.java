package src.model;

public class MarsViewExchangerImpl implements MarsViewExchanger {

    private final MarsView view;

    public MarsViewExchangerImpl(MarsView view) {
        this.view = view;
    }

    @Override
    public MarsView marsView() {
        return this.view;
    }

    @Override
    public void exchangeMarsView(MarsViewExchanger other) {
        marsView().merge(other.marsView());
        other.marsView().merge(marsView());
    }
}

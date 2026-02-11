package src.test;

import src.model.*;

class TestListener implements Mars.Listener {
    public int callCount = 0;

    @Override
    public void marsUpdated() {
        callCount++;
    }
}

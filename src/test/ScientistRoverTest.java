package src.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import src.model.*;

class ScientistRoverTest {
    private ScientistRover scientist;

    @BeforeEach
    void setUp() {
        scientist = new ScientistRover("R1", 100, 100, 2, 5, 10, 20, 2, 15);
    }

    @Test
    void testCollectSampleCapacity() {
        assertTrue(scientist.collectSample(), "Should collect 1st sample");
        assertTrue(scientist.collectSample(), "Should collect 2nd sample");
        assertFalse(scientist.collectSample(), "Should NOT collect 3rd sample (Cap is 2)");
        assertEquals(2, scientist.carriedSamples());
    }

    @Test
    void testMineSampleConsumesBattery() {
        int initialBattery = scientist.battery();
        boolean success = scientist.mineSample();

        assertTrue(success);
        assertEquals(initialBattery - 15, scientist.battery());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Test that depositing samples clears inventory")
    void testDepositSamples() {
        scientist.collectSample();
        assertEquals(1, scientist.carriedSamples());

        scientist.depositSamples();
        assertEquals(0, scientist.carriedSamples(), "Inventory should be zero after deposit");
    }

    @Test
    void testDelegationToSimpleRover() {
        scientist.move();
        assertEquals(90, scientist.battery(), "Scientist should delegate move to SimpleRover");

        scientist.recharge();
        assertEquals(100, scientist.battery(), "Scientist should delegate recharge to SimpleRover");
    }
}

package src.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import src.model.*;

class SimpleRoverTest {
    private SimpleRover rover;

    @BeforeEach
    void setUp() {
        rover = new SimpleRover("R1", 50, 100, 2, 5, 10, 30);
    }

    @Test
    void testMoveReducesBattery() {
        boolean canMove = rover.move();
        assertTrue(canMove);
        assertEquals(40, rover.battery());
    }

    @Test
    void testCannotMoveWithInsufficientBattery() {
        rover.updateBatteryWith(-50);
        assertEquals(0, rover.battery());

        boolean canMove = rover.move();
        assertFalse(canMove, "Rover should not be able to move with 0 battery");
    }

    @Test
    void testRechargeDoesNotExceedCapacity() {
        rover.updateBatteryWith(40);
        rover.recharge();

        assertEquals(100, rover.battery());
    }

    @Test
    void testUpdateBatteryFloorAtZero() {
        rover.updateBatteryWith(-1000);
        assertEquals(0, rover.battery(), "Battery should never be negative");
    }
}

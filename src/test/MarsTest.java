package src.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

import static org.junit.jupiter.api.Assertions.*;

import src.model.*;

public class MarsTest {
    private Mars mars;

    private SimpleRover simpleRoverNamed(String name) {
        return new SimpleRover(
                name,
                Config.ROVER_BATTERY_CAPACITY,
                Config.ROVER_BATTERY_CAPACITY,
                Config.ROVER_CAMERA_RANGE,
                Config.ROVER_ANTENNA_RANGE,
                Config.ROVER_MOVEMENT_ENERGY_COST,
                Config.ROVER_CHARGING_ENERGY_AMOUNT);
    }

    private ScientistRover scientistRoverNamed(String name) {
        return new ScientistRover(
                name,
                Config.ROVER_BATTERY_CAPACITY,
                Config.ROVER_BATTERY_CAPACITY,
                Config.ROVER_CAMERA_RANGE,
                Config.ROVER_ANTENNA_RANGE,
                Config.ROVER_MOVEMENT_ENERGY_COST,
                Config.ROVER_CHARGING_ENERGY_AMOUNT,
                Config.SCIENTIST_SAMPLES_CAPACITY,
                Config.SCIENTIST_MINING_SAMPLE_ENERGY_COST);
    }

    @BeforeEach
    void setUp() {
        // squareSide, obstacles, samples, mining, baseSide, baseAntenna
        mars = new Mars(10, 0.1, 0.05, 0.05, 3, 5, 12345L);
    }

    @Test
    void testInitializationBounds() {
        assertEquals(-5, mars.negativeBound());
        assertEquals(5, mars.positiveBound());
        assertTrue(mars.isInsideBounds(new Coordinates(0, 0)));
        assertFalse(mars.isInsideBounds(new Coordinates(6, 0)));
    }

    @Test
    void testSpawnRoverInsideBase() {
        Rover rover = simpleRoverNamed("R1");
        mars.spawn(rover);

        Coordinates coords = mars.roverCoordinates().get(rover);
        // Base is 3x3 centered at 0,0 -> bounds are -1 to 1
        assertTrue(coords.x() >= -1 && coords.x() <= 1);
        assertTrue(coords.y() >= -1 && coords.y() <= 1);
    }

    @Test
    void testMovementLogic() {
        Rover rover = simpleRoverNamed("R1");
        mars.spawn(rover);
        Coordinates startPos = mars.roverCoordinates().get(rover);

        Action moveAction = new Action.Move(rover, new Direction.Right());
        boolean success = mars.performAction(moveAction);

        Coordinates endPos = mars.roverCoordinates().get(rover);
        if (success) {
            assertEquals(startPos.x() + 1, endPos.x());
        } else {
            // If failed, there must be an obstacle or another rover
            Terrain t = mars.terrainAt(startPos.apply(new Direction.Right()));
            assertTrue(t instanceof Terrain.Obstacle || t instanceof Terrain.MiningSpot
                    || mars.roverAtCoordinates(endPos).isPresent());
        }
    }

    @Test
    void testListenerNotification() {
        TestListener listener = new TestListener();
        mars.addListener(listener);

        Rover rover = simpleRoverNamed("R1");
        mars.spawn(rover);

        assertTrue(listener.callCount > 0, "Listener should have been notified of spawn");

        int countBeforeAction = listener.callCount;
        mars.performAction(new Action.Recharge(rover));
        assertEquals(countBeforeAction + 1, listener.callCount, "Action should notify listeners");
    }

    @Test
    void testTerrainDensity() {
        Mars largeMars = new Mars(100, 0.1, 0.0, 0.0, 1, 1, 42L);
        long obstacles = largeMars.allCoordinates().stream()
                .filter(c -> largeMars.terrainAt(c) instanceof Terrain.Obstacle)
                .count();

        assertTrue(obstacles > 900 && obstacles < 1100);
    }

    @Test
    void testReachableRovers() {
        Rover r1 = simpleRoverNamed("R1");
        Rover r2 = simpleRoverNamed("R2");

        mars.spawn(r1);
        mars.spawn(r2);

        assertTrue(mars.reachableRovers(r1).contains(r2));

        assertTrue(mars.performAction(new Action.Move(r1, new Direction.Right())));
        assertTrue(mars.performAction(new Action.Move(r1, new Direction.Right())));
        assertTrue(mars.performAction(new Action.Move(r1, new Direction.Up())));
        assertTrue(mars.performAction(new Action.Move(r1, new Direction.Right())));
        assertTrue(mars.performAction(new Action.Move(r1, new Direction.Right())));
        assertTrue(mars.performAction(new Action.Move(r2, new Direction.Left())));
        assertTrue(mars.performAction(new Action.Move(r2, new Direction.Left())));
        assertFalse(mars.reachableRovers(r1).contains(r2));
    }

    @Test
    void testCannotMoveIntoObstacles() {
        Rover r1 = simpleRoverNamed("R1");
        mars.spawn(r1);
        assertTrue(mars.performAction(new Action.Move(r1, new Direction.Right())));
        assertTrue(mars.performAction(new Action.Move(r1, new Direction.Right())));
        assertTrue(mars
                .terrainAt(mars.roverCoordinates().get(r1).apply(new Direction.Right())) instanceof Terrain.Obstacle);
    }

    @Test
    void testRoverMovementConsumesBattery() {
        Rover rover = simpleRoverNamed("R1");
        mars.spawn(rover);

        int initialBattery = rover.battery();

        Direction dir = mars.availableDirections(rover).iterator().next();
        mars.performAction(new Action.Move(rover, dir));

        assertEquals(initialBattery - Config.ROVER_MOVEMENT_ENERGY_COST, rover.battery(),
                "Moving should reduce battery");
    }

    @Test
    void testScientistMiningAndCollecting() {
        ScientistRover scientist = scientistRoverNamed("R1");
        mars.spawn(scientist);

        Coordinates roverPos = mars.roverCoordinates().get(scientist);
        Coordinates adjacent = roverPos.apply(new Direction.Up());

        int batteryBeforeMining = scientist.battery();
        mars.performAction(new Action.MineSample(scientist, adjacent));
        assertEquals(batteryBeforeMining - Config.SCIENTIST_MINING_SAMPLE_ENERGY_COST, scientist.battery(),
                "Mining should cost battery");

        int samplesBefore = scientist.carriedSamples();
        mars.performAction(new Action.CollectSample(scientist, adjacent));
        assertEquals(samplesBefore + 1, scientist.carriedSamples(), "Collecting should increase inventory");
    }

    @Test
    void testDepositSamplesAtBase() {
        ScientistRover scientist = scientistRoverNamed("R1");
        mars.spawn(scientist);

        scientist.collectSample();
        scientist.collectSample();
        assertEquals(2, scientist.carriedSamples());

        assertTrue(mars.performAction(new Action.DepositSamples(scientist)),
                "Should be able to deposit inside the base");
        assertEquals(0, scientist.carriedSamples(), "Inventory should be empty after deposit");
    }

    @Test
    void testRechargeAtBase() {
        SimpleRover rover = simpleRoverNamed("R1");
        mars.spawn(rover);

        rover.updateBatteryWith(-50);
        final var initialBattery = rover.battery();
        mars.performAction(new Action.Recharge(rover));
        assertEquals(initialBattery + Config.ROVER_CHARGING_ENERGY_AMOUNT, rover.battery(),
                "Recharge should add the rechargeEnergyAmount");
    }

    @Test
    void testCannotMoveWithZeroBattery() {
        SimpleRover deadRover = simpleRoverNamed("R1");
        mars.spawn(deadRover);

        deadRover.updateBatteryWith(-Config.ROVER_BATTERY_CAPACITY);
        Direction dir = new Direction.Up();
        boolean moved = mars.performAction(new Action.Move(deadRover, dir));

        assertFalse(moved, "Rover should not be able to move with 0 battery");
    }
}

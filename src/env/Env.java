package src.env;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

// Environment code for project robotsOnMars
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import jason.environment.Environment;
import src.view.AppFrame;
import src.model.*;

public class Env extends Environment {

    private Logger logger = Logger.getLogger("robotsOnMars." + Env.class.getName());
    private final int MARS_SIZE = 100;
    private final float MARS_OBSTACLES_DENSITY = 0.05f;
    private final float MARS_SAMPLES_DENSITY = 0.005f;
    private final float MARS_MINING_SPOTS_DENSITY = 0.01f;
    private final int MARS_BASE_SIZE = 4;
    private final int MARS_BASE_ANTENNA_RANGE = 10;
    private final int ROVER_BATTERY_CAPACITY = 100;
    private final int ROVER_CAMERA_RANGE = 10;
    private final int ROVER_ANTENNA_RANGE = 7;

    private Mars mars = new Mars(
            MARS_SIZE,
            MARS_OBSTACLES_DENSITY,
            MARS_SAMPLES_DENSITY,
            MARS_MINING_SPOTS_DENSITY,
            MARS_BASE_SIZE,
            MARS_BASE_ANTENNA_RANGE);
    public static final Literal moveLiteral = ASSyntax.createLiteral("moveInRandomDirection");
    public static final Literal canMoveUpLit = ASSyntax.createLiteral("canMove(up)");
    public static final Literal canMoveDownLit = ASSyntax.createLiteral("canMove(down)");
    public static final Literal canMoveLeftLit = ASSyntax.createLiteral("canMove(left)");
    public static final Literal canMoveRightLit = ASSyntax.createLiteral("canMove(right)");

    private Rover simpleRoverNamed(String name) {
        return new SimpleRover(name, ROVER_BATTERY_CAPACITY, ROVER_CAMERA_RANGE, ROVER_ANTENNA_RANGE);
    }

    private Rover scientistRoverNamed(String name) {
        return new ScientistRover(name, ROVER_BATTERY_CAPACITY, ROVER_CAMERA_RANGE, ROVER_ANTENNA_RANGE);
    }

    /** Called before the MAS execution with the args informed in .mas2j */
    @Override
    public void init(String[] args) {
        super.init(args);
        mars.spawn(simpleRoverNamed("tony"));
        mars.spawn(simpleRoverNamed("bill"));
        mars.spawn(scientistRoverNamed("frank"));
        try {
            addPercept(ASSyntax.parseLiteral("percept(" + args[0] + ")"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new AppFrame(mars).setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        if (action.equals(moveLiteral)) {
            mars.performAction(new Action.Move(mars.rover(agName).get(), Direction.random()));
        }

        logger.info("executing: " + action + ", but not implemented!");
        if (true) { // you may improve this condition
            informAgsEnvironmentChanged();
        }
        return true; // the action was executed with success
    }

    @Override
    public Collection<Literal> getPercepts(String agName) {
        final var rover = mars.rover(agName).get();
        final var canMovePercepts = mars.availableDirections(rover).stream()
                .map(d -> switch (d) {
                    case Direction.Up() -> canMoveUpLit;
                    case Direction.Down() -> canMoveDownLit;
                    case Direction.Left() -> canMoveLeftLit;
                    case Direction.Right() -> canMoveRightLit;
                })
                .toList();

        // Flattening the lists
        return List.of(canMovePercepts).stream().flatMap(List::stream).toList();
    }

    /** Called before the end of MAS execution */
    @Override
    public void stop() {
        super.stop();
    }
}

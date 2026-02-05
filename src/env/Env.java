package src.env;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import jason.NoValueException;
// Environment code for project robotsOnMars
import jason.asSyntax.*;
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
        final var rover = spawnIfMissing(agName);
        Optional<Action> actionToPerform = Optional.empty();
        if (action.equals(Lit.exploreAction)) {
            final var direction = mars.bestExploreDirections(rover).stream()
                    .findFirst()
                    .orElse(Direction.random());
            actionToPerform = Optional.of(new Action.Move(rover, direction));
        } else if (action.getFunctor().equals(Lit.believeCell.getFunctor())) {
            final var coord = Lit.toCoordinates(action.getTerm(0));
            final var terrain = Lit.toTerrain(action.getTerm(1));
            mars.roverBelievesCell(rover, coord, terrain);
        } else {
            logger.info("executing: " + action + ", but not implemented!");
        }

        actionToPerform.ifPresent(a ->

        {
            mars.performAction(a);
            informAgsEnvironmentChanged();
        });
        return true; // the action was executed with success
    }

    @Override
    public Collection<Literal> getPercepts(String agName) {
        final var rover = spawnIfMissing(agName);
        // final var canMovePercepts = mars.availableDirections(rover).stream()
        // .map(d -> switch (d) {
        // case Direction.Up() -> Lit.canMoveUp;
        // case Direction.Down() -> Lit.canMoveDown;
        // case Direction.Left() -> Lit.canMoveLeft;
        // case Direction.Right() -> Lit.canMoveRight;
        // })
        // .toList();

        final var cameraPercepts = mars.cameraRangeOf(rover).stream()
                .map(coord -> {
                    final Literal terrainLiteral = switch (mars.terrainAt(coord)) {
                        case Terrain.Empty() -> ASSyntax.createAtom("empty");
                        case Terrain.Obstacle() -> ASSyntax.createAtom("obstacle");
                        case Terrain.Sample() -> ASSyntax.createAtom("sample");
                        case Terrain.MiningSpot() -> ASSyntax.createAtom("miningSpot");
                        case Terrain.Base() -> ASSyntax.createAtom("base");
                    };
                    return ASSyntax.createLiteral(
                            "see",
                            ASSyntax.createLiteral("coord",
                                    ASSyntax.createNumber(coord.x()),
                                    ASSyntax.createNumber(coord.y())),
                            terrainLiteral);
                })
                .toList();

        final var inRangePercepts = Stream.concat(
                mars.reachableRovers(rover).stream().map(r -> r.name()),
                Stream.ofNullable(mars.canReachBase(rover) ? "base" : null))
                .map(name -> ASSyntax.createLiteral("inRange", ASSyntax.createLiteral(name)))
                .toList();

        // Flattening the lists
        return List.of(cameraPercepts, inRangePercepts).stream().flatMap(List::stream).toList();
    }

    private Rover spawnIfMissing(String agName) {
        return mars.rover(agName).orElseGet(() -> {
            final var rover = simpleRoverNamed(agName);
            mars.spawn(rover);
            return rover;
        });
    }

    /** Called before the end of MAS execution */
    @Override
    public void stop() {
        super.stop();
    }

    // Group literals
    private static class Lit {
        public static final Literal exploreAction = ASSyntax.createLiteral("exploreAction");
        // TODO: more vars, are they actually needed here?
        public static final Literal believeCell = ASSyntax.createLiteral("believeCell", ASSyntax.createVar());
        public static final Literal canMoveUp = ASSyntax.createLiteral("canMove(up)");
        public static final Literal canMoveDown = ASSyntax.createLiteral("canMove(down)");
        public static final Literal canMoveLeft = ASSyntax.createLiteral("canMove(left)");
        public static final Literal canMoveRight = ASSyntax.createLiteral("canMove(right)");

        public static Literal fromBool(boolean b) {
            return b ? Literal.LTrue : Literal.LFalse;
        }

        public static Terrain toTerrain(Term t) {
            return switch (t.toString()) {
                case "empty" -> new Terrain.Empty();
                case "obstacle" -> new Terrain.Obstacle();
                case "sample" -> new Terrain.Sample();
                case "miningSpot" -> new Terrain.MiningSpot();
                case "base" -> new Terrain.Base();
                default -> throw new IllegalArgumentException();
            };
        }

        public static Coordinates toCoordinates(Term t) {
            if (!(t instanceof Literal l) || !l.getFunctor().equals("coord")) {
                throw new IllegalArgumentException();
            }
            final var x = Lit.toInt(l.getTerm(0));
            final var y = Lit.toInt(l.getTerm(1));
            return new Coordinates(x, y);
        }

        public static int toInt(Term t) {
            try {
                return (int) ((NumberTerm) t).solve();
            } catch (NoValueException e) {
                throw new IllegalArgumentException();
            }
        }

        public static long toLong(Term t) {
            try {
                return (long) ((NumberTerm) t).solve();
            } catch (NoValueException e) {
                throw new IllegalArgumentException();
            }
        }
    }
}

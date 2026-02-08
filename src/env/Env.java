package src.env;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import jason.NoValueException;
import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;
import jason.environment.Environment;
import src.view.AppFrame;
import src.view.ViewModel;
import src.model.*;

public class Env extends Environment {
    private static Env instance;

    private Logger logger = Logger.getLogger("robotsOnMars." + Env.class.getName());
    private final int MARS_SIZE = 35;
    private final float MARS_OBSTACLES_DENSITY = 0.05f;
    private final float MARS_SAMPLES_DENSITY = 0.005f;
    private final float MARS_MINING_SPOTS_DENSITY = 0.01f;
    private final int MARS_BASE_SIZE = 3;
    private final int MARS_BASE_ANTENNA_RANGE = 7;
    private final int ROVER_BATTERY_CAPACITY = 100;
    private final int ROVER_CAMERA_RANGE = 3;
    private final int ROVER_ANTENNA_RANGE = 5;
    private final int SCIENTIST_SAMPLES_CAPACITY = 15;

    private Mars mars = new Mars(
            MARS_SIZE,
            MARS_OBSTACLES_DENSITY,
            MARS_SAMPLES_DENSITY,
            MARS_MINING_SPOTS_DENSITY,
            MARS_BASE_SIZE,
            MARS_BASE_ANTENNA_RANGE);

    private Rover simpleRoverNamed(String name) {
        return new SimpleRover(name, ROVER_BATTERY_CAPACITY, ROVER_BATTERY_CAPACITY, ROVER_CAMERA_RANGE,
                ROVER_ANTENNA_RANGE);
    }

    private Rover scientistRoverNamed(String name) {
        return new ScientistRover(name, ROVER_BATTERY_CAPACITY, ROVER_BATTERY_CAPACITY, ROVER_CAMERA_RANGE,
                ROVER_ANTENNA_RANGE,
                SCIENTIST_SAMPLES_CAPACITY);
    }

    /** Called before the MAS execution with the args informed in .mas2j */
    @Override
    public void init(String[] args) {
        super.init(args);
        Env.instance = this;
        try {
            addPercept(ASSyntax.parseLiteral("percept(" + args[0] + ")"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new AppFrame(new ViewModel(mars)).setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        assert !agName.equals("base");
        final var rover = spawnIfMissing(agName);
        Optional<Action> actionToPerform = Optional.empty();
        if (action.getFunctor().equals(Lit.saveCellAction.getFunctor())) {
            final var coord = Lit.toCoordinates(action.getTerm(0));
            final var terrain = Lit.toTerrain(action.getTerm(1));
            mars.updateMarsViewOf(rover, coord, terrain);
        } else if (action.equals(Lit.exploreAction)) {
            final var direction = mars.bestExploreDirections(rover).stream()
                    .findFirst()
                    .orElse(Direction.random());
            actionToPerform = Optional.of(new Action.Move(rover, direction));
        } else if (action.getFunctor().equals(Lit.moveAction.getFunctor())) {
            final var direction = Lit.toDirection(action.getTerm(0));
            actionToPerform = Optional.of(new Action.Move(rover, direction));
        } else if (action.getFunctor().equals(Lit.rechargeAction.getFunctor())) {
            actionToPerform = Optional.of(new Action.Recharge(rover));
        } else {
            logger.info("executing: " + action + ", but not implemented!");
        }

        informAgsEnvironmentChanged();
        return actionToPerform.stream()
                .map(a -> mars.performAction(a))
                .peek(r -> informAgsEnvironmentChanged())
                .findFirst()
                .orElse(true);
    }

    @Override
    public Collection<Literal> getPercepts(String agName) {
        final var percepts = new ArrayList<Literal>();
        if (agName.equals("base")) {
            percepts.addAll(mars.reachableRovers(mars.base()).stream().map(Rover::name).map(Lit::toInRange).toList());
        } else {
            final var rover = spawnIfMissing(agName);
            final var roverCoord = mars.roverCoordinates().get(rover);

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
                                Lit.fromCoordinates(coord),
                                terrainLiteral);
                    })
                    .toList();
            percepts.addAll(cameraPercepts);

            final var roversPercepts = mars.cameraRangeOf(rover).stream()
                    .filter(coord -> mars.roverAtCoordinates(coord).isPresent())
                    .map(coord -> ASSyntax.createLiteral("rover", Lit.fromCoordinates(coord)))
                    .toList();
            percepts.addAll(roversPercepts);

            percepts.add(ASSyntax.createLiteral("battery", ASSyntax.createNumber(rover.battery())));
            percepts.add(ASSyntax.createLiteral("batteryCapacity", ASSyntax.createNumber(rover.batteryCapacity())));

            final var distanceFromBase = roverCoord.distanceTo(mars.baseCenterCoordinates());
            percepts.add(ASSyntax.createLiteral("distanceFromBase", ASSyntax.createNumber(distanceFromBase)));

            percepts.add(ASSyntax.createLiteral("selfCoord", Lit.fromCoordinates(roverCoord)));
            percepts.add(ASSyntax.createLiteral("baseCoord", Lit.fromCoordinates(mars.baseCenterCoordinates())));

            percepts.addAll(mars.reachableRovers(rover).stream().map(Rover::name).map(Lit::toInRange).toList());
            if (mars.canReachBase(rover)) {
                percepts.add(Lit.toInRange("base"));
            }
        }
        return percepts;
    }

    private Rover spawnIfMissing(String agName) {
        return mars.rover(agName).orElseGet(() -> {
            final var rover = scientistRoverNamed(agName);
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
    public static class Lit {
        public static final Literal exploreAction = ASSyntax.createLiteral("exploreAction");
        public static final Literal moveAction = ASSyntax.createLiteral("move");
        public static final Literal rechargeAction = ASSyntax.createLiteral("recharge");
        public static final Literal saveCellAction = ASSyntax.createLiteral("saveCellAction");
        public static final Literal up = ASSyntax.createAtom("up");
        public static final Literal down = ASSyntax.createAtom("down");
        public static final Literal left = ASSyntax.createAtom("left");
        public static final Literal right = ASSyntax.createAtom("right");

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

        public static Literal toInRange(String name) {
            return ASSyntax.createLiteral("inRange", ASSyntax.createLiteral(name));
        }

        public static Literal fromCoordinates(Coordinates coord) {
            return ASSyntax.createLiteral("coord", ASSyntax.createNumber(coord.x()), ASSyntax.createNumber(coord.y()));
        }

        public static Direction toDirection(Term t) {
            return switch (t.toString()) {
                case "up" -> new Direction.Up();
                case "down" -> new Direction.Down();
                case "left" -> new Direction.Left();
                case "right" -> new Direction.Right();
                default -> throw new IllegalArgumentException(t.toString());
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

    public static Env instance() {
        return Env.instance;
    }

    public Mars mars() {
        return mars;
    }
}

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
    private Logger logger = Logger.getLogger("robotsOnMars." + Env.class.getName());
    private static Env instance;

    private Mars mars = new Mars(
            Config.MARS_SIZE,
            Config.MARS_OBSTACLES_DENSITY,
            Config.MARS_SAMPLES_DENSITY,
            Config.MARS_MINING_SPOTS_DENSITY,
            Config.MARS_BASE_SIZE,
            Config.MARS_BASE_ANTENNA_RANGE);

    private Rover simpleRoverNamed(String name) {
        return new SimpleRover(
                name,
                Config.ROVER_BATTERY_CAPACITY,
                Config.ROVER_BATTERY_CAPACITY,
                Config.ROVER_CAMERA_RANGE,
                Config.ROVER_ANTENNA_RANGE,
                Config.ROVER_MOVEMENT_ENERGY_COST,
                Config.ROVER_CHARGING_ENERGY_AMOUNT);
    }

    private Rover scientistRoverNamed(String name) {
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
        int timeToSleepMs = 0;
        if (action.getFunctor().equals(Lit.saveCellAction.getFunctor())) {
            final var coord = Lit.toCoordinates(action.getTerm(0));
            final var terrain = Lit.toTerrain(action.getTerm(1));
            mars.updateMarsViewOf(rover, coord, terrain);
        } else if (action.equals(Lit.exploreAction)) {
            actionToPerform = Optional.of(new Action.Explore(rover));
            timeToSleepMs = Config.MOVEMENT_DURATION_MS;
        } else if (action.getFunctor().equals(Lit.moveAction.getFunctor())) {
            final var direction = Lit.toDirection(action.getTerm(0));
            actionToPerform = Optional.of(new Action.Move(rover, direction));
            timeToSleepMs = Config.MOVEMENT_DURATION_MS;
        } else if (action.getFunctor().equals(Lit.rechargeAction.getFunctor())) {
            actionToPerform = Optional.of(new Action.Recharge(rover));
            timeToSleepMs = Config.RECHARGE_DURATION_MS;
        } else if (action.getFunctor().equals(Lit.mineSampleAction.getFunctor())) {
            final var coord = Lit.toCoordinates(action.getTerm(0));
            actionToPerform = Optional.of(new Action.MineSample((ScientistRover) rover, coord));
            timeToSleepMs = Config.MINE_SAMPLE_DURATION_MS;
        } else if (action.getFunctor().equals(Lit.collectSampleAction.getFunctor())) {
            final var coord = Lit.toCoordinates(action.getTerm(0));
            actionToPerform = Optional.of(new Action.CollectSample((ScientistRover) rover, coord));
            timeToSleepMs = Config.COLLECT_SAMPLE_DURATION_MS;
        } else if (action.getFunctor().equals(Lit.depositSamplesAction.getFunctor())) {
            actionToPerform = Optional.of(new Action.DepositSamples((ScientistRover) rover));
            timeToSleepMs = Config.DEPOSIT_SAMPLES_DURATION_MS;
        } else {
            logger.info("executing: " + action + ", but not implemented!");
        }
        final int timeToSleepMsFinal = timeToSleepMs;

        return actionToPerform.stream()
                .map(a -> {
                    try {
                        Thread.sleep(timeToSleepMsFinal);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return mars.performAction(a);
                })
                .peek(result -> informAgsEnvironmentChanged())
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

            if (mars.exploredEverywhere(rover)) {
                percepts.add(ASSyntax.createLiteral("exploredEverywhere"));
            }

            if (rover instanceof ScientistRover scientist) {
                percepts.add(ASSyntax.createLiteral("iAmAScientist"));
                percepts.add(
                        ASSyntax.createLiteral("collectedSamples", ASSyntax.createNumber(scientist.carriedSamples())));
                percepts.add(
                        ASSyntax.createLiteral("samplesCapacity", ASSyntax.createNumber(scientist.samplesCapacity())));
                percepts.add(
                        ASSyntax.createLiteral("miningBatteryCost",
                                ASSyntax.createNumber(scientist.miningSampleEnergyCost())));
            }
        }
        return percepts;
    }

    private Rover spawnIfMissing(String agName) {
        return mars.rover(agName).orElseGet(() -> {
            final var rover = agName.endsWith("S") ? scientistRoverNamed(agName) : simpleRoverNamed(agName);
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
        public static final Literal mineSampleAction = ASSyntax.createLiteral("mineSampleAction");
        public static final Literal collectSampleAction = ASSyntax.createLiteral("collectSampleAction");
        public static final Literal depositSamplesAction = ASSyntax.createLiteral("depositSamplesAction");
        public static final Literal up = ASSyntax.createAtom("up");
        public static final Literal down = ASSyntax.createAtom("down");
        public static final Literal left = ASSyntax.createAtom("left");
        public static final Literal right = ASSyntax.createAtom("right");

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

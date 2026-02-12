# Robots on Mars

![Demo video](rovers.gif)

On the left-hand side you can see all the roaming rovers and their properties.
By clicking on the "Focus" button you can make the GUI present only what that
rover actually knows.

On the right-hand side there's Mars, you can see:

- Rovers (scientists have slightly different sprites)
- Mining spots (are represented by a pickaxe)
- Samples (represented by test tubes)
- Obstacles (represented by sprites that looks more like piles of poop)
- Base (those gray squares in the middle)
- Unkown area (represented by gray cells)
- Antennas range (represented by a light blue overlay)

If you are focusing on a rover you will be able to see only what he sees/knows.
You will be able to see other rovers only if they are in of camera.

## Abstract

This project aims at creating a Jason MAS simulating the behaviour of multiple
robots exploing and collecting samples on Mars.

### Rovers

Some new rovers just arrived on Mars to explore the planet, their main goals
are:

- building a map of the area marking all points of interest
- collect samples of specific rocks which may need drilling the terrain
- transport samples at the main base where they will be stored long term

Each rover is equipped with:

- a 360° camera
- wheels
- short range communication antenna
- a battery

Some of the rovers are "scientists" and therefore they are also equipped with:

- drills for mining rocks samples
- robotic arms to collect samples
- limited sample storage

### Environment

The environment will be generated randomly and will be modeled as a grid where
each cell can represent:

- a rover
- flat empty surface
- rock obstacle
- mining spot
- sample to collect
- base

### Exploration

Rovers should prioritize mapping areas closer to the base.

They can only move one cell at a time following the cardinal directions. While
moving they also map the environment thanks to their cameras.

### Samples collection

Some samples are immediately collectable while others needs to be drilled.

Mining spots only need to be drilled once. Rovers are able to recognise spots
that were already drilled thanks to their cameras.

Each sample occupies the same amount of space in the storage.

### Energy management

- moving has a relatively low energy consumption
- mining rocks consumes a lot of energy
- rovers can charge their batteries at the main base

### Communication

There won't be any network infrastructure so the rovers will be able to exchange
informations only when they are in range of their antennas.

The base is also equipped with an antenna and can store data as well as the
rovers can.

## How to run it

```sh
jason robotsOnMars.mas2j
```

### Configuration

By editing the [robotsOnMars.mas2j](/robotsOnMars.mas2j) file you can specify
the number of robots to instantiate. Rover whose name ends with an "S" will be
scientists.

```
/* ... */
MAS robotsOnMars {

    environment: src.env.Env()

    agents:
        base base;

        curiosity rover;
        perseverance rover;
        sojournerS rover; /* Scientists ends with an "S" */

    aslSourcePath: "src/agt";
}
```

There are multiple configuration parameters that can be edited directly in
[Config.java](/src/model/Config.java)

```Java
public final class Config {
    public static final int MARS_SIZE = 35;
    public static final float MARS_OBSTACLES_DENSITY = 0.05f;
    public static final float MARS_SAMPLES_DENSITY = 0.005f;
    public static final float MARS_MINING_SPOTS_DENSITY = 0.01f;
    public static final int MARS_BASE_SIZE = 4;
    public static final int MARS_BASE_ANTENNA_RANGE = 7;
    public static final int ROVER_BATTERY_CAPACITY = 100;
    public static final int ROVER_CAMERA_RANGE = 3;
    public static final int ROVER_ANTENNA_RANGE = 5;
    public static final int ROVER_MOVEMENT_ENERGY_COST = 1;
    public static final int ROVER_CHARGING_ENERGY_AMOUNT = 10;
    public static final int SCIENTIST_SAMPLES_CAPACITY = 4;
    public static final int SCIENTIST_MINING_SAMPLE_ENERGY_COST = 10;
    public static final int MOVEMENT_DURATION_MS = 1000;
    public static final int RECHARGE_DURATION_MS = 2000;
    public static final int MINE_SAMPLE_DURATION_MS = 3000;
    public static final int COLLECT_SAMPLE_DURATION_MS = 2000;
    public static final int DEPOSIT_SAMPLES_DURATION_MS = 500;
    public static final double RANDOM_MOVEMENT_PROBABILITY = 0.1;
}
```

## Design

To ease the reader understanding we'll cover each main aspect presented in the
abstract separately, and in the end we'll put everything toghether.

### Knowledge representation

It's helpful to describe how some beliefs are represented in order to better
understand the following sections.

Rovers (as well as the base) will store information about Mars landscape in form
of "cells". Each cell has coordinates, the perceived terrain and a timestamp of
perception.

```
cell(coord(X, Y), Terrain, Timestamp)
// X and Y are integers
// Terrain is one of: empty, base, sample, miningSpot, obstacle
// Timestamp is the UNIX timestamp
```

Coordinates are always represented as `coord(X, Y)`

### Exploration

#### Exploration perception

While moving the rover is able to see around him and will perceive optical
information about the environment.

Each time the rover sees a cell it will update it's belief about that cell.
There's no need to check previous information about that cell since there cannot
be data more up to date than what the rover's actually seeing.

```
+see(C, Terrain) <- +cell(C, Terrain, system.time).
// C represents the coordinates
```

> **Note:**
>
> The reason why we do not remove previous data about the cell is that cells are
> not actually stored as pure beliefs for performance reasons, a complete
> explanation can be found next.

#### Exploration movement

Exploration is implemented following a simplified Motor-Schemas control
architecture.

Imagine that the rover is immersed in a potential field that attracts him
towards unexplored regions. Unexplored regions that are closer to the rover
attract him with a stronger force.

This approach result in a behaviour that is not segmented and therefore very
resilient and flexible.

Eventually there will not be unexplored areas close to the base. This behaviour
emerges as rovers will eventually go back to base to recharge and once charged
up they will be attracted to those unexplored areas near the base.

```
+!explore : not(theresScienceToDo) <-
    exploreAction.
+!explore.
-!explore.
```

Computing this kind of behaviour is not trivial and therefore it was decided to
implement it Java-side (exploreAction).

```Java
// This function produces the best direction for the rover to explore
Optional<Direction> bestExploreDirection(Rover rover) {
    final var roverCoord = roverCoordinates().get(rover);
    final var knownCoord = rover.marsView().knownTerrain().keySet();
    final Function<Coordinates, Boolean> isBorder = coord -> coord.neighbours().stream()
            .filter(n -> isInsideBounds(n))
            .map(c -> knownCoord.contains(c))
            .anyMatch(hasKnownNeighbour -> hasKnownNeighbour);

    // Computing frontier unknown coordinates/cells
    final var unknownCoord = allCoordinates.stream()
            .filter(c -> !knownCoord.contains(c) && isBorder.apply(c))
            .toList();

    // Each of those coordinates/cells will attract the rover.
    // The final force is computed by summing all forces.
    final var vector = unknownCoord.stream()
            .map(c -> {
                // Closer cells will attract the rover with a stronger force (1/d)
                final double weight = 1 / (c.distanceTo(roverCoord) - rover.cameraRange());
                final var versor = c.minus(roverCoord).toVector().versor();
                return versor.mult(weight);
            })
            .collect(Collectors.reducing(new V2D(0, 0), (v1, v2) -> v1.plus(v2)));

    // We take all available directions (no obstacles/rovers, inside bounds, ecc..)
    // and sort them by how much they align with the force vector (dot product)
    return availableDirections(rover).stream()
            .map(d -> Tuple.of(d, d.toVector().dot(vector)))
            .sorted(Comparator.<Tuple<Direction, Double>>comparingDouble(t -> t._2()).reversed())
            .map(t -> t._1())
            .findFirst();
}
```

We also introduce a bit of randomness (which is parametrized) that helps:

- exiting possible local minima (unstable equilibrium)
- reducing the occurrencies of robots exploring close to each other

```Java
boolean explore(Rover rover) {
    Optional<Direction> direction;
    if (random.nextDouble() <= Config.RANDOM_MOVEMENT_PROBABILITY) {
        final var availableDirections = new ArrayList<>(availableDirections(rover));
        Collections.shuffle(availableDirections, random);
        direction = availableDirections.stream().findFirst();
    } else {
        direction = bestExploreDirection(rover);
    }
    return moveRover(rover, direction.orElse(Direction.random()));
}
```

### Communication

Communication is intuitively straightforward, rovers (as well as the base) will
immediately transfer all their knowledge as soon as they get in contact with
another rover (or base).

If the two entities exchanging knowledge keep in contact for a while they will
keep exchanging knowledge at intervals. Intervals can be set but in order to
avoid heavy computational loads to happens at the same time we add a random
delay between 0 and 1 seconds on top of a base delay of 1 second.

```
+inRange(R) <- !sendKnowledge(R).

// If i'm in range with R i will send him knowledge and reschedule sendKnowledge
// in case we keep staying in range for some time.
+!sendKnowledge(R) : inRange(R) <-
    ?allCells(Cells);
    .send(R, achieve, mergeMarsView(Cells));
    // Reschedule plan
    .random(Rng);
    .wait(Rng * 1000);
    .wait(1000);
    !!sendKnowledge(R).
+!sendKnowledge(R).
```

When rovers receive knowledge they'll have to merge it with their current one,
this is done by comparing cells timestamps and keeping the newest data.

```
// >>>>>>>>>> IDIOMATIC IMPLEMENTATION <<<<<<<<<<
+!mergeMarsView([Cell | Tail]) <-
    !updateCellIfNewer(Cell);
    !mergeMarsView(Tail).
+!mergeMarsView([]).

// If i have newer data about that cell i will do nothing.
+!updateCellIfNewer(cell(Coord, Terrain, Timestamp)) :
    cell(Coord, _, Timestamp2) & Timestamp <= Timestamp2.

// Otherwise i will update my knowledge about that cell.
+!updateCellIfNewer(cell(Coord, Terrain, Timestamp)) <-
    +cell(Coord, Terrain, Timestamp).

// >>>>>>>>>> ACTUAL IMPLEMENTATION <<<<<<<<<<
+!mergeMarsView(Cells) <-
    !cellMap(M);
    src.agt.MergeKnowledgeAction(M, Cells).
```

> **Note:**
>
> The O(N^2) complexity issue is handled by internally using a map (explained
> later). But still iterating over a large list of cells remains a heavy task
> since we need to scan the belief base at every iteration step.
>
> The actual implementation uses an internal action which allows to offload the
> iteration from the Jason engine making it much faster.

#### Emergent behaviour

Even if rovers start spreading a lot without getting in contact they will
eventually go back to the base to recharge and store there their knowledge which
will now available for other rovers that will reach the base.

### Battery management

The only actual catastrophic failure that can happen in the system is having a
rover running out of battery. Therefore battery management is very important and
will subsume every other behaviour if needed (explained in the section about
putting all the behaviours toghether).

```
batteryLow :- .intend(charge).
batteryLow :-
    selfCoord(Pos) &
    baseCoord(Base) &
    estimateBatteryUsage(Pos, Base, E) &
    battery(B) &
    batterySafetyReserve(S) &
    B <= E + S.

+battery(B) : batteryLow & not(.intend(charge)) <-
    .drop_desire(loop);
    .drop_desire(fastDeposit);
    .print("Going to base to charge");
    !charge;
    !!fastDeposit;
    !!loop.
+battery(0) <-
    .drop_all_desires;
    .print("Ran out of battery :(").

+!charge <-
    !goToBase;
    .print("Charging...");
    !rechargeFully;
    .print("Completed charging").
+!charge.

+!rechargeFully : battery(B) & batteryCapacity(C) & B < C <-
    recharge;
    !rechargeFully.
+!rechargeFully.

// ...

// Estimating the amount of energy needed to go From a position To another.
// Worst case scenario the rover will have to walk the hypotenuse of an isosceles right triangle
// having leg equals to X. And the rover will have to travel X*2 cells to reach the base.
estimateBatteryUsage(From, To, math.sqrt((D*D) / 2) * 2) :- distance(From, To, D).
```

Basically the rover will decide to go back to the base once it's battery reaches
a dinamically computed treshold. This treshold is computed by estimating the
amount of energy needed to go back to the base plus a safety margin.

The estimate of how much energy is needed to go from one place to another is
based on a pessimistic assumption. The worst case in which the rover can be is
when positioned diagonally with respect to the target coordinates (because
rovers cannot move diagonally). This means that the number of cells he'll have
to walk is the sum of the two legs of an isosceles right triangle which given
the distance (hypotenuse) D can be computed as `sqrt((D^2) / 2) * 2`.

As soon as the rover reaches this treshold it will immediately go back to base
and charge fully. The `goToBase` still introduce a just bit of randomness to
reduce the probability of the rover getting stuck.

### Samples collection

The rover will go to the nearest cell having a mining spot or a sample to
collect.

Actually distance is not the only thing taken into consideration, in fact the
rover estimates whether it has enough battery to drill or pickup the sample and
to go back to the base safely.

Each code segment is explained with comments.

```
// There's science work to do right next to me, i'll do it
+!science : theresScienceToDo & bestScienceWork(cell(Coord, Terr, TS)) & selfCoord(Pos) & adjacent(Pos, Coord) <-
    !doScienceWork(cell(Coord, Terr, TS)).
// There's science work to do i'll move towards it
+!science : theresScienceToDo & bestScienceWork(cell(Coord, Terr, TS)) <-
    !moveTowards(Coord).
// There's no science work to do right now.
+!science.
-!science.

theresScienceToDo :- hasSpaceForSample & bestScienceWork(_).

bestScienceWork(Cell) :-
    scienceWorkAt(Cells) &
    selectScienceWork(Cell, Cells).

// Given a list of cells we choose the first one
selectScienceWork(cell(Coord, Terr, TS), [cell(Coord, Terr, TS) | T]) :-
    scienceBatteryCost(Terr, Cost) &
    selfCoord(Pos) &
    baseCoord(Base) &
    estimateBatteryUsage(Pos, Coord, GoEnergy) &
    estimateBatteryUsage(Coord, Base, ReturnEnergy) &
    battery(B) &
    batterySafetyReserve(Reserve) &
    GoEnergy + Cost + ReturnEnergy + Reserve <= B.
selectScienceWork(Cell, [_ | T]) :- selectScienceWork(Cell, T).

// Here we choose the appropriate action to perform wether there's a sample or a mining spot
+!doScienceWork(cell(Coord, sample, _)) : hasSpaceForSample <-
    collectSampleAction(Coord).
+!doScienceWork(cell(Coord, miningSpot, _)) <-
    mineSampleAction(Coord).

// Check that theres at least one slot for new samples
hasSpaceForSample :- collectedSamples(S) & samplesCapacity(C) & S < C.

scienceBatteryCost(miningSpot, Cost) :- miningBatteryCost(Cost).
scienceBatteryCost(sample, 0).

// Find all coordinates for which there's science work to do and sort them by distance
scienceWorkAt(WorkCells) :-
    selfCoord(Pos) &
    allCells(Cells) &
    .findall(tuple(D, cell(C, Terr, TS)), .member(cell(C, Terr, TS), Cells) & (Terr == miningSpot | Terr == sample) & distance(Pos, C, D), Works) &
    .sort(Works, Sorted) &
    extractSecondFromTuple(Sorted, WorkCells).

extractSecondFromTuple([], []).
extractSecondFromTuple([tuple(A, B) | Tail], [B | Rest]) :-
    extractSecondFromTuple(Tail, Rest).
```

### Sample deposit

Every time the rover picks up a sample it will reactively check if it has
reached its maximum carrying capaticty. If so it will go back to the base just
to deposit those samples.

```
+collectedSamples(_) : not(.intend(deposit)) & not(batteryLow) & not(hasSpaceForSample) <-
    .drop_desire(loop);
    .print("Going to base to deposit samples");
    !deposit;
    !!loop.

+!deposit : not(.intend(deposit)) & not(inBase) <-
    ?baseCoord(Base);
    !moveTowards(Base);
    !deposit.
+!deposit : not(.intend(deposit)) & inBase <-
    depositSamplesAction;
    .print("Samples deposited").
+!deposit.
-!deposit.
```

#### "Fast" deposit

It is reasonable that if the rover happens to reach the base even when not full
of samples it should deposit them anyway (i call it "fast" deposit).

Fast deposit is a goal itself and will continously check if the rover happens to
be on the base. Since the plan is concurrent with the reactive counterpart it is
necessary to make `deposit` a singleton plan.

```
/* Initial goals */

!fastDeposit.

// ...

+!fastDeposit : not(.intend(deposit)) & not(batteryLow) & inBase & collectedSamples(S) & S > 0 <-
    .drop_desire(loop);
    !deposit;
    !!fastDeposit;
    !!loop.
+!fastDeposit <- !!fastDeposit.
```

### Putting various goals toghether

We have some goals that can be executed concurrently while other cannot.

Since most of the goals result in the robot moving they need to be exclusive:

- exploring
- collecting science
- depositing samples
- charging

The only goal that can get some benefits in a concurrent execution if the "fast"
deposit.

Given these conditions we've structured the non-concurrent goals like a
main-loop architecture, while fastDeposit is a goal on its own:

```
!loop.
!fastDeposit.

+!loop : iAmAScientist <-
    !science;
    !explore;
    !!loop.
+!loop <-
    !explore;
    !!loop.
```

This type of architecture work as long as `science` and `explore` plans execute
short lived task that we can see as "atomic".

Battery management and samples deposit are triggered reactively, they drop the
loop desire, do their stuff and then resume it. (They can be seen like
"interrupts")

Again the fact that they are reactively triggered requires careful handling and
checking that the relative plans are not already being executed.

```
batteryLow :- .intend(charge).
batteryLow :-
    selfCoord(Pos) &
    baseCoord(Base) &
    estimateBatteryUsage(Pos, Base, E) &
    battery(B) &
    batterySafetyReserve(S) &
    B <= E + S.

+battery(B) : batteryLow & not(.intend(charge)) <-
    .drop_desire(loop);
    .drop_desire(fastDeposit);
    .print("Going to base to charge");
    !charge;
    !!fastDeposit;
    !!loop.

// ...

+collectedSamples(_) : not(.intend(deposit)) & not(batteryLow) & not(hasSpaceForSample) <-
    .drop_desire(loop);
    .print("Going to base to deposit samples");
    !deposit;
    !!loop.
```

### Critical performance optimization technique

Rovers will in general store huge amount of cells, not less than thousands with
decent size maps. This means that storing each cell as a belief would rapidly
make incredibly slow each scan of the belief base.

In order to fix this we've used a single Map storing cells data, this makes
access constant instead of linear.

```
// The Map is initialized lazilly
+!cellMap(M) : cellMapInstance(M).
@[atomic]
+!cellMap(M) <-
    .map.create(M);
    +cellMapInstance(M).

// Writer proxy on the map
+cell(Coord, Terrain, Timestamp) <-
    !cellMap(M);
    saveCellAction(Coord, Terrain, Timestamp);
    .map.put(M, Coord, data(Terrain, Timestamp));
    -cell(Coord, Terrain, Timestamp).

// Reader proxy on the map
cell(Coord, Terrain, Timestamp) :-
    cellMapInstance(M) &
    .map.get(M, Coord, data(Terrain, Timestamp)).

allCells(Cells) :-
    cellMapInstance(M) &
    .findall(cell(Coord, Terrain, TS), .map.key(M, Coord) & .map.get(M, Coord, data(Terrain, TS)), Cells).
allCells([]).
```

The `saveCellAction` is an action that is used by the rover just to keep the
model in sync with its belief base. This is done in order to allow the GUI to
show precisely what's the current knowledge of each rover.

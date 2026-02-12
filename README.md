# Robots on Mars

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
+!explore : can(explore) & not(exploredEverywhere) <-
    exploreAction.
+!explore.
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
needToCharge :- // ... other conditions explained later
needToCharge :- // ... other conditions explained later
needToCharge :-
    selfCoord(Pos) &
    baseCoord(Base) &
    battery(B) &
    estimateBatteryUsage(Pos, Base, E) &
    batterySafetyReserve(S) &
    B <= E + S.

+!checkBattery : needToCharge <-
    !charge;
    !!checkBattery.
+!checkBattery <- !!checkBattery.

+battery(0) <-
    .drop_all_desires;
    .print("Ran out of battery :(").

+!charge <-
    !goToBase;
    !rechargeFully.

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

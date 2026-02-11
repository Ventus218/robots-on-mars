/* Initial beliefs and rules */
batterySafetyReserve(10).
can(explore) :- not(needToCharge) & not(.intend(explore)) & not(iAmAScientist).
can(explore) :- not(needToCharge) & not(.intend(explore)) & hasSpaceForSample & not(theresScienceToDo).
can(science) :- not(needToCharge) & not(.intend(science)) & hasSpaceForSample.
can(deposit) :- not(needToCharge) & not(.intend(deposit)).

// Think about this as a belief, it's just a way to initialize it as soon as it's needed.
+!cellMap(M) : cellMapInstance(M).
@[atomic]
+!cellMap(M) <-
    .map.create(M);
    +cellMapInstance(M).

+!saveCell(Coord, Terrain, Timestamp) <-
    !cellMap(M);
    saveCellAction(Coord, Terrain, Timestamp);
    .map.put(M, Coord, data(Terrain, Timestamp)).

cell(Coord, Terrain, Timestamp) :-
    cellMapInstance(M) &
    .map.get(M, Coord, data(Terrain, Timestamp)).

allCells(Cells) :-
    cellMapInstance(M) & 
    .findall(cell(Coord, Terrain, TS), .map.key(M, Coord) & .map.get(M, Coord, data(Terrain, TS)), Cells).
allCells([]).

/* Initial goals */

!init.

/* Plans */

+!init : iAmAScientist <- 
    !!checkBattery;
    !!checkDeposit;
    !!checkScience;
    !!checkExplore.
+!init <- 
    !!checkBattery;
    !!checkExplore.

// >>>>>>>>>> EXPLORE SECTION <<<<<<<<<<

+!checkExplore : can(explore) <- 
    !explore;
    !!checkExplore.
+!checkExplore <- !!checkExplore.

+!explore : can(explore) & not(exploredEverywhere) <-
    .print("exploring");
    exploreAction.
+!explore.
-!explore.

+see(C, Terrain) <-
    !saveCell(C, Terrain, system.time).

// >>>>>>>>>> BATTERY SECTION <<<<<<<<<<
needToCharge :- .intend(charge).
needToCharge :- exploredEverywhere & not(theresScienceToDo) & battery(B) & batteryCapacity(C) & B < C.
needToCharge :- 
    selfCoord(Pos) & 
    baseCoord(Base) & 
    battery(B) &
    estimateBatteryUsage(Pos, Base, E) & 
    batterySafetyReserve(S) & 
    B <= E + S.

+!checkBattery : needToCharge <-
    .print("Going to base to charge");
    !charge;
    .print("Completed charging");
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

// >>>>>>>>>> SCIENCE SECTION <<<<<<<<<<
// If we are here we assume that the rover is a Scientist

+!checkDeposit : can(deposit) <-
    !deposit;
    !!checkDeposit.
+!checkDeposit <- !!checkDeposit.

+!deposit : can(deposit) & not(inBase) & not(hasSpaceForSample) <- 
    ?baseCoord(Base);
    !moveTowards(Base);
    !deposit.
+!deposit : can(deposit) & inBase & collectedSamples(S) & S > 0 <- 
    depositSamplesAction;
    !deposit.
+!deposit.
-!deposit.

+!checkScience : can(science) & theresScienceToDo <- 
    !science;
    !!checkScience.
+!checkScience <- !!checkScience.

// There's science work to do right next to me, i'll do it
+!science : can(science) & bestScienceWork(cell(Coord, Terr, TS)) & selfCoord(Pos) & adjacent(Pos, Coord) <-
    !doScienceWork(cell(Coord, Terr, TS));
    .wait(10); //perceive does not work;
    !science.
// There's science work to do i'll move towards it
+!science : can(science) & bestScienceWork(cell(Coord, Terr, TS)) <-
    !moveTowards(Coord);
    !science.
// There's no science work to do right now.
+!science.
-!science.

theresScienceToDo :- .intend(science).
theresScienceToDo :- bestScienceWork(_).

bestScienceWork(Cell) :- 
    scienceWorkAt(Cells) &
    selectScienceWork(Cell, Cells).

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

+!doScienceWork(cell(Coord, sample, _)) : hasSpaceForSample <-
    collectSampleAction(Coord).
+!doScienceWork(cell(Coord, miningSpot, _)) <-
    mineSampleAction(Coord).

hasSpaceForSample :- collectedSamples(S) & samplesCapacity(C) & S < C.

// Estimating the amount of energy needed to go From a position To another.
// Worst case scenario the rover will have to walk the hypotenuse of an isosceles right triangle
// having leg equals to X. And the rover will have to travel X*2 cells to reach the base.
estimateBatteryUsage(From, To, math.sqrt((D*D) / 2) * 2) :- distance(From, To, D).

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

// >>>>>>>>>> EXCHANGING KNOWLEDGE SECTION <<<<<<<<<<
// As soon as i get in range with R i will send him my knowledge.
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

// +!mergeMarsView([Cell | Tail]) <-
//     !updateCellIfNewer(Cell);
//     !mergeMarsView(Tail).
// +!mergeMarsView([]).
// // If i have newer data about that cell i will do nothing.
// +!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) : cell(coord(X, Y), _, Timestamp2) & Timestamp <= Timestamp2.
// // Otherwise i will update my knowledge about that cell.
// +!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) <-
//     !saveCell(coord(X, Y), Terrain, Timestamp).

+!mergeMarsView(Cells) <-
    !cellMap(M);
    src.agt.MergeKnowledgeAction(M, Cells).

// >>>>>>>>>> UTILITIES SECTION <<<<<<<<<<

+!goToBase : not(inBase) <-
    ?baseCoord(Base);
    !moveTowards(Base);
    !goToBase.
+!goToBase.

inBase :- selfCoord(Pos) & cell(Pos, base, _).

+!goAdjacentTo(Dest) : selfCoord(Pos) & adjacent(Pos, Dest).
+!goAdjacentTo(Dest) <-
    !moveTowards(Dest);
    !goAdjacentTo(Dest).

// Perform one movement towards the given destination
+!moveTowards(Dest) : selfCoord(Dest).
// If i'm not adjacent to Dest i will perform a random movement with a probability of 1/10
+!moveTowards(Dest) : selfCoord(Pos) & not(adjacent(Pos, Dest)) & .random(R) & R <= 0.1 <-
    .findall(Dir, availableDirections(Dir), Dirs);
    .random(Dirs, Dir);
    !safeMove(Dir).
// Decides what's the best direction to follow to reach Dest and moves to it
+!moveTowards(Dest) <-
    ?selfCoord(Pos);
    .findall(tuple(D, Dir), availableDirections(Dir) & applyDir(Pos, Dir, C) & distance(C, Dest, D), DirsWithDistanceToDest);
    .min(DirsWithDistanceToDest, tuple(_, Dir));
    !safeMove(Dir).

+!safeMove(Dir) <- move(Dir).
-!safeMove(Dir).

canMove(Dir) :- selfCoord(Pos) & applyDir(Pos, Dir, C) & walkable(C).

availableDirections(Dir) :- direction(Dir) & canMove(Dir).

direction(up).
direction(down).
direction(left).
direction(right).

applyDir(coord(X, Y), up, coord(X, Y + 1)).
applyDir(coord(X, Y), down, coord(X, Y - 1)).
applyDir(coord(X, Y), left, coord(X - 1, Y)).
applyDir(coord(X, Y), right, coord(X + 1, Y)).

walkable(C) :- cell(C, base, _) & not(rover(C)).
walkable(C) :- cell(C, empty, _) & not(rover(C)).
walkable(C) :- cell(C, sample, _) & not(rover(C)).

adjacent(C1, C2) :- distance(C1, C2, D) & D < 2.

distance(coord(X1, Y1), coord(X2, Y2), math.sqrt((X1 - X2) ** 2 + (Y1 - Y2) ** 2)).
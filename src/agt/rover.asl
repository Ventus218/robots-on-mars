/* Initial beliefs and rules */
batterySafetyReserve(10).

// Think about this as a belief, it's just a way to initialize it as soon as it's needed.
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

/* Initial goals */

!loop.
!fastDeposit.

/* Plans */

+!loop : iAmAScientist <- 
    !science;
    !explore;
    !!loop.
+!loop <- 
    !explore;
    !!loop.

// >>>>>>>>>> EXPLORE SECTION <<<<<<<<<<
+!explore : not(theresScienceToDo) <-
    exploreAction.
+!explore.
-!explore.

+see(C, Terrain) <-
    +cell(C, Terrain, system.time).

batteryLow :- .intend(charge).
batteryLow :-
    selfCoord(Pos) & 
    baseCoord(Base) & 
    estimateBatteryUsage(Pos, Base, E) & 
    battery(B) &
    batterySafetyReserve(S) & 
    B <= E + S.

// >>>>>>>>>> BATTERY SECTION <<<<<<<<<<
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

// >>>>>>>>>> SCIENCE SECTION <<<<<<<<<<
// If we are here we assume that the rover is a Scientist

+!fastDeposit : not(.intend(deposit)) & not(batteryLow) & inBase & collectedSamples(S) & S > 0 <- 
    .drop_desire(loop);
    !deposit;
    !!fastDeposit;
    !!loop.
+!fastDeposit <- !!fastDeposit.

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

// There's science work to do right next to me, i'll do it
+!science : theresScienceToDo & bestScienceWork(cell(Coord, Terr, TS)) & selfCoord(Pos) & adjacent(Pos, Coord) <-
    !doScienceWork(cell(Coord, Terr, TS));
    // if (Terr == sample) {
    //     .wait(cell(Coord, empty, _));
    // } else {
    //     .wait(cell(Coord, sample, _));
    // }.
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
//     +cell(coord(X, Y), Terrain, Timestamp).

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
// If i'm not adjacent to Dest i will perform a random movement according to the set probability
+!moveTowards(Dest) : selfCoord(Pos) & not(adjacent(Pos, Dest)) & randomMovementProbability(P) & .random(R) & R <= P <-
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
// Agent alice in project robotsOnMars

/* Initial beliefs and rules */
movementSpeedMs(200).
rechargeSpeedMs(2000).

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

+!init <-
    !!start.

+!start <- 
    !!explore.

+!explore <- 
    exploreAction;
    ?movementSpeedMs(S);
    .wait(S);
    !!explore.
-!explore <- !!explore.

+see(C, Terrain) <-
    !saveCell(C, Terrain, system.time).

// >>>>>>>>>> BATTERY SECTION <<<<<<<<<<
// Estimating the amount of energy needed to go back to the base plus a safety energy reserve.
// Worst case scenario the rover will have to walk the hypotenuse of an isosceles right triangle
// having leg equals to X. And the rover will have to travel X*2 cells to reach the base.
+battery(B) : not(batteryLow) & distanceFromBase(D) & B <= (math.sqrt((D*D) / 2) * 2) + 10 <-
    +batteryLow;
    .drop_intention(explore);
    ?baseCoord(Dest);
    !goToBase;
    !rechargeFully;
    -batteryLow;
    !!start.
+battery(0) <-
    .drop_all_intentions;
    .print("Ran out of battery :(").

+!rechargeFully : battery(B) & batteryCapacity(C) & B < C <-
    ?rechargeSpeedMs(S);
    .wait(S);
    recharge;
    !rechargeFully.
+!rechargeFully.

// >>>>>>>>>> EXCHANGING KNOWLEDGE SECTION <<<<<<<<<<
// As soon as i get in range with R i will send him my knowledge.
+inRange(R) <- !sendKnowledge(R).

// If i'm in range with R i will send him knowledge and reschedule sendKnowledge
// in case we keep staying in range for some time.
+!sendKnowledge(R) : inRange(R) <-
    ?allCells(Cells);
    .send(R, achieve, mergeMarsView(Cells));
    // Reschedule plan
    .wait(1000);
    !!sendKnowledge(R).
+!sendKnowledge(R).

+!mergeMarsView([Cell | Tail]) <-
    !updateCellIfNewer(Cell);
    !mergeMarsView(Tail).
+!mergeMarsView([]).

// If i have newer data about that cell i will do nothing.
+!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) : cell(coord(X, Y), _, Timestamp2) & Timestamp <= Timestamp2.
// Otherwise i will update my knowledge about that cell.
+!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) <-
    !saveCell(coord(X, Y), Terrain, Timestamp).

// >>>>>>>>>> UTILITIES SECTION <<<<<<<<<<

// This is less strict than goTowards as it just requires the rover to reach the base and not a specific coord
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

// Goes to the given destination or to an adjacent cells if the destination is not walkable
+!goTowards(Dest) : selfCoord(Dest).
+!goTowards(Dest) : selfCoord(Pos) & adjacent(Pos, Dest) & not(walkable(Dest)).
+!goTowards(Dest) <-
    !goAdjacentTo(Dest);
    !moveTowards(Dest).

// Perform one movement towards the given destination
+!moveTowards(Dest) : selfCoord(Dest).
+!moveTowards(Dest) <-
    ?selfCoord(Pos);
    .findall(tuple(D, Dir), direction(Dir) & canMove(Dir) & applyDir(Pos, Dir, C) & distance(C, Dest, D), AvailableDirs);
    .min(AvailableDirs, tuple(_, Dir));
    ?movementSpeedMs(S);
    .wait(S);
    !safeMove(Dir).

+!safeMove(Dir) <- move(Dir).
-!safeMove(Dir).

canMove(Dir) :- selfCoord(Pos) & applyDir(Pos, Dir, C) & walkable(C).

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
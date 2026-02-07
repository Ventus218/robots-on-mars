// Agent alice in project robotsOnMars

/* Initial beliefs and rules */
movementSpeedMs(200).

/* Initial goals */

!explore.

/* Plans */

+!explore <- 
    exploreAction;
    ?movementSpeedMs(S);
    .wait(S);
    !!explore.
-!explore <- .print("failed explore").

+see(coord(X, Y), Terrain) <-
    -cell(coord(X, Y), _, _);
    +cell(coord(X, Y), Terrain, system.time).

// >>>>>>>>>> BATTERY SECTION <<<<<<<<<<
// Worst case scenario we have an isosceles right triangle where x is the leg 
// And the rover will have to travel x*2 cells to reach the base
// D = sqrt((x^2) * 2)
// D^2 = (x^2) * 2
// (D^2) / 2 = x^2
// sqrt((D^2) / 2) = x
+battery(B) : not(returningToBase) & distanceFromBase(D) & B <= (math.sqrt((D*D) / 2) * 2) + 10 <-
    +returningToBase;
    .drop_intention(explore);
    .print("emergency, battery low and away from base");
    !!returnToBase.

+!returnToBase <-
    ?baseCoord(B);
    !goTowards(B).


// >>>>>>>>>> EXCHANGING KNOWLEDGE SECTION <<<<<<<<<<
// As soon as i get in range with R i will send him my knowledge.
+inRange(R) <- !sendKnowledge(R).

// If i'm in range with R i will send him knowledge and reschedule sendKnowledge
// in case we keep staying in range for some time.
+!sendKnowledge(R) : inRange(R) <-
    .print("Sending knowledge to ", R);
    .findall(cell(coord(X, Y), Terrain, TS), cell(coord(X, Y), Terrain, TS), Cells);
    .send(R, achieve, mergeMarsView(Cells));
    // Reschedule plan
    .wait(1000);
    !!sendKnowledge(R).
+!sendKnowledge(R).
-!sendKnowledge <- .print("failed sendKnowledge").

+!mergeMarsView([Cell | Tail]) <-
    !updateCellIfNewer(Cell);
    !mergeMarsView(Tail).
+!mergeMarsView([]).
-!mergeMarsView <- .print("failed mergeMarsView").

// If i have newer data about that cell i will do nothing.
+!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) : cell(coord(X, Y), _, Timestamp2) & Timestamp <= Timestamp2.
// Otherwise i will update my knowledge about that cell.
+!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) <-
    -cell(coord(X, Y), _, _);
    +cell(coord(X, Y), Terrain, Timestamp).
-!updateCellIfNewer <- .print("failed updateCellIfNewer").

// >>>>>>>>>> UTILITIES SECTION <<<<<<<<<<
+!goTowards(Dest) : selfCoord(Dest) <-
    .print("Arrived at destination!").
+!goTowards(Dest) : selfCoord(Pos) & adjacent(Pos, Dest) & not(walkable(Dest))  <-
    .print("Arrived at destination!").
+!goTowards(Dest) : selfCoord(Pos) <-
    .findall(tuple(D, Dir), direction(Dir) & canMove(Dir) & applyDir(Pos, Dir, C) & distance(C, Dest, D), AvailableDirs);
    .min(AvailableDirs, tuple(_, Dir));
    move(Dir);
    ?movementSpeedMs(S);
    .wait(S);
    !!goTowards(Dest).

canMove(Dir) :- selfCoord(Pos) & applyDir(Pos, Dir, C) & walkable(C).

direction(up).
direction(down).
direction(left).
direction(right).

applyDir(coord(X, Y), up, coord(X, Y + 1)).
applyDir(coord(X, Y), down, coord(X, Y - 1)).
applyDir(coord(X, Y), left, coord(X - 1, Y)).
applyDir(coord(X, Y), right, coord(X + 1, Y)).

walkable(C) :- cell(C, base, _).
walkable(C) :- cell(C, empty, _).
walkable(C) :- cell(C, sample, _).

adjacent(C1, C2) :- distance(C1, C2, D) & D < 2.

distance(coord(X1, Y1), coord(X2, Y2), math.sqrt((X1 - X2) ** 2 + (Y1 - Y2) ** 2)).
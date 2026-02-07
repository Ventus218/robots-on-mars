// Agent alice in project robotsOnMars

/* Initial beliefs and rules */

/* Initial goals */

!explore.

/* Plans */

+!explore <- 
    exploreAction;
    .wait(200);
    !!explore.
-!explore <- .print("failed explore").

+see(coord(X, Y), Terrain) <-
    -cell(coord(X, Y), _, _);
    +cell(coord(X, Y), Terrain, system.time).

// >>>>>>>>>> EXCHANGING KNOWLEDGE SECTION <<<<<<<<<<
// Constantly trying to exchange knowledge with neighbours.
// The term represents the minimum interval at which re-sending knowledge
!exchangeKnowledge(1000).

// exchangeKnowledge MinInterval time has passed and i am in range with R.
// I will send knowledge, wait for the MinInterval and re-instantiate goal.
+!exchangeKnowledge(MinInterval) : inRange(R) <-
    !sendKnowledge(R);
    .wait(MinInterval);
    !!exchangeKnowledge(MinInterval).
// exchangeKnowledge MinInterval time has passed but i'm in range with nobody.
// I will wait for the MinInterval and re-instantiate goal.
+!exchangeKnowledge(MinInterval) <-
    .wait(MinInterval);
    !!exchangeKnowledge(MinInterval).

// As soon as i get in range of R i will send my knowledge to him
+inRange(R) <- !sendKnowledge(R).

+!sendKnowledge(R) <-
    .print("Sending knowledge to ", R);
    .findall(cell(coord(X, Y), Terrain, TS), cell(coord(X, Y), Terrain, TS), Cells);
    .send(R, achieve, mergeMarsView(Cells)).

+!mergeMarsView([]).
+!mergeMarsView([Cell | Tail]) <-
    !updateCellIfNewer(Cell);
    !mergeMarsView(Tail).
-!mergeMarsView <- .print("failed mergeMarsView").

// If i have newer data about that cell i will do nothing.
+!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) : cell(coord(X, Y), _, Timestamp2) & Timestamp <= Timestamp2.
// Otherwise i will update my knowledge about that cell.
+!updateCellIfNewer(cell(coord(X, Y), Terrain, Timestamp)) <-
    -cell(coord(X, Y), _, _);
    +cell(coord(X, Y), Terrain, Timestamp).
-!updateCellIfNewer <- .print("failed updateCellIfNewer").
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
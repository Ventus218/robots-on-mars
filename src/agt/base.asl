/* Initial beliefs and rules */

/* Initial goals */

/* Plans */

+inRange(R) <-
    .print("Sending knowledge to ", R);
    .findall(cell(coord(X, Y), Terrain, TS), cell(coord(X, Y), Terrain, TS), Cells);
    .send(R, tell, marsViewReceived(Cells)).

+marsViewReceived(Cells) <- !mergeMarsView(Cells).

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
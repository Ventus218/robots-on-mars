/* Initial beliefs and rules */

// Think about this as a belief, it's just a way to initialize it as soon as it's needed.
+!cellMap(M) : cellMapInstance(M).
@[atomic]
+!cellMap(M) <-
    .map.create(M);
    +cellMapInstance(M).

+!saveCell(Coord, Terrain, Timestamp) <-
    !cellMap(M);
    // Here there's a difference wrt rover.asl
    .map.put(M, Coord, data(Terrain, Timestamp)).

cell(Coord, Terrain, Timestamp) :-
    cellMapInstance(M) &
    .map.get(M, Coord, data(Terrain, Timestamp)).

allCells(Cells) :-
    cellMapInstance(M) & 
    .findall(cell(Coord, Terrain, TS), .map.key(M, Coord) & .map.get(M, Coord, data(Terrain, TS)), Cells).
allCells([]).

/* Initial goals */

/* Plans */

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
    !saveCell(coord(X, Y), Terrain, Timestamp).
-!updateCellIfNewer <- .print("failed updateCellIfNewer").
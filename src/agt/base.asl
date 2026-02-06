/* Initial beliefs and rules */

/* Initial goals */

/* Plans */

+inRange(R) <-
    .print("Sending knowledge to ", R);
    for (cell(coord(X, Y), Terrain, Timestamp)){
        .send(R, tell, receivedCell(coord(X, Y), Terrain, Timestamp));
    }.

+receivedCell(coord(X, Y), Terrain, Timestamp) : cell(coord(X, Y), _, Timestamp2) & Timestamp > Timestamp2 <-
    -+cell(coord(X, Y), Terrain, Timestamp).

+receivedCell(coord(X, Y), Terrain, Timestamp) : not(cell(coord(X, Y), _, _)) <-
    -+cell(coord(X, Y), Terrain, Timestamp).

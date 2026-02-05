// Agent alice in project robotsOnMars

/* Initial beliefs and rules */

/* Initial goals */

!explore.

/* Plans */

+!explore <- 
    exploreAction;
    .wait(1000);
    !!explore.

+see(coord(X, Y), Terrain) <-
    -cell(coord(X, Y), _, _);
    +cell(coord(X, Y), Terrain, system.time).

+inRange(R) <- .print("sending data to: ", R).

// Just to keep the model updated

+cell(coord(X, Y), Terrain, Timestamp) <- believeCell(coord(X, Y), Terrain).
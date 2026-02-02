// Agent alice in project robotsOnMars

/* Initial beliefs and rules */

/* Initial goals */

!explore.

/* Plans */

+!explore <- 
    moveInRandomDirection;
    .wait(100);
    !!explore.
   

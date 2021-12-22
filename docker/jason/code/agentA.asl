// Agent bob in project MAPC2018.mas2j

/* Initial beliefs and rules */

/* Initial goals */

!start.

/* Plans */

+!start : true <- 
	.print("hello massim world.").

+step(X) : true <-
	.print("Received step percept:", X).
	
+actionID(X) : true <- 
	.print("Determining my action.", X);
	skip.

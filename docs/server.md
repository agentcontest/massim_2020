# MASSim Server Documentation

The MASSim server software is located in the `server` directory.

## Running the server

You can run the server directly through the file
`server-[version]-jar-with-dependencies.jar` without the need for additional
shell scripts.

The standard command for that would be

`java -jar server-[version]-jar-with-dependencies.jar`

Make sure that the `conf` folder is located in your current working directory,
when you execute that command.

You may also directly pass a configuration file to the java command with the
`-conf [conf-file]` parameter. Also, you can pass a complete configuration
string value to the command with the `-confString [conf-string]` option.

To enable the web monitor (to view what's happening), you need to call the
server with the `--monitor` option.
The monitor will be available at [http://localhost:8000/](http://localhost:8000/) by default.

## Terminology

* __Simulation__: one round of the respective scenario lasting a predefined number of steps
* __Match__: a number of simulations played by the same teams
* __Tournament__: a number of matches

## Configuration

The MASSim server reads its configuration from JSON files. The file to use can
be directly given as an argument to the java command. Otherwise, the server
looks for a "conf" directory in the current working directory.

If possible, the server uses default values if no correct value is provided in the configuration file.

Below is the structure of a sample configuration file. The individual parts are
explained in the following.

```JSON
{
  "server" : {},
  "manual-mode" : [],
  "match" : [],
  "teams" : {}
}
```

### server block

The server block contains information about the server in general, which will hold for all simulations.

```JSON
"server" : {
    "tournamentMode" : "round-robin",
    "teamsPerMatch" : 2,
    "launch" : "key",
    "port" : 12300,
    "backlog" : 10000,
    "agentTimeout" : 4000,
    "resultPath" : "results",
    "logLevel" : "normal",
    "logPath" : "logs",
    "replayPath" : "replays",
    "maxPacketLength" : 65536,
    "waitBetweenSimulations" : 5000
  }
```

* __tournamentMode__: The tournament mode specifies, which teams play against each other in which order. Available modes are:
  * `round-robin`: Each unique combination of teams will play the set of simulations.
  * `manual`: This indicates that the actual matches will be manually configured in a separate vonfiguration block.
  * `random`: For each match, the participating teams are picked randomly until enough teams to play the match have been determined. This is repeated until the server is terminated manually.

* __teamsPerMatch__: How many teams play simultaneously in one simulation
* __launch__: How the start of the matches is delayed
  * `key`: The server will wait for the ENTER key to start.
  * `[Int]s`": The server will start after [Int] seconds (e.g. 60s).
  * `HH:mm`: The server will start at the time indicated by HH:mm (e.g. 12:00).

* __port__: The port on which to listen for incoming connections (see [protocol.md](protocol.md) for information about what to send)

* __backlog__: The backlog parameter for the Java ServerSocket

* __agentTimeout__: The time (in ms) after which an agent has to have sent an action

* __resultPath__: Where to store the result of a match

* __logLevel__: The level at which to print log messages; available levels include `debug`, `normal`, `error` and `critical`

* __logPath__: Every log message that is printed can also be written to file. This is where the log files will be saved. One log file per server run is written.

* __replayPath__: The simulation state can be saved to disk after each step. This is where these files will be saved. Those replay files can be used again e.g. with the web monitor.

* __maxPacketLength__: The maximum number of bytes of an XML message that will be processed by the server. Bytes beyond that limit will be immediately discarded.

* __waitBetweenSimulations__: A number of milliseconds to wait between to conescutive simulations.

### manual-mode block

This block specifies the manual-mode configuration. It is used (and required) if the __tournamentMode__ is set to `manual-mode`.

```JSON
"manual-mode" : [
  ["A", "B", "C"],
  ["B", "C", "D"],
  ["C", "D", "E"]
]
```

The block is an array of arrays. Each internal array specifies the teams participating in one match.
In this example, the teams A, B and C play in the first match against each other.

### match block

This block describes exactly how each simulation will be set up and is mostly scenario dependent. One object in the match array represents one simulation.
Always required are the fields

```JSON
"match" : [
  {
    "id" : "Sim-1of1",
    "steps" : 1000
  }
]
```

where

* __id__ is an identifier for the simulation,
* __steps__ is the number of steps the simulation will last.

For the remaining scenario-dependent items we refer to [scenario.md](scenario.md).

### teams block

The teams block describes the teams and their credentials. The teams listed here can always connect to the server - however, they will of course only receive percepts when they are participating in the current simulation.

```JSON
"teams" : {
    "A" : {"prefix" : "agent", "password" : "1"},
    "B" : {"prefix" : "agent", "password" : "2"},
    ...
  }
```

Each key in the ```teams``` JSON object is the name of a team. It points to an object containing the team details. Each agent's (user)name is constructed as `prefix+teamName+index`, where the index starts at 0 and goes to the maximum number of agents required in any simulation configured. 
E.g. if 3 simulations with 15, 30 and 50 agents each are configured, 50 agent accounts will be created before the first simulation and stay active until all simulations are played (though only the first X agents will receive any message during a simulation if X agents are configured for that simulation).

### JSON include mechanism

As it is often required to use e.g. the same team in different configuration files, each value can be stored in a separate file. The syntax to include external JSON elements is ```"$(path/to/include.json)"```.

Before configuration files are parsed, all such occurrences will be replaced with the content of the file found at that location. Those files may in turn reference other files. The paths are always interpreted relative to the referencing file.

## Commands

After the server has been started, it is listening for user input. The following commands are always accepted:

* __pause__: The server pauses before the next step is executed (the current step is finished first).
* __continue__: If the simulation is paused, the server continues its execution. Otherwise, nothing happens.

Commands are buffered during simulation steps and executed at a specific point between simulation steps. It is recommended to use the __pause__ command first and type further commands while the server is paused. If the command queue is emtpy, commands are immediately executed during the pause.

There is also a number of commands specific to the scenario. These are explained in [scenario.md](scenario.md).

# EISMASSim Documentation

_EISMASSim_ is based on the [Environment Interface Standard](https://github.com/eishub/) (EIS), a proposed standard for agent-environment interaction.

It maps the communication between agents and the _MASSim_ server, (i.e. sending and receiving JSON messages), to Java method calls. Also, it automatically establishes and maintains connections to a specified _MASSim_ server.

In other words, _EISMASSim_ is a proxy environment on the client side which handles communication with the _MASSim_ server completely by itself.

## Using EISMASSim

### Include the library

_EISMASSim_ is packaged as a jar file `eismassim-X.Y-jar-with-dependencies.jar` which already includes the _EIS_ package. The easiest way would be to include the jar with dependencies in your classpath. If you want to manage dependencies yourself, use `eismassim-X.Y.jar`. You can get the required EIS version (0.5.0) from the [eishub](https://github.com/eishub/eis).

### Create and start the Environment Interface instance

```Java
EnvironmentInterfaceStandard ei = new EnvironmentInterface();
```

The environment interface needs a configuration file to know how many entities it has to create. In the above case, an `eismassimconfig.json` file is expected in the working directory. Alternatively, the path to a configuration file can be given to the constructor.

```Java
try {
  ei.start();
} catch (ManagementException e) {
  // TODO handle the exception
}
```

This sets the state of the interface to `RUNNING`.

### Register your agents

Each agent you want to connect needs to be registered with the interface.

```Java
try {
  ei.registerAgent(agentName);
} catch (AgentException e) {
  // TODO handle the exception
}
```

### Associate agents with entities

Entities are the _corporeal_ parts used for perceiving and acting, i.e. the vehicles in the simulation. The available entities are specified in the `eismassimconfig.json` file which needs to match the _MASSim_ scenario requirements.

```Java
try {
  ei.associateEntity(agentName, entityName);
} catch (RelationException e) {
  // TODO handle the exception
}
```

This part automatically triggers authentication of the associated entity with the _MASSim_ server.

### Perceive the environment

Percepts can either be _polled_ or received as _notifications_.

```Java
try {
  eis.getAllPercepts(agentName);
} catch (PerceiveException e) {
  // TODO handle the exception
}
```

This would retrieve all percepts for the agent named `agentName`. The return value is a map, since the agent could be associated with more than one entity.

### Execute actions

```Java
Action action = new Action(...);
try {
  ei.performAction(agentName, action);
} catch (ActException e) {
  // TODO handle the exception
}
```

To execute an action, the name of the agent executing the action and the action itself need to be passed. All entities associated with the agent will perform this action (if possible).

## Configuration

The configuration of _EISMASSim_ is now realized with JSON files, matching the configuration of the _MASSim_ server.

Configuration example:

```JSON
{
  "scenario": "assemble2020",
  "host": "localhost",
  "port": 12300,
  "scheduling": true,
  "timeout": 40000,
  "notifications": false,
  "queued": false,
  "only-once": true,
  "entities": [
    {
      "name": "connectionA28",
      "username": "agentA28",
      "password": "1",
      "print-iilang": false,
      "print-json": true
    },
    ...
  ],
  "multi-entities": [
    {
      "name-prefix": "connectionA",
      "username-prefix": "agentA",
      "password": "1",
      "print-iilang": false,
      "print-json": false,
      "count": 28,
      "start-index": 0
    },

    "status-entity": {
      "name": "statusConnection"
    },
    ...
  ]
}
```

In the above example, one entity and one *set of entities* are configured at the same time. Usually, only one of these options needs to be chosen.

The main entries are:

* __scenario:__ the name of the MAPC scenario to handle
* __host:__ address of a _MASSim_ server
* __port:__ port the _MASSim_ server is listening on
* __scheduling:__ if `true`, an action can only be sent if a valid action-id is available; calls to `performAction` will also block until such an ID becomes available; it is recommended to not disable this
* __timeout:__ the timeout to use in combination with __scheduling__ while waiting for `performAction`
* __queued:__ if enabled, `getAllPercepts` will only yield one collection of percepts for each call (i.e. one for all percepts from a `SIM-START` message, one for all percepts from a `REQUEST-ACTION` message, etc.) in the same order as they were received from the _MASSim_ server
* __only-once:__ if enabled, `getAllPercepts` will only yield the same percepts once (if __scheduling__ and __queued__ are both disabled - each of these options has the same side effect)
* __notifications:__ if enabled, percepts will be delivered as notifications; this is detailed in the description of _EIS_

Further, there is an object for each entity in the `entities` array, containing

* __name:__ the name of the entity
* __username:__ the name to authenticate with
* __password:__ the password to authenticate with (both as configured in the _MASSim_ server)
* __print-iilang:__ whether to print the IILang version of received percepts
* __print-json:__ whether to print JSON messages sent and received by the interface

To simplify the creation of many similar entities, you can now specify a `multi-entity` array. Each object in this array contains

* __name-prefix:__ the prefix of the entity names
* __username_prefix:__ the prefix for the usernames
* __password:__ the password for all entities
* __print-iilang:__ see above
* __print-json:__ see above
* __count:__ the number of entities to create from this data (if count is -1 or missing, EISMASSim will try to retrieve the number of entities from the MASSim server)
* __start-index:__ the first index to append to the prefixes

If you do not specify the count for the multi-entities block (or set it to -1), EISMASSim will query the running MASSim server for the maximum number of entities required for any simulation.

You can also specify a `status-entity` to be created. This status-entity queries the current server status and gets the following percepts:

* `teams(l)`
  * l : List of Identifiers - names of the teams that are currently playing
* `teamSizes(l)`
  * l : List of Numerals - number of agents per simulation (e.g. [10,20,30] for three simulations with 10, 20 and 30 agents per team)
* `currentSim(n)`
  * n : Numeral - index of the simulation that is currently running. -1 if the match hasn't started yet
* `currentTeamSize(n)`
  * n : Numeral - number of agents required per team in the current simulation
* `error`
  * Indicates that no StatusResponse could be retrieved from the server.



## Example usage

EISMASSim is exemplarily used in the [javaagents](javaagents.md) package.

## IILang

Actions and percepts in _EISMASSim_ use the _Interface Intermediate Language_ (IILang) as provided by _EIS_. The IILang defines the following concepts:

* __DataContainer__: consists of a name and a number of _Parameters_
  * __Action__: used for acting
  * __Percept__: used to perceive changes in the environment
* __Parameter__: argument to _DataContainers_
  * __Identifier__: contains a string value
  * __Numeral__: contains any number value
  * __TruthValue__: contains a boolean value
  * __ParameterList__: strangely, a list of parameters
  * __Function__: has the same structure as a _DataContainer_, but can be used as a _Parameter_

Thus, any IILang _DataContainer_ forms a tree structure that can also be represented with Prolog-like syntax. For example, `car(red, 2007, [ac, radio], wheels(4))` could be a _Percept_ with the name `car`, an _Identifier_ (parameter) `red`, a _Numeral_ 2007, a _ParameterList_ containing 2 _Identifiers_ and a _Function_ named `wheels` containing a final _Numeral_.

## MAPC 2020 scenario

### Actions

The actions for the current scenario can be reviewed in [scenario.md](scenario.md). An IILang action takes a name and a number of parameters. Just pass the required parameters in the same order as described in
[scenario.md](scenario.md).

Example:

```Java
Action a = new Action("move", new Identifier("n"));
```

### Percepts

The following paragraphs describe how the JSON messages described in [protocol.md](protocol.md) and [scenario.md](scenario.md) are translated into __IILang__ percepts.

`[XYZ, ...]` denotes a _ParameterList_ of arbitrary length

#### SIM-START percepts

The following percepts might be included in a `SIM-START` message:

* `name(s)`
  * s : Identifier - name of the agent
* `team(s)`
  * s : Identifier - name of the agent's team
* `teamSize(n)`
  * n : Numeral - number of agents in the agent's team in the current round
* `steps(n)`
  * n : Numeral - number of steps
* `vision(n)`
  * n : Numeral - initial vision of the agent

#### REQUEST-ACTION percepts

The following percepts might be included in a `REQUEST-ACTION` message. Most of them should be self-explanatory.

* `actionID(id)`
  * id : Numeral - current action-id to reply with
* `timestamp(time)`
  * time : Numeral - server time the message was created at
* `deadline(time)`
  * time : Numeral - when the server expects the action
* `step(number)`
  * number : Numeral - the current step
* `lastAction(type)`
  * type : Identifier - name of the last executed action
* `lastActionResult(result)`
  * result : Identifier - result of the last executed action
* `lastActionParams([p1, ...])`
  * p1 : Identifier - first parameter of the last executed action
* `score(n)`
  * n : Numeral - the team's current score
* `thing(x, y, type, details)`
  * x/y : Numeral - relative position of a thing
  * type : Identifier - the type of the thing
  * details : Identifier - possibly more information about the thing (see scenario doc)
* `task(name, deadline, reward, [req(x,y,type),...])`
  * name : Identifier
  * deadline : Numeral - the last step the task can be completed
  * reward : Numeral
  * req : Function - a required block for the task
    * x/y : Numeral - the relative position of the required block
    * type : the type of the block
* `<terrainType>(x, y)`
  * `<terrainType>` is one of the possible terrains (`obstacle`, `goal`, ...)
  * x/y : Numeral - the relative position of the terrain
* `attached(x, y)`
  * x/y : Numeral - relative position of a thing that is attached to some entity
* `energy(n)`
  * n : Numeral - the agent's energy level
* `disabled(b)`
  * b : Identifier - true if the agent is disabled (else false)
* `accepted(t)`
  * t : Identifier - the task that the agent is currently committed to

#### SIM-END percepts

The following percepts might be included in a `SIM-END` message:

* `ranking(r)`
  * r : Numeral - the final ranking of the agent's team
* `score(s)`
  * s : Numeral - the final score of the agent's team

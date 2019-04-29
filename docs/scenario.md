# MASSim Scenario Documentation

## Agents Assemble (2019)

* [Intro](#background-story)
* [Actions](#actions)
* [Percepts](#percepts)
* [Configuration](#configuration)
* [Commands](#commands)

### Background Story

_In the year 2090, ..._

### Introduction

The scenario consists of two __teams__ of agents moving on a grid. The goal is to explore the world and acquire blocks to assemble them into complex patterns.

Agents can _attach_ things to each of their 4 sides. When the agents _move_ or _rotate_, the attached things move or rotate with them. Two agents can _connect_ things that are attached to them to create more complex structures.

__Tournament points__ are distributed according to the score of a team at the end of the simulation. Winning a simulation awards 3 points, while a draw results in 1 point for each team.

## Environment

The environment is a rectangular grid. The dimensions are not known to the agents.

### Things

There are number of __things__ that can inhabit a cell of the grid:

* __Entities__: Each agent controls an entity on the grid. Entites can move around and attach themselves to other things.
* __Blocks__: The building blocks of the scenario. Each block has a specific type. Agents can pick up blocks and stick multiple blocks together. Blocks have to be arranged into specific patterns to get score points.
* __Dispenser__: Each dispenser can be used to retrieve a specific kind of block.

### Terrain

Each cell of the grid has a specific terrain type.

* __Regular__: If nothing else is specified, a cell is *just a cell*.
* __Goal__: Agents have to be on a __goal__ cell in order to be allowed to submit a task.

## Tasks

Tasks have to be completed to get score points. They appear randomly during the course of the simulation.

* __name__: an identifier for the task
* __deadline__: the last step in which the task can be submitted
* __reward__: the number of score points that can be earned by completing the task
* __requirements__: each requirement describes a block that has to be attached to the agent
  * __x/y__: the coordinates of the block (the agent being (0,0))
  * __type__: the required type of the block

## Actions

In each step, an agent may execute _exactly one_ action. The actions are gathered and executed in random order.

All actions have the same probability to just fail randomly.

Each action has a number of `parameters`. The exact number depends on the type of action. Also, the position of each parameter determines its meaning. Parameters are always string values.

### move

Moves the agent in the specified direction.

No | Parameter | Meaning
--- | --- | ---
0 | direction | One of {n,s,e,w}, representing the direction the agent wants to move in.

Failure Code | Reason
--- | ---
failed_parameter | Not exactly 1 parameter given, or parameter is not a direction.
failed_path | Cannot move to the target location because the agent or one of its attached things are blocked.

### attach

Attaches a thing to the agent. The agent has to be directly beside the thing.

No | Parameter | Meaning
--- | --- | ---
0 | direction | One of {n,s,e,w}, representing the direction to the thing the agent wants to attach.

Failure Code | Reason
--- | ---
failed_parameter | Not exactly 1 parameter given, or parameter is not a direction.
failed | The thing could not be attached, because their was no attachable thing OR the agent already has too many things attached OR the thing is already attached to an agent of another team.

### detach

Detaches a thing from the agent. Only the connection between the agent and the thing is released.

No | Parameter | Meaning
--- | --- | ---
0 | direction | One of {n,s,e,w}, representing the direction to the thing the agent wants to detach from.

Failure Code | Reason
--- | ---
failed_parameter | Not exactly 1 parameter given, or parameter is not a direction.
failed | There was no thing in the given direction attached to the agent.

### rotate

Rotates the agent (and all attached things) 90 degrees in the given direction. For each attached thing, all _intermediate positions_ for the rotation have to be free as well. For any thing, the intermediate rotation positions are those, which have the same distance to the agent as the thing and are between the thing's current and target positions.

(TODO: example graphic)

No | Parameter | Meaning
--- | --- | ---
0 | direction | One of {cw, ccw}, representing the rotation direction (clockwise or counterclockwise).

Failure Code | Reason
--- | ---
failed_parameter | Not exactly 1 parameter given, or parameter is not a (rotation) direction.
failed | One of the things attached to the agent cannot rotate to its target position OR the agent is currently attached to another agent.

### connect

Two agents can use this action to connect things attached to them. They have to specify their partner and the block they want to connect. Both blocks are connected (i.e. attached to each other) if they are next to each other and the connection would not violate any other conditions.

No | Parameter | Meaning
--- | --- | ---
0 | agent | The agent to cooperate with.
1/2 | x/y | The local coordinates of the block to connect.

Failure Code | Reason
--- | ---
failed_parameter | Not exactly 3 parameters given OR first parameter is not an agent of the same team OR x and y cannot be parsed to valid integers.
failed_partner | The partner's action is not connect OR failed randomly.
failed | There are no blocks at the given coordinates OR the given positions are too far apart OR one of the blocks is already connected to the other agent OR one agent is already attached to the other (recursively), or connecting both blocks would violate the size limit for connected structures.

### request

Requests a new block from a dispenser.

No | Parameter | Meaning
--- | --- | ---
0 | direction | One of {n,s,e,w}, representing the direction to the position of the dispenser to use.

Failure Code | Reason
--- | ---
failed_parameter | Not exactly 1 parameter given, or parameter is not a direction.
failed | There is no dispenser in the specific position OR the position is currently blocked by another agent or thing.

### submit

Submit the pattern of things that are attached to the agent to complete a task.

No | Parameter | Meaning
--- | --- | ---
0 | task | The name of an active task.

Failure Code | Reason
--- | ---
failed_parameter | Not exactly 1 parameter given.
failed | There is no such task active OR one or more of the requested blocks are missing OR the agent is not on a goal terrain.

### all actions

All actions can also have the following failure codes:

Failure Code | Reason
--- | ---
failed_random | The action failed randomly.
unknown_action | The action is unknown to the server.

## Percepts

Percepts are sent by the server as JSON files and contain information about the current simulation. Initital percepts (sent via `SIM-START` messages) contain static information while other percepts (sent via `REQUEST-ACTION` messages) contain information about the current simulation state.

The complete JSON format is discussed in [protocol.md](protocol.md).

### Initial percept

This percept contains information that does not change during the whole simulation. As mentioned in the protocol description, everything is contained in a `simulation` element.

Complete Example (with bogus values):

```JSON
{
  "TODO" : true
}
```

#### Simulation details

The `simulation` tag has attributes for the simulation `id`, the name of the `map` that is used and its `bounds`, the `seed capital`, the number of simulation `steps` to be played and the name of the agent's `team`.

#### Role details

The percept also contains the agent's `role` and its details; the name and base and max values for speed, load, battery, vision and skill.

### Step percept

This percept contains information about the simulation state at the beginning of each step.

Example:

```XML
<message timestamp="1518532127931" type="request-action">
...
```

The information is contained in the `percept` element within the message. This element contains an arbitrary number of child nodes, each representing an element of the simulation.

## Configuration

Each simulation configuration is one object in the `match` array.

Example:

```JSON
{
  "id" : "2019-SampleSimulation",
  "steps" : 1000,
  "randomSeed" : 17,
  "randomFail" : 1,

  "entities" : [
    {"standard" : 10}
  ],

  "generate" : {}
}
```

The `generate` block will be subject of the [Random generation](#random-generation) section.

For each simulation, the following parameters may be specified:

* __id__: a name for the simulation; e.g. used in replays together with the starting time
* __steps__: the number of steps the simulation will take
* __randomSeed__: the random seed that is used for map generation and action execution
* __randomFail__: the probability for any action to fail (in %)

The number of agents per role is defined in the `entities` array. Each object may have only one key (the name of the role). The value for the key is the number of agents for that role.

Currently, there is only one standard agent role.

## Random generation

## Commands

[Currently, no special scenario commands are available.]
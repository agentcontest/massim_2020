# MASSim Protocol Documentation

The MASSim protocol defines sequences of JSON messages which are exchanged between the agents and the MASSim server. Agents communicate with the server using standard TCP sockets.

## Sequence

An agent may initiate communication by sending an `AUTH-REQUEST` message, containing its login credentials.

The server tries to authenticate the agent and answers with an `AUTH-RESPONSE` message.

If the result was positive, the server now sends various messages over the socket depending on its state:

* a `SIM-START` message, whenever a new simulation starts
* a `REQUEST-ACTION` message right before each simulation step
* a `SIM-END` message after each simulation
* a `BYE` message when all matches have been run, just before the server closes all sockets

An agent should respond with an `ACTION` message to all `REQUEST-ACTION` messages it receives (within the time limit).


Aside from these necessary messages, the clients may also send optional `STATUS-REQUEST` messages which are answered by `STATUS-RESPONSE` messages containing some information about the current server configuration.

## Reconnection

If an agent loses the connection to the server, it may reconnect using the standard `AUTH-REQUEST` message. Auhtentication proceeds as before. If authentication was successful and the agent reconnects into a running simulation, the `SIM-START` message is sent again. If it coincides with a new simulation step, the order of `SIM-START` and `REQUEST-ACTION` messages is not guaranteed.

## Message formats

__Each message is terminated by a separate `0 byte`.__ The server buffers everything up to the 0 byte and tries to parse a JSON string from that.

Each message is a JSON object following the base format

```json
{
   "type": "some-type",
   "content": {...}
}
```

where value of `type` denotes the type of the message. The JSON object under the `content` key depends on the message type.

### AUTH-REQUEST

* Who? - Agents
* Why? - Initiate communication with the server.

```json
{
  "type": "auth-request",
  "content": {
    "user": "agentA1",
    "pw": "1"
  }
}
```

* __user__: username of the agent that is configured in the server
* __pw__: the agent's password to authenticate with

### AUTH-RESPONSE

* Who? - Server
* Why? - Tell agent the result of authentication after an `AUTH-REQUEST` message.

```json
{
  "type": "auth-response",
  "content": {"result": "ok"}
}
```

* __result__: the result of the authentication; either __"ok"__ or __"fail"__

### SIM-START

* Who? - Server
* Why? - Indicate that a simulation has started or is currently running.

```json
{
  "type": "sim-start",
  "content": {
    "time": 1489514146201,
    "percept": {
      ...
    }
  }
}
```

* __time__: when the message was created
* __percept__: static simulation info

The contents of the percept object depend on the scenario (see Percepts section of [scenario.md](scenario.md)).

### REQUEST-ACTION

* Who? - Server
* Why? - Indicate a new simulation step, convey simulation state and request an action.

```json
{
  "type": "request-action",
  "content": {
    "id": 2,
    "time": 1556636930397,
    "deadline": 1556636934400,
    "step": 27,
    "percept": {
      ...
    }
  }
}
```

* __deadline__: the time by which the server expects an `ACTION` message
* __id__: the action-id; this id must be used in the `ACTION` message so that it can be associated with the correct request (which prevents older actions from being executed if they arrive too late)
* __step__: the current simulation step
* __percept__: the current simulation state

The contents of the percept object depend on the scenario (see Percepts section of [scenario.md](scenario.md)).

### ACTION

* Who? - Agent
* Why? - Respond to `REQUEST-ACTION` message

```json
{
  "type": "action",
  "content": {
    "id": 2,
    "type": "move",
    "p": ["n"]
  }
}
```

* __id__: the action-id that was received in the `REQUEST-ACTION` message
* __type__: the type of action to use
* __p__: an array of parameters for the action

### SIM-END

* Who? - Server
* Why? - Indicate that a simulation ended and give results.

```json
{
  "type": "sim-end",
  "content": {
    "score": 9001,
    "ranking": 1,
    "time": 1556638423323
   }
}
```

* __ranking__: the team's rank at the end of the simulation
* __score__: the team's final score

### BYE

* Who? - Server
* Why? - Indicate that all simulations have finished and the sockets will be closed.

```json
{
  "type": "bye",
  "content": {}
}
```

### STATUS-REQUEST

* Who? - Agent
* Why? - Ask about current server configuration.

```json
{
  "type": "status-request",
  "content": {}
}
```

### STATUS-RESPONSE

* Who? - Server
* Why? - Answers a status request.

```json
{
  "type": "status-response",
  "content": {
    "teams":["A","B"],
    "time":1588865434131,
    "teamSizes":[15,30,50],
    "currentSimulation":0
  }
}
```

* __teams__: the teams that are currently playing (empty if the simulation hasn't started yet)
* __time__: the server time when the message was created
* __teamSizes__: how many agents play in each simulation per team (the size of this array corresponds to the number of simulations)
* __currentSimulation__: the index of the current simulation (starts at 0, will be -1 if the first simulation has not started yet)
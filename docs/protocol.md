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

## Reconnection

If an agent loses the connection to the server, it may reconnect using the standard `AUTH-REQUEST` message. Auhtentication proceeds as before. If authentication was successful and the agent reconnects into a running simulation, the `SIM-START` message is sent again. If it coincides with a new simulation step, the order of `SIM-START` and `REQUEST-ACTION` messages is not guaranteed however.

## Message formats

__Each message is terminated by a separate `0 byte`.__ The server buffers everything up to the 0 byte and tries to parse a JSON string from that.

### AUTH-REQUEST

Used by agents to initiate communication with the server.

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<message type="auth-request">
  <auth-request username="agentA1" password="1"/>
</message>
```

All messages are enclosed in `<message>` tags and have a `type` attribute describing what kind of protocol message they represent.

This message has one `<auth-request>` element with the attributes `username` and `password`.

### AUTH-RESPONSE

Sent by the server in reply to `AUTH-REQUEST` messages.

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<message timestamp="1489514144783" type="auth-response">
  <auth-response result="ok"/>
</message>
```

* __timestamp__: is the server time of when the message was created (in ms since 1970)
* __result__: the result of the authentication; either __ok__ or __fail__

### SIM-START

```XML
<message timestamp="1489514141992" type="sim-start">
  <simulation id="2017-TestSim-1of1" steps="1000" team="A">
    ...
  </simulation>
</message>
```

Also contains a timestamp. The content of the simulation element, as well as further attributes, depend on the scenario (see Percepts section of [scenario.md](scenario.md)).

### REQUEST-ACTION

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<message timestamp="1489514146201" type="request-action">
  <percept deadline="1489514150201" id="1">
    <simulation step="1"/>
    ...
  </percept>
</message>
```

A `REQUEST-ACTION` message also contains a server timestamp and its type as attribute. The main element is `<percept>`, containing the current simulation state as perceived by the agent (i.e. its percepts).

* __deadline__: the time at which the server stops waiting for an `ACTION` message; the time between __timestamp__ and __deadline__ should be the timeout specified in the server config
*  __id__: the action-id; this id must be used in the `ACTION` message so that it can be associated with the correct request (which prevents older actions from being executed if they arrive too late); in case multiple actions are sent for the same action-id, the first action is processed
* __simulation__: contains global simulation information
  * __step__: the current simulation step

The remaining content of the percept element, as well as further attributes, depend on the scenario (see Percepts section of [scenario.md](scenario.md)).

### ACTION

The response to a `REQUEST-ACTION` message; sent by agents to the server.

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<message type="action">
  <action id="0" type="goto">
    <p>51.6</p>
    <p>11.4</p>
  </action>
</message>
```

* __id__: the action-id that the agent received in the `REQUEST-ACTION` message
* __type__: the type of the action; depends on the current scenario
* __p__: a number of parameters for the action, which depend on the action that is used

### SIM-END

Contains the result of 1 simulation for the team.

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<message timestamp="1489575032814" type="sim-end">
  <sim-result ranking="1" score="50000"/>
</message>
```

* __ranking__: the team's rank at the end of the simulation
* __score__: the score the team achieved in the simulation

### BYE

This message indicates that the server has run all simulations it was configured for and will close the socket afterwards.

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<message timestamp="1489575049861" type="bye">
  <bye/>
</message>
```

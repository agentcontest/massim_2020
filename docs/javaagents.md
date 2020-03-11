# MASSim Javaagents Documentation

This module provides some very basic agent framework written in Java. EISMASSim
is integrated so that agents can communicate with the MASSim server out of the box.

Some very basic agents are included mainly for testing purposes.

## Create your own agent
* Add a new class for your agent somewhere in _massim.javaagents.agents_
 * Make your class extend _massim.javaagents.agents.Agent_
 * Add your class with a type name to the _setEnvironment()_ method of _massim.javaagents.Scheduler_
 * Create a JSON configuration file for your agents

### Java agents configuration file
A sample configuration might look like this

```json
{
  "agents" : [
    {
      "count": 20,
      "start-index": 0,
      "agent-prefix": "A",
      "entity-prefix": "connectionA",
      "team": "A",
      "class": "BasicAgent"
    },
    ...
  ]
}
```

The attributes are
* __count__: how many agents to create
* __start-index__: the index that is first appended to the prefixes
* __agent-prefix__: the prefix for all agents' names
* __entity-prefix__: the prefix of all entity connections
* __team__: the agents' team name
* __class__: the agents' type as registered in the scheduler class

Of course you can specify multiple blocks to configure multiple teams or sets of agents with different agent classes in the same file.
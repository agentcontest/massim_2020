MASSim 2020: Agents Assemble II
===============================

[![Continuous Integration](https://github.com/agentcontest/massim_2020/workflows/Continuous%20Integration/badge.svg)](https://github.com/agentcontest/massim_2020/actions?query=workflow%3A%22Continuous+Integration%22)

_MASSim_ (Multi-Agent Systems Simulation Platform), the simulation (server)
software used in the
[Multi-Agent Programming Contest](https://multiagentcontest.org/),
where participants program agents to compete with each other in a
predefined game.

_MASSim_ simulations run in discrete steps. Agents connect remotely to the
contest server, receive percepts and send their actions, which are in turn
executed by _MASSim_.

<p align="center">
  <img src="https://multiagentcontest.org/2019/banner.png">
</p>

Download
--------

We upload **binary releases** to GitHub: https://github.com/agentcontest/massim_2020/releases

There also are (potentially unstable) [development snapshots](https://github.com/agentcontest/massim_2020/actions?query=workflow%3A%22Continuous+Integration%22) attached as artifacts to each commit.

Building MASSim
---------------

The build requires Maven and OpenJDK 13.

Run `mvn package` in the main directory. Maven should automatically
fetch all necessary dependencies.

Documentation
-------------

[server.md](docs/server.md) describes how the _MASSim_ server can be configured and started.

[scenario.md](docs/scenario.md) contains the description of the current scenario.

[protocol.md](docs/protocol.md) describes the _MASSim_ protocol, i.e. message formats for communicating with the _MASSim_ server.

[eismassim.md](docs/eismassim.md) explains _EISMASSim_, a Java library using the Environment Interface Standard (EIS) to communicate with the _MASSim_ server, that can be used with platforms which support the EIS.

[javaagents.md](docs/javaagents.md) gives a short introduction to the java agents framework, which holds skeletons that can already communicate with the MASSim server and have basic agent capabilities.

[monitor.md](docs/monitor.md) describes how to view live matches and replays in the browser.

License
-------

_MASSim_ is licensed under the AGPLv3+. See COPYING.txt for the full
license text.

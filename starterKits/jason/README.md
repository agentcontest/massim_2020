# MASSim Starter Kit for Jason 3.0

This is a starter kit to connect [Jason](http://jason.sourceforge.net/) agents
to the [MASSim server](https://multiagentcontest.org) in a few simple steps.

1. Prepare the Jason project
- Create a ``lib`` folder.
- Find the ``jason-3.0.jar`` and put it into the ``lib`` folder (it's part of the Jason package)
- Download or compile EISMASSim and put ``eismassim-X.Y-jar-with-dependencies.jar``
 into the ``lib`` folder as well. Use the version matching the server you want to
 connect to.
- Navigate to the ``conf`` folder and add enough entities matching the current MASSim scenario.
- Adapt the number of agents in the ``mas2j`` file.
1. Start the MASSim server.
- follow the instructions provided
1. Done. You can run the agents now, e.g. from the Jason IDE.

## Details

The Jason environment is provided in the ``EISAdapter`` class. It uses EISMASSim
to communicate with the MASSim server to get percepts and reply with actions.
You can find the available percepts and actions in the MASSim documentation.

# MASSim Scenario Documentation

## Agents in the City (2018)

* [Intro](#background-story)
* [Locations](#locations)
* [Items](#items)
* [Facilities](#facilities)
* [Roles](#roles)
* [Upgrades](#job) (new!)
* [Jobs](#job)
* [Actions](#actions)
* [Percepts](#percepts)
* [Configuration](#configuration)
* [Commands](#commands)

### Background Story

_In the year 2044, following the great water conflicts of 2033, humankind has finally colonized and terraformed Mars; so much in fact that it greatly (structurally) resembles parts of old Earth. Unfortunately, scientists are still working on making the atmosphere breathable, so humans still need (costly) special equipment to leave their homes. Luckily, some entrepreneurial individuals found remnants of the old 'All Terrain Planetary Vehicles' which they retrofitted to do some serious business. Wanting to improve life for everyone on Mars (and to make a living off of that), they are sending their ATPVs out to gather the planet's natural resources and supply everyone in need with useful items._

_2045 A.D.: Another water crisis has struck the Mars people and collapsed the currency system. Unfortunately, the wells of 2033 have also completely dried up. The World Emperor - once again forced to step in - offered a remarkable reward for the group building the most wells and supplying the most water to humankind (maybe once again pushing competition over the limit). To build the wells, the ATPVs have to earn and use as much as possible of the recently discovered, versatile element massium._

### Introduction

The scenario consists of two __teams__ of a number of agents each moving through the streets of a realistic city. The goal for each team is to build and keep as many __wells__ as possible to generate the most points (__score__).
Wells are built with massium (the game's currency), which is rewarded for completing certain __jobs__ or __trading__ assembled items in general.
An agent is characterized by its __battery__ (i.e. how long it can move without recharging), its __capacity__ (i.e. how much volume it can carry) its __speed__, its __vision__ (how far away it can perceive) and its __skill__ (how fast it can complete certain taskInfos). The scenario features 4 distinct __roles__: drones, motorcycles, cars and trucks, sorted by increasing capacity and energy, and decreasing speed.
The city map is taken from __OpenStreetMap__ data and routing is provided by the Contest server. As the simulation is divided into discrete steps, each agent can move a fixed distance each turn (unless its destination is closer than this distance).

Each simulation features a set of random __items__. Items differ by their volume and how they can be acquired.

The agents are positioned randomly on the map, as are a number of __facilities__, among them __shops__, __charging stations__, __workshops__, __resource nodes__, __storage__ facilities, and __dumps__.

__Jobs__ comprise the acquisition, assembling, and transportation of goods. These jobs are randomly created by the system. There are three types of jobs: __regular jobs__, __auctions__ and __missions__.

A team can accept an __auction__ job by bidding on it. The lowest bid wins the auction, as the auction winner will receive the bid amount of massium. If an auction job is not completed in time, the corresponding team is fined to discourage auction “hoarding”.

__Regular jobs__ have their rewards defined upfront. It is given to the first team to complete that job, while the other team goes away empty-handed. The teams have to decide which jobs to complete and how to do that.
While auction jobs have to be more thoroughly assessed in order to determine a minimum threshold at which the team would still earn massium, they provide some safety since the team that did not win the auction is effectively barred from completing it.

__Missions__ appear separately assigned to each team. The teams have to complete the mission or face a fine.

__Tournament points__ are distributed according to the score of a team at the end of the simulation (massium deciding in case of a draw). Winning a simulation awards 3 points, while a draw results in 1 point for each team.

## Locations

Locations in the scenario are given as pairs of latitude and longitude double values. For the sake of simplicity, we assume them to form a uniform grid (which will be okay, if we don't go near the poles).

To simplify reaching a location, these values are rounded and approximated with the help of two parameters:

__Proximity:__ The proximity value specifies which precise locations are still considered the same, which is important when an agent needs to be at the same location as e.g. another agent or a facility.
So, it determines how many decimal places of the latitude and longitude values are compared when checking equality of Locations.

_Example:_ 51.1111 and 51.1112 would still be considered equal with a proximity of 3 and different with a proximity of 4.

__CellSize:__ The cell size specifies the length in meters an agent with speed 1 could travel in a single step.

## Items

Each simulation features a (random) set of item types. Items have a unique __name__ and a __volume__, which comes into play when items must be carried or stored.

Also, many items need to be _assembled_ from other items. Each of those
items has a number of __requirements__, i.e. quantities of other items which need to be present to assemble the item. These items are consumed during the process. Also, some items require certain roles to take part in the process.

## Facilities

Facilities are placed randomly on the map. Each facility has a unique name and location.

### Shops

In __shops__, items which have been assembled can be traded in by the agents. Each shop buys items at specific (unknown) prices. Also, agents may buy upgrades only in shops.

### Charging stations

__Charging stations__ have to be frequently visited by agents in order to recharge their battery. They have a __rate__ which expresses the amount of charge that
can be restored in one step (i.e. a charging station with a rate of 40 would
restore 40 charge units of an agent's battery).

### Workshops

In __workshops__, agents can assemble items out of other required items. Most items also need specific roles to be present. Agents must cooperatively assemble. In that case, the resources of those agents are combined.

* The __initiating agent__ receives the finished item (if all prerequisites are satisfied).
* Items are preferably taken from the initiating agent.
* If multiple __assistants__ carry the same required item, it is not guranteed which item is consumed.

### Dumps

Items can be destroyed at __dumps__ (to free capacity). Those items cannot be retrieved.

### Storage

__Storage__ facilities allow to store items up to a specific volume and also are the target for completing jobs.

The storage has a limited __capacity__ which counts for all teams, i.e. one team can fill a storage while not leaving any space for other teams.

Each team has a separate (unlimited) compartment in each storage. This compartment cannot be filled directly but only as a consequence of other actions:

* If a team delivers items towards job completion, but a) another team is faster or b) the job ends (due to its time limit), the partial delivery is moved to this compartment.

### Resouce nodes

__Resource nodes__ are the only place where resource items can be acquired. The nodes are initially hidden and have to be discovered by agents during the simulation. Each node provides a fixed type of resource. Agents can use the __gather__ action to retrieve resources. The gathering speed depends on an agent's __skill__ attribute (internally, each node accumulates skill values and gives out a resource when the accumulated values surpass a given threshold - so, even multiple resources could be acquired per action if the agent's skill is high enough).

If there are multiple agents gathering at the same resource node, the action execution order might decide which agent gets a resource.

The location of resource nodes is not common knowledge and agents are only able to perceive the nodes when they are close to them.

### Wells

__Wells__ are not placed by the system but built by the agents during the simulation. Each well consists of _cost_, _efficiency_ and (initial) _integrity_. The cost describes how much massium a team has to pay to build the well while the efficiency describes how many (score) points the well will generate in each step. The _integrity_ describes how durable a well is. Each well starts with a certain initial integrity and has to be built up to its max integrity to start generating points.

Once a well has started generating points, it will do so until it is completely dismantled. The team that uses the final dismantle action will receive up to 50% of the massium that was spent to initially build it.

For each simulation, there is a number of random well types which can be built (determining a combination of cost, integrity and efficiency).

As resource nodes, wells are also not common knowledge and have to be discovered. This also means, teams can try to hide them from the other team, which might want to dismantle them quickly.

## Teams

The scenario can be played with any number of teams simultaneously. Each team contains the same number of agents per __role__.

## Roles

The roles in the scenario can be configured under the top-level `roles` key in the simulation JSON object (which is one element of the `match` array).

```JSON
"roles" : {
  "drone" : {
    "baseSpeed" : 5,
    "maxSpeed" : 7,
    "baseLoad" : 15,
    "maxLoad" : 25,
    "baseBattery" : 10,
    "maxBattery" : 25,
    "baseSkill" : 1,
    "maxSkill" : 3,
    "baseVision" : 600,
    "maxVision" : 1000,
    "roads" : ["air"]
        },
  ...
}
```

(Example values)

Each role has its name as key and the following parameters:

* __base/maxSpeed__: how many 'units' the agent can move in one step
* __base/maxLoad__: how much volume the agent may carry
* __base/maxBattery__: the agent's battery size
* __base/maxVision__: how far the agent can perceive (in m)
* __base/maxSkill__: how fast the agent is at gathering/building/dismantling
* __roads__: which roads the agent can navigate (currently 'road'  for all roads and 'air' for travelling linear distances between two points)

The first 5 attributes have a base and a max value. Each agent starts at the base value, but can upgrade the attribute up to the max value (using __upgrade__ in a __shop__).

These 4 roles will also be used in the contest, however, the parameters are still subject to change.

## Upgrades

Upgrades can be bought in shops individually for each agent. Each update has

* a __name__ describing the attribute that is upgraded,
* a __cost__ that has to be paid with massium, and
* a __step__ describing how much the attribute is increased with one upgrade.

The attributes that can be upgraded are _speed_, _load_ (carrying capacity), _battery_, _vision_ and _skill_. Each attribute can be upgraded up to its maximum value (see [Roles](#roles) above).

## Jobs

A job is the general way to earn massium in this scenario.

* __begin__: the job begins in this step, i.e. this is the first step where the job is perceived by all agents
* __end__: the last step in which the job can be completed. At the end of this step, the job cannot be perceived anymore.
* __reward__: the amount of massium that is earned by completing the job
* __storage__: to complete the job, items have to be delivered to this storage

### AuctionJob

An auction job has an initial phase in which agent teams can bid for it.
The teams are bidding the amount of massium which they want to be paid for completing
the job. Of course, the team with the lowest bid wins.

**New in 2018**: The auction time (and the job's end step) are increased by 1, if

* one team underbid the other team, and
* the current step would be the last one of the auction phase.

This type of job has the advantage, that a team can be sure to be the only one
working on it if it won the auction. However, the teams have to estimate
how low they can bid to still have an advantage.

Each step during the auction phase, the current lowest bid can be perceived by all agents.

* __begin__: the step in which the auctioning begins.
* __auctionTime__: the duration of the auction phase. The winner of the auction will be determined at the end of step (begin + auctionTime - 1 [+ delay from underbidding]).
* __end__: the latest step in which items can be delivered to complete the job
* __reward__: for this job, the reward is the maximum reward possible, i.e. bids that go above this value will be ignored.
* __fine__: at the end of the auction job, if it has not been completed, the team that won the auction has to pay this fine.

### Mission

A mission is a special type of job that is given out randomly. All teams will receive an instance of the same mission to complete. This mission has to be completed (or the fine paid).

Note: Technically, a mission is an auction that is immediately assigned to each team in the simulation.

## Actions

In each step, an agent may execute _exactly one_ action. The actions are gathered and executed in random order.

All actions have the same probability to just fail randomly.

Some actions may lead to __conflicts__. For example, two agents might want to buy the same item from a shop (and only one of these items is left). In that case, the agent whose action is executed first gets the item, while the action of the other agent is treated as though no item is available (which is actually true).

Each action has a number of `parameters`. The exact number depends on the type of action. Also, the position of each parameter determines its meaning. Parameters are always string values.

### goto

Moves the agent towards a destination. Consumes __10__ charge units if successful. Can be used with 0, 1 or 2 parameters. If 0 parameters are used, the agent needs to have an existing route which can be followed.

No | Parameter | Meaning
--- | --- | ---
0 | Facility | The name of a facility the agent wants to move to.

Note: Names of _resource nodes_ and __wells__ are not allowed, as they are not common knowledge.

No | Parameter | Meaning
--- | --- | ---
0 | latitude | The latitude of the agent's desired destination.
1 | longitude | The longitude of the agent's desired destination.

Failure Code | Reason
--- | ---
failed_wrong_param | The agent has no route to follow (0 parameters), more than 2 parameters were given or the given coordinates were not valid double values (2 parameters).
failed_unknown_facility | No facility by the given name exists (1 parameter).
failed_no_route | No route to the destination exists or the charge is insufficient to reach the next waypoint.

### give

Gives a number of items to another agent in the same location.

No | Parameter | Meaning
--- | --- | ---
0 | Agent | Name of the agent to receive the items.
1 | Item | Name of the item to give.
2 | Amount | How many items to give.

Failure Code | Reason
--- | ---
failed_wrong_param | More or less than 3 parameters have been given, no agent by the name is known or an amount < 0 was specified.
failed_unknown_item | No item by the given name is known.
failed_counterpart | The receiving agent did not use the receive action.
failed_location | The agents are not in the same location.
failed_item_amount | The giving agent does not carry enough items to give.
failed_capacity | The receiving agent could not carry all given items.

### receive

Receives items from other agents. Can receive items from multiple agents in the same step.

No parameters.

Failure Code | Reason
--- | ---
failed_counterpart | No agent gave items to this agent.

### store

Stores a number of items in a storage facility.

No | Parameter | Meaning
--- | --- | ---
0 | Item | Name of the item to store.
1 | Amount | How many items to store.

Failure Code | Reason
--- | ---
failed_wrong_param | More or less than 2 parameters were given.
failed_location | The agent is not located in a facility.
failed_wrong_facility | The agent is not in a storage facility.
failed_unknown_item | No item by the given name is known.
failed_item_amount | The given amount is not an integer, less than 1 or greater than what the agent is carrying.
failed_capacity | The storage does not have enough free space.
failed | An unforeseen error has occurred.

### retrieve & retrieve_delivered

Retrieves a number of items from a storage. The first can be used to retrieve items that have been stored before, while the second is used to retrieve items from the team's 'special' compartment (see [Storage](#storage)).

No | Parameter | Meaning
--- | --- | ---
0 | Item | Name of the item to retrieve.
1 | Amount | How many items to retrieve.

Failure Code | Reason
--- | ---
failed_wrong_param | More or less than 2 parameters have been given.
failed_location | The agent is not located in a facility.
failed_wrong_facility | The agent is not in a storage facility.
failed_unknown_item | No item by the given name is known.
failed_item_amount | The given amount is not an integer, less than 1 or more than available.
failed_capacity | The agent has not enough free space to carry the items.

#### assemble

Assembles an item.

No | Parameter | Meaning
--- | --- | ---
0 | Item | Name of the item to assemble.

Failure Code | Reason
--- | ---
failed_wrong_param | Not exactly 1 parameter has been given.
failed_location | The agent is not in a facility.
failed_wrong_facility | The agent is not in a workshop.
failed_unknown_item | No item by the given name is known.
failed_item_type | The item cannot be assembled (since it has no requirements).
failed_tools | Some agent role (implicitly holding a tool) is missing.
failed_item_amount | At least one required item is missing.
failed_capacity | The agent does not have enough free space to carry the assembled item (after required items have been removed).

### assist_assemble

Marks the agent as an assistant for assembly.

If multiple agents could provide the same item for assembly, it is preferably taken from the agent that used the _assemble_ action. If that agent cannot provide the item, the assistants are sorted by name (i.e. first by length and then lexicographically, as the last part of the name is traditionally their number) as provided in the server's team config. Then, the item is taken from the assistants in that order.

_Example:_ Imagine _agentA4_, _agentA3_ and _agentA20_ want to assemble an item that requires 5 pieces of _item1_. Further, let all agents carry 2 pieces of _item1_ and _agentA4_ be the "main" assembler (i.e. the one that uses the _assemble_ action). Then, the first 2 pieces of _item1_ are taken from _agentA4_ since it is the initiator. Another 2 pieces are taken from _agentA3_ and the last one is taken from _agentA20_ (since _agentA3_'s name is shorter).

No | Parameter | Meaning
--- | --- | ---
0 | Agent | Name of an agent who uses the _assemble_ action and whom this agent should help.

Failure Code | Reason
--- | ---
failed_wrong_param | Not exactly 1 parameter has been given.
failed_unknown_agent | No agent by the given name is known.
failed_counterpart | The initiator's action has failed or is not _assemble_.
failed_tools | Some agent role is missing.
failed_location | The given agent is too far away.

### deliver_job

Delivers items towards the completion of a job. The agent is automatically drained of all items matching the job's remaining requirements.

No | Parameter | Meaning
--- | --- | ---
0 | Job | The name of the job to deliver items for.

Failure Code | Reason
--- | ---
failed_wrong_param | Not exactly 1 parameter has been given.
failed_unknown_job | No job by the given name is known.
failed_job_status | The given job is not active, or the job is an auction and has not been assigned yet or has not been assigned to the agent's team.
failed_location | The agent is not in the storage associated with the job.
successful_partial | Not really a failue. Items have been delivered but the job has not been completed by this action.
useless | The agent does not have any items to contribute to the job.

### bid_for_job

Places a bid for an auction job. The bid has to be lower than the current lowest bid.

No | Parameter | Meaning
--- | --- | ---
0 | Job | Name of the job to bid on.
1 | Bid | The bid to place.

Failure Code | Reason
--- | ---
failed_wrong_param | Not exactly 2 parameters have been given, or the bid is not a positive integer.
failed_unknown_job | No job by the given name is known.
failed_job_type | The job is not an auction.
failed_job_status | The job's auctioning phase is over.

### dump

Destroys a number of items at a dump facility.

No | Parameter | Meaning
--- | --- | ---
0 | Item | Name of the item to destroy.
1 | Amount | How many items to destroy.

Failure Code | Reason
--- | ---
failed_wrong_param | Not exactly 2 parameters have been given.
failed_location | The agent is not in a facility.
failed_wrong_facility | The agent is not at a dump location.
failed_unknown_item | No item by the given name is known.
failed_item_amount | The given amount is not a positive integer or more than the agent is carrying.

#### charge

Charges the agent's battery at a charging station.

No parameters.

Failure Code | Reason
--- | ---
failed_wrong_param | Parameters have been given.
failed_location | The agent is not in a facility.
failed_wrong_facility | The agent is not in a charging station.
failed_facility_state | The charging station is currently out of order due to a blackout.

### recharge

**Tries** to use the agent's solar collectors to recharge its battery (by 1).

No parameters.

Failure Code | Reason
--- | ---
failed_wrong_param | Parameters have been given.
failed | The action failed because it's not supposed to always succeed.

### gather

Gathers a resource from a resource node.

No parameters.

Failure Code | Reason
--- | ---
failed_wrong_param | Parameters have been given.
failed_location | The agent is not in a facility.
failed_wrong_facility | The agent is not in a resource node.
failed_capacity | The agent does not have enough free space to carry the resource.
partial_success | Worked on the node, but not enough yet to get a resource.

### build

Builds a _well_ at the agent's current location.

Uses zero (0) parameters to build up an existing well, or one (i) parameter to build a new well:

No | Parameter | Meaning
--- | --- | ---
0 | WellType | The name of the well type to build.

Failure Code | Reason
--- | ---
failed_wrong_param | More than 1 parameter given.
failed_location | No well to build up (0) or current location already occupied by another facility (i).
failed_unknown_facility | Specified well type not found.
failed_resources | Not enough massium to build the well.
failed_wrong_facility | Current location is not a well.

### dismantle

Dismantles an existing well.

No parameters.

Failure Code | Reason
--- | ---
failed_wrong_param | Parameters were given.
failed_location | Agent is not at a well location.

### trade

Sells an item at a _shop_.

No | Parameter | Meaning
--- | --- | ---
0 | Item | Name of the item to sell.
1 | Amount | Quantity of that item to sell.

Failure Code | Reason
--- | ---
failed_wrong_param | Not exactly 2 parameters have been given.
failed_unknown_item | No item by the given name is known.
failed_item_type | The item is a resource (not tradeable).
failed_item_amount | Invalid amount parameter (non-positive or more than carried).
failed_wrong_facility | Agent is not at a shop location.

### continue

Follows an agent's route or does nothing if the agent has no route.

No parameters.

Failure Code | Reason
--- | ---
failed_wrong_param | Parameters have been given.
failed_no_route | The agent's route could not be followed any longer (charge may be too low).

### abort

Does nothing and clears the agent's route (if it exists).

### unknownAction

This action is substituted if an agent submitted an action of unknown type.

### randomFail

This action is substituted if the agent's action randomly failed.

### noAction

This action is substituted if the agent did not send an action in time.

## Percepts

Percepts are sent by the server as XML files and contain information about the current simulation. Initital percepts (sent via `SIM-START` messages) contain static information while other percepts (sent via `REQUEST-ACTION` messages) contain information about the current simulation state.

The complete XML format is discussed in [protocol.md](protocol.md).

### Initial percept

This percept contains information that does not change during the whole simulation. As mentioned in the protocol description, everything is contained in a `simulation` element.

Complete Example (with bogus values):

```XML
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<message timestamp="1518531678919" type="sim-start">
  <simulation cellSize="500" centerLat="48.8424" centerLon="2.3209"
    id="2017-QuickTest-Sim" map="paris" maxLat="48.9" maxLon="2.41"
    minLat="48.82" minLon="2.26" name="agentA1" proximity="5"
    seedCapital="10" steps="10000" team="A">
    <role baseBattery="10000" baseLoad="10000" baseSkill="50" baseSpeed="10000"
      baseVision="600" maxBattery="11000" maxLoad="11000" maxSkill="10000"
      maxSpeed="11000" maxVision="1000" name="car"/>
    <item name="item0" volume="5"/>
    <item name="item2" volume="8"/>
    <item name="item1" volume="7"/>
    <item name="item7" volume="6">
      <item name="item1"/>
      <item name="item4"/>
      <item name="item0"/>
      <role name="car"/>
      <role name="truck"/>
    </item>
    <item name="item4" volume="6">
      <item name="item1"/>
      <item name="item2"/>
      <item name="item0"/>
      <role name="drone"/>
      <role name="truck"/>
    </item>
    <item name="item3" volume="9">
      <item name="item1"/>
      <item name="item2"/>
      <item name="item0"/>
      <role name="truck"/>
    </item>
    <item name="item6" volume="6">
      <item name="item1"/>
      <item name="item2"/>
      <item name="item3"/>
      <role name="car"/>
    </item>
    <item name="item5" volume="7">
      <item name="item1"/>
      <item name="item4"/>
      <item name="item2"/>
      <item name="item3"/>
      <item name="item0"/>
      <role name="car"/>
    </item>
    <upgrade cost="1000" name="vision" step="50"/>
    <upgrade cost="200" name="load" step="10"/>
    <upgrade cost="1000" name="skill" step="1"/>
    <upgrade cost="300" name="battery" step="5"/>
    <upgrade cost="1000" name="speed" step="1"/>
    <well cost="2112" efficiency="17" initialIntegrity="30"
      integrity="61" name="wellType2"/>
    <well cost="1546" efficiency="12" initialIntegrity="45"
      integrity="90" name="wellType1"/>
    <well cost="964" efficiency="7" initialIntegrity="34"
      integrity="69" name="wellType0"/>
  </simulation>
</message>

```

#### Simulation details

The `simulation` tag has attributes for the simulation `id`, the name of the `map` that is used and its `bounds`, the `seed capital`, the number of simulation `steps` to be played and the name of the agent's `team`.

#### Role details

The percept also contains the agent's `role` and its details; the name and base and max values for speed, load, battery, vision and skill.

#### Item details

Each item type present in the simulation has a child node in the simulation element. It contains the item's unique `name` and `volume`.

If the item has to be assembled, the necessary parts are included as child nodes (`item` and `role` elements) of the item element.

#### Contest note

The roles and their details will be defined (and made public) in before and not change between simulations.

### Step percept

This percept contains information about the simulation state at the beginning of each step.

Example:

```XML
<message timestamp="1518532127931" type="request-action">
  <percept deadline="0" id="0">
    <simulation step="28"/>
    <self charge="9999" chargeMax="10000" facility="shop1" lat="48.8321" load="27" loadMax="11000" lon="2.37036" name="agentA1" role="truck" skill="50" speed="10000" team="A" vision="600">
      <action result="successful" type="goto">
        <params>shop1</params>
      </action>
      <items amount="4" name="item0"/>
      <items amount="1" name="item1"/>
    </self>
    <team massium="0" score="0"/>
    <entity lat="48.8321" lon="2.37036" name="agentA1" role="car" team="A"/>
    <chargingStation lat="48.85235" lon="2.2966" name="chargingStation0" rate="63"/>
    <chargingStation lat="48.82805" lon="2.34884" name="chargingStation1" rate="74"/>
    <chargingStation lat="48.85461" lon="2.38234" name="chargingStation2" rate="91"/>
    <chargingStation lat="48.89517" lon="2.2898" name="chargingStation3" rate="66"/>
    <chargingStation lat="48.86315" lon="2.33847" name="chargingStation4" rate="115"/>
    <chargingStation lat="48.86259" lon="2.35423" name="chargingStation5" rate="61"/>
    <dump lat="48.85051" lon="2.27233" name="dump0"/>
    <dump lat="48.84405" lon="2.33852" name="dump1"/>
    <dump lat="48.82756" lon="2.36615" name="dump2"/>
    <dump lat="48.86625" lon="2.28654" name="dump3"/>
    <dump lat="48.86241" lon="2.30136" name="dump4"/>
    <dump lat="48.88224" lon="2.3806" name="dump5"/>
    <dump lat="48.87846" lon="2.40522" name="dump6"/>
    <shop lat="48.85576" lon="2.32994" name="shop0"/>
    <shop lat="48.8321" lon="2.37036" name="shop1"/>
    <shop lat="48.85703" lon="2.38503" name="shop2"/>
    <shop lat="48.86482" lon="2.27006" name="shop3"/>
    <shop lat="48.88191" lon="2.33994" name="shop4"/>
    <shop lat="48.87743" lon="2.37683" name="shop5"/>
    <storage lat="48.83797" lon="2.27845" name="storage0" totalCapacity="12908" usedCapacity="0"/>
    <storage lat="48.82751" lon="2.31396" name="storage1" totalCapacity="13083" usedCapacity="10">
      <item delivered="0" name="item0" stored="2"/>
    </storage>
    <storage lat="48.85846" lon="2.37058" name="storage2" totalCapacity="10475" usedCapacity="0"/>
    <storage lat="48.834" lon="2.39991" name="storage3" totalCapacity="11656" usedCapacity="0"/>
    <storage lat="48.86021" lon="2.27621" name="storage4" totalCapacity="13676" usedCapacity="0"/>
    <storage lat="48.89115" lon="2.34152" name="storage5" totalCapacity="12432" usedCapacity="0"/>
    <storage lat="48.87917" lon="2.40604" name="storage6" totalCapacity="13994" usedCapacity="0"/>
    <workshop lat="48.82829" lon="2.28847" name="workshop0"/>
    <workshop lat="48.85105" lon="2.37446" name="workshop1"/>
    <workshop lat="48.83679" lon="2.40162" name="workshop2"/>
    <workshop lat="48.8963" lon="2.29449" name="workshop3"/>
    <workshop lat="48.88298" lon="2.31242" name="workshop4"/>
    <workshop lat="48.89543" lon="2.3725" name="workshop5"/>
    <workshop lat="48.8862" lon="2.39122" name="workshop6"/>
    <job end="57" id="job1" reward="19" start="5" storage="storage6">
      <required amount="1" name="item3"/>
      <required amount="1" name="item5"/>
      <required amount="1" name="item6"/>
    </job>
    <job end="79" id="job6" reward="26" start="17" storage="storage3">
      <required amount="1" name="item3"/>
      <required amount="2" name="item6"/>
      <required amount="1" name="item7"/>
    </job>
    <job end="79" id="job8" reward="19" start="20" storage="storage4">
      <required amount="2" name="item5"/>
      <required amount="1" name="item6"/>
    </job>
    <job end="126" id="job11" reward="777" start="27" storage="storage1">
      <required amount="3" name="item0"/>
    </job>
    <job end="93" id="job13" reward="28" start="28" storage="storage4">
      <required amount="1" name="item3"/>
      <required amount="1" name="item6"/>
      <required amount="1" name="item7"/>
    </job>
    <auction auctionTime="5" end="74" fine="41" id="job2" reward="41" start="9" storage="storage5">
      <required amount="2" name="item3"/>
      <required amount="3" name="item5"/>
      <required amount="5" name="item6"/>
    </auction>
    <auction auctionTime="2" end="126" fine="10002" id="job12" reward="1001" start="27" storage="storage1">
      <required amount="3" name="item0"/>
    </auction>
    <mission auctionTime="0" end="126" fine="1000" id="job10" lowestBid="1000" reward="1000" start="27" storage="storage1">
      <required amount="3" name="item0"/>
    </mission>
  </percept>
</message>
```

The information is contained in the `percept` element within the message. This element contains an arbitrary number of child nodes, each representing an element of the simulation.

#### Self details

The `self` element contains information about the agent itself; its current battery charge and its maximum battery charge (depending on the current upgrade status); the same for carrying capacity (load). Also the agent's current vision, skill, position, role and team are listed. The action the agent executed in the last step together with its result is included in a child node of the `self` element.

Also, a child element for each carried item type is nested. Finally, if the agent currently follows a route, each waypoint of that route is listed in a child node, containing its location and index within the route.

#### Team details

The `team` element contains information about the agent's team; currently only how much massium it owns.

#### Entity details

For each entity (or agent) in the simulation __in the current vision radius__, one `entity` element is added.

Example:

```XML
<entity lat="51.4659" lon="-0.1035" name="agentB3" role="SampleRole" team="B"/>
```

Each of these elements contains the entity's name, position, role and team.

#### Facility details

For each facility, a specific element is included.

##### Shop details

Example:

```XML
<shop lat="51.4861" lon="-0.1477" name="shop1" restock="3">
  <item amount="8" name="tool7" price="211"/>
  <item amount="10" name="item0" price="217"/>
</shop>
```

For each shop, its name, position and restock value are included. Each shop contains a child node for each item type that can currently be bought, consisting of the item's name, its price and the available quantity.

##### Workshop details

Example:

```XML
<workshop lat="51.4983" lon="-0.0356" name="workshop3"/>
```

##### Charging station details

Example:

```XML
<chargingStation lat="51.5182" lon="-0.0361" name="chargingStation6" rate="88"/>
```

##### Dump details

Example:

```XML
<dump lat="51.5163" lon="-0.1588" name="dump2"/>
```

##### Storage details

Example:

```XML
<storage lat="51.4906" lon="-0.0825" name="storage6" totalCapacity="9277"
         usedCapacity="0">
         <item delivered="3" name="item0" stored="0"/>
</storage>
```

The storage contains a child node for each item type that is stored or delivered (or both) for an agent's team.

##### ResourceNode details

Example:

```XML
<resourceNode lat="51.478" lon="-0.03632" name="resourceNode1" resource="item7"/>
```

The resource node element contains the item that can be mined. Attention: This percept is only visible if the agent is "close" to the node (see `visibilityRange` parameter).

##### Well details

Example:

```XML
<well lat="51.478" lon="-0.03632" team="A" type="wellType1" integrity="555"/>
```

#### Job details

An element for each job is added (`job`, `auction`, `mission` or `posted`).

__Regular job example:__ (all non-auction jobs not posted by the team)

```XML
<job start="59" end="159" id="job0" reward="5018" storage="storage2">
  <required amount="3" name="item0"/>
</job>
```

__Auction job example:__ (all auctions in auction state and active auctions assigned to the team)

```XML
<auction auctionTime="5" start="10" end="100" fine="500" id="job2"
         lowestBid="20" reward="1000" storage="storage0">
  <required amount="777" name="item0"/>
</auction>
```

Please note that the `lowestBid` attribute is not present if no bids have been placed yet.

Assigned auctions can be recognized by comparing `start + auctionTime` with the current step. If the auction is still visible after the `auctionTime`, the team has won the auction.

__Mission job example:__ (all currently active missions for the team)

```XML
<mission auctionTime="0" start="10" end="100" fine="500" id="job2"
         lowestBid="1000" reward="1001" storage="storage0">
  <required amount="42" name="item1"/>
</mission>
```

## Configuration

Each simulation configuration is one object in the `match` array.

Example:

```JSON
{
  "id" : "2018-SampleSimulation",
  "scenarioClass" : "city.CitySimulation",
  "steps" : 1000,
  "map" : "paris",
  "seedCapital" : 5000,
  "minLon" : 2.26,
  "maxLon" : 2.41,
  "minLat" : 48.82,
  "maxLat" : 48.90,
  "centerLat" : 48.8424,
  "centerLon" : 2.3209,
  "proximity" : 5,
  "cellSize" : 200,
  "randomSeed" : 17,
  "randomFail" : 1,
  "gotoCost" : 1,
  "rechargeRate" : 2,

  "upgrades" : {},

  "roles" : {},

  "entities" : [
    {"car" : 2},
    {"drone" : 2},
    {"motorcycle" : 1},
    {"truck" : 1}
  ],

  "generate" : {}
}
```

The `roles` object has already been discussed in the [Roles](#roles) section, while the `generate` block will be subject of the [Random generation](#random-generation) section.

For each simulation, the following parameters may be specified:

* __id__: a name for the simulation; e.g. used in replays together with the starting time
* __scenarioClass__: the class containing the scenario; needs to be written as above for this scenario
* __steps__: the number of steps the simulation will take
* __map__: the map to use; needs to be in the `server/osm` folder as `XYZ.osm.pbf` file
* __seedCapital__: the amount of massium owned by each team at the start of the simulation
* __min/maxLon/Lat__: the map bounds; the declared area must be present in the map file
* __centerLon/Lat__: an internal value used for positioning things and routing; basically, any location of the map is considered reachable if there exists a route between this "center" and that location in both directions
* __proximity__: the proximity value (see [Locations](#locations) section)
* __cellSize__: the cellSize value (see [Locations](#locations) section)
* __randomSeed__: the random seed that is used for map generation and action execution
* __randomFail__: the probability for any action to fail (in %)
* __gotoCost__: the energy cost for 1 goto action
* __rechargeRate__: the energy that is restored between 1 and 2 times with 1 recharge action

The number of agents per role is defined in the `entities` array. Each object may have only one key (the name of the role). The value for the key is the number of agents for that role.

Agents are assigned their role according to their position in the team config. In the above example, the first two agents would be Car agents, the next two would be Drone agents, etc.

## Random generation

In the first section of the random generation the parameters for the generation of the facilities can be specified:

* __quadSize__: cell size of the grid that is used for positioning
* Parameters for chaging stations:
  * __density__: probability of placing a facility per quadrant (or number of facilities to place if >1)
  * __rateMin/Max__: bounds for charging rate
* Parameters for shops:
  * __density__: see above
  * __tradeModMin/Max__: Multiplier for prices items are bought at.
* Parameters for dumps:
  * __density__: see above
* Parameters for workshops:
  * __density__: see above
* Parameters for storage:
  * __density__: see above
  * __capacityMin/Max__: bounds for the storage's capacity
* Parameters for resource nodes:
  * __density__: see above
  * __thresholdMin/Max__: bounds for the threshold that has to be surpassed with accumulated skill values to yield a resource
* Parameters for wells:
  * __wellTypesMin/Max__: bounds for the number of well types
  * __baseEfficiencyMin/Max__: bounds for the efficiency of the "worst" well type
  * __efficiencyIncreaseMin/Max__: bounds for the increase of efficiency for each next type
  * __baseIntegrityMin/Max__: bounds for the well's integrity
  * __costFactor__: multiplyer for the well's price (combined with efficiency)

Second section - generation of items:

* __resourcesMin/Max__: bounds for the number of resource items
* __levelDecreaseMin/Max__: bounds for the number of items by which the next level of the item graph will decrease
* __graphDepthMin/Max__: bounds for the number of layers of the item graph
* __volMin/Max__: bounds for the volume of the items
* __partsMin/Max__: bounds for the number of items required to assemble an item

Third section - generation of jobs:

* __jobProbability__: probability of a job appearing in each step of the simulation
* __auctionProbability__: same but for auctions
* __missionProbability__: same but for missions
* __jobDurationMin/Max__: bounds for a job's duration
* __itemCountMin/Max__: bounds for the number of items a job may
* __rewardScale__: a scale factor for the jobs' rewards
* __rewardModMin/Max__: bounds for the value that is added to calculated rewards
* Parameters for auctions:
  * __auctionTime__: the duration of the auction part

## Commands

A number of commands are handled by this scenario, which may help with debugging or testing your agents.

`print facilities`: prints all facilities to the console

`print items`: prints all items to the console

`give itemX agentY Z`: gives Z units of item "itemX" to an agent "agentY" (if the agent has enough free space)

`store storageX itemY A Z`: stores Z units of item "itemY" for team "A" in storage "storageX" (if the storage has enough free space)

`addJob X Y Z storage0 item0 A item1 B ...`: adds a new job to the system, starting in step X, ending in step Y, with reward Z, target storage "storage0", and requiring A units of "item0", B units of "item1", etc.

`addAuction A B C D E storage0 item0 X item1 Y ...`: similar to `addJob`, A is the start of the job, B the job's end, C the reward, D the auction time and E the auction's fine
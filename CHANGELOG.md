# MASSim CHANGELOG

## 2020

### MASSim 2020-2.0 - Registration release

* scenario
  * new more distinctive failure codes for some actions
  * new percept for team size
  * new status messages in the protocol for querying which teams are currently playing which simulation
  * randomized which agents start in clusters
  * starting clusters have variable size (configurable interval)
  * task boards can now be added via the setup file
  * some new sample configs
* eismassim
  * new entity for the new status messages
* monitor
  * now wraps around (+ marks the origin)
  * new view for each agent's local perspective
  * zoom!
  * select agents!
  * track accepted tasks

### MASSim 2020-1.0 - Initial release

* general
  * update to Java 13
* scenario
  * the grid now *loops* horizontally and vertically (is it a donut?)
  * new taskboards
  * task rewards now decrease over time
  * agents may now start close to another agent
* server
  * simpler team configuration (create many "accounts" at once)
    * similar adjustments to eismassim and javaagents
  * new option to add a pause between consecutive simulations
* eismassim
  * update to newest EIS version
  * new option that delivers percepts only once

## 2019

### MASSim 2.1 - bug-fixes

* some not so minor bugs fixed
* monitor improvements

### MASSim 2.0 - feature complete

* many bug-fixes
* monitor improvements
* scenario
  * added percept for vision and team
  * added percept for which things are currently attached to another agent
  * added "clear" action that removes blocks and obstacles and disables agents
    * agents may now become temporarily disabled
    * agents now have "energy"
      * new percepts for energy and disabled status
  * added map events
    * are perceived similar to clear actions
    * will clear the marked area
    * will create new obstacles around the center of the event

### MASSim 1.0 - new scenario!

## 2018

### Package release 1.2 - the final 2018 package

* monitor
  * show the sum of current wells' efficiency
* eismassim
  * fix an entity type set exception
* server
  * cache routes if they do not change
  * fix build action result
  * fix generator using wrong parameter

### Package release 1.1

* protocol
  * changed xml structure of items to attributes
* eismassim
  * upgraded to EIS 0.6.0
  * fix: added percepts that were missing
  * entity type is now set to the name of the role when the info is received
* scenario
  * made auction duration dynamic
  * added some more sample configurations

### Package release 1.0

* scenario
  * changed goal: get the highest score!
  * major additions
    * added wells to generate score points with
    * added build/dismantle actions to build/dismantle wells
    * roles now have a new skill attribute determining the efficiency of build, dismantle and gather actions
    * roles now have a new vision attribute describing an agent's vision radius
    * roles now have values for maximum speed, battery, load, vision and skill to support the
    * new upgrade system! each agent can upgrade its speed, battery, load, vision and skill
  * minor revisions
    * entities (and wells) are now only visible in an agent's vision radius
    * currency renamed to massium
      * used to build wells or upgrade agents
    * removed blackouts
  * item-related
    * shops do not sell items any more
      * added new trade action to sell assembled items in shops
    * tools have been removed, assembly now requires the presence of roles instead
    * assembled items require at most one instance of each part
  * removed actions
    * post_job
      * nobody used it anyway
    * skip (redundant)
  * under the hood changes
    * simulation instance generation has been simplified
    * resource nodes are more common
    * resource nodes use skill system to determine when they yield a resource
    * volume of assembled items does not depend on required parts anymore
    * simplified job generation

## 2017

### Package release 1.7

* monitor
  * added replay mechanism
* scenario
  * added the 2 missing tournament maps
  * added config files for the new maps
* server
  * added some more log output

### Package release 1.6

* scenario
  * added measure to prevent empty jobs
  * fixed issue where agents could be trapped in some parts of the map
  * changed cellSize to Integer
* monitor
  * added rendering of agents' routes

### Package release 1.5

* scenario
  * added percepts for map center, cellSize and proximity
* monitor
  * fixed display of stored items
  * added a third team color
  * added display of action parameters
* eismassim
  * improved entity re/connection
  * added the new percepts
* server
  * fixed a bug where actions could go missing on extremely slow hardware
  * refactored the scenario generation

### Package release 1.4

* protocol
  * added the agent's name to sim-start
  * fixed sim-end message not being deserialized
  * XML request-action messages now organized for better readability
* eismassim
  * added percept for agent's name
  * fixed abort action deemed not being supported
  * fixed a bug where an EISEntity could create multiple listening threads
  * added method for checking an entity's connection to a server to the environment interface
  * starting the environment now also starts its main thread, which will cause it to try to connect its entities with a massim server if they are not connected
* monitor
  * selection of multiple agents in the same location now cycles through all of them
  * zoom to new location on map change#
* scenario
  * fixed tools not being listed in storages after being stored
  * items of completed jobs are now mostly fed back into random shops
* server
  * will now only parse as many agents from team config as required and make up its own names if too few agents are configured

### Package release 1.3

* scenario:
  * blackouts added (charging stations may not work for a couple of steps)
  * assembly assistants are now sorted by their number (i.e. first by length of their name, then lexicographically) when determining the order in which to remove their items
  * fixed missions' rewards being higher than lowestBid
  * adapted configuration for 28 agents
  * fixed a severe bug when abort action was used
* monitor: limit number of displayed jobs and fix displayed items in shops
* protocol: added min/max lat and lon to sim-start percept
* eismassim: removed obsolete missionId parameter from mission percepts

### Package release 1.2

* changed xml format of tools in roles and item-requirements
* added tools to sim-start message
* fixed a possible memory leak that could occur if some agents would not connect
* added a special testing mode for simple (non-assembly) jobs
  * set difficultyMin/Max and missionDifficultyMax to 0
* changed "item" percept to "hasItem" for carried items (eismassim)
* added missing facility percept (eismassim)

### Package release 1.1

* improved job generation (replaced temporary one)
* fixed a bug where assembled items could (or would) have volume 0
* fixed a bug where missions were not added to the percept

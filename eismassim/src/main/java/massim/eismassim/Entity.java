package massim.eismassim;

import eis.PerceptUpdate;
import eis.exceptions.PerceiveException;

public abstract class Entity implements Runnable {

    private String name;

    public Entity(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Retrieves all percepts for this entity.
     * If scheduling is enabled, the method blocks until a new action id, i.e. new percepts, are received
     * or the configured timeout is reached.
     * If queued is enabled, scheduling is overridden. Also, if queued is enabled, this method has to be called
     * repeatedly, as only one collection of percepts is removed from the queue with each call (until an empty list
     * is returned).
     * @return the percepts for this entity
     * @throws PerceiveException if timeout configured and occurred
     */
    public abstract PerceptUpdate getPercepts() throws PerceiveException;
}
package massim.simulation.game.environment;

import massim.protocol.messages.scenario.data.Thing;

import java.util.HashSet;
import java.util.Set;

public abstract class Attachable extends Positionable {

    private Set<Attachable> attachments = new HashSet<>();

    public Attachable(Position position) {
        super(position);
    }

    public abstract Thing toPercept();

    void attach(Attachable other) {
        attachments.add(other);
        other.requestAttachment(this);
    }

    void detach(Attachable other) {
        attachments.remove(other);
        other.requestDetachment(this);
    }

    Set<Attachable> getAttachments() {
        return new HashSet<>(attachments);
    }

    void detachAll() {
        attachments.forEach(this::detach);
    }


    private void requestAttachment(Attachable requester) {
        attachments.add(requester);
    }

    private void requestDetachment(Attachable requester) {
        attachments.remove(requester);
    }
}

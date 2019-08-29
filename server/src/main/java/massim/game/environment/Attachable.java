package massim.game.environment;

import massim.game.Entity;
import massim.protocol.data.Position;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class Attachable extends Positionable {

    private Set<Attachable> attachments = new HashSet<>();

    public Attachable(Position position) {
        super(position);
    }

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

    public void detachAll() {
        new ArrayList<>(attachments).forEach(this::detach);
    }

    private void requestAttachment(Attachable requester) {
        attachments.add(requester);
    }

    private void requestDetachment(Attachable requester) {
        attachments.remove(requester);
    }

    /**
     * @return a set of all attachments and attachments attached to these attachments (and so on)
     * including this Attachable
     */
    public Set<Attachable> collectAllAttachments() {
        var attachables = new HashSet<Attachable>();
        attachables.add(this);
        Set<Attachable> newAttachables = new HashSet<>(attachables);
        while (!newAttachables.isEmpty()) {
            var tempAttachables = new HashSet<Attachable>();
            for (Attachable a : newAttachables) {
                for (Attachable a2 : a.getAttachments()) {
                    if (attachables.add(a2)) tempAttachables.add(a2);
                }
            }
            newAttachables = tempAttachables;
        }
        return attachables;
    }

    public boolean isAttachedToAnotherEntity() {
        return collectAllAttachments().stream().anyMatch(a -> a instanceof Entity && a != this);
    }
}

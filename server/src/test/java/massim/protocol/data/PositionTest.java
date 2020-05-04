package massim.protocol.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class PositionTest {

    @Test
    public void distanceTo() {
        int dim = 100;
        Position.setGridDimensions(dim, dim);

        var p1 = Position.of(99,99);
        var p2 = Position.of(0,99);
        var p3 = Position.of(99,0);
        var p4 = Position.of(0,0);
        var p5 = Position.of(0,50);

        assert p1.distanceTo(p2) == 1;
        assert p1.distanceTo(p3) == 1;
        assert p1.distanceTo(p4) == 2;
        assert p2.distanceTo(p3) == 2;
        assert p2.distanceTo(p4) == 1;
        assert p3.distanceTo(p4) == 1;

        assert p1.distanceTo(p2) == p2.distanceTo(p1);
        assert p1.distanceTo(p3) == p3.distanceTo(p1);
        assert p1.distanceTo(p4) == p4.distanceTo(p1);
        assert p2.distanceTo(p3) == p3.distanceTo(p2);
        assert p2.distanceTo(p4) == p4.distanceTo(p2);
        assert p3.distanceTo(p4) == p4.distanceTo(p3);

        assert p4.distanceTo(p5) == 50;
        assert p5.distanceTo(p4) == 50;
    }
}
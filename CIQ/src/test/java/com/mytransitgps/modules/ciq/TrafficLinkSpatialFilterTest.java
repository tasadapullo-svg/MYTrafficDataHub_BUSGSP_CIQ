package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 验证 API01 道路筛选必须以线段与 polygon 相交为主，而不是 midpoint。 */
class TrafficLinkSpatialFilterTest {
    @Test
    void lineCrossingPolygonIsInScopeEvenWhenMidpointIsOutside() {
        Rect core = new Rect(0, 0, 2, 2);
        Segment link = new Segment(-1, 1, 5, 3.2);

        assertTrue(intersects(link, core));
        assertFalse(core.contains(link.midX(), link.midY()));
    }

    @Test
    void lineFullyOutsidePolygonIsOutOfScope() {
        Rect core = new Rect(0, 0, 2, 2);
        Segment link = new Segment(3, 3, 4, 4);

        assertFalse(intersects(link, core));
    }

    private static boolean intersects(Segment segment, Rect rect) {
        if (rect.contains(segment.x1, segment.y1) || rect.contains(segment.x2, segment.y2)) return true;
        return segmentsIntersect(segment, new Segment(rect.minX, rect.minY, rect.maxX, rect.minY))
                || segmentsIntersect(segment, new Segment(rect.maxX, rect.minY, rect.maxX, rect.maxY))
                || segmentsIntersect(segment, new Segment(rect.maxX, rect.maxY, rect.minX, rect.maxY))
                || segmentsIntersect(segment, new Segment(rect.minX, rect.maxY, rect.minX, rect.minY));
    }

    private static boolean segmentsIntersect(Segment a, Segment b) {
        return orientation(a.x1, a.y1, a.x2, a.y2, b.x1, b.y1)
                * orientation(a.x1, a.y1, a.x2, a.y2, b.x2, b.y2) <= 0
                && orientation(b.x1, b.y1, b.x2, b.y2, a.x1, a.y1)
                * orientation(b.x1, b.y1, b.x2, b.y2, a.x2, a.y2) <= 0;
    }

    private static double orientation(double ax, double ay, double bx, double by, double cx, double cy) {
        return Math.signum((bx - ax) * (cy - ay) - (by - ay) * (cx - ax));
    }

    private record Rect(double minX, double minY, double maxX, double maxY) {
        boolean contains(double x, double y) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }

    private record Segment(double x1, double y1, double x2, double y2) {
        double midX() { return (x1 + x2) / 2.0; }
        double midY() { return (y1 + y2) / 2.0; }
    }
}

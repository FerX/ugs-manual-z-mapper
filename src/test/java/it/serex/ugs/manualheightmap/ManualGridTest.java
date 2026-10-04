package it.serex.ugs.manualheightmap;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class ManualGridTest {
    @Test public void gridAndSerpentineOrder() {
        ManualGrid grid = ManualGrid.create(0, 20, 0, 20, 3, 3);
        assertEquals(9, grid.size());
        assertEquals(0, grid.measuredCount());
        for (ManualGrid.Point point : grid.points()) assertEquals(0, point.z(), 0);
        int[] expected = {0, 1, 2, 5, 4, 3, 6, 7, 8};
        int current = -1;
        for (int index : expected) {
            current = grid.nextUnmeasuredAfter(current);
            assertEquals(index, current);
            grid.record(index, 0);
        }
        assertTrue(grid.complete());
        assertEquals(-1, grid.nextUnmeasuredAfter(8));
    }

    @Test public void compatibleLastIntervalAndExport() throws Exception {
        ManualGrid grid = ManualGrid.create(0, 100, 0, 60, 5, 5);
        assertEquals(5, grid.columns());
        assertEquals(4, grid.rows());
        assertEquals(25, grid.step(), 1e-9);
        for (int i = 0; i < grid.size(); i++) grid.record(i, i * 0.01);
        Path xyz = Files.createTempFile("heightmap-", ".xyz");
        try {
            XyzExporter.write(xyz, grid);
            List<String> lines = Files.readAllLines(xyz);
            assertEquals(20, lines.size());
            assertEquals("0.000000 0.000000 0.000000", lines.get(0));
            assertEquals("0.000000 25.000000 0.010000", lines.get(1));
            assertEquals("0.000000 60.000000 0.030000", lines.get(3));
            assertEquals("100.000000 60.000000 0.190000", lines.get(19));
        } finally { Files.deleteIfExists(xyz); }
    }

    @Test public void sessionRoundTripKeepsUnmeasuredZeroDistinct() throws Exception {
        ManualGrid grid = ManualGrid.create(0, 20, 0, 20, 3, 3);
        grid.record(0, 0);
        grid.record(1, -0.03);
        MeasurementSession session = new MeasurementSession(grid, "example.nc", 123L,
                1, 6, 100, -10, -20, -30);
        Path file = Files.createTempFile("heightmap-session-", ".properties");
        try {
            SessionStore.save(file, session);
            MeasurementSession restored = SessionStore.load(file);
            assertEquals(2, restored.grid.measuredCount());
            assertTrue(restored.grid.point(0).measured());
            assertEquals(0, restored.grid.point(0).z(), 0);
            assertFalse(restored.grid.point(2).measured());
            assertEquals(-0.03, restored.grid.point(1).z(), 1e-9);
            assertEquals(-30, restored.workOffsetZ, 0);
        } finally { Files.deleteIfExists(file); }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsIncompatibleAspectRatio() {
        ManualGrid.create(0, 100, 0, 10, 5, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNearDuplicateEdgePoints() {
        ManualGrid.create(0, 100, 0, 50.001, 5, 5);
    }

    @Test(expected = IllegalStateException.class)
    public void incompleteGridCannotExport() throws Exception {
        XyzExporter.write(Files.createTempFile("heightmap-incomplete-", ".xyz"),
                ManualGrid.create(0, 20, 0, 20, 3, 3));
    }
}

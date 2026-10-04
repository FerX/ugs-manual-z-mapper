package it.serex.ugs.manualheightmap;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class WorkAreaAnalyzerTest {
    @Test public void cuttingMovesExcludeRapidTravel() throws Exception {
        Path gcode = Files.createTempFile("heightmap-cut-", ".nc");
        try {
            Files.writeString(gcode, "G21 G90\nG0 Z5\nG0 X100 Y100\nG0 X10 Y10\nG1 Z-1 F100\nG1 X20 Y10\nG1 X20 Y20\nG1 X10 Y20\nG1 X10 Y10\nG0 Z5\nG0 X200 Y200\n");
            WorkAreaAnalyzer.Bounds bounds = WorkAreaAnalyzer.analyze(gcode.toFile());
            assertEquals(10, bounds.minX(), 1e-9);
            assertEquals(20, bounds.maxX(), 1e-9);
            assertEquals(10, bounds.minY(), 1e-9);
            assertEquals(20, bounds.maxY(), 1e-9);
        } finally { Files.deleteIfExists(gcode); }
    }

    @Test(expected = IllegalArgumentException.class)
    public void arcNeedsExplicitArea() throws Exception {
        Path gcode = Files.createTempFile("heightmap-arc-", ".nc");
        try {
            Files.writeString(gcode, "G21 G90\nG0 X0 Y0\nG1 Z-1\nG2 X10 Y10 I5 J0\n");
            WorkAreaAnalyzer.analyze(gcode.toFile());
        } finally { Files.deleteIfExists(gcode); }
    }
}

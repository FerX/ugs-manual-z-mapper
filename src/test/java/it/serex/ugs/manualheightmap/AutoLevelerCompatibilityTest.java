package it.serex.ugs.manualheightmap;

import com.willwinder.ugs.platform.surfacescanner.SurfaceScanner;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UnitUtils.Units;
import com.willwinder.universalgcodesender.utils.AutoLevelSettings;
import com.willwinder.universalgcodesender.utils.Settings;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Runs the grid reconstruction and point matching used by UGS AutoLeveler 2.1.26. */
public class AutoLevelerCompatibilityTest {
    @Test public void xyzFillsAutoLevelerGridIncludingShortLastInterval() throws Exception {
        ManualGrid grid = ManualGrid.create(0, 100, 0, 60, 5, 5);
        for (int i = 0; i < grid.size(); i++) grid.record(i, i * 0.01);
        Path xyz = Files.createTempFile("heightmap-autoleveler-", ".xyz");
        try {
            XyzExporter.write(xyz, grid);
            List<Position> positions = new ArrayList<>();
            for (String line : Files.readAllLines(xyz)) {
                String[] values = line.split(" ");
                positions.add(new Position(Double.parseDouble(values[0]), Double.parseDouble(values[1]),
                        Double.parseDouble(values[2]), Units.MM));
            }
            Settings settings = new Settings();
            settings.setPreferredUnits(Units.MM);
            BackendAPI backend = mock(BackendAPI.class);
            when(backend.getSettings()).thenReturn(settings);
            SurfaceScanner scanner = new SurfaceScanner(backend);
            AutoLevelSettings level = settings.getAutoLevelSettings();
            Position min = positions.get(0);
            Position max = positions.get(positions.size() - 1);
            level.setMin(min);
            level.setMax(max);
            level.setStepResolution(positions.get(1).getY() - positions.get(0).getY());
            scanner.update(min, max);
            scanner.reset();
            assertEquals(grid.columns(), scanner.getProbePositionGrid().length);
            assertEquals(grid.rows(), scanner.getProbePositionGrid()[0].length);

            int imported = 0;
            while (scanner.getNextProbePoint().isPresent()) {
                Position expected = scanner.getNextProbePoint().orElseThrow();
                Position actual = positions.stream().filter(p ->
                        Math.abs(p.getX() - expected.getX()) <= 0.01
                                && Math.abs(p.getY() - expected.getY()) <= 0.01).findFirst().orElseThrow();
                scanner.probeEvent(actual);
                imported++;
            }
            assertEquals(grid.size(), imported);
            assertTrue(scanner.isValid());
        } finally { Files.deleteIfExists(xyz); }
    }
}

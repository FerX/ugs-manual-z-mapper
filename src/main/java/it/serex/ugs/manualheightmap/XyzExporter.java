package it.serex.ugs.manualheightmap;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** AutoLeveler expects column-major data, with the first two lines differing in Y. */
public final class XyzExporter {
    private XyzExporter() {}

    public static void write(Path path, ManualGrid grid) throws IOException {
        if (!grid.complete()) throw new IllegalStateException("Misurare tutti i punti prima di esportare");
        Path target = path.toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), ".manual-heightmap-", ".xyz.tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (ManualGrid.Point point : grid.points()) {
                    writer.write(String.format(Locale.ROOT, "%.6f %.6f %.6f%n", point.x(), point.y(), point.z()));
                }
            }
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}

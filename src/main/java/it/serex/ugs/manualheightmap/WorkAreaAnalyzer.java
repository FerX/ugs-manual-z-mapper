package it.serex.ugs.manualheightmap;

import com.willwinder.universalgcodesender.gcode.GcodeParser;
import com.willwinder.universalgcodesender.gcode.GcodeParser.GcodeMeta;
import com.willwinder.universalgcodesender.gcode.util.Code;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UnitUtils.Units;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Extracts bounds of unambiguous cutting moves from the currently loaded G-code. */
public final class WorkAreaAnalyzer {
    public record Bounds(double minX, double maxX, double minY, double maxY) {}

    private WorkAreaAnalyzer() {}

    public static Bounds analyze(File file) throws Exception {
        if (file == null || !file.isFile()) throw new IOException("Nessun G-code caricato");
        GcodeParser parser = new GcodeParser();
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        boolean found = false;
        Position previous = null;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (GcodeMeta meta : parser.addCommand(line)) {
                    if (meta.point == null) continue;
                    Position end = meta.point.point().getPositionIn(Units.MM);
                    if (meta.code == Code.G2 || meta.code == Code.G3) {
                        throw new IllegalArgumentException("Il percorso contiene archi: impostare manualmente l'area X/Y per coprirne l'intera estensione");
                    }
                    if (meta.code == Code.G1 && Double.isFinite(end.getZ()) && end.getZ() <= 0
                            && Double.isFinite(end.getX()) && Double.isFinite(end.getY())) {
                        minX = Math.min(minX, end.getX()); maxX = Math.max(maxX, end.getX());
                        minY = Math.min(minY, end.getY()); maxY = Math.max(maxY, end.getY());
                        found = true;
                        if (previous != null && Double.isFinite(previous.getX()) && Double.isFinite(previous.getY())) {
                            minX = Math.min(minX, previous.getX()); maxX = Math.max(maxX, previous.getX());
                            minY = Math.min(minY, previous.getY()); maxY = Math.max(maxY, previous.getY());
                        }
                    }
                    previous = end;
                }
            }
        }
        if (!found || maxX <= minX || maxY <= minY) {
            throw new IllegalArgumentException("Impossibile riconoscere un'area di lavorazione bidimensionale: impostare i limiti X/Y manualmente");
        }
        return new Bounds(minX, maxX, minY, maxY);
    }
}

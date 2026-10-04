package it.serex.ugs.manualheightmap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Versioned, atomic session persistence. No initial Z=0 point becomes measured on load. */
public final class SessionStore {
    private SessionStore() {}

    public static void save(Path target, MeasurementSession session) throws IOException {
        Properties p = new Properties();
        ManualGrid g = session.grid;
        p.setProperty("format", "1");
        p.setProperty("units", "mm");
        p.setProperty("workFile", session.workFile);
        put(p, "workModified", session.workModified);
        put(p, "minX", g.minX()); put(p, "maxX", g.maxX());
        put(p, "minY", g.minY()); put(p, "maxY", g.maxY());
        put(p, "step", g.step()); put(p, "columns", g.columns()); put(p, "rows", g.rows());
        put(p, "approachZ", session.approachZ); put(p, "transferZ", session.transferZ);
        put(p, "descentFeed", session.descentFeed);
        put(p, "offsetX", session.workOffsetX); put(p, "offsetY", session.workOffsetY);
        put(p, "offsetZ", session.workOffsetZ);
        for (int i = 0; i < g.size(); i++) {
            ManualGrid.Point point = g.point(i);
            p.setProperty("point." + i, point.column() + "," + point.row() + "," + point.x()
                    + "," + point.y() + "," + point.z() + "," + point.measured());
        }
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, ".manual-heightmap-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "UGS manual heightmap session"); }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }

    public static MeasurementSession load(Path path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        try {
            if (!"1".equals(p.getProperty("format")) || !"mm".equals(p.getProperty("units")))
                throw new IllegalArgumentException("Versione o unità della sessione non supportate");
            int columns = integer(p, "columns"), rows = integer(p, "rows");
            if (columns < 2 || rows < 2 || (long) columns * rows > 10000)
                throw new IllegalArgumentException("Dimensioni della griglia non valide");
            List<ManualGrid.Point> points = new ArrayList<>(columns * rows);
            for (int i = 0; i < columns * rows; i++) {
                String[] parts = required(p, "point." + i).split(",", -1);
                if (parts.length != 6 || (!"true".equals(parts[5]) && !"false".equals(parts[5])))
                    throw new IllegalArgumentException("Punto non valido: " + i);
                points.add(new ManualGrid.Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4]), Boolean.parseBoolean(parts[5])));
            }
            ManualGrid grid = ManualGrid.restore(number(p, "minX"), number(p, "maxX"),
                    number(p, "minY"), number(p, "maxY"), number(p, "step"), columns, rows, points);
            double approach = number(p, "approachZ"), transfer = number(p, "transferZ"),
                    descent = number(p, "descentFeed"), ox = number(p, "offsetX"),
                    oy = number(p, "offsetY"), oz = number(p, "offsetZ");
            if (!Double.isFinite(approach) || !Double.isFinite(transfer) || transfer < approach
                    || !Double.isFinite(descent) || descent <= 0 || !Double.isFinite(ox)
                    || !Double.isFinite(oy) || !Double.isFinite(oz))
                throw new IllegalArgumentException("Parametri della sessione non validi");
            return new MeasurementSession(grid, required(p, "workFile"), Long.parseLong(required(p, "workModified")),
                    approach, transfer, descent, ox, oy, oz);
        } catch (IllegalArgumentException e) {
            throw new IOException("Sessione non valida: " + e.getMessage(), e);
        }
    }

    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null) throw new IllegalArgumentException("Campo mancante: " + key);
        return value;
    }
    private static double number(Properties p, String key) { return Double.parseDouble(required(p, key)); }
    private static int integer(Properties p, String key) { return Integer.parseInt(required(p, key)); }
    private static void put(Properties p, String key, double value) { p.setProperty(key, Double.toString(value)); }
    private static void put(Properties p, String key, long value) { p.setProperty(key, Long.toString(value)); }
}

package it.serex.ugs.manualheightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A rectangular grid with one common nominal step on X and Y. */
public final class ManualGrid {
    private static final double MIN_INTERVAL_MM = 0.02;
    public record Point(int column, int row, double x, double y, double z, boolean measured) {
        Point measuredAt(double value) {
            return new Point(column, row, x, y, value, true);
        }
    }

    private final double minX, maxX, minY, maxY, step;
    private final int columns, rows;
    private final List<Point> points;

    private ManualGrid(double minX, double maxX, double minY, double maxY,
                       double step, int columns, int rows, List<Point> points) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.step = step;
        this.columns = columns;
        this.rows = rows;
        this.points = points;
    }

    public static ManualGrid create(double minX, double maxX, double minY, double maxY,
                                    int maxColumns, int maxRows) {
        if (!finite(minX, maxX, minY, maxY) || maxX <= minX || maxY <= minY
                || maxColumns < 2 || maxRows < 2 || maxColumns > 1000 || maxRows > 1000) {
            throw new IllegalArgumentException("Area o limiti della griglia non validi");
        }
        double width = maxX - minX;
        double height = maxY - minY;
        double step = Math.max(width / (maxColumns - 1), height / (maxRows - 1));
        if (!Double.isFinite(step) || step <= 0 || step > Math.min(width, height)) {
            throw new IllegalArgumentException("Area troppo allungata per i limiti di righe e colonne: aumentare il limite sull'asse più lungo");
        }
        int columns = (int) Math.ceil(width / step - 1e-10) + 1;
        int rows = (int) Math.ceil(height / step - 1e-10) + 1;
        if (step < MIN_INTERVAL_MM || width - (columns - 2) * step < MIN_INTERVAL_MM
                || height - (rows - 2) * step < MIN_INTERVAL_MM) {
            throw new IllegalArgumentException("Punti troppo vicini per AutoLeveler: modifica i limiti dell'area o i massimi della griglia");
        }
        if (columns > maxColumns || rows > maxRows || (long) columns * rows > 10000) {
            throw new IllegalArgumentException("Troppi punti nella griglia");
        }
        List<Point> points = new ArrayList<>(columns * rows);
        for (int c = 0; c < columns; c++) {
            double x = c == columns - 1 ? maxX : minX + c * step;
            for (int r = 0; r < rows; r++) {
                double y = r == rows - 1 ? maxY : minY + r * step;
                points.add(new Point(c, r, x, y, 0, false));
            }
        }
        return new ManualGrid(minX, maxX, minY, maxY, step, columns, rows, points);
    }

    public static ManualGrid restore(double minX, double maxX, double minY, double maxY,
                                     double step, int columns, int rows, List<Point> saved) {
        if (!finite(minX, maxX, minY, maxY, step) || maxX <= minX || maxY <= minY
                || step <= 0 || step > Math.min(maxX - minX, maxY - minY)
                || columns < 2 || rows < 2 || (long) columns * rows > 10000
                || saved.size() != columns * rows) {
            throw new IllegalArgumentException("Dimensioni della sessione incoerenti");
        }
        List<Point> expectedPoints = new ArrayList<>(columns * rows);
        for (int c = 0; c < columns; c++) {
            double x = c == columns - 1 ? maxX : minX + c * step;
            for (int r = 0; r < rows; r++) {
                double y = r == rows - 1 ? maxY : minY + r * step;
                expectedPoints.add(new Point(c, r, x, y, 0, false));
            }
        }
        ManualGrid grid = new ManualGrid(minX, maxX, minY, maxY, step, columns, rows, expectedPoints);
        if (Math.ceil((maxX - minX) / step - 1e-10) + 1 != columns
                || Math.ceil((maxY - minY) / step - 1e-10) + 1 != rows
                || step < MIN_INTERVAL_MM
                || maxX - minX - (columns - 2) * step < MIN_INTERVAL_MM
                || maxY - minY - (rows - 2) * step < MIN_INTERVAL_MM) {
            throw new IllegalArgumentException("Passo della sessione incoerente");
        }
        for (int i = 0; i < saved.size(); i++) {
            Point expected = grid.points.get(i);
            Point actual = saved.get(i);
            if (actual.column() != expected.column() || actual.row() != expected.row()
                    || Math.abs(actual.x() - expected.x()) > 1e-6
                    || Math.abs(actual.y() - expected.y()) > 1e-6
                    || !Double.isFinite(actual.z())) {
                throw new IllegalArgumentException("Punto della sessione non valido: " + i);
            }
            grid.points.set(i, actual);
        }
        return grid;
    }

    public Point point(int index) { return points.get(index); }
    public List<Point> points() { return Collections.unmodifiableList(points); }
    public int columns() { return columns; }
    public int rows() { return rows; }
    public double minX() { return minX; }
    public double maxX() { return maxX; }
    public double minY() { return minY; }
    public double maxY() { return maxY; }
    public double step() { return step; }
    public int size() { return points.size(); }
    public boolean complete() { return points.stream().allMatch(Point::measured); }
    public long measuredCount() { return points.stream().filter(Point::measured).count(); }

    public void record(int index, double z) {
        if (!Double.isFinite(z)) throw new IllegalArgumentException("Z non valida");
        points.set(index, points.get(index).measuredAt(z));
    }

    /** Column-major serpentine, wrapping around to earlier missing points. */
    public int nextUnmeasuredAfter(int current) {
        for (int n = 1; n <= size(); n++) {
            int order = Math.floorMod(orderIndex(current) + n, size());
            int c = order / rows;
            int within = order % rows;
            int r = c % 2 == 0 ? within : rows - 1 - within;
            int index = c * rows + r;
            if (!points.get(index).measured()) return index;
        }
        return -1;
    }

    private int orderIndex(int index) {
        if (index < 0) return -1;
        int c = index / rows;
        int r = index % rows;
        return c * rows + (c % 2 == 0 ? r : rows - 1 - r);
    }

    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }
}

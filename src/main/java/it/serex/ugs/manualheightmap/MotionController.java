package it.serex.ugs.manualheightmap;

import com.willwinder.universalgcodesender.listeners.ControllerState;
import com.willwinder.universalgcodesender.listeners.UGSEventListener;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.PartialPosition;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UGSEvent;
import com.willwinder.universalgcodesender.model.UnitUtils.Units;
import com.willwinder.universalgcodesender.model.events.ControllerStatusEvent;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** One machine move at a time. Every transition waits for a new controller status at its target. */
public final class MotionController implements UGSEventListener {
    public enum Stage { CONFIGURATION, READY, LIFT, XY, APPROACH, ADJUST, JOG, COMPLETE, SUSPENDED }

    private final BackendAPI backend;
    private final Consumer<String> notice;
    private final Consumer<Stage> stageChanged;
    private final ExecutorService commands = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ugs-manual-heightmap-motion");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong statuses = new AtomicLong();
    private final Timer watchdog;
    private volatile Stage stage = Stage.CONFIGURATION;
    private MeasurementSession session;
    private int activePoint = -1;
    private long baseline;
    private long sentAt;
    private double targetX, targetY, targetZ;
    private volatile long operation = 0;
    private boolean commandAccepted;

    public MotionController(BackendAPI backend, Consumer<String> notice, Consumer<Stage> stageChanged) {
        this.backend = backend;
        this.notice = notice;
        this.stageChanged = stageChanged;
        this.watchdog = new Timer(250, e -> inspect());
        this.watchdog.start();
        backend.addUGSEventListener(this);
    }

    public Stage stage() { return stage; }
    public int activePoint() { return activePoint; }
    public Position workPosition() { return backend.getWorkPosition().getPositionIn(Units.MM); }

    public void activate(MeasurementSession session) {
        requireIdle();
        if (backend.getSettings().getPreferredUnits() != Units.MM) throw new IllegalStateException("UGS deve usare millimetri");
        if (session.transferZ < session.approachZ || session.descentFeed <= 0) throw new IllegalStateException("Quote o velocità non valide");
        Position work = workPosition();
        Position machine = backend.getMachinePosition().getPositionIn(Units.MM);
        if (Math.abs(work.getX() - machine.getX() - session.workOffsetX) > 0.05
                || Math.abs(work.getY() - machine.getY() - session.workOffsetY) > 0.05
                || Math.abs(work.getZ() - machine.getZ() - session.workOffsetZ) > 0.05) {
            throw new IllegalStateException("Lo zero di lavoro è cambiato: verificare la sessione");
        }
        this.session = session;
        this.activePoint = -1;
        change(Stage.READY);
    }

    public void select(int index) {
        if (stage != Stage.READY && stage != Stage.ADJUST && stage != Stage.COMPLETE)
            throw new IllegalStateException("Attendere la fine del movimento");
        requireIdle();
        if (index < 0 || index >= session.grid.size()) throw new IllegalArgumentException("Punto non valido");
        activePoint = index;
        operation++;
        Position current = workPosition();
        if (current.getZ() < session.transferZ - 0.005) {
            send(Stage.LIFT, Double.NaN, Double.NaN, session.transferZ,
                    "G90 G21 G1 Z" + fmt(session.transferZ) + " F" + fmt(backend.getSettings().getJogFeedRate()));
        } else {
            moveXY();
        }
    }

    private void moveXY() {
        ManualGrid.Point point = session.grid.point(activePoint);
        Position current = workPosition();
        if (Math.abs(current.getX() - point.x()) <= 0.005
                && Math.abs(current.getY() - point.y()) <= 0.005) {
            approach();
            return;
        }
        send(Stage.XY, point.x(), point.y(), Double.NaN,
                "G90 G21 G1 X" + fmt(point.x()) + " Y" + fmt(point.y())
                        + " F" + fmt(backend.getSettings().getJogFeedRate()));
    }

    private void approach() {
        if (Math.abs(workPosition().getZ() - session.approachZ) <= 0.005) {
            change(Stage.ADJUST);
            return;
        }
        send(Stage.APPROACH, Double.NaN, Double.NaN, session.approachZ,
                "G90 G21 G1 Z" + fmt(session.approachZ) + " F" + fmt(session.descentFeed));
    }

    public void jog(double delta) {
        if (stage != Stage.ADJUST) throw new IllegalStateException("Punto non pronto per il jog");
        requireIdle();
        if (!Double.isFinite(delta) || delta == 0 || Math.abs(delta) > 10)
            throw new IllegalArgumentException("Passo Z non valido");
        long token = ++operation;
        targetZ = workPosition().getZ() + delta;
        targetX = Double.NaN; targetY = Double.NaN;
        baseline = statuses.get(); sentAt = System.currentTimeMillis();
        commandAccepted = false;
        change(Stage.JOG);
        commands.execute(() -> {
            try {
                if (token != operation) return;
                backend.adjustManualLocation(PartialPosition.builder(Units.MM).setZ(delta).build(),
                        backend.getSettings().getJogFeedRate());
                onCommandAccepted(token);
            } catch (Exception ex) { onCommandError(token, ex); }
        });
    }

    public double confirmPosition() {
        if (stage != Stage.ADJUST) throw new IllegalStateException("Attendere la fine del jog");
        requireIdle();
        ManualGrid.Point point = session.grid.point(activePoint);
        Position actual = workPosition();
        if (Math.abs(point.x() - actual.getX()) > 0.05 || Math.abs(point.y() - actual.getY()) > 0.05)
            throw new IllegalStateException("La posizione X/Y non corrisponde al punto selezionato");
        double z = actual.getZ();
        if (!Double.isFinite(z)) throw new IllegalStateException("Posizione Z non valida");
        change(Stage.READY);
        return z;
    }

    public void advanceAfterSave() {
        if (stage != Stage.READY) throw new IllegalStateException("Sessione non pronta");
        int next = session.grid.nextUnmeasuredAfter(activePoint);
        if (next >= 0) select(next);
        else finish();
    }

    private void finish() {
        Position current = workPosition();
        if (current.getZ() < session.transferZ - 0.005) {
            operation++;
            send(Stage.LIFT, Double.NaN, Double.NaN, session.transferZ,
                    "G90 G21 G1 Z" + fmt(session.transferZ) + " F" + fmt(backend.getSettings().getJogFeedRate()));
            activePoint = -1;
        } else change(Stage.COMPLETE);
    }

    private void send(Stage phase, double x, double y, double z, String command) {
        long token = operation;
        targetX = x; targetY = y; targetZ = z;
        baseline = statuses.get(); sentAt = System.currentTimeMillis();
        commandAccepted = false;
        change(phase);
        commands.execute(() -> {
            try { if (token == operation) { backend.sendGcodeCommand(true, command); onCommandAccepted(token); } }
            catch (Exception ex) { onCommandError(token, ex); }
        });
    }

    private void inspect() {
        try { inspectCurrentState(); }
        catch (Exception ex) {
            if (stage != Stage.CONFIGURATION && stage != Stage.SUSPENDED)
                suspend("Stato macchina non leggibile: " + ex.getMessage());
        }
    }

    private void inspectCurrentState() {
        if (session != null && stage != Stage.CONFIGURATION && stage != Stage.SUSPENDED
                && stage != Stage.COMPLETE) {
            if (!backend.isConnected() || backend.getControllerState() == ControllerState.ALARM) {
                suspend("Connessione persa o allarme macchina"); return;
            }
            if (backend.getSettings().getPreferredUnits() != Units.MM) {
                suspend("Unità cambiate: verificare la sessione"); return;
            }
            Position work = workPosition();
            Position machine = backend.getMachinePosition().getPositionIn(Units.MM);
            if (Math.abs(work.getX() - machine.getX() - session.workOffsetX) > 0.05
                    || Math.abs(work.getY() - machine.getY() - session.workOffsetY) > 0.05
                    || Math.abs(work.getZ() - machine.getZ() - session.workOffsetZ) > 0.05) {
                suspend("Zero di lavoro cambiato: verificare la sessione"); return;
            }
        }
        if (stage != Stage.LIFT && stage != Stage.XY && stage != Stage.APPROACH && stage != Stage.JOG) return;
        if (System.currentTimeMillis() - sentAt > 180_000) {
            suspend("Movimento scaduto: verificare la macchina"); return;
        }
        if (!commandAccepted || statuses.get() <= baseline || !backend.isIdle()) return;
        Position actual = workPosition();
        if ((Double.isFinite(targetX) && Math.abs(actual.getX() - targetX) > 0.05)
                || (Double.isFinite(targetY) && Math.abs(actual.getY() - targetY) > 0.05)
                || (Double.isFinite(targetZ) && Math.abs(actual.getZ() - targetZ) > 0.05)) return;
        switch (stage) {
            case LIFT -> { if (activePoint < 0) change(Stage.COMPLETE); else moveXY(); }
            case XY -> approach();
            case APPROACH, JOG -> change(Stage.ADJUST);
            default -> { }
        }
    }

    public void suspend(String reason) {
        operation++;
        change(Stage.SUSPENDED);
        notice.accept(reason);
    }

    public void stop() {
        if (stage == Stage.CONFIGURATION || stage == Stage.SUSPENDED) return;
        suspend("Movimento interrotto: controlla la posizione prima di riprendere");
        Thread interrupt = new Thread(() -> {
            try { backend.getController().cancelJog(); }
            catch (Exception ex) { SwingUtilities.invokeLater(() -> notice.accept("Arresto non confermato: usa i comandi di emergenza della macchina. " + ex.getMessage())); }
        }, "ugs-manual-heightmap-stop");
        interrupt.setDaemon(true);
        interrupt.start();
    }

    public void resetToConfiguration() {
        operation++;
        session = null;
        activePoint = -1;
        change(Stage.CONFIGURATION);
    }

    private void requireIdle() {
        if (!backend.isConnected() || !backend.isIdle() || backend.isSendingFile())
            throw new IllegalStateException("La macchina deve essere collegata e ferma");
    }

    private void change(Stage next) { stage = next; stageChanged.accept(next); }
    private void onCommandError(long token, Exception ex) {
        SwingUtilities.invokeLater(() -> { if (token == operation) suspend("Comando non eseguito: " + ex.getMessage()); });
    }
    private void onCommandAccepted(long token) {
        SwingUtilities.invokeLater(() -> { if (token == operation) commandAccepted = true; });
    }
    private static String fmt(double value) { return String.format(Locale.ROOT, "%.5f", value); }

    @Override public void UGSEvent(UGSEvent event) {
        if (event instanceof ControllerStatusEvent) statuses.incrementAndGet();
    }

    public void close() {
        watchdog.stop();
        backend.removeUGSEventListener(this);
        commands.shutdownNow();
    }
}

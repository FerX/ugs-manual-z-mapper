package it.serex.ugs.manualheightmap;

import com.willwinder.universalgcodesender.model.GUIBackend;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UnitUtils.Units;
import com.willwinder.universalgcodesender.utils.Settings;
import org.junit.Assume;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Opt-in real serial integration test: UGS_SIM_PORT=/path/to/pty mvnw -Dtest=SimulatorIntegrationTest test. */
public class SimulatorIntegrationTest {
    @Test public void movesThroughRealUgsBackendAndGrblhalSimulator() throws Exception {
        String port = System.getenv("UGS_SIM_PORT");
        Assume.assumeTrue(port != null && !port.isBlank());
        GUIBackend backend = new GUIBackend();
        Settings settings = new Settings();
        settings.setPreferredUnits(Units.MM);
        backend.applySettings(settings);
        backend.connect("GRBL", port, 115200);
        try {
            await(() -> backend.isConnected() && backend.isIdle(), "connessione simulator");
            Position work = backend.getWorkPosition().getPositionIn(Units.MM);
            Position machine = backend.getMachinePosition().getPositionIn(Units.MM);
            ManualGrid grid = ManualGrid.create(work.getX(), work.getX() + 10,
                    work.getY(), work.getY() + 10, 2, 2);
            double approach = work.getZ() + 1;
            double transfer = work.getZ() + 3;
            MeasurementSession session = new MeasurementSession(grid, "", 0, approach, transfer, 200,
                    work.getX() - machine.getX(), work.getY() - machine.getY(), work.getZ() - machine.getZ());
            AtomicReference<MotionController> ref = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> ref.set(new MotionController(backend,
                    s -> System.out.println("PLUGIN NOTICE: " + s),
                    s -> System.out.println("PLUGIN STAGE: " + s))));
            MotionController motion = ref.get();
            try {
                SwingUtilities.invokeAndWait(() -> { motion.activate(session); motion.select(3); });
                try { await(() -> motion.stage() == MotionController.Stage.ADJUST, "avvicinamento al punto"); }
                catch (AssertionError ex) {
                    System.out.println("DEBUG stage=" + motion.stage() + " state=" + backend.getControllerState()
                            + " idle=" + backend.isIdle() + " work=" + backend.getWorkPosition()
                            + " machine=" + backend.getMachinePosition());
                    throw ex;
                }
                Position reached = backend.getWorkPosition().getPositionIn(Units.MM);
                assertEquals(grid.point(3).x(), reached.getX(), 0.05);
                assertEquals(grid.point(3).y(), reached.getY(), 0.05);
                assertEquals(approach, reached.getZ(), 0.05);
                SwingUtilities.invokeAndWait(() -> motion.jog(-0.1));
                await(() -> motion.stage() == MotionController.Stage.ADJUST, "micro movimento Z");
                AtomicReference<Double> measured = new AtomicReference<>();
                SwingUtilities.invokeAndWait(() -> measured.set(motion.confirmPosition()));
                assertEquals(approach - 0.1, measured.get(), 0.05);
            } finally { SwingUtilities.invokeAndWait(motion::close); }
        } finally { backend.disconnect(); }
    }

    private static void await(java.util.function.BooleanSupplier ready, String label) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < deadline) {
            if (ready.getAsBoolean()) return;
            Thread.sleep(100);
        }
        fail("Timeout: " + label);
    }
}

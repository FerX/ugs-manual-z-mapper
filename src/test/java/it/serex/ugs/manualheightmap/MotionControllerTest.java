package it.serex.ugs.manualheightmap;

import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UnitUtils.Units;
import com.willwinder.universalgcodesender.model.events.ControllerStatusEvent;
import com.willwinder.universalgcodesender.utils.Settings;
import org.junit.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MotionControllerTest {
    @Test public void waitsForStatusBeforeEachMoveAndStopsAfterApproach() throws Exception {
        BackendAPI backend = mock(BackendAPI.class);
        Settings settings = mock(Settings.class);
        when(backend.getSettings()).thenReturn(settings);
        when(settings.getPreferredUnits()).thenReturn(Units.MM);
        when(settings.getJogFeedRate()).thenReturn(500.0);
        when(backend.isConnected()).thenReturn(true);
        when(backend.isIdle()).thenReturn(true);
        AtomicReference<Position> position = new AtomicReference<>(new Position(0, 0, 0, Units.MM));
        when(backend.getWorkPosition()).thenAnswer(i -> position.get());
        when(backend.getMachinePosition()).thenAnswer(i -> position.get());
        List<String> commands = new ArrayList<>();
        doAnswer(i -> { synchronized (commands) { commands.add(i.getArgument(1)); } return null; })
                .when(backend).sendGcodeCommand(anyBoolean(), anyString());

        ManualGrid grid = ManualGrid.create(0, 20, 0, 20, 3, 3);
        MeasurementSession session = new MeasurementSession(grid, "", 0, 1, 6, 100, 0, 0, 0);
        AtomicReference<MotionController> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> ref.set(new MotionController(backend, s -> {}, s -> {})));
        MotionController motion = ref.get();
        try {
            SwingUtilities.invokeAndWait(() -> { motion.activate(session); motion.select(4); });
            awaitCommands(commands, 1);
            assertTrue(commands.get(0).contains("Z6.00000"));
            Thread.sleep(350);
            assertEquals(1, commands.size());

            position.set(new Position(0, 0, 6, Units.MM));
            motion.UGSEvent(new ControllerStatusEvent(null, null));
            awaitCommands(commands, 2);
            assertTrue(commands.get(1).contains("X10.00000 Y10.00000"));

            position.set(new Position(10, 10, 6, Units.MM));
            motion.UGSEvent(new ControllerStatusEvent(null, null));
            awaitCommands(commands, 3);
            assertTrue(commands.get(2).contains("Z1.00000"));

            position.set(new Position(10, 10, 1, Units.MM));
            motion.UGSEvent(new ControllerStatusEvent(null, null));
            awaitStage(motion, MotionController.Stage.ADJUST);
            SwingUtilities.invokeAndWait(() -> { assertEquals(1, motion.confirmPosition(), 0); });
        } finally {
            SwingUtilities.invokeAndWait(motion::close);
        }
    }

    private static void awaitCommands(List<String> commands, int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            synchronized (commands) { if (commands.size() >= count) return; }
            Thread.sleep(20);
        }
        fail("Comando " + count + " non inviato");
    }

    private static void awaitStage(MotionController motion, MotionController.Stage expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            AtomicReference<MotionController.Stage> current = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> current.set(motion.stage()));
            if (current.get() == expected) return;
            Thread.sleep(20);
        }
        fail("Fase " + expected + " non raggiunta");
    }
}

package it.serex.ugs.manualheightmap;

import com.willwinder.ugs.nbp.lib.Mode;
import com.willwinder.ugs.nbp.lib.services.LocalizingService;
import com.willwinder.universalgcodesender.listeners.UGSEventListener;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UGSEvent;
import com.willwinder.universalgcodesender.model.UnitUtils.Units;
import com.willwinder.universalgcodesender.model.events.ControllerStatusEvent;
import com.willwinder.universalgcodesender.model.events.FileStateEvent;
import com.willwinder.universalgcodesender.services.LookupService;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.prefs.Preferences;

/** User interface for measuring a complete manual Z map in UGS Platform. */
@TopComponent.Description(preferredID = "ManualHeightmapTopComponent")
@TopComponent.Registration(mode = Mode.LEFT_BOTTOM, openAtStartup = false)
@ActionID(category = LocalizingService.CATEGORY_WINDOW, id = "it.serex.ugs.manualheightmap.ManualHeightmapTopComponent")
@ActionReference(path = LocalizingService.MENU_WINDOW_PLUGIN)
@TopComponent.OpenActionRegistration(displayName = "Mappa Z manuale", preferredID = "ManualHeightmapTopComponent")
public final class ManualHeightmapTopComponent extends TopComponent implements UGSEventListener {
    private final BackendAPI backend = LookupService.lookup(BackendAPI.class);
    private final Preferences preferences = Preferences.userNodeForPackage(ManualHeightmapTopComponent.class);
    private final JTextField minX = field("0"), maxX = field("20"), minY = field("0"), maxY = field("20");
    private final JTextField maxRows = field("3"), maxColumns = field("3");
    private final JTextField approach = field("1"), transfer = field("");
    private final JTextField descent = field("");
    private final JTextField jogBase = field("0.01"), shiftFactor = field("5"), ctrlFactor = field("10");
    private final JLabel status = new JLabel("Configurazione: nessun movimento abilitato");
    private final JLabel currentZ = new JLabel("Z: —");
    private final JLabel gridInfo = new JLabel("Genera una griglia");
    private final JPanel gridPanel = new JPanel();
    private final JButton startButton = new JButton("Inizia misura");
    private final JButton recordButton = new JButton("Registra quota e avanti");
    private final JButton stopButton = new JButton("Interrompi movimento");
    private final JButton exportButton = new JButton("Esporta mappa");
    private final JButton[] pointButtons = new JButton[10000];
    private final JButton[] jogButtons = new JButton[6];
    private MotionController motion;
    private ManualGrid grid;
    private MeasurementSession session;
    private Path recoveryPath;
    private String areaSource = "manuale";

    public ManualHeightmapTopComponent() {
        setName("Mappa Z manuale");
        setToolTipText("Misura la superficie manualmente senza sonda");
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel controls = new JPanel(new GridLayout(1, 3, 10, 10));
        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Area X/Y (mm)"), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JPanel bounds = new JPanel(new GridLayout(2, 4, 6, 6));
        bounds.add(new JLabel("X min")); bounds.add(minX); bounds.add(new JLabel("X max")); bounds.add(maxX);
        bounds.add(new JLabel("Y min")); bounds.add(minY); bounds.add(new JLabel("Y max")); bounds.add(maxY);
        area.add(bounds, BorderLayout.NORTH);
        area.add(button("Dal G-code", this::readArea), BorderLayout.SOUTH);
        controls.add(area);
        controls.add(row("Griglia", new JLabel("Righe max"), maxRows, new JLabel("Colonne max"), maxColumns,
                button("Genera griglia", this::generateGrid)));
        controls.add(row("Quote", new JLabel("Avvicinamento Z (mm)"), approach,
                new JLabel("Trasferimento Z (mm)"), transfer, new JLabel("Discesa mm/min"), descent));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Punti della griglia"));
        gridPanel.setPreferredSize(new Dimension(360, 280));
        JPanel adjustment = new JPanel(new BorderLayout(6, 10));
        adjustment.setBorder(BorderFactory.createTitledBorder("Regolazione Z"));
        currentZ.setFont(currentZ.getFont().deriveFont(Font.BOLD, 20f));
        currentZ.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel adjustmentControls = new JPanel(new BorderLayout(6, 10));
        adjustmentControls.add(currentZ, BorderLayout.NORTH);
        JPanel jog = createJogPanel();
        jog.setPreferredSize(new Dimension(330, 100));
        adjustmentControls.add(jog, BorderLayout.CENTER);
        adjustmentControls.add(row("Passi di regolazione", new JLabel("Passo base (mm)"), jogBase,
                new JLabel("Moltiplicatore 1 (Maiusc)"), shiftFactor,
                new JLabel("Moltiplicatore 2 (Ctrl)"), ctrlFactor), BorderLayout.SOUTH);
        adjustment.add(adjustmentControls, BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(3, 1, 6, 6));
        actions.add(startButton); actions.add(stopButton); actions.add(recordButton);
        recordButton.setFont(recordButton.getFont().deriveFont(Font.BOLD));
        actions.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
        adjustment.add(actions, BorderLayout.SOUTH);
        JPanel measurementPanel = new JPanel(new BorderLayout(10, 10));
        measurementPanel.add(gridPanel, BorderLayout.CENTER);
        measurementPanel.add(adjustment, BorderLayout.EAST);
        JPanel content = new ResponsiveContent();
        content.add(controls, BorderLayout.NORTH);
        content.add(measurementPanel, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(6, 6));
        JPanel files = new JPanel(new GridLayout(1, 3, 6, 6));
        files.add(button("Salva sessione", this::saveSession));
        files.add(button("Apri sessione", this::openSession)); files.add(exportButton);
        footer.add(files, BorderLayout.NORTH);
        JPanel messages = new JPanel(new GridLayout(2, 1, 0, 4));
        messages.add(gridInfo); messages.add(status);
        footer.add(messages, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            private int previousColumns = -1;
            private boolean previousCompact;
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int width = scroll.getViewport().getWidth();
                int columns = width >= 1000 ? 3 : width >= 680 ? 2 : 1;
                boolean compact = width < 760;
                if (columns == previousColumns && compact == previousCompact) return;
                previousColumns = columns; previousCompact = compact;
                controls.setLayout(new GridLayout(0, columns, 10, 10));
                measurementPanel.remove(adjustment);
                measurementPanel.add(adjustment, compact ? BorderLayout.SOUTH : BorderLayout.EAST);
                content.revalidate();
            }
        });
        startButton.addActionListener(e -> run(this::start));
        recordButton.addActionListener(e -> run(this::record));
        stopButton.addActionListener(e -> run(() -> motion.stop()));
        exportButton.addActionListener(e -> run(this::export));
        descent.setText(preferences.get("descentFeed", ""));
        jogBase.setText(preferences.get("jogBase", "0.01"));
        shiftFactor.setText(preferences.get("shiftFactor", "5"));
        ctrlFactor.setText(preferences.get("ctrlFactor", "10"));
        installJogLabelUpdates();
        updateJogLabels();
        updateButtons();
        installKeys();
    }

    private static final class ResponsiveContent extends JPanel implements Scrollable {
        ResponsiveContent() { super(new BorderLayout(10, 10)); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(20, (orientation == SwingConstants.VERTICAL ? r.height : r.width) - 20);
        }
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() != null && getParent().getWidth() >= 360;
        }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
        }
    }

    private static JTextField field(String value) {
        JTextField text = new JTextField(value, 7);
        text.setMinimumSize(new Dimension(60, text.getPreferredSize().height));
        return text;
    }
    private static JPanel row(String title, Component... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title), BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        for (int i = 0; i < components.length; i++) {
            c.gridx = 0; c.weightx = 0; c.gridwidth = 1;
            c.fill = GridBagConstraints.NONE;
            if (components[i] instanceof JLabel && i + 1 < components.length
                    && components[i + 1] instanceof JTextField) {
                panel.add(components[i], c);
                c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
                panel.add(components[++i], c);
            } else {
                c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
                panel.add(components[i], c);
            }
            c.gridy++;
        }
        c.gridx = 0; c.weighty = 1; c.fill = GridBagConstraints.VERTICAL;
        panel.add(Box.createVerticalGlue(), c);
        return panel;
    }
    private static JButton button(String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(e -> action.run());
        return button;
    }

    private JPanel createJogPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        for (int slot = 0; slot < 3; slot++) {
            int factorSlot = slot;
            jogButtons[slot] = button("▲", () -> jog(1, factorSlot));
            panel.add(jogButtons[slot]);
        }
        for (int slot = 0; slot < 3; slot++) {
            int factorSlot = slot;
            jogButtons[slot + 3] = button("▼", () -> jog(-1, factorSlot));
            panel.add(jogButtons[slot + 3]);
        }
        return panel;
    }

    private void installJogLabelUpdates() {
        DocumentListener listener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateJogLabels(); }
            @Override public void removeUpdate(DocumentEvent e) { updateJogLabels(); }
            @Override public void changedUpdate(DocumentEvent e) { updateJogLabels(); }
        };
        jogBase.getDocument().addDocumentListener(listener);
        shiftFactor.getDocument().addDocumentListener(listener);
        ctrlFactor.getDocument().addDocumentListener(listener);
    }

    private void updateJogLabels() {
        for (int slot = 0; slot < 3; slot++) {
            String distance;
            try { distance = String.format(Locale.ROOT, "%.3f", jogDistance(slot)); }
            catch (Exception ex) { distance = "?"; }
            jogButtons[slot].setText("<html><center>▲<br>" + distance + " mm</center></html>");
            jogButtons[slot + 3].setText("<html><center>▼<br>" + distance + " mm</center></html>");
        }
    }

    private double jogDistance(int factorSlot) {
        double factor = factorSlot == 1 ? number(shiftFactor) : factorSlot == 2 ? number(ctrlFactor) : 1;
        double distance = number(jogBase) * factor;
        if (!Double.isFinite(distance) || distance <= 0)
            throw new IllegalArgumentException("Passo di jog non valido");
        return distance;
    }

    private void jog(int direction, int factorSlot) {
        if (motion == null || motion.stage() != MotionController.Stage.ADJUST) return;
        run(() -> motion.jog(direction * jogDistance(factorSlot)));
    }
    private static double number(JTextField field) { return Double.parseDouble(field.getText().trim().replace(',', '.')); }
    private static int integer(JTextField field) { return Integer.parseInt(field.getText().trim()); }
    private static String fmt(double value) { return String.format(Locale.ROOT, "%.3f", value); }

    private void run(Runnable operation) {
        try { operation.run(); }
        catch (Exception ex) {
            status.setText(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            JOptionPane.showMessageDialog(this, status.getText(), "Mappa Z manuale", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void readArea() {
        File file = backend.getProcessedGcodeFile();
        if (file == null) file = backend.getGcodeFile();
        final File activeFile = file;
        status.setText("Analisi della lavorazione in corso...");
        new SwingWorker<WorkAreaAnalyzer.Bounds, Void>() {
            @Override protected WorkAreaAnalyzer.Bounds doInBackground() throws Exception {
                return WorkAreaAnalyzer.analyze(activeFile);
            }
            @Override protected void done() {
                try {
                    WorkAreaAnalyzer.Bounds b = get();
                    minX.setText(fmt(b.minX())); maxX.setText(fmt(b.maxX()));
                    minY.setText(fmt(b.minY())); maxY.setText(fmt(b.maxY()));
                    areaSource = "lavorazione G-code";
                    status.setText("Area proposta dal G-code; controlla i limiti X/Y e genera la griglia");
                } catch (Exception ex) {
                    areaSource = "manuale";
                    status.setText("Analisi incerta: inserisci manualmente i limiti X/Y e controlla la griglia");
                    JOptionPane.showMessageDialog(ManualHeightmapTopComponent.this,
                            status.getText() + "\n" + ex.getMessage(), "Area manuale", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void generateGrid() {
        if (motion != null && motion.stage() != MotionController.Stage.CONFIGURATION
                && motion.stage() != MotionController.Stage.SUSPENDED) {
            throw new IllegalStateException("Sospendi la sessione prima di rigenerare la griglia");
        }
        if (grid != null && grid.measuredCount() > 0 && JOptionPane.showConfirmDialog(this,
                "La nuova griglia sostituirà le misure correnti. Continuare?", "Conferma", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        ManualGrid candidate = ManualGrid.create(number(minX), number(maxX), number(minY), number(maxY),
                integer(maxColumns), integer(maxRows));
        grid = candidate;
        session = null;
        recoveryPath = null;
        if (motion != null) motion.resetToConfiguration();
        if (transfer.getText().isBlank() && backend != null) {
            transfer.setText(fmt(number(approach) + backend.getSettings().getSafetyHeight()));
        }
        renderGrid();
        status.setText("Griglia pronta. Controlla i punti e premi Inizia misura");
    }

    private void renderGrid() {
        gridPanel.removeAll();
        if (grid == null) return;
        gridPanel.setLayout(new GridLayout(grid.rows(), grid.columns(), 4, 4));
        for (int row = grid.rows() - 1; row >= 0; row--) {
            for (int col = 0; col < grid.columns(); col++) {
                int index = col * grid.rows() + row;
                ManualGrid.Point point = grid.point(index);
                JButton item = new JButton();
                item.setToolTipText("X=" + fmt(point.x()) + " Y=" + fmt(point.y()));
                item.addActionListener(e -> run(() -> select(index)));
                pointButtons[index] = item;
                gridPanel.add(item);
            }
        }
        gridInfo.setText("Area " + areaSource + " | " + grid.rows() + " righe × " + grid.columns()
                + " colonne = " + grid.size() + " punti | passo " + fmt(grid.step()) + " mm");
        refreshGrid();
        gridPanel.revalidate(); gridPanel.repaint();
    }

    private void refreshGrid() {
        if (grid == null) return;
        int active = motion == null ? -1 : motion.activePoint();
        for (int i = 0; i < grid.size(); i++) {
            ManualGrid.Point point = grid.point(i);
            JButton button = pointButtons[i];
            if (button == null) continue;
            button.setText((i + 1) + (point.measured() ? " ✓" : " ·"));
            button.setToolTipText("X=" + fmt(point.x()) + " Y=" + fmt(point.y())
                    + (point.measured() ? " Z=" + fmt(point.z()) + " mm" : " · da misurare"));
            button.setBackground(i == active ? new Color(255, 221, 120)
                    : point.measured() ? new Color(174, 225, 174) : null);
            button.setOpaque(true);
        }
        gridInfo.setText("Area " + areaSource + " | " + grid.rows() + " × " + grid.columns()
                + " = " + grid.size() + " punti | misurati " + grid.measuredCount()
                + " | passo " + fmt(grid.step()) + " mm");
        updateButtons();
    }

    private void select(int index) {
        if (motion == null || motion.stage() == MotionController.Stage.CONFIGURATION) {
            ManualGrid.Point point = grid.point(index);
            status.setText("Punto " + (index + 1) + " X=" + fmt(point.x()) + " Y=" + fmt(point.y())
                    + ". Premi Inizia misura per abilitare il movimento");
            return;
        }
        motion.select(index);
        refreshGrid();
    }

    private void start() {
        if (grid == null) throw new IllegalStateException("Genera prima la griglia");
        if (motion == null) throw new IllegalStateException("Pannello non inizializzato");
        if (motion.stage() != MotionController.Stage.CONFIGURATION && motion.stage() != MotionController.Stage.SUSPENDED)
            throw new IllegalStateException("Sessione già attiva");
        if (!backend.isConnected() || !backend.isIdle() || backend.isSendingFile())
            throw new IllegalStateException("Collega la macchina e attendi che sia ferma");
        double approachZ = number(approach), transferZ = number(transfer), descentFeed = number(descent);
        if (!Double.isFinite(approachZ) || !Double.isFinite(transferZ) || transferZ < approachZ
                || !Double.isFinite(descentFeed) || descentFeed <= 0)
            throw new IllegalArgumentException("Quote o velocità non valide");
        if (backend.getSettings().getPreferredUnits() != Units.MM)
            throw new IllegalStateException("Imposta UGS in millimetri prima della misura");
        if (session == null) {
            Position work = backend.getWorkPosition().getPositionIn(Units.MM);
            Position machine = backend.getMachinePosition().getPositionIn(Units.MM);
            File file = backend.getGcodeFile();
            session = new MeasurementSession(grid, file == null ? "" : file.getAbsolutePath(),
                    file == null ? 0 : file.lastModified(), approachZ, transferZ, descentFeed,
                    work.getX() - machine.getX(), work.getY() - machine.getY(), work.getZ() - machine.getZ());
            recoveryPath = recoveryDirectory().resolve(UUID.randomUUID() + ".properties");
        } else {
            File file = backend.getGcodeFile();
            if (!session.workFile.isEmpty() && (file == null
                    || !session.workFile.equals(file.getAbsolutePath()) || session.workModified != file.lastModified()))
                throw new IllegalStateException("Il G-code caricato è cambiato rispetto alla sessione salvata");
            if (Math.abs(session.approachZ - approachZ) > 1e-6
                    || Math.abs(session.transferZ - transferZ) > 1e-6
                    || Math.abs(session.descentFeed - descentFeed) > 1e-6)
                throw new IllegalStateException("Parametri cambiati: rigenera la sessione o ripristina i valori salvati");
        }
        preferences.put("descentFeed", descent.getText().trim());
        preferences.put("jogBase", jogBase.getText().trim());
        preferences.put("shiftFactor", shiftFactor.getText().trim());
        preferences.put("ctrlFactor", ctrlFactor.getText().trim());
        motion.activate(session);
        try { SessionStore.save(recoveryPath, session); }
        catch (Exception ex) { motion.suspend("Impossibile salvare la sessione: " + ex.getMessage()); return; }
        status.setText("Sessione attiva: clicca un punto per iniziare");
        updateButtons();
    }

    private void record() {
        if (motion == null) throw new IllegalStateException("Sessione non attiva");
        int index = motion.activePoint();
        double z = motion.confirmPosition();
        grid.record(index, z);
        refreshGrid();
        try { SessionStore.save(recoveryPath, session); }
        catch (Exception ex) { motion.suspend("Misura conservata in memoria; salvataggio fallito: " + ex.getMessage()); return; }
        motion.advanceAfterSave();
        refreshGrid();
    }

    private void saveSession() {
        if (grid == null) throw new IllegalStateException("Nessuna griglia da salvare");
        if (session == null) throw new IllegalStateException("Premi Inizia misura per creare una sessione");
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("mappa-z-sessione.properties"));
        if (confirmSave(chooser)) {
            try { SessionStore.save(chooser.getSelectedFile().toPath(), session); status.setText("Sessione salvata"); }
            catch (Exception ex) { throw new IllegalStateException("Salvataggio fallito: " + ex.getMessage(), ex); }
        }
    }

    private void openSession() {
        if (motion != null && motion.stage() != MotionController.Stage.CONFIGURATION
                && motion.stage() != MotionController.Stage.SUSPENDED)
            throw new IllegalStateException("Sospendi prima la sessione attiva");
        JFileChooser chooser = new JFileChooser(recoveryDirectory().toFile());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        MeasurementSession loaded;
        try { loaded = SessionStore.load(chooser.getSelectedFile().toPath()); }
        catch (Exception ex) { throw new IllegalStateException("Apertura fallita: " + ex.getMessage(), ex); }
        session = loaded; grid = loaded.grid;
        recoveryPath = recoveryDirectory().resolve(UUID.randomUUID() + ".properties");
        areaSource = "sessione salvata";
        minX.setText(fmt(grid.minX())); maxX.setText(fmt(grid.maxX()));
        minY.setText(fmt(grid.minY())); maxY.setText(fmt(grid.maxY()));
        maxRows.setText(Integer.toString(grid.rows())); maxColumns.setText(Integer.toString(grid.columns()));
        approach.setText(fmt(loaded.approachZ)); transfer.setText(fmt(loaded.transferZ));
        descent.setText(fmt(loaded.descentFeed));
        if (motion != null) motion.resetToConfiguration();
        renderGrid();
        status.setText("Sessione aperta. Verifica pezzo, utensile e zero, poi premi Inizia misura");
    }

    private void export() {
        if (grid == null || !grid.complete()) throw new IllegalStateException("Misurare tutti i punti prima dell'esportazione");
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("mappa-z-mm.xyz"));
        if (confirmSave(chooser)) {
            try { XyzExporter.write(chooser.getSelectedFile().toPath(), grid); status.setText("Mappa XYZ esportata: aprila in AutoLeveler"); }
            catch (Exception ex) { throw new IllegalStateException("Esportazione fallita: " + ex.getMessage(), ex); }
        }
    }

    private boolean confirmSave(JFileChooser chooser) {
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return false;
        File file = chooser.getSelectedFile();
        return !file.exists() || JOptionPane.showConfirmDialog(this,
                "Il file esiste già. Sovrascriverlo?\n" + file.getAbsolutePath(),
                "Conferma salvataggio", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void installKeys() {
        InputMap input = gridPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actions = gridPanel.getActionMap();
        bind(input, actions, "UP", "up", 1, 0);
        bind(input, actions, "DOWN", "down", -1, 0);
        bind(input, actions, "shift UP", "shift-up", 1, 1);
        bind(input, actions, "shift DOWN", "shift-down", -1, 1);
        bind(input, actions, "ctrl UP", "ctrl-up", 1, 2);
        bind(input, actions, "ctrl DOWN", "ctrl-down", -1, 2);
        input.put(KeyStroke.getKeyStroke("shift ctrl UP"), "ignore");
        input.put(KeyStroke.getKeyStroke("shift ctrl DOWN"), "ignore");
        actions.put("ignore", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) {} });
        input.put(KeyStroke.getKeyStroke("ENTER"), "record");
        actions.put("record", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (motion != null && motion.stage() == MotionController.Stage.ADJUST) run(ManualHeightmapTopComponent.this::record);
            }
        });
    }

    private void bind(InputMap input, ActionMap actions, String key, String name, int direction, int factorSlot) {
        input.put(KeyStroke.getKeyStroke(key), name);
        actions.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                jog(direction, factorSlot);
            }
        });
    }

    private static Path recoveryDirectory() {
        return Path.of(System.getProperty("user.home"), ".local", "state", "ugs-manual-heightmap", "recovery");
    }

    private void updateButtons() {
        boolean active = motion != null && (motion.stage() == MotionController.Stage.READY
                || motion.stage() == MotionController.Stage.ADJUST
                || motion.stage() == MotionController.Stage.COMPLETE);
        startButton.setEnabled(grid != null && !active);
        recordButton.setEnabled(motion != null && motion.stage() == MotionController.Stage.ADJUST);
        stopButton.setEnabled(motion != null && motion.stage() != MotionController.Stage.CONFIGURATION
                && motion.stage() != MotionController.Stage.SUSPENDED);
        exportButton.setEnabled(grid != null && grid.complete());
        boolean canJog = motion != null && motion.stage() == MotionController.Stage.ADJUST;
        for (JButton button : jogButtons) if (button != null) button.setEnabled(canJog);
    }

    private void onStage(MotionController.Stage stage) {
        String detail = "Stato: " + stage;
        if (grid != null && motion != null && motion.activePoint() >= 0) {
            ManualGrid.Point point = grid.point(motion.activePoint());
            detail = "Punto " + (motion.activePoint() + 1) + " X=" + fmt(point.x())
                    + " Y=" + fmt(point.y()) + (point.measured() ? " | Z precedente=" + fmt(point.z()) : "")
                    + " | " + stage;
        }
        status.setText(detail);
        status.setToolTipText(detail);
        refreshGrid();
    }

    @Override protected void componentOpened() {
        super.componentOpened();
        if (backend == null) { status.setText("Backend UGS non disponibile"); return; }
        if (motion == null) motion = new MotionController(backend, status::setText, this::onStage);
        backend.addUGSEventListener(this);
        updateButtons();
    }

    @Override protected void componentClosed() {
        super.componentClosed();
        if (motion != null) { motion.stop(); motion.close(); motion = null; }
        if (backend != null) backend.removeUGSEventListener(this);
    }

    @Override public void UGSEvent(UGSEvent event) {
        if (event instanceof ControllerStatusEvent) {
            SwingUtilities.invokeLater(() -> {
                try { if (backend.isConnected()) currentZ.setText("Z lavoro: " + fmt(backend.getWorkPosition().getPositionIn(Units.MM).getZ()) + " mm"); }
                catch (Exception ignored) { currentZ.setText("Z: —"); }
            });
        } else if (event instanceof FileStateEvent) {
            SwingUtilities.invokeLater(() -> {
                if (motion != null && motion.stage() != MotionController.Stage.CONFIGURATION) motion.suspend("G-code cambiato: verifica la sessione");
            });
        }
    }
}

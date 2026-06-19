//سجود زياد دغلس...12323525
//جنى عمار باسين...12323618
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.List;
import java.io.PrintStream;
import java.util.Locale;
import java.io.File;
import java.util.Scanner;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainGUI extends JFrame {

    private List<Plant> plants = new ArrayList<>();
    private Perceptron perceptron = new Perceptron(0.1, 100);
    private SimulatedAnnealing sa = new SimulatedAnnealing(1000, 0.003, 2000);
    private int[] bestOrder = null;
    private JSpinner spVisitCount;
    private int currentStep = 0;
    private Timer stepTimer = null;
    private double[][] uploadedX = null;
    private int[] uploadedY = null;

    private GardenPanel gardenPanel;
    private DefaultTableModel plantTableModel;
    private JTextArea logArea;
    static final Color C_BG        = new Color(13, 17, 23);
    static final Color C_SURFACE   = new Color(22, 27, 34);
    static final Color C_SURFACE2  = new Color(30, 37, 46);
    static final Color C_BORDER    = new Color(48, 54, 61);
    static final Color C_GREEN     = new Color(63, 185, 80);
    static final Color C_GREEN_DIM = new Color(35, 110, 47);
    static final Color C_BLUE      = new Color(88, 166, 255);
    static final Color C_ORANGE    = new Color(255, 167, 38);
    static final Color C_PURPLE    = new Color(188, 140, 255);
    static final Color C_RED       = new Color(248, 81, 73);
    static final Color C_TEXT      = new Color(230, 237, 243);
    static final Color C_TEXT_DIM  = new Color(139, 148, 158);
    static final Color C_PATH      = new Color(88, 166, 255, 160);

    static final Color COL_BG     = C_BG;
    static final Color COL_ACCENT = C_GREEN;
    static final Color COL_WATER  = C_BLUE;
    static final Color COL_DRY    = C_ORANGE;
    static final Color COL_PANEL  = C_SURFACE;
    static final Color COL_PATH   = C_PATH;

    private ArrayList<Plant> saSequence = new ArrayList<>();

    public MainGUI() {
        super("Smart Plant Watering Scheduler");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(C_BG);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(C_BG);
        tabs.setForeground(C_TEXT);
        tabs.setBorder(new EmptyBorder(8, 8, 8, 8));
        tabs.addTab("  Garden  ",       buildGardenTab());
        tabs.addTab("  Perceptron  ",   buildPerceptronTab());
        tabs.addTab("  SA Optimizer  ", buildSATab());

        JPanel header = buildHeader();
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(C_BG);
        content.add(header, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);

        add(content);
        setVisible(true);
        loadDemoPlants();
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(C_SURFACE);
        h.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(12, 20, 12, 20)
        ));
        JLabel title = new JLabel("Smart Plant Watering Scheduler");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(C_GREEN);
        JLabel sub = new JLabel("   Perceptron + Simulated Annealing");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(C_TEXT_DIM);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(title); left.add(sub);
        h.add(left, BorderLayout.WEST);
        return h;
    }

    private void performTraining(double[][] dataX, int[] dataY, double ratio) {
        int epochs = (int) spEpochs.getValue();
        double lr  = (double) spLR.getValue();
        perceptron = new Perceptron(lr, epochs);
        double testAcc  = perceptron.trainWithValidation(dataX, dataY, ratio);
        double trainAcc = perceptron.epochAccuracy.get(perceptron.epochAccuracy.size() - 1);
        int totalCorrect = 0;
        for (int i = 0; i < dataX.length; i++)
            if (perceptron.predict(dataX[i]) == dataY[i]) totalCorrect++;
        double overallAcc = (double) totalCorrect / dataX.length;

        lblAccuracy.setText(String.format("%.1f%%", overallAcc * 100));
        log("-----------------------------------------");
        log("Training Source: " + (dataX == uploadedX ? "Uploaded File" : "Default Data"));
        log(String.format(">>> Overall Accuracy: %.1f%%", overallAcc * 100));
        log(String.format("  - Train Set Accuracy (%d%%): %.1f%%", (int)(ratio*100), trainAcc * 100));
        log(String.format("  - Test Set Accuracy (%d%%): %.1f%%",  (int)((1-ratio)*100), testAcc * 100));
        log("  Weights: " + perceptron.getWeightsSummary());
        log("-----------------------------------------");
        lblWeights.setText(perceptron.getWeightsSummary());
        perceptron.predictAll(plants);
        refreshTable();
        gardenPanel.repaint();
        lcPanel.setData(perceptron.epochLoss, perceptron.epochAccuracy);
    }

    private void showUploadTrainingDialog() {
        JDialog dialog = new JDialog(this, "File Uploaded - Setup Split", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(380, 230);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(C_SURFACE);

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setBackground(C_SURFACE);
        inner.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 4, 6, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lTitle = new JLabel("Select Training / Testing Split");
        lTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lTitle.setForeground(C_TEXT);
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; inner.add(lTitle, gc);
        gc.gridwidth = 1;

        JSpinner spinTrain = darkSpinner(new SpinnerNumberModel(80, 70, 80, 1));
        JSpinner spinTest  = darkSpinner(new SpinnerNumberModel(20, 20, 30, 1));
        spinTrain.addChangeListener(_ -> spinTest.setValue(100 - (int)spinTrain.getValue()));

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0.6; inner.add(darkLabel("Training % (70-80):"), gc);
        gc.gridx = 1; gc.weightx = 0.4; inner.add(spinTrain, gc);
        gc.gridx = 0; gc.gridy = 2; gc.weightx = 0.6; inner.add(darkLabel("Testing %:"), gc);
        gc.gridx = 1; gc.weightx = 0.4; inner.add(spinTest, gc);

        JButton btnGo = accentButton("Train Now", C_GREEN);
        btnGo.addActionListener(e -> { performTraining(uploadedX, uploadedY, (int)spinTrain.getValue() / 100.0); dialog.dispose(); });
        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.insets = new Insets(16, 4, 4, 4);
        inner.add(btnGo, gc);

        dialog.add(inner, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void loadDataFromCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try (Scanner scanner = new Scanner(file)) {
                List<double[]> xList = new ArrayList<>();
                List<Integer>  yList = new ArrayList<>();
                if (scanner.hasNextLine()) scanner.nextLine();
                while (scanner.hasNextLine()) {
                    String[] parts = scanner.nextLine().split(",");
                    if (parts.length >= 4) {
                        xList.add(new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])});
                        yList.add(Integer.parseInt(parts[3].trim()));
                    }
                }
                uploadedX = xList.toArray(new double[0][]);
                uploadedY = yList.stream().mapToInt(Integer::intValue).toArray();
                log("File loaded: " + file.getName());
                showUploadTrainingDialog();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error reading CSV: " + e.getMessage());
            }
        }
    }

    private JPanel buildGardenTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(C_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        gardenPanel = new GardenPanel();
        JPanel gardenCard = wrapInCard(gardenPanel, "Garden View");

        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setBackground(C_BG);
        sidebar.setPreferredSize(new Dimension(430, 0));

        String[] cols = {"Name","X","Y","Moisture","Last Watered","Type","Needs Water"};
        plantTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(plantTableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(C_TEXT);
        table.setBackground(C_SURFACE);
        table.setSelectionBackground(new Color(48, 54, 70));
        table.setSelectionForeground(C_TEXT);
        table.setGridColor(C_BORDER);
        table.setRowHeight(26);
        table.setShowVerticalLines(false);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(C_SURFACE2);
        table.getTableHeader().setForeground(C_TEXT_DIM);
        table.getTableHeader().setBorder(new MatteBorder(0, 0, 1, 0, C_BORDER));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                                                           boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel) {
                    String nw = (String) t.getValueAt(row, 6);
                    if ("Yes".equals(nw)) { setBackground(new Color(30, 50, 80)); setForeground(C_BLUE); }
                    else if ("No".equals(nw)) { setBackground(new Color(50, 35, 20)); setForeground(C_ORANGE); }
                    else { setBackground(C_SURFACE); setForeground(C_TEXT); }
                } else {
                    setBackground(new Color(48, 54, 70)); setForeground(C_TEXT);
                }
                return this;
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBackground(C_SURFACE);
        tableScroll.getViewport().setBackground(C_SURFACE);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel tableCard = wrapInCard(tableScroll, "Plants");

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.setBackground(C_BG);
        JButton btnRemove = accentButton("Remove Selected", C_RED);
        btnRemove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) { plants.remove(row); plantTableModel.removeRow(row); gardenPanel.repaint(); }
        });
        JButton btnClear = accentButton("Clear All", new Color(80, 50, 20));
        btnClear.addActionListener(e -> {
            plants.clear(); plantTableModel.setRowCount(0);
            bestOrder = null; saSequence.clear(); gardenPanel.repaint();
        });
        btnRow.add(btnRemove); btnRow.add(btnClear);

        JPanel tableSection = new JPanel(new BorderLayout(0, 6));
        tableSection.setBackground(C_BG);
        tableSection.add(tableCard, BorderLayout.CENTER);
        tableSection.add(btnRow, BorderLayout.SOUTH);

        sidebar.add(buildAddPlantForm(), BorderLayout.NORTH);
        sidebar.add(tableSection, BorderLayout.CENTER);
        sidebar.add(buildLegend(), BorderLayout.SOUTH);

        root.add(gardenCard, BorderLayout.CENTER);
        root.add(sidebar, BorderLayout.WEST);
        return root;
    }

    private JPanel buildAddPlantForm() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(C_SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLbl = new JLabel("Add Plant");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(C_GREEN);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4; card.add(titleLbl, g);
        g.gridwidth = 1;

        JTextField fName = darkField("Plant A");
        JTextField fX    = darkField("100");
        JTextField fY    = darkField("100");
        JSpinner spMoist = darkSpinner(new SpinnerNumberModel(30, 0, 100, 1));
        JSpinner spLast  = darkSpinner(new SpinnerNumberModel(12, 0, 48, 1));
        JComboBox<String> cbType = darkCombo(new String[]{"Cactus", "Flower", "Herb"});

        Object[][] rows = {
                {"Name:", fName, "X:", fX},
                {"Y:", fY, "Moisture (0-100):", spMoist},
                {"Last Watered (h):", spLast, "Type:", cbType}
        };
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < 4; c++) {
                g.gridx = c; g.gridy = r + 1;
                g.weightx = (c % 2 == 0) ? 0 : 1;
                if (rows[r][c] instanceof String) card.add(darkLabel((String)rows[r][c]), g);
                else card.add((Component)rows[r][c], g);
            }
        }

        JButton btnAdd = accentButton("Add Plant", C_GREEN);
        g.gridx = 0; g.gridy = 4; g.gridwidth = 4; g.insets = new Insets(10, 6, 3, 6);
        card.add(btnAdd, g);

        btnAdd.addActionListener(e -> {
            try {
                String name = fName.getText().trim();
                if (name.isEmpty()) name = "Plant " + (plants.size() + 1);
                double x    = Double.parseDouble(fX.getText().trim());
                double y    = Double.parseDouble(fY.getText().trim());
                double mois = ((Number) spMoist.getValue()).doubleValue();
                double last = ((Number) spLast.getValue()).doubleValue();
                int type    = cbType.getSelectedIndex();
                x = Math.max(10, Math.min(490, x));
                y = Math.max(10, Math.min(490, y));
                Plant pl = new Plant(name, x, y, mois, last, type);
                if (perceptron.getWeights()[0] != 0 || perceptron.getWeights()[1] != 0)
                    pl.setNeedsWater(perceptron.predict(pl.getFeatures()));
                plants.add(pl);
                refreshTable();
                gardenPanel.repaint();
                fName.setText("Plant " + (plants.size() + 1));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return card;
    }

    private JPanel buildLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        p.setBackground(C_BG);
        addLegendDot(p, C_BLUE,     "Needs Water");
        addLegendDot(p, C_ORANGE,   "Does Not Need Water");
        addLegendDot(p, C_TEXT_DIM, "Not Predicted Yet");
        addLegendDot(p, C_PATH,     "Watering Path (SA)");
        return p;
    }

    private void addLegendDot(JPanel p, Color c, String label) {
        JLabel dot = new JLabel("●");
        dot.setForeground(c);
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(C_TEXT_DIM);
        p.add(dot); p.add(lbl);
    }

    private LearningCurvePanel lcPanel;
    private JLabel lblAccuracy, lblWeights;
    private JTextField tfTestMoist, tfTestLast;
    private JComboBox<String> cbTestType;
    private JLabel lblTestResult;
    private JSpinner spEpochs, spLR;

    private JPanel buildPerceptronTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(C_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel trainCard = new JPanel(new GridLayout(0, 2, 8, 8));
        trainCard.setBackground(C_SURFACE);
        trainCard.setBorder(new CompoundBorder(new LineBorder(C_BORDER, 1, true), new EmptyBorder(12, 14, 12, 14)));
        spEpochs = darkSpinner(new SpinnerNumberModel(100, 10, 1000, 10));
        spLR     = darkSpinner(new SpinnerNumberModel(0.1, 0.01, 1.0, 0.01));
        JLabel trainHeader = sectionLabel("Training Settings");
        trainCard.add(trainHeader); trainCard.add(new JLabel());
        trainCard.add(darkLabel("Epochs:")); trainCard.add(spEpochs);
        trainCard.add(darkLabel("Learning Rate:")); trainCard.add(spLR);

        JButton btnUpload = accentButton("Upload Training File (CSV)", new Color(60, 70, 80));
        btnUpload.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnUpload.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnUpload.addActionListener(e -> loadDataFromCSV());

        JButton btnTrain = accentButton("Train Perceptron", C_GREEN);
        btnTrain.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTrain.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnTrain.addActionListener(e -> trainPerceptron());

        JPanel accCard = new JPanel(new BorderLayout(0, 6));
        accCard.setBackground(C_SURFACE);
        accCard.setBorder(new CompoundBorder(new LineBorder(C_GREEN_DIM, 1, true), new EmptyBorder(14, 16, 14, 16)));
        JLabel accTitle = new JLabel("Overall Accuracy");
        accTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        accTitle.setForeground(C_TEXT_DIM);
        accTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblAccuracy = new JLabel("—", SwingConstants.CENTER);
        lblAccuracy.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblAccuracy.setForeground(C_GREEN);
        lblWeights = new JLabel("Weights: not trained yet", SwingConstants.CENTER);
        lblWeights.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblWeights.setForeground(C_TEXT_DIM);
        accCard.add(accTitle, BorderLayout.NORTH);
        accCard.add(lblAccuracy, BorderLayout.CENTER);
        accCard.add(lblWeights, BorderLayout.SOUTH);

        JPanel testCard = new JPanel(new GridLayout(0, 2, 8, 8));
        testCard.setBackground(C_SURFACE);
        testCard.setBorder(new CompoundBorder(new LineBorder(C_BORDER, 1, true), new EmptyBorder(12, 14, 12, 14)));
        tfTestMoist = darkField("30");
        tfTestLast  = darkField("12");
        cbTestType  = darkCombo(new String[]{"Cactus","Flower","Herb"});
        JLabel testHeader = sectionLabel("Test Single Plant");
        testCard.add(testHeader); testCard.add(new JLabel());
        testCard.add(darkLabel("Moisture:")); testCard.add(tfTestMoist);
        testCard.add(darkLabel("Last Watered:")); testCard.add(tfTestLast);
        testCard.add(darkLabel("Plant Type:")); testCard.add(cbTestType);

        JButton btnTest = accentButton("Test", C_BLUE);
        btnTest.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnTest.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        lblTestResult = new JLabel("Result: —", SwingConstants.CENTER);
        lblTestResult.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTestResult.setForeground(C_TEXT_DIM);
        lblTestResult.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnTest.addActionListener(e -> {
            try {
                double m = Double.parseDouble(tfTestMoist.getText().trim());
                double l = Double.parseDouble(tfTestLast.getText().trim());
                int    t = cbTestType.getSelectedIndex();
                int    r = perceptron.predict(new double[]{m, l, t});
                lblTestResult.setText(r == 1 ? "Needs Water!" : "No Water Needed");
                lblTestResult.setForeground(r == 1 ? C_BLUE : C_GREEN);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(C_BG);
        right.setPreferredSize(new Dimension(300, 0));

        right.add(Box.createVerticalStrut(2));
        right.add(trainCard);
        right.add(Box.createVerticalStrut(8));
        right.add(btnUpload);
        right.add(Box.createVerticalStrut(6));
        right.add(btnTrain);
        right.add(Box.createVerticalStrut(10));
        right.add(accCard);
        right.add(Box.createVerticalStrut(10));
        right.add(testCard);
        right.add(Box.createVerticalStrut(6));
        right.add(btnTest);
        right.add(Box.createVerticalStrut(6));
        right.add(lblTestResult);
        right.add(Box.createVerticalGlue());

        lcPanel = new LearningCurvePanel();
        JPanel lcCard = wrapInCard(lcPanel, "Learning Curve");
        root.add(lcCard, BorderLayout.CENTER);
        root.add(right,  BorderLayout.EAST);
        return root;
    }

    private void trainPerceptron() {
        int    epochs = (int) spEpochs.getValue();
        double lr     = (double) spLR.getValue();
        perceptron = new Perceptron(lr, epochs);
        double testAcc  = perceptron.trainWithValidation(TrainingData.X, TrainingData.Y, 0.8);
        double trainAcc = perceptron.epochAccuracy.get(perceptron.epochAccuracy.size() - 1);
        int totalCorrect = 0;
        for (int i = 0; i < TrainingData.X.length; i++)
            if (perceptron.predict(TrainingData.X[i]) == TrainingData.Y[i]) totalCorrect++;
        double overallAcc = (double) totalCorrect / TrainingData.X.length;

        lblAccuracy.setText(String.format("%.1f%%", overallAcc * 100));
        log("Training Finished.");
        log(String.format("  Overall Accuracy (Total): %.1f%%", overallAcc * 100));
        log(String.format("  - On Training set (80%%): %.1f%%", trainAcc * 100));
        log(String.format("  - On Testing set (20%%): %.1f%%", testAcc * 100));
        lblWeights.setText(perceptron.getWeightsSummary());
        perceptron.predictAll(plants);
        refreshTable();
        gardenPanel.repaint();
        lcPanel.setData(perceptron.epochLoss, perceptron.epochAccuracy);
    }


    private CostCurvePanel costPanel;
    private JSpinner spTemp, spCooling, spIter;
    private JTextArea taResult;

    private JPanel buildSATab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(C_BG);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(C_BG);
        right.setPreferredSize(new Dimension(300, 0));

        JPanel saCard = new JPanel(new GridLayout(0, 2, 8, 8));
        saCard.setBackground(C_SURFACE);
        saCard.setBorder(new CompoundBorder(new LineBorder(C_BORDER, 1, true), new EmptyBorder(12, 14, 12, 14)));
        spTemp    = darkSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        spCooling = darkSpinner(new SpinnerNumberModel(0.003, 0.001, 0.05, 0.001));
        spIter    = darkSpinner(new SpinnerNumberModel(2000, 500, 10000, 500));
        JLabel saHeader = sectionLabel("SA Parameters");
        saCard.add(saHeader); saCard.add(new JLabel());
        saCard.add(darkLabel("Initial Temp:")); saCard.add(spTemp);
        saCard.add(darkLabel("Cooling Rate:")); saCard.add(spCooling);
        saCard.add(darkLabel("Max Iter:"));     saCard.add(spIter);

        JPanel inputCard = new JPanel(new GridLayout(0, 1, 6, 6));
        inputCard.setBackground(C_SURFACE);
        inputCard.setBorder(new CompoundBorder(new LineBorder(C_BORDER, 1, true), new EmptyBorder(12, 14, 12, 14)));
        JLabel inHeader = sectionLabel("Optimization Input");
        JLabel lblInfo  = new JLabel("How many plants you want to water?");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblInfo.setForeground(C_TEXT_DIM);
        spVisitCount = darkSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        inputCard.add(inHeader); inputCard.add(lblInfo); inputCard.add(spVisitCount);

        JButton btnRun = accentButton("Run SA Optimizer", C_PURPLE);
        btnRun.setAlignmentX(CENTER_ALIGNMENT);
        btnRun.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnRun.addActionListener(_ -> runSA());

        JButton btnAnimate = accentButton("Show SA Steps", new Color(120, 60, 160));
        btnAnimate.setAlignmentX(CENTER_ALIGNMENT);
        btnAnimate.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnAnimate.addActionListener(_ -> showSASteps());

        taResult = new JTextArea(7, 0);
        taResult.setEditable(false);
        taResult.setFont(new Font("Consolas", Font.PLAIN, 12));
        taResult.setBackground(C_SURFACE);
        taResult.setForeground(C_TEXT);
        taResult.setBorder(new EmptyBorder(8, 10, 8, 10));
        taResult.setText("Run SA to see the optimal watering order...");
        JScrollPane resultScroll = new JScrollPane(taResult);
        resultScroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel resultCard = wrapInCard(resultScroll, "Optimal Watering Order");
        resultCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        right.add(saCard);
        right.add(Box.createVerticalStrut(8));
        right.add(inputCard);
        right.add(Box.createVerticalStrut(8));
        right.add(btnRun);
        right.add(Box.createVerticalStrut(6));
        right.add(btnAnimate);
        right.add(Box.createVerticalStrut(8));
        right.add(resultCard);
        right.add(Box.createVerticalGlue());

        costPanel = new CostCurvePanel();
        JPanel costCard = wrapInCard(costPanel, "SA Cost Over Iterations");

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(10, 13, 18));
        logArea.setForeground(new Color(160, 200, 120));
        logArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        JPanel logCard = wrapInCard(logScroll, "Log");

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, costCard, logCard);
        leftSplit.setResizeWeight(0.75);
        leftSplit.setBackground(C_BG);
        leftSplit.setBorder(BorderFactory.createEmptyBorder());
        leftSplit.setDividerSize(6);

        root.add(leftSplit, BorderLayout.CENTER);  // charts on LEFT
        root.add(right,     BorderLayout.EAST);    // controls on RIGHT
        return root;
    }

    private void runSA() {
        if (plants.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The garden is empty! Add plants first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int numToVisit = (int) spVisitCount.getValue();
        if (numToVisit > plants.size()) {
            JOptionPane.showMessageDialog(this,
                    "Invalid Input! You requested " + numToVisit + " plants, but you only have " + plants.size() + " in the garden.",
                    "Limit Reached", JOptionPane.WARNING_MESSAGE);
            return;
        }
        sa = new SimulatedAnnealing(
                ((Number) spTemp.getValue()).doubleValue(),
                ((Number) spCooling.getValue()).doubleValue(),
                ((Number) spIter.getValue()).intValue()
        );
        this.saSequence = new ArrayList<>(this.plants);
        bestOrder = sa.optimize(this.plants, numToVisit);

        StringBuilder sb = new StringBuilder("Optimal Sequence:\n");
        for (int i = 0; i < bestOrder.length; i++) {
            Plant p = plants.get(bestOrder[i]);
            sb.append(String.format("  %d. %s\n", i + 1, p.getName()));
        }
        taResult.setText(sb.toString());
        costPanel.setData(sa.costHistory);
        gardenPanel.repaint();
    }

    private void showSASteps() {
        if (sa.orderHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Run SA first!", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (stepTimer != null) stepTimer.stop();
        currentStep = 0;
        stepTimer = new Timer(300, e -> {
            if (currentStep >= sa.orderHistory.size()) { stepTimer.stop(); log("Animation finished!"); return; }
            bestOrder = sa.orderHistory.get(currentStep);
            gardenPanel.repaint();
            log("Step " + (currentStep * 50) + " | Cost: " +
                    String.format("%.2f", sa.costHistory.get(Math.min(currentStep * 50, sa.costHistory.size()-1))));
            currentStep++;
        });
        stepTimer.start();
        log("Showing SA steps...");
    }

    class GardenPanel extends JPanel {
        GardenPanel() { setBackground(new Color(15, 22, 15)); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight();


            g.setColor(new Color(35, 55, 35, 90));
            g.setStroke(new BasicStroke(0.5f));
            for (int x = 0; x < W; x += 50) g.drawLine(x, 0, x, H);
            for (int y = 0; y < H; y += 50) g.drawLine(0, y, W, y);

            if (bestOrder != null && saSequence.size() > 1) {
                g.setColor(new Color(88, 166, 255, 40));
                g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < bestOrder.length - 1; i++) {
                    Plant a = saSequence.get(bestOrder[i]); Plant b = saSequence.get(bestOrder[i + 1]);
                    g.drawLine(px(a, W), py(a, H), px(b, W), py(b, H));
                }
                g.setColor(C_PATH);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < bestOrder.length - 1; i++) {
                    Plant a = saSequence.get(bestOrder[i]); Plant b = saSequence.get(bestOrder[i + 1]);
                    g.drawLine(px(a, W), py(a, H), px(b, W), py(b, H));
                }
                g.setFont(new Font("Segoe UI", Font.BOLD, 10));
                for (int i = 0; i < bestOrder.length; i++) {
                    Plant p = saSequence.get(bestOrder[i]);
                    g.setColor(C_BLUE);
                    g.drawString(String.valueOf(i + 1), px(p, W) + 12, py(p, H) - 10);
                }
            }

            for (Plant p : plants) {
                int cx = px(p, W), cy = py(p, H);
                Color fill = p.getNeedsWater() == 1 ? C_BLUE
                        : p.getNeedsWater() == 0 ? C_ORANGE : C_TEXT_DIM;
                g.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 45));
                g.fillOval(cx - 20, cy - 20, 40, 40);
                g.setColor(fill);
                g.fillOval(cx - 13, cy - 13, 26, 26);
                g.setColor(fill.darker());
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(cx - 13, cy - 13, 26, 26);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.BOLD, 8));
                String icon = p.getPlantType() == 0 ? "CAC" : p.getPlantType() == 1 ? "FLW" : "HRB";
                g.drawString(icon, cx - 8, cy + 3);
                g.setColor(C_TEXT);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.drawString(p.getName(), cx - 12, cy + 22);
            }

            if (plants.isEmpty()) {
                g.setColor(new Color(50, 80, 50));
                g.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                g.drawString("<- Add plants using the form", W / 2 - 120, H / 2);
            }
        }
        private int px(Plant p, int W) { return (int)(p.getX() / 500.0 * W); }
        private int py(Plant p, int H) { return (int)(p.getY() / 500.0 * H); }
    }


    class LearningCurvePanel extends JPanel {
        private List<Double> loss = new ArrayList<>();
        private List<Double> acc  = new ArrayList<>();
        LearningCurvePanel() { setBackground(C_SURFACE); }
        void setData(List<Double> l, List<Double> a) { loss = l; acc = a; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (loss.isEmpty()) { drawHint(g0, "Train the perceptron to see the learning curve."); return; }
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth() - 70, H = getHeight() - 70;
            int ox = 50, oy = 20;

            g.setColor(C_BORDER);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(ox, oy, ox, oy + H);
            g.drawLine(ox, oy + H, ox + W, oy + H);

            for (int i = 0; i <= 5; i++) {
                int y = oy + H - i * H / 5;
                g.setColor(new Color(48, 54, 61, 60));
                g.drawLine(ox, y, ox + W, y);
                g.setColor(C_TEXT_DIM);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.drawString(String.format("%.1f", i * 0.2), ox - 36, y + 4);
            }

            drawCurve(g, loss, ox, oy, W, H, C_RED);
            drawCurve(g, acc,  ox, oy, W, H, C_GREEN);

            int lx = ox + W - 130;
            g.setColor(C_RED);   g.fillRoundRect(lx, oy + 8, 14, 4, 2, 2);
            g.setColor(C_TEXT_DIM); g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.drawString("Loss", lx + 18, oy + 14);
            g.setColor(C_GREEN); g.fillRoundRect(lx, oy + 24, 14, 4, 2, 2);
            g.setColor(C_TEXT_DIM);
            g.drawString("Accuracy", lx + 18, oy + 30);
            g.drawString("Epoch", ox + W / 2 - 15, oy + H + 38);
        }

        private void drawCurve(Graphics2D g, List<Double> data, int ox, int oy, int W, int H, Color color) {
            int n = data.size();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 35));
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < n; i++) {
                g.drawLine(ox + (i-1)*W/n, Math.max(oy, oy+H-(int)(data.get(i-1)*H)),
                        ox + i*W/n,     Math.max(oy, oy+H-(int)(data.get(i)*H)));
            }
            g.setColor(color);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < n; i++) {
                g.drawLine(ox + (i-1)*W/n, Math.max(oy, oy+H-(int)(data.get(i-1)*H)),
                        ox + i*W/n,     Math.max(oy, oy+H-(int)(data.get(i)*H)));
            }
        }
    }


    class CostCurvePanel extends JPanel {
        private List<Double> costs = new ArrayList<>();
        CostCurvePanel() { setBackground(C_SURFACE); }
        void setData(List<Double> c) { costs = c; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (costs.isEmpty()) { drawHint(g0, "Run the SA optimizer to see the cost curve."); return; }
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth() - 80, H = getHeight() - 70;
            int ox = 60, oy = 20;
            double maxC = costs.stream().mapToDouble(d->d).max().orElse(1);
            double minC = costs.stream().mapToDouble(d->d).min().orElse(0);
            double range = maxC - minC == 0 ? 1 : maxC - minC;

            g.setColor(C_BORDER);
            g.setStroke(new BasicStroke(1f));
            g.drawLine(ox, oy, ox, oy + H);
            g.drawLine(ox, oy + H, ox + W, oy + H);

            for (int i = 0; i <= 5; i++) {
                int y = oy + H - i * H / 5;
                g.setColor(new Color(48, 54, 61, 60));
                g.drawLine(ox, y, ox + W, y);
                g.setColor(C_TEXT_DIM);
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g.drawString(String.format("%.1f", minC + i * range / 5), ox - 48, y + 4);
            }

            int n = costs.size();
            g.setColor(new Color(C_PURPLE.getRed(), C_PURPLE.getGreen(), C_PURPLE.getBlue(), 35));
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < n; i++) {
                g.drawLine(ox+(i-1)*W/n, oy+H-(int)((costs.get(i-1)-minC)/range*H),
                        ox+i*W/n,     oy+H-(int)((costs.get(i)-minC)/range*H));
            }
            g.setColor(C_PURPLE);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < n; i++) {
                g.drawLine(ox+(i-1)*W/n, oy+H-(int)((costs.get(i-1)-minC)/range*H),
                        ox+i*W/n,     oy+H-(int)((costs.get(i)-minC)/range*H));
            }

            g.setColor(C_TEXT_DIM);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g.drawString("Iteration", ox + W / 2 - 20, oy + H + 38);
            g.drawString("Cost", 6, oy + H / 2);
        }
    }

    private JPanel wrapInCard(Component inner, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(C_SURFACE);
        card.setBorder(new LineBorder(C_BORDER, 1, true));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 7));
        header.setBackground(C_SURFACE2);
        header.setBorder(new MatteBorder(0, 0, 1, 0, C_BORDER));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(C_TEXT_DIM);
        header.add(lbl);
        card.add(header, BorderLayout.NORTH);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JButton accentButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        return b;
    }

    private JTextField darkField(String def) {
        JTextField tf = new JTextField(def, 8);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tf.setBackground(C_SURFACE2);
        tf.setForeground(C_TEXT);
        tf.setCaretColor(C_GREEN);
        tf.setBorder(new CompoundBorder(new LineBorder(C_BORDER, 1, true), new EmptyBorder(3, 7, 3, 7)));
        return tf;
    }

    private JSpinner darkSpinner(SpinnerModel model) {
        JSpinner sp = new JSpinner(model);
        sp.setBackground(C_SURFACE2);
        sp.setForeground(C_TEXT);
        JComponent ed = sp.getEditor();
        if (ed instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) ed).getTextField();
            tf.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            tf.setBackground(C_SURFACE2);
            tf.setForeground(C_TEXT);
            tf.setCaretColor(C_GREEN);
            tf.setBorder(new EmptyBorder(3, 6, 3, 6));
        }
        sp.setBorder(new LineBorder(C_BORDER, 1, true));
        return sp;
    }

    private <T> JComboBox<T> darkCombo(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cb.setBackground(C_SURFACE2);
        cb.setForeground(C_TEXT);
        cb.setBorder(new LineBorder(C_BORDER, 1, true));
        return cb;
    }

    private JLabel darkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(C_TEXT_DIM);
        return l;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(C_GREEN);
        return l;
    }


    private void refreshTable() {
        plantTableModel.setRowCount(0);
        String[] typeNames = {"Cactus","Flower","Herb"};
        for (Plant p : plants) {
            String nw = p.getNeedsWater() == 1 ? "Yes" : p.getNeedsWater() == 0 ? "No" : "-";
            plantTableModel.addRow(new Object[]{
                    p.getName(), String.format("%.0f", p.getX()), String.format("%.0f", p.getY()),
                    String.format("%.0f", p.getSoilMoisture()), String.format("%.0f", p.getLastWatered()),
                    typeNames[p.getPlantType()], nw
            });
        }
    }

    private void loadDemoPlants() {
        Object[][] demo = {
                {"Rose",     100, 80,  22, 36, 1},
                {"Cactus A", 280, 60,  80,  3, 0},
                {"Basil",    160, 260, 30, 14, 2},
                {"Daisy",    380, 200, 15, 42, 1},
                {"Mint",     80,  380, 40, 20, 2},
        };
        for (Object[] d : demo)
            plants.add(new Plant((String)d[0], (int)d[1], (int)d[2], (int)d[3], (int)d[4], (int)d[5]));
        refreshTable();
    }

    private void log(String msg) {
        if (logArea != null) {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private void drawHint(Graphics g, String msg) {
        g.setColor(C_BORDER);
        g.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        g.drawString(msg, 40, getHeight() / 2);
    }


    public static void main(String[] args) throws UnsupportedEncodingException {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));
        Locale.setDefault(Locale.ENGLISH);
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new MainGUI();
        });
    }
}

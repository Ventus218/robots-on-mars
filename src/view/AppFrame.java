package src.view;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import src.model.Mars;
import src.model.Rover;
import src.model.ScientistRover;

public class AppFrame extends JFrame implements ViewModel.Listener {

    private final JPanel leftPanel;
    private final GridPanel gridPanel;
    private final ViewModel model;
    private final Mars mars;

    @Override
    public void viewModelChanged() {
        SwingUtilities.invokeLater(() -> {
            leftPanel.removeAll();

            mars.rovers().stream().forEach(r -> {
                JPanel roverRow = createRoverRow(r);
                leftPanel.add(roverRow);
                leftPanel.add(Box.createVerticalStrut(10));
            });

            leftPanel.add(Box.createVerticalGlue());
            leftPanel.revalidate();
            leftPanel.repaint();
        });
    }

    public AppFrame(ViewModel model) throws IOException {
        super("Robots on Mars");
        this.model = model;
        model.addListener(this);
        this.mars = model.mars();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        // --- Left Panel Setup ---
        leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(250, 0));
        leftPanel.setBackground(Color.LIGHT_GRAY);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Main Layout ---
        gridPanel = new GridPanel(model, 20);
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, scrollPane);

        viewModelChanged();

        add(splitPane);
    }

    private JPanel createRoverRow(Rover r) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(new LineBorder(Color.GRAY, 1));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel dataPanel = new JPanel();
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.Y_AXIS));
        dataPanel.setOpaque(false);
        dataPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel nameLabel = new JLabel(r.name());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        dataPanel.add(nameLabel);

        JLabel batteryLabel = new JLabel("Battery: " + r.battery() + "/" + r.batteryCapacity());
        dataPanel.add(batteryLabel);

        if (r instanceof ScientistRover s) {
            // Optional: Change color to distinguish scientists
            nameLabel.setForeground(new Color(0, 102, 204));
            JLabel samplesLabel = new JLabel("Samples: " + s.carriedSamples() + "/" + s.samplesCapacity());
            dataPanel.add(samplesLabel);
        }

        // 2. Button Section (Right Side)
        JPanel buttonPanel = new JPanel(new GridBagLayout()); // Center the button vertically
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(0, 5, 0, 5));

        final var isSelectedRover = model.selectedRover().map(sel -> sel.equals(r)).orElse(false);
        JToggleButton eyeButton = new JToggleButton("Focus", isSelectedRover);
        eyeButton.setFocusPainted(false);
        eyeButton.setToolTipText("View Rover POV");
        if (isSelectedRover) {
            eyeButton.setText("Unfocus");
        }

        eyeButton.addActionListener(e -> {
            model.selectRoverNamed(r.name());
        });

        buttonPanel.add(eyeButton);

        // Add sections to the card
        row.add(dataPanel, BorderLayout.CENTER);
        row.add(buttonPanel, BorderLayout.EAST);

        return row;
    }
}

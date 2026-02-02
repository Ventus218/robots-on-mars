package src.view;

import java.awt.*;
import java.io.IOException;

import javax.swing.*;
import src.model.Mars;

public class AppFrame extends JFrame {

    private GridPanel gridPanel;

    public AppFrame(Mars model) throws IOException {
        super("Robots on Mars");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        // TODO: add info about robots
        JPanel leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(200, 0));
        leftPanel.setBackground(Color.LIGHT_GRAY);

        gridPanel = new GridPanel(model, 10);

        JScrollPane scrollPane = new JScrollPane(gridPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, scrollPane);
        splitPane.setDividerLocation(200);

        add(splitPane);
    }
}

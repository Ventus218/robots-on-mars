package src.view;

import java.awt.*;
import javax.swing.*;
import src.model.*;
import java.util.*;

class GridPanel extends JPanel implements Mars.Listener {

    private final Mars model;
    private final int cellSize;

    private final Map<Point, CellData> cells = new HashMap<>();

    public GridPanel(Mars model, int cellSize) {
        this.model = model;
        this.cellSize = cellSize;

        int size = model.side() * cellSize;
        setPreferredSize(new Dimension(size, size));
        setBackground(Color.WHITE);

        this.model.addListener(this);
        redraw();
    }

    @Override
    public void marsUpdated() {
        redraw();
    }

    private void redraw() {
        for (var x = model.negativeBound(); x <= model.positiveBound(); x++) {
            for (var y = model.negativeBound(); y <= model.positiveBound(); y++) {
                final var coordinates = new Coordinates(x, y);
                switch (model.cellAt(new Coordinates(x, y))) {
                    case Cell.Base() -> setCell(coordinates, Color.CYAN, "", null);
                    case Cell.Obstacle() -> setCell(coordinates, Color.BLACK, "", null);
                    case Cell.Empty() -> setCell(coordinates, Color.WHITE, "", null);
                    case Cell.MiningSpot(var mined) -> setCell(coordinates, Color.WHITE, "M", null);
                    case Cell.Sample() -> setCell(coordinates, Color.WHITE, "S", null);
                    case Cell.Rover(var name) -> setCell(coordinates, Color.GREEN, "", null);
                }

            }
        }
        repaint();
    }

    private void setCell(Coordinates coordinates, Color color, String text, Image image) {
        final var point = new Point(coordinates.x() + model.positiveBound(), coordinates.y() + model.positiveBound());
        cells.put(point, new CellData(color, text, image));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // Draw grid
        g2.setColor(Color.GRAY);
        for (int r = 0; r <= model.side(); r++) {
            g2.drawLine(0, r * cellSize, model.side() * cellSize, r * cellSize);
        }
        for (int c = 0; c <= model.side(); c++) {
            g2.drawLine(c * cellSize, 0, c * cellSize, model.side() * cellSize);
        }

        // Draw cells
        for (Map.Entry<Point, CellData> entry : cells.entrySet()) {
            Point p = entry.getKey();
            CellData data = entry.getValue();

            int x = p.x * cellSize;
            int y = p.y * cellSize;

            // Fill color
            if (data.color != null) {
                g2.setColor(data.color);
                g2.fillRect(x + 1, y + 1, cellSize - 1, cellSize - 1);
            }

            // Draw image
            if (data.image != null) {
                g2.drawImage(data.image, x, y, cellSize, cellSize, null);
            }

            // Draw text
            if (data.text != null && !data.text.isEmpty()) {
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (cellSize - fm.stringWidth(data.text)) / 2;
                int ty = y + (cellSize + fm.getAscent()) / 2 - 2;
                g2.drawString(data.text, tx, ty);
            }
        }
    }

    static class CellData {
        Color color;
        String text;
        Image image;

        CellData(Color color, String text, Image image) {
            this.color = color;
            this.text = text;
            this.image = image;
        }
    }
}

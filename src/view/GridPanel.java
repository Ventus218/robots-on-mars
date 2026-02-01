package src.view;

import java.awt.*;
import javax.swing.*;
import src.model.*;
import java.util.*;
import java.util.stream.Collectors;

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
        final var areaCoveredByAntennas = model.rovers().stream().map(r -> model.antennaRangeOf(r))
                .flatMap(Set::stream).collect(Collectors.toSet());
        for (var x = model.negativeBound(); x <= model.positiveBound(); x++) {
            for (var y = model.negativeBound(); y <= model.positiveBound(); y++) {
                final var coordinates = new Coordinates(x, y);
                final var terrain = model.terrainAt(new Coordinates(x, y));
                final var cellData = switch (terrain) {
                    case Terrain.Base() -> new CellData(Color.BLUE, "", null);
                    case Terrain.Obstacle() -> new CellData(Color.BLACK, "", null);
                    case Terrain.Empty() -> new CellData(Color.WHITE, "", null);
                    case Terrain.MiningSpot(var mined) -> new CellData(Color.WHITE, "M", null);
                    case Terrain.Sample() -> new CellData(Color.WHITE, "S", null);
                };
                if (model.roverAtCoordinates(coordinates).isPresent()) {
                    cellData.text = "R";
                }
                if (areaCoveredByAntennas.contains(coordinates) && !(terrain instanceof Terrain.Base)) {
                    cellData.color = Color.CYAN;
                }
                setCell(coordinates, cellData);
            }
        }
        repaint();
    }

    private void setCell(Coordinates coordinates, CellData cellData) {
        final var point = new Point(coordinates.x() + model.positiveBound(), coordinates.y() + model.positiveBound());
        cells.put(point, cellData);
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

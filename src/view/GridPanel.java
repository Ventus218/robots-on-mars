package src.view;

import java.awt.*;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import src.model.*;
import java.util.*;
import java.util.stream.Collectors;

class GridPanel extends JPanel implements Mars.Listener {

    private final Mars model;
    private final int cellSize;
    private final Image miningSpotImg;
    private final Image minedSpotImg;
    private final Image sampleImg;
    private final Image obstacleImg;
    private final Color terrainColor = new Color(243, 147, 107);

    private final Map<Point, CellData> cells = new HashMap<>();

    public GridPanel(Mars model, int cellSize) throws IOException {
        this.model = model;
        this.cellSize = cellSize;
        miningSpotImg = ImageIO.read(getClass().getResource("/mining_spot.png"));
        minedSpotImg = ImageIO.read(getClass().getResource("/mined_spot.png"));
        sampleImg = ImageIO.read(getClass().getResource("/sample.png"));
        obstacleImg = ImageIO.read(getClass().getResource("/obstacle.png"));

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
        areaCoveredByAntennas.addAll(model.antennaRangeOfBase());
        final var knownArea = model.knownArea();
        for (var x = model.negativeBound(); x <= model.positiveBound(); x++) {
            for (var y = model.negativeBound(); y <= model.positiveBound(); y++) {
                final var coordinates = new Coordinates(x, y);
                final var terrain = model.terrainAt(new Coordinates(x, y));
                final var cellData = new CellData(Color.GRAY, "", null);
                if (knownArea.contains(coordinates)) {
                    cellData.color = terrainColor;
                    if (areaCoveredByAntennas.contains(coordinates)) {
                        cellData.color = Color.CYAN;
                    }
                    switch (terrain) {
                        case Terrain.Base() -> cellData.color = Color.BLUE;
                        case Terrain.Obstacle() -> cellData.image = obstacleImg;
                        case Terrain.Empty() -> {
                        }
                        case Terrain.Sample() -> cellData.image = sampleImg;
                        case Terrain.MiningSpot(var mined) -> {
                            if (mined) {
                                cellData.image = minedSpotImg;
                            } else {
                                cellData.image = miningSpotImg;
                            }
                        }
                    }
                    if (model.roverAtCoordinates(coordinates).isPresent()) {
                        cellData.text = "R";
                    }
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

        // Draw cells
        for (Map.Entry<Point, CellData> entry : cells.entrySet()) {
            Point p = entry.getKey();
            CellData data = entry.getValue();

            int x = p.x * cellSize;
            int y = p.y * cellSize;

            // Fill color
            if (data.color != null) {
                g2.setColor(data.color);
                g2.fillRect(x, y, cellSize, cellSize);
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

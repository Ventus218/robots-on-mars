package src.view;

import java.awt.*;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import src.model.*;
import java.util.*;
import java.util.stream.Collectors;

class GridPanel extends JPanel implements ViewModel.Listener {

    private final ViewModel model;
    private final Mars mars;
    private final int cellSize;
    private final Image miningSpotImg;
    private final Image sampleImg;
    private final Image obstacleImg;
    private final Image baseImg;
    private final Image simpleRoverImg;
    private final Image scientistRoverImg;
    private final Color terrainColor = new Color(243, 147, 107);
    private final Color antennaOverlayColor = new Color(0.26f, 0.7f, 0.95f, 0.2f);

    private final Map<Coordinates, CellData> cells = new HashMap<>();

    public GridPanel(ViewModel model, int cellSize) throws IOException {
        this.model = model;
        this.model.addListener(this);
        this.mars = model.mars();
        this.cellSize = cellSize;
        miningSpotImg = ImageIO.read(getClass().getResource("/mining_spot.png"));
        sampleImg = ImageIO.read(getClass().getResource("/sample.png"));
        obstacleImg = ImageIO.read(getClass().getResource("/obstacle.png"));
        baseImg = ImageIO.read(getClass().getResource("/base.png"));
        simpleRoverImg = ImageIO.read(getClass().getResource("/simple_rover.png"));
        scientistRoverImg = ImageIO.read(getClass().getResource("/scientist_rover.png"));

        int size = mars.side() * cellSize;
        setPreferredSize(new Dimension(size, size));
        setBackground(Color.WHITE);

        redraw();
    }

    @Override
    public void viewModelChanged() {
        redraw();
    }

    private void redraw() {
        final var areaCoveredByAntennas = mars.rovers().stream().map(r -> mars.antennaRangeOf(r))
                .flatMap(Set::stream).collect(Collectors.toSet());
        areaCoveredByAntennas.addAll(mars.antennaRangeOfBase());
        final var knownArea = model.knownArea();
        final var selectedRover = model.selectedRover();
        for (var x = mars.negativeBound(); x <= mars.positiveBound(); x++) {
            for (var y = mars.negativeBound(); y <= mars.positiveBound(); y++) {
                final var coordinates = new Coordinates(x, y);
                setCellColor(coordinates, Color.GRAY);
                setCellImg(coordinates, null);
                setCellOverlay(coordinates, null);
                if (knownArea.keySet().contains(coordinates)) {
                    final var terrain = knownArea.get(coordinates);
                    setCellColor(coordinates, terrainColor);
                    switch (terrain) {
                        case Terrain.Base() -> setCellImg(coordinates, baseImg);
                        case Terrain.Obstacle() -> setCellImg(coordinates, obstacleImg);
                        case Terrain.Sample() -> setCellImg(coordinates, sampleImg);
                        case Terrain.MiningSpot() -> setCellImg(coordinates, miningSpotImg);
                        case Terrain.Empty() -> {
                        }
                    }
                }
            }
        }
        if (selectedRover.isPresent()) {
            final var rover = selectedRover.get();
            final var coord = mars.roverCoordinates().get(rover);
            displayRoverAt(coord, rover);
            displayAntennaRangeOf(rover);
            // Rovers in sight
            mars.cameraRangeOf(rover)
                    .forEach(c -> mars.roverAtCoordinates(c).ifPresent(neighbour -> displayRoverAt(c, neighbour)));
            // Rovers in antenna range
            mars.reachableRovers(rover).forEach(r -> displayAntennaRangeOf(r));
            if (mars.canReachBase(rover)) {
                displayAntennaRangeOfBase();
            }
        } else {
            mars.roverCoordinates().entrySet().stream()
                    .forEach(e -> {
                        displayRoverAt(e.getValue(), e.getKey());
                        displayAntennaRangeOf(e.getKey());
                    });
            displayAntennaRangeOfBase();
        }
        repaint();
    }

    private void displayRoverAt(Coordinates coordinates, Rover rover) {
        if (rover instanceof SimpleRover) {
            setCellImg(coordinates, simpleRoverImg);
        } else {
            setCellImg(coordinates, scientistRoverImg);
        }
    }

    private void displayAntennaRangeOfBase() {
        displayAntennaRange(mars.antennaRangeOfBase());
    }

    private void displayAntennaRangeOf(Rover rover) {
        displayAntennaRange(mars.antennaRangeOf(rover));
    }

    private void displayAntennaRange(Set<Coordinates> coord) {
        coord.forEach(c -> setCellOverlay(c, antennaOverlayColor));
    }

    private void setCellImg(Coordinates coordinates, Image image) {
        final var data = cells.getOrDefault(coordinates, new CellData(null, null, null));
        data.image = image;
        cells.put(coordinates, data);
    }

    private void setCellColor(Coordinates coordinates, Color color) {
        final var data = cells.getOrDefault(coordinates, new CellData(null, null, null));
        data.color = color;
        cells.put(coordinates, data);
    }

    private void setCellOverlay(Coordinates coordinates, Color overlay) {
        final var data = cells.getOrDefault(coordinates, new CellData(null, null, null));
        data.overlay = overlay;
        cells.put(coordinates, data);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // Draw cells
        for (Map.Entry<Coordinates, CellData> entry : cells.entrySet()) {
            final int translatedX = entry.getKey().x() + mars.positiveBound();
            final int translatedY = mars.side() - (entry.getKey().y() + mars.positiveBound());
            Coordinates c = new Coordinates(translatedX, translatedY);
            CellData data = entry.getValue();

            int x = c.x() * cellSize;
            int y = c.y() * cellSize;

            // Fill color
            if (data.color != null) {
                g2.setColor(data.color);
                g2.fillRect(x, y, cellSize, cellSize);
            }

            // Draw image
            if (data.image != null) {
                g2.drawImage(data.image, x, y, cellSize, cellSize, null);
            }
            // Fill color
            if (data.overlay != null) {
                g2.setColor(data.overlay);
                g2.fillRect(x, y, cellSize, cellSize);
            }
        }
    }

    static class CellData {
        Color color;
        Image image;
        Color overlay;

        CellData(Color color, Image image, Color overlay) {
            this.color = color;
            this.image = image;
            this.overlay = overlay;
        }
    }
}

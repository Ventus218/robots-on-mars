package src.view;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

import src.model.*;
import src.utils.Tuple;

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
    private final Map<Coordinates, CellData> changes = new HashMap<>();

    private BufferedImage gridCanvas;

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

        gridCanvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        redraw();
    }

    @Override
    public void viewModelChanged() {
        SwingUtilities.invokeLater(() -> {
            redraw();
        });
    }

    private void redraw() {
        final var knownArea = model.knownArea();
        final var antennasToDisplay = new HashSet<Coordinates>();
        final var roversToDisplay = new HashMap<Coordinates, Rover>();
        if (model.selectedRover().isPresent()) {
            final var rover = model.selectedRover().get();
            antennasToDisplay.addAll(mars.reachableRovers(rover).stream().flatMap(r -> mars.antennaRangeOf(r).stream())
                    .toList());
            if (mars.canReachBase(rover)) {
                antennasToDisplay.addAll(mars.antennaRangeOfBase());
            }
            antennasToDisplay.addAll(mars.antennaRangeOf(rover));
            roversToDisplay.putAll(
                    mars.cameraRangeOf(rover).stream()
                            .flatMap(c -> mars.roverAtCoordinates(c).map(r -> Tuple.of(r, c)).stream())
                            .collect(Collectors.toMap(t -> t._2(), t -> t._1())));
            roversToDisplay.put(mars.roverCoordinates().get(rover), rover);
        } else {
            antennasToDisplay.addAll(mars.rovers().stream().flatMap(r -> mars.antennaRangeOf(r).stream())
                    .toList());
            antennasToDisplay.addAll(mars.antennaRangeOfBase());
            roversToDisplay.putAll(mars.roverCoordinates().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getValue(), e -> e.getKey())));
        }
        for (var x = mars.negativeBound(); x <= mars.positiveBound(); x++) {
            for (var y = mars.negativeBound(); y <= mars.positiveBound(); y++) {
                var cellData = new CellData(Color.GRAY, null, null);
                final var coordinates = new Coordinates(x, y);
                if (knownArea.keySet().contains(coordinates)) {
                    final var terrain = knownArea.get(coordinates);
                    cellData = cellData.withColor(terrainColor);
                    switch (terrain) {
                        case Terrain.Base() -> cellData = cellData.withImage(baseImg);
                        case Terrain.Obstacle() -> cellData = cellData.withImage(obstacleImg);
                        case Terrain.Sample() -> cellData = cellData.withImage(sampleImg);
                        case Terrain.MiningSpot() -> cellData = cellData.withImage(miningSpotImg);
                        case Terrain.Empty() -> {
                        }
                    }
                }
                if (antennasToDisplay.contains(coordinates)) {
                    cellData = cellData.withOverlay(antennaOverlayColor);
                }
                if (roversToDisplay.containsKey(coordinates)) {
                    final var rover = roversToDisplay.get(coordinates);
                    if (rover instanceof SimpleRover) {
                        cellData = cellData.withImage(simpleRoverImg);
                    } else {
                        cellData = cellData.withImage(scientistRoverImg);
                    }
                }
                if (!cells.getOrDefault(coordinates, new CellData(null, null, null)).equals(cellData)) {
                    changes.put(coordinates, cellData);
                    cells.put(coordinates, cellData);
                }
            }
        }

        updateCanvas();
        repaint();
    }

    private void updateCanvas() {
        Graphics2D g2 = gridCanvas.createGraphics();

        for (var entry : changes.entrySet()) {
            final var coord = entry.getKey();
            CellData data = entry.getValue();

            final int translatedX = coord.x() + mars.positiveBound();
            final int translatedY = mars.side() - (coord.y() + mars.positiveBound());
            int x = translatedX * cellSize;
            int y = translatedY * cellSize;

            g2.setBackground(getBackground());
            g2.clearRect(x, y, cellSize, cellSize);

            if (data.color != null) {
                g2.setColor(data.color);
                g2.fillRect(x, y, cellSize, cellSize);
            }

            if (data.image != null) {
                g2.drawImage(data.image, x, y, cellSize, cellSize, null);
            }

            if (data.overlay != null) {
                g2.setColor(data.overlay);
                g2.fillRect(x, y, cellSize, cellSize);
            }
        }

        g2.dispose();
        changes.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (gridCanvas != null) {
            g.drawImage(gridCanvas, 0, 0, null);
        }
    }

    static record CellData(Color color, Image image, Color overlay) {

        CellData withColor(Color color) {
            return new CellData(color, image, overlay);
        }

        CellData withImage(Image image) {
            return new CellData(color, image, overlay);
        }

        CellData withOverlay(Color overlay) {
            return new CellData(color, image, overlay);
        }
    }
}

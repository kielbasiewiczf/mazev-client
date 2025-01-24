package example;

import example.domain.Request;
import example.domain.Response;
import example.domain.game.Cave;
import example.domain.game.Direction;
import example.domain.game.Item;
import example.domain.game.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.Utilities;
import java.lang.reflect.Array;
import java.util.*;

public class Strategy {

    private static final Logger logger  = LoggerFactory.getLogger(Strategy.class);
    public static final int HEALTH_CHECK = 30;
    private final GameInfo gameInfo;

    public Strategy(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }

    public Direction makeMove() {
        Cave cave = this.gameInfo.cave();
        Player player = this.gameInfo.currentPlayer();

        final Response.StateLocations.PlayerLocation currentLocation = this.gameInfo.
                players().
                stream().
                filter(playerLocation -> playerLocation.entity().equals(player)).
                findAny().
                get();

        int row = currentLocation.location().row();
        int column = currentLocation.location().column();

        var healthItems = gameInfo.items().stream().filter(x -> x.entity() instanceof Item.Health).toList();
        var goldItems = gameInfo.items().stream().filter(x -> x.entity() instanceof Item.Gold).toList();


        if (gameInfo.health() <= HEALTH_CHECK && !healthItems.isEmpty()) {
            return null;
        } else if (!goldItems.isEmpty()){
            return null;
        }
        else
            return null;
    }

    private MapUtilities.Node[] calculateDistancesUsingDijkstra(int start_row, int start_column) {
        Cave cave = gameInfo.cave();

        var nodes = new MapUtilities.Node[gameInfo.cave().columns() * gameInfo.cave().rows()];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = new MapUtilities.Node(i);
        var dragonTiles = calculateDragonCoordinates();

        HashSet<Integer>[] neighbours = new HashSet[cave.rows() * cave.columns()];
        for (int i = 0; i < neighbours.length; i++) {
            neighbours[i] = new HashSet<>();
        }
        for (int i = 0; i < cave.rows(); i++) {
            for (int j = 0; j < cave.columns(); j++) {
                int index = MapUtilities.coordinates2DTo1D(i, j, cave);
                if (!cave.rock(i, j)) {
                    if (!cave.rock(i - 1, j)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(i - 1, j, cave));
                    }
                    if (!cave.rock(i + 1, j)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(i + 1, j, cave));
                    }
                    if (!cave.rock(i, j - 1)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(i, j - 1, cave));
                    }
                    if (!cave.rock(i, j + 1)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(i, j + 1, cave));
                    }
                }
            }
        }

        int myPosition = MapUtilities.coordinates2DTo1D(start_row, start_column, cave);
        nodes[myPosition].distance = 0;

        PriorityQueue<MapUtilities.Node> queue = new PriorityQueue<>((MapUtilities.Node x, MapUtilities.Node y) -> {
            if (x.distance < y.distance) return -1; else return 1;
        });

        queue.add(nodes[myPosition]);
        while (!queue.isEmpty()) {
            MapUtilities.Node currentNode = queue.poll();
            if(currentNode.visited)
                continue;
            currentNode.visited = true;
            for (int neighbour : neighbours[currentNode.index]) {
                if (!nodes[neighbour].visited) {
                    int distanceToNeighbour = dragonTiles[neighbour] ? MapUtilities.DISTANCE_DRAGON_PENALTY : MapUtilities.DISTANCE_NORMAL; // odległość sąsiada od OBECNEGO punktu
                    int neighbourDistance = nodes[neighbour].distance; // odległość sąsiada od punktu POCZĄTKOWEGO
                    if (neighbourDistance > distanceToNeighbour + currentNode.distance) {
                        nodes[neighbour].distance = distanceToNeighbour + currentNode.distance;
                        nodes[neighbour].parent = currentNode.index;
                    }
                    queue.add(nodes[neighbour]);
                }
            }
        }
        return nodes;
    }

    private boolean[] calculateDragonCoordinates() {
        var dragonTiles = new boolean[gameInfo.cave().columns() * gameInfo.cave().rows()];
        for (var location : gameInfo.players()) {
            if (location.entity() instanceof Player.Dragon) {
                Player.Dragon dragon = (Player.Dragon)location.entity();
                Player.Dragon.Size size = dragon.size();
                int radius = 0;
                switch (size) {
                    case Small -> radius = MapUtilities.DRAGON_SMALL_RADIUS;
                    case Medium -> radius = MapUtilities.DRAGON_MEDIUM_RADIUS;
                    case Large -> radius = MapUtilities.DRAGON_LARGE_RADIUS;
                    default -> radius = 0;
                }

                int row = location.location().row();
                int column = location.location().column();
                for (int i = row - radius; i <= row + radius; i++)
                    for (int j = column - radius; j <= column + radius; j++)
                        dragonTiles[MapUtilities.coordinates2DTo1D(i, j, gameInfo.cave())] = true;

            }
        }
        return dragonTiles;
    }
}

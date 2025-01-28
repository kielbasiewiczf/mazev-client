package example;

import example.domain.Response;
import example.domain.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Strategy {

    private static final Logger logger = LoggerFactory.getLogger(Strategy.class);
    public static final int HEALTH_CHECK = 121;
    private final GameInfo gameInfo;

    //konstruktor klasy Strategy
    public Strategy(GameInfo gameInfo) {
        this.gameInfo = gameInfo;
    }


    // wykonaj ruch w stronę zlota/hp
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

        var nodes = calculateDistancesUsingDijkstra(row, column);
        List<Response.StateLocations.ItemLocation> items = null;
        if (gameInfo.health() <= HEALTH_CHECK && !healthItems.isEmpty()) items = healthItems;
        else if (!goldItems.isEmpty()) items = goldItems;
        else return null;
        // dla każdego hp itemu znajdujemy jego położenie na mapie i wybieramy najbliższe HP...
        // backtrackujemy wierzchołki od HP do naszej pozycji TO SAMO DLA GOLDA
        // rozważyć jeśli zloto lub HP otoczone kamieniami
        int minDist = MapUtilities.DISTANCE_INFINITY;
        int minDistIndex = 0;
        for (var item : items) {
            if (nodes[MapUtilities.coordinates2DTo1D(item.location().row(), item.location().column(), cave)].distance <= minDist) {
                minDist = nodes[MapUtilities.coordinates2DTo1D(item.location().row(), item.location().column(), cave)].distance;
                minDistIndex = MapUtilities.coordinates2DTo1D(item.location().row(), item.location().column(), cave);
            }
        }
        if (minDist < MapUtilities.DISTANCE_INFINITY) {
            //zmiana
            //for(var playerLoc : Collection<Response.StateLocations.PlayerLocation> playerLocations)
            //zmiana
            List<Integer> path = backtrackFromItem(row, minDistIndex, nodes);
            int currentIndex = path.get(0), nextIndex = path.get(1);
            Location nextLocation = MapUtilities.coordinates1DTo2D(nextIndex, cave);
            int nextRow = nextLocation.row();
            int nextColumn = nextLocation.column();
            return getDirection(row, column, nextRow, nextColumn);
        }
        return null;
    }

    private MapUtilities.Node[] calculateDistancesUsingDijkstra(int start_row, int start_column) {
        Cave cave = gameInfo.cave();

        // tworze obiekt typu Node dla każdej kratki na mapie
        var nodes = new MapUtilities.Node[gameInfo.cave().columns() * gameInfo.cave().rows()];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = new MapUtilities.Node(i);
        var dragonTiles = calculateDragonCoordinates();

        HashSet<Integer>[] neighbours = new HashSet[cave.rows() * cave.columns()];
        for (int i = 0; i < neighbours.length; i++) {
            neighbours[i] = new HashSet<>();
        }
        // tablica 1D sąsiadów dla każdego wierzchołka na mapie
        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                int index = MapUtilities.coordinates2DTo1D(row, column, cave);
                if (!cave.rock(row, column)) {
                    if (!cave.rock(row - 1, column)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(row - 1, column, cave));
                    }
                    if (!cave.rock(row + 1, column)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(row + 1, column, cave));
                    }
                    if (!cave.rock(row, column - 1)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(row, column - 1, cave));
                    }
                    if (!cave.rock(row, column + 1)) {
                        neighbours[index].add(MapUtilities.coordinates2DTo1D(row, column + 1, cave));
                    }
                }
            }
        }

        int myPosition = MapUtilities.coordinates2DTo1D(start_row, start_column, cave);
        nodes[myPosition].distance = 0;

        //komparator, inicjalizacja kolejki priorytetowej
        PriorityQueue<MapUtilities.Node> queue = new PriorityQueue<>((MapUtilities.Node x, MapUtilities.Node y) -> {
            if (x.distance < y.distance) return -1;
            else return 1;
        });

        queue.add(nodes[myPosition]);
        while (!queue.isEmpty()) {
            MapUtilities.Node currentNode = queue.poll();
            if (currentNode.visited)
                continue;
            currentNode.visited = true;
            for (int neighbour : neighbours[currentNode.index]) {
                if (!nodes[neighbour].visited) {
                    int distanceToNeighbour = dragonTiles[neighbour] ? MapUtilities.DISTANCE_DRAGON_PENALTY : MapUtilities.DISTANCE_NORMAL; // odległość sąsiada od OBECNEGO punktu
                    int neighbourDistanceFromStart = nodes[neighbour].distance; // odległość sąsiada od punktu POCZĄTKOWEGO
                    if (neighbourDistanceFromStart > distanceToNeighbour + currentNode.distance) {
                        nodes[neighbour].distance = distanceToNeighbour + currentNode.distance;
                        nodes[neighbour].parentIndex = currentNode.index;
                        queue.add(nodes[neighbour]);
                    }
                }
            }
        }
        return nodes;
    }

    private boolean[] calculateDragonCoordinates() {
        var dragonTiles = new boolean[gameInfo.cave().columns() * gameInfo.cave().rows()];
        for (var location : gameInfo.players()) {
            if (location.entity() instanceof Player.Dragon) {
                Player.Dragon dragon = (Player.Dragon) location.entity();
                Player.Dragon.Size dragonSize = dragon.size();
                int radius = 0;
                switch (dragonSize) {
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

    // zwraca ścieżkę (jako listę indeksów) w kierunku od poczatku do konca
    List<Integer> backtrackFromItem(int startIndex, int targetIndex, MapUtilities.Node[] nodes) {
        List<Integer> path = new ArrayList<>();
        MapUtilities.Node currentNode = nodes[targetIndex];
        path.add(targetIndex);
        while (currentNode.parentIndex != MapUtilities.NO_PARENT) {
            currentNode = nodes[currentNode.parentIndex];
            path.addFirst(currentNode.index);
        }
        return path;
    }

    Direction getDirection(int startRow, int startColumn, int endRow, int endColumn) {
        if (endRow == startRow && endColumn == startColumn + 1) return Direction.Right;
        else if (endRow == startRow && endColumn == startColumn - 1) return Direction.Left;
        else if (endRow == startRow - 1 && endColumn == startColumn) return Direction.Up;
        else if (endRow == startRow + 1 && endColumn == startColumn) return Direction.Down;
        else return null;
    }
}

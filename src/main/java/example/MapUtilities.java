package example;

import example.domain.game.Cave;
import example.domain.game.Location;


public class MapUtilities {
    //TODO: dodać public static final int PLAYER_PENALTY = 2? // w przypadku małej ilości HP aby uniknąć zderzeń
    public static final int DISTANCE_INFINITY = Integer.MAX_VALUE;
    public static final int DISTANCE_DRAGON_PENALTY = 10;
    public static final int DISTANCE_NORMAL = 1;
    public static final int DRAGON_SMALL_RADIUS = 1;
    public static final int DRAGON_MEDIUM_RADIUS = 2;
    public static final int DRAGON_LARGE_RADIUS = 3;
    public static final int NO_PARENT = -1;

    // konstruktor dla Node do Dijkstry, parent wierzchołka jest tylko indexem dla uproszczenia pisania algorytmu
    public static class Node {
        public int index;
        public int distance;
        public int parentIndex;
        public boolean visited;


        public Node(int index) {
            this.index = index;
            this.distance = DISTANCE_INFINITY;
            this.parentIndex = NO_PARENT;
            this.visited = false;
        }

        public Node(int index, int distance) {
            this.index = index;
            this.distance = distance;
            this.parentIndex = NO_PARENT;
            this.visited = false;
        }


    }

    public static int coordinates2DTo1D(int row, int column, Cave cave) {
        row = row % cave.rows();
        column = column % cave.columns();
        return row * cave.columns() + column;
    }

    public static int coordinates2DTo1D(Location coords, Cave cave) {
        int row = coords.row() % cave.rows();
        int column = coords.column() % cave.columns();
        return row * cave.columns() + column;
    }

    public static Location coordinates1DTo2D(int index, Cave cave) {
        int row = index / cave.columns();
        int column = index % cave.columns();
        return new Location(row, column);
    }
}

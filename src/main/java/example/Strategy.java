package example;

import example.domain.Request;
import example.domain.Response;
import example.domain.game.Cave;
import example.domain.game.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Strategy {
    // czytamy info o jaskini
    // zapisujemy ile mamy życia :
    // jesli HP<30 szukamy HP na mapie Dijkstrą - wybieramy najblizszy ()

    public static final int BIG_DIST = 99999;

    private static final Logger logger = LoggerFactory.getLogger(Strategy.class);


    private int convertTableIndex(int row, int column, Cave cave) {
        row = row % cave.rows();
        column = column % cave.columns();
        return row * cave.columns() + column;
    }

    public Direction makeMove(Cave cave,
                              Collection<Response.StateLocations.PlayerLocation> playerLocation,
                              Collection<Response.StateLocations.ItemLocation> itemLocation,
                              Response.StateLocations.PlayerLocation player) {


        int[] distances = new int[cave.rows() * cave.columns()];
        for (int i = 0; i < cave.rows(); i++) {
            for (int j = 0; j < cave.columns(); j++) {
                distances[convertTableIndex(i, j, cave)] = BIG_DIST;
            }
        }

        int x_cord, y_cord;
        y_cord = player.location().row();
        x_cord = player.location().column();
        distances[convertTableIndex(x_cord, y_cord, cave)] = 0;

        dijkstra(distances, x_cord, y_cord, cave);
        return null;
    }


    public void dijkstra(int[] distances, int x_cord, int y_cord, Cave cave) {
        HashSet<Integer>[] neighbours = new HashSet[cave.rows() * cave.columns()];
        for (int i = 0; i < neighbours.length; i++) {
            neighbours[i] = new HashSet<>();
        }

        boolean[] visited = new boolean[cave.rows() * cave.columns()];
        for (int i = 0; i < visited.length; i++) {
            visited[i] = false;
        }

        for (int i = 0; i < cave.rows(); i++) {

            for (int j = 0; j < cave.columns(); j++) {
                int index = convertTableIndex(i, j, cave);
                if (!cave.rock(i, j)) {
                    if (!cave.rock(i - 1, j)) {
                        neighbours[index].add(convertTableIndex(i - 1, j, cave));
                    }
                    if (!cave.rock(i + 1, j)) {
                        neighbours[index].add(convertTableIndex(i + 1, j, cave));
                    }
                    if (!cave.rock(i, j - 1)) {
                        neighbours[index].add(convertTableIndex(i, j - 1, cave));
                    }
                    if (!cave.rock(i, j + 1)) {
                        neighbours[index].add(convertTableIndex(i, j + 1, cave));
                    }
                }
            }
        }

        int myPosition = convertTableIndex(x_cord, y_cord, cave);

        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();

        queue.addFirst(myPosition);
        while (!queue.isEmpty()) {
            int currentPosition = queue.removeFirst();
            visited[currentPosition] = true;
            var neighboursList = neighbours[currentPosition];
            for (int neighbour : neighboursList) {
                logger.info("{},{}", distances[neighbour], distances[currentPosition]);
                if (!visited[neighbour] ) {
                    distances[neighbour] = distances[currentPosition] + 1;
                    queue.addLast(neighbour);
                }
            }

        }
        for (int i = 0; i < distances.length; i++) {
            if (distances[i] != BIG_DIST)
                logger.info("distances[{},{}] = {}", i / cave.columns(), i % cave.columns(), distances[i]);
        }
        // TODO: dla każdego wierzchołka dodanie jego poprzednika + logika (szukanie HP / GOLDA)
    }


}

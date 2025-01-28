package example;

import example.domain.Response;
import example.domain.game.Cave;
import example.domain.game.Item;
import example.domain.game.Player;

import java.util.Collection;

public class Printer {
    public static char[] render(Cave cave,
                                Collection<Response.StateLocations.PlayerLocation> playerLocation,
                                Collection<Response.StateLocations.ItemLocation> itemLocation) {
        final var board = new char[cave.columns() * cave.rows()];
        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                if (cave.rock(row, column)) {
                    board[row * cave.columns() + column] = 'X';
                } else {
                    board[row * cave.columns() + column] = ' ';
                }
            }
        }


        for (final var entry : playerLocation) {
            final var location = entry.location();
            final var entity = entry.entity();
            if (entity instanceof Player.HumanPlayer humanPlayer) {
                if (humanPlayer.name().equals("Filip Kiełbasiewicz"))
                    board[location.row() * cave.columns() + location.column()] = 'F';
                else board[location.row() * cave.columns() + location.column()] = 'P';
            } else {
                board[location.row() * cave.columns() + location.column()] = 'D';
            }
        }

        for (final var entry : itemLocation) {
            final var location = entry.location();

            if (entry.entity() instanceof Item.Gold) {
                board[location.row() * cave.columns() + location.column()] = 'G';
            } else if (entry.entity() instanceof Item.Health) {
                board[location.row() * cave.columns() + location.column()] = 'H';
            }
        }

        for (int row = 0; row < cave.rows(); row++) {
            for (int column = 0; column < cave.columns(); column++) {
                System.out.print(board[row * cave.columns() + column]);
            }
            System.out.println();
        }
        return board;
    }

}

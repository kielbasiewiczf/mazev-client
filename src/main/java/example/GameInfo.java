package example;

import example.domain.Response;
import example.domain.game.Cave;
import example.domain.game.Player;

import java.util.Collection;

public record GameInfo(Cave cave,
                       Player currentPlayer,
                       Collection<Response.StateLocations.PlayerLocation> players,
                       Collection<Response.StateLocations.ItemLocation> items,
                       int health,
                       int gold) {
}
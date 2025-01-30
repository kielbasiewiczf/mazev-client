package example;

import com.fasterxml.jackson.databind.ObjectMapper;
import example.domain.Request;
import example.domain.Response;
import example.domain.game.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Collection;


public class Client {
    private static final String HOST    = "35.208.184.138";
    private static final int PORT       = 8080;
    private static final String KEY     = "Kh9PJSj2";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Client.class);


    public static void main(String[] args) {
        new Client().startClient();
    }
    //800 ms na pobranie i wysłanie danych, serwer odświeża się co sekundę
    public void startClient() {
        try (final var socket = new Socket(HOST, PORT);
             final var is = socket.getInputStream();
             final var isr = new InputStreamReader(is);
             final var reader = new BufferedReader(isr);
             final var os = socket.getOutputStream();
             final var osr = new OutputStreamWriter(os);
             final var writer = new BufferedWriter(osr)) {
            logger.info("Connected to server at {}:{}", HOST, PORT);
            {
                final var json = objectMapper.writeValueAsString(new Request.Authorize(KEY));
                writer.write(json);
                writer.newLine();
                writer.flush();
                logger.info("Sent command: {}", json);
            }

            // inicjalizacja, game info
            Cave cave = null;
            Collection<Response.StateLocations.ItemLocation> itemLocations;
            Collection<Response.StateLocations.PlayerLocation> playerLocations;
            int health, gold;
            Player myPlayer = null;

            while (!Thread.currentThread().isInterrupted()) {
                final var line = reader.readLine();
                if (line == null) {
                    break;
                }

                final var response = objectMapper.readValue(line, Response.class);
                switch (response) {
                    case Response.Authorized authorized -> {
                        myPlayer = authorized.humanPlayer();
                    }
                    case Response.Unauthorized unauthorized -> {
                        return;
                    }
                    case Response.StateCave stateCave -> {
                        cave = stateCave.cave();
                    }
                    // pobieranm aktualne info o mapie...
                    case Response.StateLocations stateLocations -> {
                        itemLocations       = stateLocations.itemLocations();
                        playerLocations     = stateLocations.playerLocations();
                        health              = stateLocations.health();
                        gold                = stateLocations.gold();

                        // agreguję dane w jednym recordzie
                        GameInfo gameInfo   = new GameInfo(cave, myPlayer, playerLocations, itemLocations, health, gold);
                        Printer.boardPrinter(cave, playerLocations, itemLocations);
                        Strategy strategy = new Strategy(gameInfo);
                        var strategyResult = strategy.makeMove();
                        logger.info("HP: {}, Gold: {}", health, gold);
                        final var cmd = new Request.Command(strategyResult);
                        final var cmdJson = objectMapper.writeValueAsString(cmd);
                        writer.write(cmdJson);
                        writer.newLine();
                        writer.flush();
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error in client operation", e);
        } finally {
            logger.info("Client exiting");
        }
    }

}
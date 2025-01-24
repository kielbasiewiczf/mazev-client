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
    private static final String HOST = "35.208.184.138";
//    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final String filipKey = "Kh9PJSj2";
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
                final var json = objectMapper.writeValueAsString(new Request.Authorize(filipKey));
                writer.write(json);
                writer.newLine();
                writer.flush();
                logger.info("Sent command: {}", json);
            }

            Cave cave = null;
            Player player = null;
            Collection<Response.StateLocations.ItemLocation> itemLocations;
            Collection<Response.StateLocations.PlayerLocation> playerLocations;

            while (!Thread.currentThread().isInterrupted()) {
                final var line = reader.readLine();
                if (line == null) {
                    break;
                }

                final var response = objectMapper.readValue(line, Response.class);
                Player finalPlayer = player;
                switch (response) {
                    case Response.Authorized authorized -> {
                        player = authorized.humanPlayer();
                        logger.info("authorized: {}", authorized);
                    }
                    case Response.Unauthorized unauthorized -> {
                        logger.error("unauthorized: {}", unauthorized);
                        return;
                    }
                    case Response.StateCave stateCave -> {
                        cave = stateCave.cave();
                        logger.info("cave: {}", cave);
                    }
                    case Response.StateLocations stateLocations -> {
                        itemLocations = stateLocations.itemLocations();
                        playerLocations = stateLocations.playerLocations();
                        //logger.info("itemLocations: {}", itemLocations);
                        //logger.info("playerLocations: {}", playerLocations);
                        BoardPrinter.render(cave, playerLocations, itemLocations);

                        // START
                        final var me = playerLocations.stream().filter(playerLocation -> playerLocation.entity().equals(finalPlayer)).findAny().get();
                        logger.info("My location {} ", me.location());
                        Strategy strat = new Strategy();
                        strat.makeMove(cave, playerLocations, itemLocations, me);
                        final var cmd = new Request.Command(Direction.Right);
                        //STOP

                        final var cmdJson = objectMapper.writeValueAsString(cmd);
                        writer.write(cmdJson);
                        writer.newLine();
                        writer.flush();
                        logger.info("Sent command: {}", cmd);
                    }
                }




                //
            }
        } catch (IOException e) {
            logger.error("Error in client operation", e);
        } finally {
            logger.info("Client exiting");
        }
    }
    //na przykład direction = Direction.Down
    private static void moveMyPlayer(BufferedWriter writer, Direction direction) throws IOException {
        final var cmd = new Request.Command(direction);
        final var cmdJson = objectMapper.writeValueAsString(cmd);
        writer.write(cmdJson);
        writer.newLine();
        writer.flush();
        logger.info("Sent command: {}", cmd);
    }


}
package hello;

import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@RestController
public class Application {

    static class WriteCommittedStream {

        final JsonStreamWriter jsonStreamWriter;

        public WriteCommittedStream(String projectId, String datasetName, String tableName) throws IOException, Descriptors.DescriptorValidationException, InterruptedException {

            try (BigQueryWriteClient client = BigQueryWriteClient.create()) {

                WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build();
                TableName parentTable = TableName.of(projectId, datasetName, tableName);
                CreateWriteStreamRequest createWriteStreamRequest =
                        CreateWriteStreamRequest.newBuilder()
                                .setParent(parentTable.toString())
                                .setWriteStream(stream)
                                .build();

                WriteStream writeStream = client.createWriteStream(createWriteStreamRequest);

                jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build();
            }
        }

        public ApiFuture<AppendRowsResponse> send(Arena arena) {
            Instant now = Instant.now();
            JSONArray jsonArray = new JSONArray();

            arena.state.forEach((url, playerState) -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("x", playerState.x);
                jsonObject.put("y", playerState.y);
                jsonObject.put("direction", playerState.direction);
                jsonObject.put("wasHit", playerState.wasHit);
                jsonObject.put("score", playerState.score);
                jsonObject.put("player", url);
                jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000);
                jsonArray.put(jsonObject);
            });

            return jsonStreamWriter.append(jsonArray);
        }

    }

    private final WriteCommittedStream writeCommittedStream;

    public Application() throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
        final String projectId = ServiceOptions.getDefaultProjectId();
        final String datasetName = "snowball";
        final String tableName = "events";
        writeCommittedStream = new WriteCommittedStream(projectId, datasetName, tableName);
    }

    private static String MYSELF = "https://34.149.220.180.sslip.io/";
    private static int THROW_DISTANCE = 3;

    static class Self {
        public String href;
    }

    static class Links {
        public Self self;
    }

    static class PlayerState {
        public Integer x;
        public Integer y;
        public String direction;
        public Boolean wasHit;
        public Integer score;

        public PlayerState withDirection(String direction) {
            PlayerState copy = new PlayerState();
            copy.x = this.x;
            copy.y = this.y;
            copy.direction = direction;
            copy.wasHit = this.wasHit;
            copy.score = this.score;
            return copy;
        }

        public PlayerState goStraight() {
            PlayerState copy = new PlayerState();
            copy.x = (direction.equals("E") || direction.equals("W")) ? goStraightX(x, direction) : this.x;
            copy.y = direction.equals("N") || direction.equals("S") ? goStraightY(y, direction) : this.y;
            copy.direction = this.direction;
            copy.wasHit = this.wasHit;
            copy.score = this.score;
            return copy;
        }

        private int goStraightX(int x, String direction) {
            if (direction.equals("E")) {
                return x + 1;
            } else return x - 1;
        }

        private int goStraightY(int y, String direction) {
            if (direction.equals("S")) {
                return y + 1;
            } else return y - 1;
        }
    }

    static class Arena {
        public List<Integer> dims;
        public Map<String, PlayerState> state;
    }

    static class ArenaUpdate {
        public Links _links;
        public Arena arena;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @GetMapping("/")
    public String index() {
        return "Let the battle begin!";
    }

    @PostMapping("/**")
    public String index(@RequestBody ArenaUpdate arenaUpdate) {
        writeCommittedStream.send(arenaUpdate.arena);
        Map<String, PlayerState> statesOfPlayers = arenaUpdate.arena.state;
        PlayerState myState = statesOfPlayers.get(MYSELF);


        Stream<PlayerState> playersStatesStream = statesOfPlayers.entrySet()
                .stream()
                .filter(it -> !it.getKey().equals(MYSELF))
                .map(Map.Entry::getValue);

        if (thereIsSomebodyOnMyWay(playersStatesStream, myState)) {
            return "T";
        } else {
            List<PlayerState> playersStates = playersStatesStream.collect(Collectors.toList());
            if (thereIsSomeoneOnMyRight(playersStates.stream(), myState)) {
                return "R";
            } else if (thereIsSomeoneOnMyLeft(playersStates.stream(), myState)) {
                return "L";
            } else if (iCanGoStraight(myState, arenaUpdate)) {
                return "F";
            } else {
                return "R";
            }
        }
    }

    private boolean iCanGoStraight(PlayerState myState, ArenaUpdate arenaUpdate) {
        PlayerState afterIGoStraight = myState.goStraight();
        List<Integer> dims = arenaUpdate.arena.dims;
        int x = dims.get(0);
        int y = dims.get(1);
        return afterIGoStraight.x < x && afterIGoStraight.y < y;
    }

    private boolean thereIsSomeoneOnMyRight(Stream<PlayerState> players, PlayerState myState) {
        switch (myState.direction) {
            case "N":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("E"));
            case "E":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("S"));
            case "S":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("W"));
            case "W":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("N"));
            default:
                return false;
        }
    }

    private boolean thereIsSomeoneOnMyLeft(Stream<PlayerState> players, PlayerState myState) {
        switch (myState.direction) {
            case "N":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("W"));
            case "E":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("N"));
            case "S":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("E"));
            case "W":
                return thereIsSomebodyOnMyWay(players, myState.withDirection("S"));
            default:
                return false;
        }
    }

    private boolean thereIsSomebodyOnMyWay(Stream<PlayerState> players, PlayerState myState) {
        int myXCooridinate = myState.x;
        int myYCooridinate = myState.y;

        switch (myState.direction) {
            case "N":
                return players.filter(it -> it.x == myXCooridinate)
                        .filter(it -> it.y <= myYCooridinate)
                        .anyMatch(it -> Math.abs(it.y - myYCooridinate) <= THROW_DISTANCE);
            case "S":
                return players.filter(it -> it.x == myXCooridinate)
                        .filter(it -> it.y >= myYCooridinate)
                        .anyMatch(it -> Math.abs(it.y - myYCooridinate) <= THROW_DISTANCE);
            case "E":
                return players.filter(it -> it.y == myYCooridinate)
                        .filter(it -> it.x >= myXCooridinate)
                        .anyMatch(it -> Math.abs(it.x - myXCooridinate) <= THROW_DISTANCE);
            case "W":
                return players.filter(it -> it.y == myYCooridinate)
                        .filter(it -> it.x <= myXCooridinate)
                        .anyMatch(it -> Math.abs(it.x - myXCooridinate) <= THROW_DISTANCE);
        }

        return false;
    }
}


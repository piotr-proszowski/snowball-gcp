package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@RestController
public class Application {

  private static String MYSELF = "https://java-springboot-qfkijwitea-uc.a.run.app/";
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
    System.out.println(arenaUpdate);
    Map<String, PlayerState> statesOfPlayers = arenaUpdate.arena.state;
    PlayerState myState = statesOfPlayers.get(MYSELF);

    List<PlayerState> playersStates = statesOfPlayers.entrySet().stream().filter(it -> !it.getKey().equals(MYSELF)).map(it -> it.getValue()).collect(Collectors.toList());

    if(thereIsSomebodyOnMyWay(playersStates, myState)) {
        return "T";
    } else {
        if(thereIsSomeoneOnMyRight(playersStates, myState)) {
            return "R";
        } else {
            return "L";
        }
    }
  }

  private boolean thereIsSomeoneOnMyRight(List<PlayerState> players, PlayerState myState) {
    if(myState.direction == "N") {
        return thereIsSomebodyOnMyWay(players, myState.withDirection("E"));
    } else if(myState.direction == "E") {
        return thereIsSomebodyOnMyWay(players, myState.withDirection("S"));
    } else if(myState.direction == "S") {
        return thereIsSomebodyOnMyWay(players, myState.withDirection("W"));
    } else if(myState.direction == "W") {
        return thereIsSomebodyOnMyWay(players, myState.withDirection("N"));
    } else {
        return false;
    }
  }

  private boolean thereIsSomebodyOnMyWay(List<PlayerState> players, PlayerState myState) {
      int myXCooridinate = myState.x;
      int myYCooridinate = myState.y;

      if(myState.direction.equals("N")) {
          return players.stream()
            .filter(it -> it.x == myXCooridinate)
            .filter(it -> it.y <= myYCooridinate)
            .filter(it -> Math.abs(it.y - myYCooridinate) <= THROW_DISTANCE)
            .findAny()
            .isPresent();            
      } else if(myState.direction.equals("S")) {
          return players.stream()
            .filter(it -> it.x == myXCooridinate)
            .filter(it -> it.y >= myYCooridinate)
            .filter(it -> Math.abs(it.y - myYCooridinate) <= THROW_DISTANCE)
            .findAny()
            .isPresent();  
      } else if(myState.direction.equals("E")) {
          return players.stream().filter(it -> it.y == myYCooridinate)
            .filter(it -> it.x >= myXCooridinate)
            .filter(it -> Math.abs(it.x - myXCooridinate) <= THROW_DISTANCE)
            .findAny()
            .isPresent();
      } else if(myState.direction.equals("W")) {
          return players.stream().filter(it -> it.y == myYCooridinate)
            .filter(it -> it.x <= myXCooridinate)
            .filter(it -> Math.abs(it.x - myXCooridinate) <= THROW_DISTANCE)
            .findAny()
            .isPresent(); 
      }

      return false;
  }
}


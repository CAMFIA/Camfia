package s05.p12a104.mafia.stomp.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.StompMessageType;

@Getter
public class GameStatusRes {

  private StompMessageType type;
  private GameStatus gameStatus;
  private Map<String, PlayerStatus> playerMap;

  public static GameStatusRes of(GameSession gameSession, List<Player> players) {
    GameStatusRes gameStatusRes = new GameStatusRes();
    gameStatusRes.type = StompMessageType.PHASE_CHANGED;
    gameStatusRes.gameStatus = GameStatus.of(gameSession);
    gameStatusRes.playerMap = new HashMap<>();

    players.forEach(player -> gameStatusRes.playerMap.put(player.getId(), PlayerStatus.of(player)));
    return gameStatusRes;
  }

}

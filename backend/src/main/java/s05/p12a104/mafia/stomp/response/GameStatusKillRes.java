package s05.p12a104.mafia.stomp.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.StompMessageType;

@Getter
public class GameStatusKillRes {

  private StompMessageType type;
  private GameStatusKill gameStatus;
  private Map<String, PlayerStatus> playerMap;

  public static GameStatusKillRes of(GameSession gameSession, List<Player> players, Player dead) {
    GameStatusKillRes gameStatusRes = new GameStatusKillRes();
    gameStatusRes.type = StompMessageType.PHASE_CHANGED;
    gameStatusRes.gameStatus = GameStatusKill.of(gameSession, dead);
    gameStatusRes.playerMap = new HashMap<>();
    for (Player player : players) {
      gameStatusRes.playerMap.put(player.getId(), PlayerStatus.of(player));
    }
    return gameStatusRes;
  }

}

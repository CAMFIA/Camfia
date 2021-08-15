package s05.p12a104.mafia.stomp.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.enums.StompMessageType;

@Getter
@RequiredArgsConstructor
public class StompResForRejoiningPlayer {

  private final StompMessageType type = StompMessageType.REJOIN;
  private final String hostId;
  private final GameStatus gameStatus;
  private final Map<String, StompExistingPlayer> playerMap;
  private final GameRole role;
  private final List<String> mafias;

  public StompResForRejoiningPlayer(GameSession gameSession, List<Player> players, String reJoiningplayerId) {
    this.playerMap = new HashMap<>();
    Player rejoiningPlayer = null;
    for (Player player : players) {
      playerMap.put(player.getId(), StompExistingPlayer.of(player));
      if (player.getId().equals(reJoiningplayerId)) {
        rejoiningPlayer = player;
      }
    }
    this.hostId = gameSession.getHostId();
    this.gameStatus = GameStatus.of(gameSession);
    this.role = rejoiningPlayer.getRole();
    this.mafias = gameSession.getMafias();
  }
}

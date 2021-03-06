package s05.p12a104.mafia.stomp.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.enums.StompMessageType;

@Getter
@ToString
@RequiredArgsConstructor
public class StompResForRejoiningPlayer {

  private final StompMessageType type = StompMessageType.REJOIN;
  private final String hostId;
  private final GameStatus gameStatus;
  private final Map<String, StompExistingPlayer> playerMap;
  private final GameRole role;
  private final List<String> mafias;

  public StompResForRejoiningPlayer(GameSession gameSession, List<Player> players,
      String reJoiningplayerId, Map<String, Boolean> confirmResult) {

    Player rejoiningPlayer = null;
    this.playerMap = players.stream().collect(Collectors.toMap(Player::getId,
        player -> StompExistingPlayer
            .of(player, confirmResult.getOrDefault(player.getId(), false))));

    for (Player player : players) {
      if (player.getId().equals(reJoiningplayerId)) {
        rejoiningPlayer = player;
        break;
      }
    }

    this.hostId = gameSession.getHostId();
    this.gameStatus = GameStatus.of(gameSession);
    this.role = rejoiningPlayer.getRole();
    this.mafias = this.role == GameRole.MAFIA ? gameSession.getMafias() : null;
  }
}
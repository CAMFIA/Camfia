package s05.p12a104.mafia.stomp.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import s05.p12a104.mafia.domain.entity.Player;
import lombok.ToString;
import s05.p12a104.mafia.domain.enums.StompMessageType;

@Getter
@RequiredArgsConstructor
@ToString
public class GameSessionStompJoinRes {

  private final StompMessageType type = StompMessageType.JOIN;
  private final String hostId;
  private final Map<String, PlayerStompJoinRes> playerMap;

  public static GameSessionStompJoinRes of(String hostId, List<Player> players) {
    Map<String, PlayerStompJoinRes> newPlayerMap = new HashMap<>();
    for (Player player : players) {
      newPlayerMap.put(player.getId(), PlayerStompJoinRes.of(player));
    }

    return new GameSessionStompJoinRes(hostId, newPlayerMap);
  }
}

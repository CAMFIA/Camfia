package s05.p12a104.mafia.api.service;

import java.util.List;
import java.util.Map;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import s05.p12a104.mafia.api.requset.GameSessionPostReq;
import s05.p12a104.mafia.api.response.GameSessionJoinRes;
import s05.p12a104.mafia.common.exception.GameSessionException;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.entity.User;
import s05.p12a104.mafia.domain.enums.GameRole;

public interface GameSessionService {
  GameSession createRoom(User user, GameSessionPostReq typeInfo)
      throws GameSessionException, OpenViduJavaClientException, OpenViduHttpException;

  GameSessionJoinRes getPlayerJoinableState(String roomId, String playerId);

  void deleteRoomById(String roomId);

  GameSession findById(String roomId);

  void update(GameSession update);

  GameSessionJoinRes addUser(String roomId, String nickname);

  void removeUser(String roomId, String playerId);

  void startGame(String roomId);

  boolean isDone(GameSession gameSession, List<Player> players, List<String> victims);

  void endGame(String roomId);

  Map<String, GameRole> getAllPlayerRole(String roomId, String playerId);
}

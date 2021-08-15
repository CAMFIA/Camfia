package s05.p12a104.mafia.stomp.controller;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import s05.p12a104.mafia.api.service.GameSessionService;
import s05.p12a104.mafia.common.exception.RedissonLockNotAcquiredException;
import s05.p12a104.mafia.common.util.TimeUtils;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.enums.GameState;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;
import s05.p12a104.mafia.redispubsub.RedisPublisher;
import s05.p12a104.mafia.stomp.response.GameSessionStompJoinRes;
import s05.p12a104.mafia.stomp.response.GameSessionStompLeaveRes;
import s05.p12a104.mafia.stomp.response.GameStatusRes;
import s05.p12a104.mafia.stomp.response.ObserverJoinRes;
import s05.p12a104.mafia.stomp.response.PlayerRoleRes;
import s05.p12a104.mafia.stomp.response.StompRejoinPlayer;
import s05.p12a104.mafia.stomp.response.StompResForRejoiningPlayer;
import s05.p12a104.mafia.stomp.task.StartFinTimerTask;

@Slf4j
@RequiredArgsConstructor
@Controller
public class RoomController {

  private final GameSessionService gameSessionService;
  private final RedissonClient redissonClient;
  private final RedisTemplate<String, Object> objRedisTemplate;
  private final PlayerRedisRepository playerRedisRepository;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final RedisPublisher redisPublisher;
  private final ChannelTopic topicStartFin;

  @MessageMapping("/{roomId}/join")
  public void joinGameSession(@DestinationVariable String roomId) {
    if (objRedisTemplate.opsForHash().entries("GameSession:" + roomId).isEmpty()) {
      return;
    }
    String hostId = objRedisTemplate.opsForHash().get("GameSession:" + roomId, "hostId").toString();
    List<Player> players = playerRedisRepository.findByRoomId(roomId);

    GameSessionStompJoinRes res = GameSessionStompJoinRes.of(hostId, players);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId, res);
  }

  @MessageMapping("/{roomId}/leave")
  public void leaveGameSession(SimpMessageHeaderAccessor accessor,
      @DestinationVariable String roomId) {
    String playerId = accessor.getUser().getName();

    gameSessionService.removeUser(roomId, playerId);
    // 마지막 player가 나가서 gameSession이 삭제되었는지 확인
    if (objRedisTemplate.opsForHash().entries("GameSession:" + roomId).isEmpty()) {
      return;
    }
    String hostId = objRedisTemplate.opsForHash().get("GameSession:" + roomId, "hostId").toString();

    GameSessionStompLeaveRes res = new GameSessionStompLeaveRes(hostId, playerId);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId, res);
  }

  @MessageMapping("/{roomId}/rejoin")
  public void rejoinGameSession(SimpMessageHeaderAccessor accessor,
      @DestinationVariable String roomId) {
    String playerId = accessor.getUser().getName();
    Optional<Player> playerOptional = playerRedisRepository.findById(playerId);
    if (!playerOptional.isPresent()) {
      return;
    }

    GameSession gameSession = gameSessionService.findById(roomId);
    if (gameSession.getState() != GameState.STARTED) {
      joinGameSession(roomId);
      return;
    }

    List<Player> players = playerRedisRepository.findByRoomId(roomId);
    StompResForRejoiningPlayer ResForRejoiningPlayer =
        new StompResForRejoiningPlayer(gameSession, players, playerId);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId + "/" + playerId, ResForRejoiningPlayer);

    StompRejoinPlayer resForExistingPlayer = StompRejoinPlayer.of(playerOptional.get());
    simpMessagingTemplate.convertAndSend("/sub/" + roomId, resForExistingPlayer);
  }

  @MessageMapping("/{roomId}/start")
  public void startGame(SimpMessageHeaderAccessor accessor, @DestinationVariable String roomId) {

    RLock lock = redissonClient.getLock("GameSession" + roomId);
    boolean isLocked = false;
    try {
      isLocked = lock.tryLock(2, 3, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    if (!isLocked) {
      throw new RedissonLockNotAcquiredException(
          "Lock을 얻을 수 없습니다 - Key : " + "GameSession" + roomId);
    }

    try {
      // 방장이 시작했는지 확인
      GameSession gameSession = gameSessionService.findById(roomId);
      String playerId = accessor.getUser().getName();
      if (gameSession.getState() == GameState.STARTED || !playerId.equals(gameSession.getHostId())) {
        return;
      }

      // 초기 설정하기
      gameSessionService.startGame(roomId);
      gameSession = gameSessionService.findById(roomId);

      Timer timer = new Timer();
      StartFinTimerTask task = new StartFinTimerTask(redisPublisher, topicStartFin);
      task.setRoomId(roomId);
      timer.schedule(task, TimeUtils.convertToDate(gameSession.getTimer()));

      List<Player> players = playerRedisRepository.findByRoomId(roomId);

      // 전체 전송
      simpMessagingTemplate.convertAndSend("/sub/" + roomId, GameStatusRes.of(gameSession, players));
      // 개인 전송
      for (Player player : players) {
        if (player.getRole() == GameRole.MAFIA) {
          simpMessagingTemplate.convertAndSend("/sub/" + roomId + "/" + player.getId(),
              PlayerRoleRes.of(player, gameSession.getMafias()));
        } else {
          simpMessagingTemplate.convertAndSend("/sub/" + roomId + "/" + player.getId(),
              PlayerRoleRes.of(player));
        }
      }

    } finally {
      lock.unlock();
    }
  }

  @MessageMapping("/{roomId}/OBSERVER")
  public void observerJoin(SimpMessageHeaderAccessor accessor, @DestinationVariable String roomId) {
    String playerId = accessor.getUser().getName();

    Map<String, GameRole> playersRole = gameSessionService.getAllPlayerRole(roomId, playerId);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId + "/" + GameRole.OBSERVER,
        ObserverJoinRes.of(playersRole));
  }

}

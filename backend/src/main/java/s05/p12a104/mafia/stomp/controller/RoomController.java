package s05.p12a104.mafia.stomp.controller;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import s05.p12a104.mafia.stomp.service.GameSessionVoteService;
import s05.p12a104.mafia.stomp.task.StartFinTimerTask;

@Slf4j
@RequiredArgsConstructor
@Controller
public class RoomController {

  private final GameSessionService gameSessionService;
  private final RedissonClient redissonClient;
  private final RedisTemplate<String, Object> objRedisTemplate;
  private final PlayerRedisRepository playerRedisRepository;
  private final GameSessionVoteService gameSessionVoteService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final RedisPublisher redisPublisher;
  private final ChannelTopic topicStartFin;

  @MessageMapping("/{roomId}/join")
  public void joinGameSession(@DestinationVariable String roomId) {
    log.info("req STOMP /ws/gamesession/{roomId}/join, roomId : {}", roomId);

    if (objRedisTemplate.opsForHash().entries("GameSession:" + roomId).isEmpty()) {
      return;
    }
    String hostId = objRedisTemplate.opsForHash().get("GameSession:" + roomId, "hostId").toString();
    List<Player> players = playerRedisRepository.findByRoomId(roomId);

    GameSessionStompJoinRes res = GameSessionStompJoinRes.of(hostId, players);

    log.info("res STOMP /ws/gamesession/{roomId}/join -  roomid : {}, res : {}", roomId, res);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId, res);
  }

  @MessageMapping("/{roomId}/leave")
  public void leaveGameSession(SimpMessageHeaderAccessor accessor,
      @DestinationVariable String roomId) {
    String playerId = accessor.getUser().getName();
    log.info("req STOMP /ws/gamesession/{roomId}/leave - roomId : {}, playerId : {}", roomId,
        playerId);

    gameSessionService.removeUser(roomId, playerId);

    // 마지막 player가 나가서 gameSession이 삭제되었는지 확인
    if (objRedisTemplate.opsForHash().entries("GameSession:" + roomId).isEmpty()) {
      return;
    }
    String hostId = objRedisTemplate.opsForHash().get("GameSession:" + roomId, "hostId").toString();

    GameSessionStompLeaveRes res = new GameSessionStompLeaveRes(hostId, playerId);

    log.info("res STOMP /ws/gamesession/{roomId}/leave - roomId : {}, res : {}", roomId, res);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId, res);
  }

  @MessageMapping("/{roomId}/rejoin")
  public void rejoinGameSession(SimpMessageHeaderAccessor accessor,
      @DestinationVariable String roomId) {
    String playerId = accessor.getUser().getName();
    log.info("req STOMP /ws/gamesession/{roomId}/rejoin - roomId : {}, playerId : {}", roomId,
        playerId);
    Optional<Player> playerOptional = playerRedisRepository.findById(playerId);
    if (!playerOptional.isPresent()) {
      return;
    }

    GameSession gameSession = gameSessionService.findById(roomId);
    if (gameSession.getState() != GameState.STARTED) {
      joinGameSession(roomId);
      return;
    }

    Map<String, Boolean> confirmResult = gameSessionVoteService.getConfirm(roomId, playerId);
    log.info("Room {} rejoin confirmSize {}", gameSession.getRoomId(), confirmResult.size());

    List<Player> players = playerRedisRepository.findByRoomId(roomId);
    StompResForRejoiningPlayer resForRejoiningPlayer =
        new StompResForRejoiningPlayer(gameSession, players, playerId, confirmResult);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId + "/" + playerId, resForRejoiningPlayer);

    StompRejoinPlayer resForExistingPlayer = StompRejoinPlayer.of(playerOptional.get());
    simpMessagingTemplate.convertAndSend("/sub/" + roomId, resForExistingPlayer);

    log.info("req STOMP /ws/gamesession/{roomId}/rejoin - roomId : {}, resForRejoiningPlayer : {}",
        roomId, resForRejoiningPlayer);
    log.info("req STOMP /ws/gamesession/{roomId}/rejoin - roomId : {}, resForExistingPlayer : {}",
        roomId, resForExistingPlayer);
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
      if (gameSession.getState() == GameState.STARTED || !playerId
          .equals(gameSession.getHostId())) {
        return;
      }

      // 초기 설정하기
      gameSessionService.startGame(roomId);
      gameSession = gameSessionService.findById(roomId);

      StartFinTimerTask task = new StartFinTimerTask(redisPublisher, topicStartFin);
      task.setRoomId(roomId);
      new Timer().schedule(task, TimeUtils.convertToDate(gameSession.getTimer()));

      List<Player> players = playerRedisRepository.findByRoomId(roomId);

      log.info("Room {} start game", roomId);

      // 전체 전송
      simpMessagingTemplate
          .convertAndSend("/sub/" + roomId, GameStatusRes.of(gameSession, players));
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
  public void observerJoin(SimpMessageHeaderAccessor accessor, @DestinationVariable String
      roomId) {
    String playerId = accessor.getUser().getName();
    log.info("req STOMP /ws/gamesession/{}/OBSERVER - playerId : {}", roomId, playerId);

    Map<String, GameRole> playersRole = gameSessionService.getAllPlayerRole(roomId, playerId);
    simpMessagingTemplate.convertAndSend("/sub/" + roomId + "/" + GameRole.OBSERVER,
        ObserverJoinRes.of(playersRole));
  }

}

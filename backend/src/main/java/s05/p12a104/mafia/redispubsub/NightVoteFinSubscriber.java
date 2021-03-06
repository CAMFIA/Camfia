package s05.p12a104.mafia.redispubsub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import s05.p12a104.mafia.api.service.GameSessionService;
import s05.p12a104.mafia.common.exception.RedissonLockNotAcquiredException;
import s05.p12a104.mafia.common.util.TimeUtils;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GamePhase;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;
import s05.p12a104.mafia.redispubsub.message.NightVoteMessage;
import s05.p12a104.mafia.stomp.response.GameStatusKillRes;
import s05.p12a104.mafia.stomp.response.PlayerDeadRes;
import s05.p12a104.mafia.stomp.response.SuspectVoteRes;
import s05.p12a104.mafia.stomp.task.StartFinTimerTask;

@Slf4j
@RequiredArgsConstructor
@Service
public class NightVoteFinSubscriber {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate template;
  private final RedisPublisher redisPublisher;
  private final GameSessionService gameSessionService;
  private final PlayerRedisRepository playerRedisRepository;
  private final ChannelTopic topicStartFin;
  private final RedissonClient redissonClient;
  private static final String KEY = "GameSession";

  public void sendMessage(String message) {
    try {
      NightVoteMessage nightVoteMessage = objectMapper.readValue(message, NightVoteMessage.class);
      String roomId = nightVoteMessage.getRoomId();
      Map<GameRole, String> roleVote = nightVoteMessage.getRoleVoteResult();

      RLock lock = redissonClient.getLock(KEY + roomId);
      boolean isLocked = false;
      try {
        isLocked = lock.tryLock(2, 3, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (!isLocked) {
        throw new RedissonLockNotAcquiredException("Lock??? ?????? ??? ???????????? - Key : " + KEY + roomId);
      }
      GameSession gameSession = null;
      String deadPlayerId = roleVote.get(GameRole.MAFIA);
      String protectedPlayerId = roleVote.get(GameRole.DOCTOR);
      String suspectPlayerId = roleVote.get(GameRole.POLICE);

      log.info("Room {} NightVote deadPlayer: {}", roomId, deadPlayerId);
      log.info("Room {} NightVote protectedPlayer: {}", roomId, protectedPlayerId);
      log.info("Room {} NightVote suspectPlayer: {}", roomId, suspectPlayerId);

      // ????????? ????????? ?????? ??????
      if (deadPlayerId != null && deadPlayerId.equals(protectedPlayerId)) {
        deadPlayerId = null;
      }

      try {
        gameSession = gameSessionService.findById(roomId);

        List<Player> players = playerRedisRepository.findByRoomId(roomId);
        Map<String, Player> playerMap = players.stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        Player deadPlayer = playerMap.get(deadPlayerId);

        Player suspectPlayer = playerMap.get(suspectPlayerId);

        List<String> victims = setNightToDay(gameSession, deadPlayerId, protectedPlayerId);

        // ?????? ?????? ??????
        if (gameSessionService.isDone(gameSession, players, victims)) {
          return;
        }

        log.info("Room {} start Day {} {} ", gameSession.getRoomId(), gameSession.getDay(),
            gameSession.getPhase());

        // ????????? ??????
        template.convertAndSend("/sub/" + roomId, GameStatusKillRes.of(gameSession, players, deadPlayer));

        // ????????? OBSERVER ??????
        if (deadPlayer != null) {
          template.convertAndSend("/sub/" + roomId + "/" + deadPlayerId, PlayerDeadRes.of());
        }

        // ????????? Role ??????
        if (suspectPlayer != null) {
          template.convertAndSend("/sub/" + roomId + "/" + GameRole.POLICE,
              SuspectVoteRes.of(suspectPlayer));
          template.convertAndSend("/sub/" + roomId + "/" + GameRole.OBSERVER,
              SuspectVoteRes.of(suspectPlayer));
        }
      } finally {
        lock.unlock();
      }

      // Timer??? ?????? ????????? ????????? ????????? ????????? ?????? ??????
      StartFinTimerTask task = new StartFinTimerTask(redisPublisher, topicStartFin);
      task.setRoomId(roomId);
      new Timer().schedule(task, TimeUtils.convertToDate(gameSession.getTimer()));
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private List<String> setNightToDay(GameSession gameSession, String deadPlayerId,
      String protectedPlayerId) {
    // ?????? ?????? ?????? ??? ?????? ??????
    gameSession.changePhase(GamePhase.NIGHT_TO_DAY, 7);
    List<String> victims = new ArrayList<>();
    List<Player> players = playerRedisRepository.findByRoomId(gameSession.getRoomId());
    for (Player player : players) {
      if (!player.isLeft() || player.getLeftPhaseCount() >= gameSession.getPhaseCount()) {
        continue;
      }
      player.setAlive(false);
      player.setLeftPhaseCount(null);
      playerRedisRepository.save(player);
      gameSession.eliminatePlayer(player);
      victims.add(player.getNickname());
    }

    Optional<Player> deadPlayerOptional = playerRedisRepository.findById(deadPlayerId);
    if (deadPlayerOptional.isPresent()) {
      Player deadplayer = deadPlayerOptional.get();
      if (deadplayer.isAlive()) {
        deadplayer.setAlive(false);
        playerRedisRepository.save(deadplayer);
        gameSession.eliminatePlayer(deadplayer);
        victims.add(deadplayer.getNickname());
      }
    }
    log.info("Room {} NightVote deadPlayer: {}", gameSession.getRoomId(), deadPlayerId);

    gameSessionService.update(gameSession);

    return victims;
  }
}

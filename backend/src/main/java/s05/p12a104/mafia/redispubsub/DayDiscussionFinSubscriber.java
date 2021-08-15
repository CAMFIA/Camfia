package s05.p12a104.mafia.redispubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import s05.p12a104.mafia.api.service.GameSessionVoteService;
import s05.p12a104.mafia.common.exception.RedissonLockNotAcquiredException;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GamePhase;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;
import s05.p12a104.mafia.redispubsub.message.DayDiscussionMessage;
import s05.p12a104.mafia.redispubsub.message.DayEliminationMessage;
import s05.p12a104.mafia.stomp.response.GameStatusRes;

@Slf4j
@RequiredArgsConstructor
@Service
public class DayDiscussionFinSubscriber {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate template;
  private final RedisPublisher redisPublisher;
  private final GameSessionService gameSessionService;
  private final PlayerRedisRepository playerRedisRepository;
  private final GameSessionVoteService gameSessionVoteService;
  private final ChannelTopic topicDayEliminationFin;
  private final RedissonClient redissonClient;
  private static final String KEY = "GameSession";

  public void sendMessage(String message) {
    try {
      DayDiscussionMessage dayDisscusionMessage =
          objectMapper.readValue(message, DayDiscussionMessage.class);
      String roomId = dayDisscusionMessage.getRoomId();

      RLock lock = redissonClient.getLock(KEY + roomId);
      boolean isLocked = false;
      try {
        isLocked = lock.tryLock(2, 3, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (!isLocked) {
        throw new RedissonLockNotAcquiredException("Lock을 얻을 수 없습니다 - Key : " + KEY + roomId);
      }

      GameSession gameSession = null;
      Map<String, String> alivePlayerMap = new HashMap<>();
      try {
        gameSession = gameSessionService.findById(roomId);
        List<String> suspiciousList = dayDisscusionMessage.getSuspiciousList();
        // DAY_TO_NIGHT으로
        if (suspiciousList.isEmpty()) {
          setDayToNight(roomId);
          return;
        }

        // DAY ELIMINATION으로
        List<String> victims = setDayElimination(gameSession, suspiciousList);

        List<Player> players = playerRedisRepository.findByRoomId(roomId);
        // 종료 여부 체크
        if (gameSessionService.isDone(gameSession, players, victims)) {
          return;
        }

        template.convertAndSend("/sub/" + roomId, GameStatusRes.of(gameSession, players));

        for (Player player : players) {
          if (player.isAlive()) {
            alivePlayerMap.put(player.getId(), null);
          }
        }

      } finally {
        lock.unlock();
      }

      gameSessionVoteService.startVote(roomId, gameSession.getPhase(), gameSession.getTimer(),
          alivePlayerMap);
      log.info("DAY_ELIMINATION 투표 생성! {}", roomId);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  private List<String> setDayElimination(GameSession gameSession, List<String> suspiciousList) {
    log.info("suspiciousList : " + suspiciousList.toString());

    List<String> victims = new ArrayList<>();
    gameSession.changePhase(GamePhase.DAY_ELIMINATION, 30 * suspiciousList.size());
    List<Player> players = playerRedisRepository.findByRoomId(gameSession.getRoomId());
    for (Player player : players) {
      if (!player.isLeft() || player.getLeftPhaseCount() >= gameSession.getPhaseCount()) {
        continue;
      }
      player.setAlive(false);
      playerRedisRepository.save(player);
      gameSession.eliminatePlayer(player);
      victims.add(player.getNickname());
    }

    // 의심자 체크
    players.forEach(player -> {
      if (suspiciousList.contains(player.getId())) {
        player.setSuspicious(true);
        playerRedisRepository.save(player);
      }
    });

    gameSessionService.update(gameSession);

    return victims;
  }

  private void setDayToNight(String roomId) {
    log.info("no suspiciousList - {}", roomId);
    DayEliminationMessage dayEliminationMessage = new DayEliminationMessage(roomId, null);
    redisPublisher.publish(topicDayEliminationFin, dayEliminationMessage);
  }

}

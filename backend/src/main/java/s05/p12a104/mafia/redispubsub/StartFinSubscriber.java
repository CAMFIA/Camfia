package s05.p12a104.mafia.redispubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import s05.p12a104.mafia.api.service.GameSessionService;
import s05.p12a104.mafia.api.service.GameSessionVoteService;
import s05.p12a104.mafia.common.exception.RedissonLockNotAcquiredException;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GamePhase;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;
import s05.p12a104.mafia.stomp.response.GameStatusRes;

@Slf4j
@RequiredArgsConstructor
@Service
public class StartFinSubscriber {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate template;
  private final GameSessionService gameSessionService;
  private final GameSessionVoteService gameSessionVoteService;
  private final PlayerRedisRepository playerRedisRepository;
  private final RedissonClient redissonClient;
  private static final String KEY = "GameSession";

  public void sendMessage(String redisMessageStr) {
    try {
      String roomId = objectMapper.readValue(redisMessageStr, String.class);

      RLock lock = redissonClient.getLock(KEY + roomId);
      boolean isLocked = false;
      try {
        isLocked = lock.tryLock(2, 200, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (!isLocked) {
        throw new RedissonLockNotAcquiredException("Lock을 얻을 수 없습니다 - Key : " + KEY + roomId);
      }

      try {
        GameSession gameSession = gameSessionService.findById(roomId);

        gameSession.changePhase(GamePhase.DAY_DISCUSSION, 100);
        gameSession.passADay();

        // 나간 사람 체크
        List<String> victims = new ArrayList<>();
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
        gameSessionService.update(gameSession);

        // 종료 여부 체크
        if (gameSessionService.isDone(gameSession, players, victims)) {
          return;
        }

        log.info("Start Day " + gameSession.getDay());

        template.convertAndSend("/sub/" + roomId, GameStatusRes.of(gameSession, players));

        Map<String, String> alivePlayerMap = new HashMap<>();
        for (Player player : players) {
          if (player.isAlive()) {
            alivePlayerMap.put(player.getId(), null);
          }
        }

        gameSessionVoteService.startVote(roomId, gameSession.getPhase(), gameSession.getTimer(),
            alivePlayerMap);
        log.info("DAY_DISCUSSION 투표 생성! - {}", roomId);

      } finally {
        lock.unlock();
      }

    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

}

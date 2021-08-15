package s05.p12a104.mafia.redispubsub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;
import s05.p12a104.mafia.stomp.response.GameStatusRes;

@Slf4j
@RequiredArgsConstructor
@Service
public class DayToNightFinSubscriber {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate template;
  private final GameSessionService gameSessionService;
  private final PlayerRedisRepository playerRedisRepository;
  private final GameSessionVoteService gameSessionVoteService;
  private final RedissonClient redissonClient;
  private static final String KEY = "GameSession";

  public void sendMessage(String redisMessageStr) {
    try {
      String roomId = objectMapper.readValue(redisMessageStr, String.class);

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
      Map<String, String> aliveNotCivilians = new HashMap<>();
      try {
        gameSession = gameSessionService.findById(roomId);
        // 나간 사람 체크 및 기본 세팅
        gameSession.changePhase(GamePhase.NIGHT_VOTE, 30);

        List<String> victims = new ArrayList<>();
        List<Player> players = playerRedisRepository.findByRoomId(roomId);
        for (Player player : players) {
          if (!player.isLeft() || player.getLeftPhaseCount() >= gameSession.getPhaseCount()) {
            continue;
          }
          player.setAlive(false);
          playerRedisRepository.save(player);
          gameSession.eliminatePlayer(player);
          victims.add(player.getNickname());
        }

        gameSession.setAliveNotCivilian((int) players.stream()
            .filter(e -> e.getRole() != GameRole.CIVILIAN)
            .filter(Player::isAlive).count());

        gameSessionService.update(gameSession);

        // 종료 여부 체크
        if (gameSessionService.isDone(gameSession, victims)) {
          return;
        }

        template.convertAndSend("/sub/" + roomId, GameStatusRes.of(gameSession, players));

        players.forEach(player -> {
          if (player.isAlive() && player.getRole() != GameRole.CIVILIAN) {
            aliveNotCivilians.put(player.getId(), null);
          }
        });

      } finally {
        lock.unlock();
      }

      gameSessionVoteService.startVote(roomId, gameSession.getPhase(), gameSession.getTimer(),
          aliveNotCivilians);
      log.info("NIGHT_VOTE 투표 생성! - {}", roomId);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

}

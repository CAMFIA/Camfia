package s05.p12a104.mafia.stomp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import s05.p12a104.mafia.api.service.GameSessionService;
import s05.p12a104.mafia.common.util.TimeUtils;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GamePhase;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;
import s05.p12a104.mafia.domain.repository.VoteRepository;
import s05.p12a104.mafia.redispubsub.RedisPublisher;
import s05.p12a104.mafia.redispubsub.message.DayDiscussionMessage;
import s05.p12a104.mafia.redispubsub.message.DayEliminationMessage;
import s05.p12a104.mafia.redispubsub.message.NightVoteMessage;
import s05.p12a104.mafia.stomp.request.GameSessionVoteReq;
import s05.p12a104.mafia.stomp.task.VoteFinTimerTask;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameSessionVoteServiceImpl implements GameSessionVoteService {

  private final RedisPublisher redisPublisher;
  private final VoteRepository voteRepository;

  private final GameSessionService gameSessionService;
  private final PlayerRedisRepository playerRedisRepository;
  private final ChannelTopic topicDayDiscussionFin;
  private final ChannelTopic topicDayEliminationFin;
  private final ChannelTopic topicNightVoteFin;

  @Override
  public void startVote(String roomId, int phaseCount, GamePhase phase, LocalDateTime time,
      Map<String, GameRole> players) {
    log.info("Room {} start Vote for {}", roomId, phase);
    voteRepository.startVote(roomId, phaseCount, phase, players);
    Timer timer = new Timer();
    VoteFinTimerTask task = new VoteFinTimerTask(this);
    task.setRoomId(roomId);
    task.setPhaseCount(phaseCount);
    task.setPhase(phase);
    timer.schedule(task, TimeUtils.convertToDate(time));
  }


  @Override
  public void endVote(String roomId, int phaseCount, GamePhase phase) {
    if (voteRepository.isEnd(roomId, phaseCount)) {
      return;
    } else {
      Map<String, String> vote = voteRepository.getVoteResult(roomId);
      log.info("Room {} end Vote for {}", roomId, phase);
      voteRepository.endVote(roomId, phase);
      publishRedis(roomId, vote);
    }
  }

  @Override
  public Map<String, String> vote(String roomId, String playerId, GameSessionVoteReq req) {

    if (!voteRepository.isValid(playerId, req.getPhase())) {
      return null;
    }
    log.info("Room {} Player {} Voted At {}", roomId, playerId, req.getPhase());
    return voteRepository.vote(roomId, playerId, req.getVote());
  }

  @Override
  public Map<String, String> nightVote(String roomId, String playerId, GameSessionVoteReq req,
      GameRole roleName) {

    if (!voteRepository.isValid(playerId, req.getPhase())) {
      return null;
    }

    return voteRepository.nightVote(roomId, playerId, req.getVote(), roleName);
  }

  @Override
  public Map<String, Boolean> confirmVote(String roomId, String playerId, GameSessionVoteReq req) {

    if (!voteRepository.isValid(playerId, req.getPhase())) {
      return new HashMap<String, Boolean>();
    }

    return voteRepository.confirmVote(roomId, playerId);
  }

  @Override
  public Map<String, Boolean> getNightConfirm(String roomId, String playerId,
      GameSessionVoteReq req, GameRole roleName) {

    if (!voteRepository.isValid(playerId, req.getPhase())) {
      return new HashMap<String, Boolean>();
    }

    return voteRepository.getNightConfirm(roomId, roleName);
  }

  @Override
  public Map<String, Boolean> getConfirm(String roomId, String playerId) {
    GameSession gameSession = gameSessionService.findById(roomId);
    Player player = playerRedisRepository.findById(playerId).get();

    if (voteRepository.isEnd(roomId, gameSession.getPhaseCount())) {
      return new HashMap<String, Boolean>();
    }

    if (gameSession.getPhase() != GamePhase.NIGHT_VOTE || !player.isAlive()) {
      return voteRepository.getConfirm(roomId, playerId);
    } else {
      return voteRepository.getNightConfirm(roomId, player.getRole());
    }
  }

  @Override
  public Map<String, String> getVoteResult(String roomId, GameSessionVoteReq req) {
    return voteRepository.getVoteResult(roomId);
  }

  private void publishRedis(String roomId, Map<String, String> vote) {
    GameSession gameSession = gameSessionService.findById(roomId);

    if (gameSession.getPhase() == GamePhase.DAY_DISCUSSION) {
      DayDiscussionMessage dayDiscussionMessage =
          new DayDiscussionMessage(roomId, getSuspiciousList(gameSession, vote));
      redisPublisher.publish(topicDayDiscussionFin, dayDiscussionMessage);
    } else if (gameSession.getPhase() == GamePhase.DAY_ELIMINATION) {
      DayEliminationMessage dayEliminationMessage =
          new DayEliminationMessage(roomId, getEliminationPlayer(gameSession, vote));
      redisPublisher.publish(topicDayEliminationFin, dayEliminationMessage);
    } else if (gameSession.getPhase() == GamePhase.NIGHT_VOTE) {
      NightVoteMessage nightVoteMessage =
          new NightVoteMessage(roomId, getNightVoteResult(gameSession, vote));
      redisPublisher.publish(topicNightVoteFin, nightVoteMessage);
    }
  }

  private List<String> getSuspiciousList(GameSession gameSession, Map<String, String> voteResult) {
    List<String> suspiciousList = new ArrayList<>();

    Map<String, Integer> voteNum = new HashMap<String, Integer>();
    int voteCnt = 0;
    for (String vote : voteResult.values()) {
      if (vote == null) {
        continue;
      }

      voteCnt++;
      voteNum.put(vote, voteNum.getOrDefault(vote, 0) + 1);
    }

    // 의심자 찾기
    int alivePlayer = gameSession.getAlivePlayer();
    if (voteCnt > (alivePlayer - 1) / 2) {
      List<String> suspects = new ArrayList<>(voteNum.keySet());
      // 투표수 오름차순
      Collections.sort(suspects, (o1, o2) -> voteNum.get(o2).compareTo(voteNum.get(o1)));

      List<Player> players = playerRedisRepository.findByRoomId(gameSession.getRoomId());
      Map<String, Player> playerMap = players.stream()
          .collect(Collectors.toMap(Player::getId, p -> p));

      int voteMax = voteNum.get(suspects.get(0));
      for (String suspect : suspects) {
        // 동점자가 아니면 더이상 동점자가 없기때문에 끝내기
        if (voteNum.get(suspect) != voteMax) {
          break;
        }

        // 중간 나간 사람이 포함되어 있을 수 있으므로 살아있는지 체크
        if (playerMap.get(suspect).isAlive()) {
          suspiciousList.add(suspect);
        }
      }

      // 살아있는 사람 기준으로 6명이상이면 3명까지 5이하면 2명까지
      if (suspiciousList.size() > 3 || (alivePlayer <= 5 && suspiciousList.size() > 2)) {
        suspiciousList.clear();
      }
    }

    return suspiciousList;
  }

  private String getEliminationPlayer(GameSession gameSession, Map<String, String> voteResult) {
    String deadPlayerId = null;
    Map<String, Integer> voteNum = new HashMap<>();
    int voteCnt = 0;
    for (String vote : voteResult.values()) {
      if (vote == null) {
        continue;
      }

      voteCnt++;
      voteNum.put(vote, voteNum.getOrDefault(vote, 0) + 1);
    }

    int alivePlayer = gameSession.getAlivePlayer();
    if (voteCnt > (alivePlayer - 1) / 2) {

      // 최다 득표 수 구하기
      Integer max = voteNum.entrySet().stream()
          .max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getValue();

      // 최다 득표한 Player List
      List<String> deadList = voteNum.entrySet().stream().filter(entry -> entry.getValue() == max)
          .map(Map.Entry::getKey).collect(Collectors.toList());

      // 한명일 경우
      if (deadList.size() == 1) {
        deadPlayerId = deadList.get(0);
      }
    }
    return deadPlayerId;
  }

  private Map<GameRole, String> getNightVoteResult(GameSession gameSession,
      Map<String, String> voteResult) {

    List<Player> players = playerRedisRepository.findByRoomId(gameSession.getRoomId());
    Map<String, Player> playerMap = players.stream()
        .collect(Collectors.toMap(Player::getId, p -> p));

    // 마피아가 아닌 직업들 결과에 담기
    Map<GameRole, String> result = voteResult.entrySet().stream()
        .filter(e -> playerMap.get(e.getKey()).getRole() != GameRole.MAFIA)
        .filter(e -> e.getValue() != null)
        .collect(Collectors.toMap(e -> playerMap.get(e.getKey()).getRole(), e -> e.getValue()));

    // 마피아들의 투표만 추려서 Map<투표 받은 사람,List<투표한사람>>으로 저장
    Map<String, List<String>> mafiaVote =
        voteResult.keySet().stream().filter(key -> playerMap.get(key).getRole() == GameRole.MAFIA)
            .filter(key -> voteResult.get(key) != null)
            .collect(Collectors.groupingBy(key -> voteResult.get(key)));

    // 마피아가 투표를 했을 경우
    if (mafiaVote.size() > 0) {

      // 최다 득표 수 구하기
      int max = mafiaVote.entrySet().stream()
          .max((entry1, entry2) -> entry1.getValue().size() > entry2.getValue().size() ? 1 : -1)
          .get().getValue().size();

      // 최다 득표한 Player List
      List<String> deadList =
          mafiaVote.entrySet().stream().filter(entry -> entry.getValue().size() == max)
              .map(Map.Entry::getKey).collect(Collectors.toList());

      // 한명일 경우
      if (deadList.size() == 1) {
        result.put(GameRole.MAFIA, deadList.get(0));
      }
    }

    return result;
  }
}

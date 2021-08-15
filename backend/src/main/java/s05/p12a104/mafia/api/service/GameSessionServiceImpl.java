package s05.p12a104.mafia.api.service;

import io.openvidu.java.client.ConnectionProperties;
import io.openvidu.java.client.ConnectionType;
import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.OpenViduHttpException;
import io.openvidu.java.client.OpenViduJavaClientException;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.java.client.Session;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import s05.p12a104.mafia.api.requset.GameSessionPostReq;
import s05.p12a104.mafia.api.response.GameSessionJoinRes;
import s05.p12a104.mafia.api.response.PlayerJoinRoomState;
import s05.p12a104.mafia.common.exception.AlreadyGameStartedException;
import s05.p12a104.mafia.common.exception.GameSessionNotFoundException;
import s05.p12a104.mafia.common.exception.OpenViduRuntimeException;
import s05.p12a104.mafia.common.exception.OpenViduSessionNotFoundException;
import s05.p12a104.mafia.common.exception.OverMaxIndividualRoomCountException;
import s05.p12a104.mafia.common.exception.OverMaxPlayerCountException;
import s05.p12a104.mafia.common.exception.OverMaxTotalRoomCountException;
import s05.p12a104.mafia.common.exception.PlayerNotFoundException;
import s05.p12a104.mafia.common.exception.RedissonLockNotAcquiredException;
import s05.p12a104.mafia.common.util.RoleUtils;
import s05.p12a104.mafia.common.util.RoomIdUtils;
import s05.p12a104.mafia.common.util.TimeUtils;
import s05.p12a104.mafia.common.util.UrlUtils;
import s05.p12a104.mafia.domain.dao.GameSessionDao;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.entity.User;
import s05.p12a104.mafia.domain.enums.Color;
import s05.p12a104.mafia.domain.enums.GamePhase;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.enums.GameState;
import s05.p12a104.mafia.domain.mapper.GameSessionDaoMapper;
import s05.p12a104.mafia.domain.repository.GameSessionRedisRepository;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;
import s05.p12a104.mafia.redispubsub.RedisPublisher;
import s05.p12a104.mafia.redispubsub.message.EndMessgae;
import s05.p12a104.mafia.stomp.response.GameResult;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameSessionServiceImpl implements GameSessionService {

  private final GameSessionRedisRepository gameSessionRedisRepository;

  private final PlayerRedisRepository playerRedisRepository;

  private final RedisKeyValueTemplate redisKVTemplate;

  private final RedisTemplate<String, Object> objRedisTemplate;

  private final RedisPublisher redisPublisher;

  private final RedissonClient redissonClient;

  private final ChannelTopic topicEnd;

  private final OpenVidu openVidu;

  private static final int MAX_TOTAL_ROOM_COUNT = 200;
  private static final int MAX_INDIVIDUAL_ROOM_COUNT = 2;
  private static final int MAX_PLAYER_COUNT = 10;
  private static final String KEY = "GameSession";

  @Override
  public GameSession makeGame(User user, GameSessionPostReq typeInfo)
      throws OpenViduJavaClientException, OpenViduHttpException {

    if (gameSessionRedisRepository.count() >= MAX_TOTAL_ROOM_COUNT) {
      throw new OverMaxTotalRoomCountException();
    }

    if (gameSessionRedisRepository.findByCreatorEmail(user.getEmail())
        .size() >= MAX_INDIVIDUAL_ROOM_COUNT) {
      throw new OverMaxIndividualRoomCountException();
    }

    Session newSession = openVidu.createSession();
    String newRoomId =
        RoomIdUtils.getIdPrefix(typeInfo.getAccessType()) + newSession.getSessionId().split("_")[1];

    LocalDateTime createdTime = LocalDateTime.now();
    GameSession newGameSession = GameSession.builder(newRoomId, user.getEmail(),
        typeInfo.getAccessType(), typeInfo.getRoomType(), createdTime, newSession)
        .finishedTime(createdTime).build();

    GameSessionDao newDao = GameSessionDaoMapper.INSTANCE.toDao(newGameSession);
    return GameSession.of(gameSessionRedisRepository.save(newDao), openVidu);
  }

  @Override
  public GameSessionJoinRes getPlayerJoinableState(String roomId, String playerId) {

    List<Player> players = playerRedisRepository.findByRoomId(roomId);
    Set<String> playerIds = players.stream().map(Player::getId).collect(Collectors.toSet());
    if (playerId == null || !playerIds.contains(playerId)) {

      GameState gameState = GameState.valueOf(
          objRedisTemplate.opsForHash().get("GameSession:" + roomId, "state").toString());
      validateToBePossibleToJoin(gameState, players);

      return new GameSessionJoinRes(PlayerJoinRoomState.JOINABLE, null, null);
    }

    Player player = playerRedisRepository.findById(playerId)
        .orElseThrow(PlayerNotFoundException::new);
    if (!player.getRoomId().equals(roomId)) {
      throw new PlayerNotFoundException();
    }

    String sessionId = objRedisTemplate.opsForHash()
        .get("GameSession:" + roomId, "sessionId").toString();
    String token = createOpenViduToken(sessionId, player.getNickname());
    objRedisTemplate.opsForHash().put("Player:" + playerId, "token", token);

    return new GameSessionJoinRes(PlayerJoinRoomState.REJOIN, token, playerId);
  }

  @Override
  public void deleteById(String roomId) {
    gameSessionRedisRepository.deleteById(roomId);
    log.info("Room {} removed and closed", roomId);
  }

  @Override
  public GameSession findById(String roomId) {
    GameSessionDao gameSessionDao =
        gameSessionRedisRepository.findById(roomId).orElseThrow(GameSessionNotFoundException::new);

    return GameSession.of(gameSessionDao, openVidu);
  }

  @Override
  public void update(GameSession update) {
    GameSessionDao updateDao = GameSessionDaoMapper.INSTANCE.toDao(update);
    redisKVTemplate.update(updateDao);
    log.info("Room {} updated", update.getRoomId());
  }

  @Override
  public GameSessionJoinRes addUser(String roomId, String nickname) {

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

    try {
      GameState gameState = GameState.valueOf(
          objRedisTemplate.opsForHash().get("GameSession:" + roomId, "state").toString());
      List<Player> players = playerRedisRepository.findByRoomId(roomId);
      validateToBePossibleToJoin(gameState, players);

      String sessionId = objRedisTemplate.opsForHash()
          .get("GameSession:" + roomId, "sessionId").toString();
      String token = createOpenViduToken(sessionId, nickname);

      // ex> tok_A1c0pNsLJFwVJTeb
      String playerId = UrlUtils.getUrlQueryParam(token, "token")
          .orElseThrow(OpenViduRuntimeException::new).substring(4);

      Player newPlayer =
          Player.builder(playerId, roomId, nickname, getNewColor(players)).token(token).build();

      playerRedisRepository.save(newPlayer);
      if (players.size() == 0) {
        objRedisTemplate.opsForHash().put("GameSession:" + roomId, "hostId", playerId);
      }

      return new GameSessionJoinRes(PlayerJoinRoomState.JOIN, token, playerId);

    } finally {
      lock.unlock();
    }
  }

  @Override
  public void removeUser(String roomId, String playerId) {

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

    try {

      GameState gameState = GameState.valueOf(objRedisTemplate.opsForHash()
          .get("GameSession:" + roomId, "state").toString());
      if (gameState == GameState.STARTED) {
        removePlayerInGame(roomId, playerId);
      } else {
        removeReadyPlayer(roomId, playerId);
      }

    } finally {
      lock.unlock();
    }
  }

  /**
   * 게임 진행 중에 나간 Player 제거.
   *
   * @param roomId : 나간 방의 id
   * @param delPlayerId : 나간 player id
   */
  private void removePlayerInGame(String roomId, String delPlayerId) {
    Player delPlayer = playerRedisRepository.findById(delPlayerId)
        .orElseThrow(PlayerNotFoundException::new);

    if (!delPlayer.isAlive()) {
      return;
    }

    int curPhaseCount = Integer.parseInt(objRedisTemplate.opsForHash()
        .get("GameSession:" + roomId, "phaseCount").toString());
    delPlayer.setLeftPhaseCount(curPhaseCount);
    delPlayer.setLeft(true);
    playerRedisRepository.save(delPlayer);

    final int TIME_TO_DIE = 30; // 30초
    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
      @Override
      public void run() {
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

        try {
          Player delPlayer = playerRedisRepository.findById(delPlayerId)
              .orElseThrow(PlayerNotFoundException::new);
          if (!delPlayer.isLeft()) {
            return;
          }
          GameState gameState = GameState.valueOf(objRedisTemplate.opsForHash()
              .get("GameSession:" + roomId, "state").toString());
          if (gameState != GameState.STARTED) {
            return;
          }

          playerRedisRepository.deleteById(delPlayerId);

        } finally {
          lock.unlock();
        }
      }
    };
    timer.schedule(timerTask, TIME_TO_DIE * 1000);
  }

  /**
   * 아직 게임이 시작하지 않은 방에서 나간 Player 제거.
   *
   * @param roomId   : Player가 나간 room id
   * @param playerId : 나간 player id
   */
  private void removeReadyPlayer(String roomId, String playerId) {
    playerRedisRepository.deleteById(playerId);

    List<Player> players = playerRedisRepository.findByRoomId(roomId);
    if (players.size() <= 0) {
      gameSessionRedisRepository.deleteById(roomId);
      return;
    }

    objRedisTemplate.opsForHash()
        .put("GameSession:" + roomId, "hostId", players.get(0).getId());
  }

  private Color getNewColor(List<Player> players) {
    Set<Color> usedColors = new HashSet<>();
    for (Player player : players) {
      usedColors.add(player.getColor());
    }

    for (int i = 0; i < MAX_PLAYER_COUNT; i++) {
      Color newColor = Color.randomColor();
      if (!usedColors.contains(newColor)) {
        return newColor;
      }
    }
    return Color.RED;
  }

  @Override
  public void startGame(String roomId) {
    GameSession gameSession = findById(roomId);

    // player 초기화
    List<Player> players = playerRedisRepository.findByRoomId(roomId);
    for (Player player : players) {
      player.setAlive(true);
      player.setSuspicious(false);
      playerRedisRepository.save(player);
    }

    // 각 역할에 맞는 인원수 구하기
    Map<GameRole, Integer> roleNum = RoleUtils.getRoleNum(gameSession, players.size());

    // game 초기 setting
    gameSession.setState(GameState.STARTED);
    gameSession.setDay(0);
    gameSession.setAliveMafia(roleNum.get(GameRole.MAFIA));
    gameSession.setPhase(GamePhase.START);
    gameSession.setTimer(TimeUtils.getFinTime(15));

    // alive player 초기화
    gameSession.setAlivePlayer(players.size());

    // 역할 부여
    gameSession.setMafias(RoleUtils.assignRole(roleNum, players));
    for (Player player : players) {
      playerRedisRepository.save(player);
    }

    // alive Not Civilian 초기화
    int notCivilianCnt = (int) players.stream()
        .filter(e -> e.getRole() != GameRole.CIVILIAN)
        .count();

    gameSession.setAliveNotCivilian(notCivilianCnt);

    update(gameSession);
  }

  @Override
  public boolean isDone(GameSession gameSession, List<String> victims) {
    GameResult gameResult = GameResult.of(gameSession, victims);
    if (gameResult.getWinner() == null) {
      return false;
    }

    redisPublisher.publish(topicEnd, new EndMessgae(gameSession.getRoomId(), gameResult));
    return true;
  }

  @Override
  public void endGame(String roomId) {
    GameSession gameSession = findById(roomId);
    gameSession.setState(GameState.READY);
    gameSession.setTimer(TimeUtils.getFinTime(0));
    gameSession.setDay(0);
    gameSession.setAliveMafia(0);

    List<Player> players = playerRedisRepository.findByRoomId(roomId);
    for (Player player : players) {
      if (player.isLeft()) {
        removeReadyPlayer(roomId, player.getId());
      }
    }

    update(gameSession);
  }

  @Override
  public Map<String, GameRole> getAllPlayerRole(String roomId, String playerId) {
    return playerRedisRepository.findByRoomId(roomId).stream()
        .filter(e -> e.getRole() != null)
        .collect(Collectors.toMap(Player::getId, Player::getRole));
  }

  private Session getSession(String sessionId) {
    for (Session session : openVidu.getActiveSessions()) {
      if (session.getSessionId().equals(sessionId)) {
        return session;
      }
    }
    throw new OpenViduSessionNotFoundException();
  }

  private String createOpenViduToken(String sessionId, String nickname) {
    String serverData = "{\"serverData\": \"" + nickname + "\"}";

    // Build connectionProperties object with the serverData and the role
    ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
        .type(ConnectionType.WEBRTC).data(serverData).role(OpenViduRole.PUBLISHER).build();
    try {
      // ex> wss://localhost:4443?sessionId=ses_Ogize1yQIj&token=tok_A1c0pNsLJFwVJTeb
      return getSession(sessionId).createConnection(connectionProperties).getToken();
    } catch (OpenViduJavaClientException e1) {
      // If internal error generate an error message and return it to client
      throw new OpenViduRuntimeException(e1.getMessage());
    } catch (OpenViduHttpException e2) {
      if (404 == e2.getStatus()) {
        //
      }
      throw new OpenViduRuntimeException(e2.getMessage());
    }
  }

  private void validateToBePossibleToJoin(GameState gameState, List<Player> players) {
    if (gameState == GameState.STARTED) {
      throw new AlreadyGameStartedException();
    }

    if (players.size() >= MAX_PLAYER_COUNT) {
      throw new OverMaxPlayerCountException();
    }
  }

//  private void validateToBePossibleToJoin(GameSession gameSession) {
//    if (gameSession.getState() == GameState.STARTED) {
//      throw new AlreadyGameStartedException();
//      List<Player> players = playerRedisRepository.findByRoomId(roomId);
//      if (players.size() <= 0) {
//        gameSessionRedisRepository.deleteById(roomId);
//        return;
//      }
//
//      if (gameSession.getPlayerMap().size() >= MAX_PLAYER_COUNT) {
//        throw new OverMaxPlayerCountException();
//      }
//      objRedisTemplate.opsForHash()
//          .put("GameSession:" + roomId, "hostId", players.get(0).getId());
//    }
//  }
}

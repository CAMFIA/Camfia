package s05.p12a104.mafia;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.HashMap;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import io.openvidu.java.client.OpenVidu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import s05.p12a104.mafia.common.exception.GameSessionNotFoundException;
import s05.p12a104.mafia.domain.dao.GameSessionDao;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.AccessType;
import s05.p12a104.mafia.domain.enums.Color;
import s05.p12a104.mafia.domain.enums.GameState;
import s05.p12a104.mafia.domain.enums.RoomType;
import s05.p12a104.mafia.domain.repository.GameSessionRedisRepository;
import s05.p12a104.mafia.domain.repository.PlayerRedisRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("default")
public class DummyGameSessionCreationRunner implements ApplicationRunner {

  private final GameSessionRedisRepository gameSessionRedisRepository;
  private final PlayerRedisRepository playerRedisRepository;
  private final OpenVidu openVidu;
  private final RedisTemplate<String, Object> objRedisTemplate;
  private final StringRedisTemplate stringRedisTemplate;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    LocalDateTime createdTime = LocalDateTime.now();
    String roomId = "V1234";
    GameSessionDao newGameSessionDao = GameSessionDao.builder()
        .roomId(roomId).accessType(AccessType.PRIVATE).roomType(RoomType.BASIC)
        .creatorEmail("dummy@dummy.com").createdTime(createdTime)
        .finishedTime(createdTime)
        .state(GameState.READY)
        .sessionId(openVidu.createSession().getSessionId())
        .build();

    GameSessionDao saved = gameSessionRedisRepository.save(newGameSessionDao);


    stringRedisTemplate.opsForHash().put("GameSession:" + roomId, "hostId", "hihi");

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.registerModules(new JavaTimeModule(), new Jdk8Module());

//    redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
//    redisTemplate.setHashValueSerializer(new StringRedisSerializer());
    objRedisTemplate.opsForHash().put("GameSession:" + roomId, "hostId", "hihi");

    Object hostId = objRedisTemplate.opsForHash().get("GameSession:" + roomId, "hostId");

    GameState gameState = GameState.valueOf(
        objRedisTemplate.opsForHash().get("GameSession:" + roomId, "state").toString());

//    Object aa = ops.get("hostId");



//    HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
//    ops.get("GameSession:" + roomId, "hostId");
//    ops.
//    ObjectMapper mapper = new ObjectMapper();
//    String www = "www.sample.pl";
//    Weather weather = mapper.readValue(www, Weather.class);
//    Object hostId = redisTemplate.opsForHash().getOperations().boundHashOps()


    GameSessionDao found = gameSessionRedisRepository.findById(roomId).orElseThrow(GameSessionNotFoundException::new);

    Player newPlayer =
        Player.builder("dummyId", roomId, "nick", Color.RED).token("tokentoken").build();
    playerRedisRepository.save(newPlayer);



    log.info("Dummy Gamesession Created - roomId : {}", saved.getRoomId());
  }
}

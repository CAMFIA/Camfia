package s05.p12a104.mafia.domain.entity;

import io.openvidu.java.client.OpenVidu;
import io.openvidu.java.client.Session;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import s05.p12a104.mafia.common.exception.OpenViduSessionNotFoundException;
import s05.p12a104.mafia.common.util.TimeUtils;
import s05.p12a104.mafia.domain.dao.GameSessionDao;
import s05.p12a104.mafia.domain.enums.AccessType;
import s05.p12a104.mafia.domain.enums.GamePhase;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.enums.GameState;
import s05.p12a104.mafia.domain.enums.RoomType;

@Setter
@Getter
@Builder
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameSession {

  @NonNull
  private final String roomId;

  private int day;

  private int phaseCount;

  private boolean isNight;

  private int aliveMafia;

  private int alivePlayer;

  private int aliveNotCivilian;

  private LocalDateTime timer;

  private List<String> mafias;

  @NonNull
  private final String creatorEmail;

  @NonNull
  @Enumerated(EnumType.STRING)
  private final AccessType accessType;

  @NonNull
  @Enumerated(EnumType.STRING)
  private RoomType roomType;

  @NonNull
  @Enumerated(EnumType.STRING)
  private GameState state;

  @Enumerated(EnumType.STRING)
  private GamePhase phase;

  @NonNull
  private final LocalDateTime createdTime;

  @NonNull
  private LocalDateTime finishedTime;

  private String lastEnter;

  @NonNull
  private final Session session;

  private String hostId;

  public static GameSessionBuilder builder(String roomId, String creatorEmail,
      AccessType accessType, RoomType roomType, LocalDateTime createdTime, Session session) {
    return new GameSessionBuilder().roomId(roomId).creatorEmail(creatorEmail).accessType(accessType)
        .roomType(roomType).createdTime(createdTime).session(session).state(GameState.READY);
  }

  public void eliminatePlayer(Player player) {
    alivePlayer--;
    if (player.getRole() == GameRole.MAFIA) {
      aliveMafia--;
    }
  }

  public void changePhase(GamePhase phase, int timer) {
    this.phase = phase;
    this.phaseCount++;
    setTimer(TimeUtils.getFinTime(timer));
  }

  public void passADay() {
    this.day++;
  }

  public static GameSession of(GameSessionDao dao, OpenVidu openVidu) {
    Session entitySession = null;
    for (Session session : openVidu.getActiveSessions()) {
      if (session.getSessionId().equals(dao.getSessionId())) {
        entitySession = session;
        break;
      }
    }
    if (entitySession == null) {
      throw new OpenViduSessionNotFoundException();
    }


    List<String> mafias = dao.getMafias();
    if (mafias == null) {
      mafias = new ArrayList<>();
    }

    GameSession gameSession = GameSession
        .builder(dao.getRoomId(), dao.getCreatorEmail(), dao.getAccessType(), dao.getRoomType(),
            dao.getCreatedTime(), entitySession)
        .finishedTime(dao.getFinishedTime()).day(dao.getDay()).isNight(dao.isNight())
        .aliveMafia(dao.getAliveMafia()).timer(dao.getTimer()).phase(dao.getPhase())
        .phaseCount(dao.getPhaseCount()).lastEnter(dao.getLastEnter()).state(dao.getState())
        .mafias(mafias).alivePlayer(dao.getAlivePlayer()).hostId(dao.getHostId())
        .aliveNotCivilian(dao.getAliveNotCivilian()).build();

    return gameSession;
  }
}

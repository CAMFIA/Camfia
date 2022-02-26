package s05.p12a104.mafia.domain.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import s05.p12a104.mafia.domain.enums.Color;
import s05.p12a104.mafia.domain.enums.GameRole;

@Slf4j
@Getter
@Setter
@RedisHash("Player")
@Builder
public class Player implements Serializable {

  @Id
  private final String id;

  @Indexed
  private final String roomId;

  private final String nickname;


  private GameRole role;

  private boolean alive;

  private final Color color;

  private String token;

  private boolean suspicious;

  // Player가 게임 진행 중 나간 시점의 phaseCount (phase 변환 시 해당 값을 확인하여 사망 처리하기 위한 목적)
  private Integer leftPhaseCount;

  private boolean isLeft;

  public static PlayerBuilder builder(String id, String roomId, String nickname, Color color) {
    return new PlayerBuilder()
        .id(id)
        .roomId(roomId)
        .nickname(nickname)
        .color(color);
  }

}

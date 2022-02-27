package s05.p12a104.mafia.stomp.response;

import lombok.Getter;
import s05.p12a104.mafia.domain.enums.StompMessageType;

@Getter
public class GameSessionDeleteRes {
  final private StompMessageType type = StompMessageType.DELETE;
}

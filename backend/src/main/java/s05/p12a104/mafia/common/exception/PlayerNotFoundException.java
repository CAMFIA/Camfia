package s05.p12a104.mafia.common.exception;

public class PlayerNotFoundException extends RuntimeException {
  public PlayerNotFoundException() {
    super("플레이어 정보를 찾을 수 없습니다");
  }
}
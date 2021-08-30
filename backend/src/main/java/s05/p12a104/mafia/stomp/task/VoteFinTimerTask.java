package s05.p12a104.mafia.stomp.task;

import java.util.TimerTask;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import s05.p12a104.mafia.domain.enums.GamePhase;
import s05.p12a104.mafia.stomp.service.GameSessionVoteService;

@RequiredArgsConstructor
@Service
@Setter
public class VoteFinTimerTask extends TimerTask {

  private final GameSessionVoteService gameSessionVoteService;
  private String roomId;
  private GamePhase phase;
  private int phaseCount;

  @Override
  public void run() {
    gameSessionVoteService.endVote(roomId, phaseCount, phase);
  }

}

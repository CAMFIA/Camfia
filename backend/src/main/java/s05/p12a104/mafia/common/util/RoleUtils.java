package s05.p12a104.mafia.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import s05.p12a104.mafia.domain.entity.GameSession;
import s05.p12a104.mafia.domain.entity.Player;
import s05.p12a104.mafia.domain.enums.GameRole;
import s05.p12a104.mafia.domain.enums.RoomType;

public class RoleUtils {

  public static Map<GameRole, Integer> getRoleNum(GameSession gameSession, int playerCount) {
    Map<GameRole, Integer> roleNum = new HashMap<>();

    RoomType roomType = gameSession.getRoomType();
    int num = playerCount;

    int mafia = 0;
    int doctor = 0;
    int police = 0;
    if (roomType == RoomType.BASIC) {
      mafia = num / 3;
      doctor = num > 4 ? 1 : 0;
      police = num > 5 ? 1 : 0;
    }
    // } else {
    // mafia = gameConfigReq.getMafia();
    // doctor = gameConfigReq.getDoctor();
    // police = gameConfigReq.getPolice();

    roleNum.put(GameRole.CIVILIAN, num - (mafia + doctor + police));
    roleNum.put(GameRole.MAFIA, mafia);
    roleNum.put(GameRole.DOCTOR, doctor);
    roleNum.put(GameRole.POLICE, police);

    return roleNum;

  }

  public static List<String> assignRole(Map<GameRole, Integer> roleNum, List<Player> players) {
    List<String> mafias = new ArrayList<>();

    int playerCount = players.size();
    boolean[] isRole = new boolean[playerCount];
    Random random = new Random();
    roleNum.forEach((role, num) -> {
      while (num-- > 0) {
        int idx = -1;
        do {
          idx = random.nextInt(playerCount * 100) % playerCount;
        } while (isRole[idx]);

        Player player = players.get(idx);
        player.setRole(role);
        isRole[idx] = true;

        if (role == GameRole.MAFIA) {
          mafias.add(player.getId());
        }
      }
    });

    return mafias;
  }
}

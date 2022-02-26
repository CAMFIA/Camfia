package s05.p12a104.mafia.domain.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import s05.p12a104.mafia.domain.entity.Player;

@Repository
public interface PlayerRedisRepository extends CrudRepository<Player, String> {

  List<Player> findByRoomId(String roomId);

  @Override
  List<Player> findAll();

}

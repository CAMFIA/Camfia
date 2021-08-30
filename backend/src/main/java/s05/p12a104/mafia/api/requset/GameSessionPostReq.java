package s05.p12a104.mafia.api.requset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import s05.p12a104.mafia.domain.enums.AccessType;
import s05.p12a104.mafia.domain.enums.RoomType;

@Getter
@Setter
@ToString
@ApiModel
public class GameSessionPostReq {
  @ApiModelProperty(name = "Room Type", example = "basic")
  RoomType roomType;

  @ApiModelProperty(name = "Access Type", example = "private")
  AccessType accessType;
}

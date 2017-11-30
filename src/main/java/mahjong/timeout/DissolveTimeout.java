package mahjong.timeout;

import com.alibaba.fastjson.JSON;
import mahjong.constant.Constant;
import mahjong.entrance.MahjongTcpService;
import mahjong.mode.GameBase;
import mahjong.mode.Room;
import mahjong.mode.Seat;
import mahjong.redis.RedisService;

/**
 * Created by pengyi
 * Date : 17-8-31.
 * desc:
 */
public class DissolveTimeout extends Thread {

    private Integer roomNo;
    private RedisService redisService;
    private GameBase.BaseConnection.Builder response;

    public DissolveTimeout(Integer roomNo, RedisService redisService) {
        this.roomNo = roomNo;
        this.redisService = redisService;
        this.response = GameBase.BaseConnection.newBuilder();
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait(Constant.dissolve);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (redisService.exists("room" + roomNo)) {
            while (!redisService.lock("lock_room" + roomNo)) {
            }
            if (redisService.exists("dissolve" + roomNo)) {
                Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);
                GameBase.DissolveConfirm dissolveConfirm = GameBase.DissolveConfirm.newBuilder().setDissolved(true).build();
                response.setOperationType(GameBase.OperationType.DISSOLVE_CONFIRM).setData(dissolveConfirm.toByteString());
                for (Seat seat : room.getSeats()) {
                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                    }
                }
                room.roomOver(response, redisService);
                redisService.delete("dissolve" + roomNo);
            }
            redisService.unlock("lock_room" + roomNo);
        }
    }
}

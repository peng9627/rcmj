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
public class PlayCardTimeout extends Thread {

    private int userId;
    private String roomNo;
    private int operationCount;
    private int gameCount;
    private RedisService redisService;
    private GameBase.BaseConnection.Builder response;

    public PlayCardTimeout(int userId, String roomNo, int operationCount, int gameCount, RedisService redisService) {
        this.userId = userId;
        this.roomNo = roomNo;
        this.operationCount = operationCount;
        this.gameCount = gameCount;
        this.redisService = redisService;
        this.response = GameBase.BaseConnection.newBuilder();
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (MahjongTcpService.userClients.containsKey(userId)) {
            synchronized (this) {
                try {
                    wait(Constant.playCardTimeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (redisService.exists("room" + roomNo)) {
            while (!redisService.lock("lock_room" + roomNo)) {
            }
            Room room = JSON.parseObject(redisService.getCache("room" + roomNo), Room.class);

            if (room.getGameCount() == gameCount && room.getHistoryList().size() == operationCount) {
                //找到人和牌
                for (Seat seat : room.getSeats()) {
                    if (seat.getUserId() == userId) {
                        room.playCard(seat.getCards().get(seat.getCards().size() - 1), userId, GameBase.BaseAction.newBuilder(), response, redisService);
                        break;
                    }
                }
            }
            if (null != room.getRoomNo()) {
                redisService.addCache("room" + roomNo, JSON.toJSONString(room));
            }
            redisService.unlock("lock_room" + roomNo);
        }
    }
}

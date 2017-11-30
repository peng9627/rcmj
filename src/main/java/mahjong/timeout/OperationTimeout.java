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
public class OperationTimeout extends Thread {

    private int userId;
    private String roomNo;
    private int operationCount;
    private int gameCount;
    private RedisService redisService;
    private boolean hu;
    private GameBase.BaseConnection.Builder response;

    public OperationTimeout(int userId, String roomNo, int operationCount, int gameCount, RedisService redisService, boolean hu) {
        this.userId = userId;
        this.roomNo = roomNo;
        this.operationCount = operationCount;
        this.gameCount = gameCount;
        this.redisService = redisService;
        this.hu = hu;
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
                if (hu) {
                    room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> seat.setOperation(1));
                    for (Seat seat : room.getSeats()) {
                        if (seat.getUserId() == userId) {
                            seat.setOperation(1);
                            break;
                        }
                    }
                    room.hu(userId, response, redisService);//胡
                } else {
                    room.getSeats().stream().filter(seat -> seat.getUserId() == userId).forEach(seat -> {
                        if (room.getOperationSeatNo() != seat.getSeatNo()) {
                            seat.setOperation(4);
                            if (MahjongTcpService.userClients.containsKey(userId)) {
                                MahjongTcpService.userClients.get(userId).send(response.setOperationType(GameBase.OperationType.ACTION)
                                        .setData(GameBase.BaseAction.newBuilder().setOperationId(GameBase.ActionId.PASS).clearData().build().toByteString()).build(), userId);
                            }
                            if (!room.passedChecked()) {//如果都操作完了，继续摸牌
                                room.getCard(response, room.getNextSeat(room.getOperationSeatNo()), redisService);
                            } else {//if (room.checkSurplus()) { //如果可以碰、杠牌，则碰、杠
                                room.pengOrGang(GameBase.BaseAction.newBuilder().setID(seat.getUserId()), response, redisService, userId);
                            }
                        } else {
                            if (MahjongTcpService.userClients.containsKey(userId)) {
                                MahjongTcpService.userClients.get(userId).send(response.setOperationType(GameBase.OperationType.ACTION)
                                        .setData(GameBase.BaseAction.newBuilder().setOperationId(GameBase.ActionId.PASS).clearData().build().toByteString()).build(), userId);
                            }
                            new PlayCardTimeout(seat.getUserId(), roomNo, room.getHistoryList().size(), room.getGameCount(), redisService).start();
                        }
                    });
                }
            }
            if (null != room.getRoomNo()) {
                redisService.addCache("room" + roomNo, JSON.toJSONString(room));
            }
            redisService.unlock("lock_room" + roomNo);
        }
    }
}

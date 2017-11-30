package mahjong.timeout;

import com.alibaba.fastjson.JSON;
import mahjong.constant.Constant;
import mahjong.entrance.MahjongTcpService;
import mahjong.mode.GameBase;
import mahjong.mode.MatchInfo;
import mahjong.mode.MatchUser;
import mahjong.redis.RedisService;

/**
 * Created by pengyi
 * Date : 17-8-31.
 * desc:
 */
public class MatchScoreTimeout extends Thread {

    private int matchNo;
    private RedisService redisService;

    public MatchScoreTimeout(int matchNo, RedisService redisService) {
        this.matchNo = matchNo;
        this.redisService = redisService;
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait(Constant.matchEliminateScoreTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (redisService.exists("match_info" + matchNo)) {
            while (!redisService.lock("lock_match_info" + matchNo)) {
            }
            MatchInfo matchInfo = JSON.parseObject(redisService.getCache("match_info" + matchNo), MatchInfo.class);
            if (matchInfo.getStatus() == 1) {
                matchInfo.setMatchEliminateScore(matchInfo.getMatchEliminateScore() + Constant.matchEliminateScore);

                GameBase.BaseConnection response = GameBase.BaseConnection.newBuilder().setOperationType(GameBase.OperationType.MATCH_ELIMINATE_SCORE)
                        .setData(GameBase.MatchEliminateScore.newBuilder().setScore(matchInfo.getMatchEliminateScore()).build().toByteString()).build();
                for (MatchUser matchUser : matchInfo.getMatchUsers()) {
                    if (MahjongTcpService.userClients.containsKey(matchUser.getUserId())) {
                        MahjongTcpService.userClients.get(matchUser.getUserId()).send(response, matchUser.getUserId());
                    }
                }
                new MatchScoreTimeout(matchNo, redisService).start();
            }
            redisService.addCache("match_info" + matchNo, JSON.toJSONString(matchInfo));
            redisService.unlock("lock_match_info" + matchNo);
        }
    }
}

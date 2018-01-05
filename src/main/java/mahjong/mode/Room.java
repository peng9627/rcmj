package mahjong.mode;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import mahjong.constant.Constant;
import mahjong.entrance.MahjongTcpService;
import mahjong.redis.RedisService;
import mahjong.timeout.OperationTimeout;
import mahjong.timeout.PlayCardTimeout;
import mahjong.timeout.ReadyTimeout;
import mahjong.utils.HttpUtil;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Author pengyi
 * Date 17-3-7.
 */
public class Room {

    private String roomNo;  //桌号
    private List<Seat> seats = new ArrayList<>();//座位
    private List<Integer> seatNos;
    private int operationSeatNo;
    private List<OperationHistory> historyList = new ArrayList<>();
    private List<Integer> surplusCards;//剩余的牌
    private GameStatus gameStatus;

    private int lastOperation;

    private int banker;//庄家
    private int gameTimes; //游戏局数
    private int count;//人数
    private int gameRules;////游戏规则  高位到低位顺序（换三张，后四必胡，GPS，ip一致不能进房间）
    private Integer[] dice;//骰子
    private List<Record> recordList = new ArrayList<>();//战绩
    private int gameCount;
    private boolean aa;//AA支付

    private int roomOwner;

    private Date startDate;
    private int changeDice;
    private int tempBanker;

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    public List<Integer> getSeatNos() {
        return seatNos;
    }

    public void setSeatNos(List<Integer> seatNos) {
        this.seatNos = seatNos;
    }

    public int getOperationSeatNo() {
        return operationSeatNo;
    }

    public void setOperationSeatNo(int operationSeatNo) {
        this.operationSeatNo = operationSeatNo;
    }

    public List<OperationHistory> getHistoryList() {
        return historyList;
    }

    public void setHistoryList(List<OperationHistory> historyList) {
        this.historyList = historyList;
    }

    public List<Integer> getSurplusCards() {
        return surplusCards;
    }

    public void setSurplusCards(List<Integer> surplusCards) {
        this.surplusCards = surplusCards;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getLastOperation() {
        return lastOperation;
    }

    public void setLastOperation(int lastOperation) {
        this.lastOperation = lastOperation;
    }

    public int getBanker() {
        return banker;
    }

    public void setBanker(int banker) {
        this.banker = banker;
    }

    public int getGameTimes() {
        return gameTimes;
    }

    public void setGameTimes(int gameTimes) {
        this.gameTimes = gameTimes;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getGameRules() {
        return gameRules;
    }

    public void setGameRules(int gameRules) {
        this.gameRules = gameRules;
    }

    public Integer[] getDice() {
        return dice;
    }

    public void setDice(Integer[] dice) {
        this.dice = dice;
    }

    public List<Record> getRecordList() {
        return recordList;
    }

    public void setRecordList(List<Record> recordList) {
        this.recordList = recordList;
    }

    public int getGameCount() {
        return gameCount;
    }

    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }

    public boolean isAa() {
        return aa;
    }

    public void setAa(boolean aa) {
        this.aa = aa;
    }

    public int getRoomOwner() {
        return roomOwner;
    }

    public void setRoomOwner(int roomOwner) {
        this.roomOwner = roomOwner;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getChangeDice() {
        return changeDice;
    }

    public void setChangeDice(int changeDice) {
        this.changeDice = changeDice;
    }

    public int getTempBanker() {
        return tempBanker;
    }

    public void setTempBanker(int tempBanker) {
        this.tempBanker = tempBanker;
    }

    public void addSeat(User user, int score) {
        Seat seat = new Seat();
        seat.setRobot(false);
        seat.setReady(false);
        seat.setAreaString(user.getArea());
        seat.setHead(user.getHead());
        seat.setNickname(user.getNickname());
        seat.setSex(user.getSex().equals("1"));
        seat.setScore(score);
        seat.setIp(user.getLastLoginIp());
        seat.setGameCount(user.getGameCount());
        seat.setSeatNo(seatNos.get(0));
        seatNos.remove(0);
        seat.setUserId(user.getUserId());
        seats.add(seat);
        seats.sort(new Comparator<Seat>() {
            @Override
            public int compare(Seat o1, Seat o2) {
                return o1.getSeatNo() > o2.getSeatNo() ? 1 : -1;
            }
        });
    }

    public void dealCard() {
        startDate = new Date();
        surplusCards = Card.getAllCard();
        //卖马 发牌
        for (Seat seat : seats) {
            seat.setReady(false);
            List<Integer> cardList = new ArrayList<>();
            int cardIndex;
            for (int i = 0; i < 13; i++) {
                cardIndex = (int) (Math.random() * surplusCards.size());
                cardList.add(surplusCards.get(cardIndex));
                surplusCards.remove(cardIndex);
            }

            seat.setCards(cardList);
            seat.setInitialCards(cardList);

            if (seat.getUserId() == banker) {
                operationSeatNo = seat.getSeatNo();
                cardIndex = (int) (Math.random() * surplusCards.size());
                seat.getCards().add(surplusCards.get(cardIndex));
                surplusCards.remove(cardIndex);
            }
        }
    }

    public int getNextSeat(int next) {
        if (count == next) {
            next = 1;
        } else {
            next += 1;
        }
        for (Seat seat : seats) {
            if (next == seat.getSeatNo() && 0 != seat.getHuCard()) {
                return getNextSeat(next);
            }
        }
        return next;
    }

    private void clear(Map<Integer, Integer> huCard) {
        Record record = new Record();
        record.setDice(dice);
        record.setBanker(banker);
        record.setStartDate(startDate);
        record.setGameCount(gameCount);
        record.setChangeDice(changeDice);
        List<SeatRecord> seatRecords = new ArrayList<>();
        seats.forEach(seat -> {
            SeatRecord seatRecord = new SeatRecord();
            seatRecord.setUserId(seat.getUserId());
            seatRecord.setNickname(seat.getNickname());
            seatRecord.setHead(seat.getHead());
            seatRecord.setCardResult(seat.getCardResult());
            seatRecord.getMingGangResult().addAll(seat.getMingGangResult());
            seatRecord.getAnGangResult().addAll(seat.getAnGangResult());
            seatRecord.getInitialCards().addAll(seat.getInitialCards());
            seatRecord.getCards().addAll(seat.getCards());
            seatRecord.getPengCards().addAll(seat.getPengCards());
            seatRecord.getAnGangCards().addAll(seat.getAnGangCards());
            seatRecord.getMingGangCards().addAll(seat.getMingGangCards());
            seatRecord.setScore(seat.getScore());
            seatRecord.setSex(seat.isSex());
            seatRecord.setIp(seat.getIp());
            seatRecord.setSeatNo(seat.getSeatNo());
            seatRecord.setGameCount(gameCount);
            seatRecord.getChangeInCard().addAll(seat.getChangeInCards());
            seatRecord.getChangeOutCard().addAll(seat.getChangeOutCards());
            if (null != huCard && huCard.containsKey(seatRecord.getUserId())) {
                seatRecord.setHuCard(huCard.get(seatRecord.getUserId()));
            }
            final int[] winOrLose = {0};
            seat.getMingGangResult().forEach(gameResult -> winOrLose[0] += gameResult.getScore());
            seat.getAnGangResult().forEach(gameResult -> winOrLose[0] += gameResult.getScore());
            if (null != seat.getCardResult()) {
                winOrLose[0] += seat.getCardResult().getScore();
            }
            seatRecord.setWinOrLose(winOrLose[0]);
            seatRecords.add(seatRecord);
        });
        record.setSeatRecordList(seatRecords);
        record.getHistoryList().addAll(historyList);
        recordList.add(record);

        historyList.clear();
        surplusCards.clear();
        gameStatus = GameStatus.READYING;
        lastOperation = 0;
        dice = null;
        seats.forEach(Seat::clear);
        startDate = new Date();
        changeDice = 0;
    }

    public void getCard(GameBase.BaseConnection.Builder response, int seatNo, RedisService redisService) {
        if (0 == surplusCards.size()) {
            gameOver(response, redisService, 0);
            return;
        }
        GameBase.BaseAction.Builder actionResponse = GameBase.BaseAction.newBuilder().setOperationId(GameBase.ActionId.GET_CARD);
        operationSeatNo = seatNo;
        int cardIndex = (int) (Math.random() * surplusCards.size());
        Integer card1 = surplusCards.get(cardIndex);
        surplusCards.remove(cardIndex);
        final Integer[] username = new Integer[1];
        seats.stream().filter(seat -> seat.getSeatNo() == seatNo).forEach(seat -> username[0] = seat.getUserId());
        actionResponse.setID(username[0]);

        historyList.add(new OperationHistory(username[0], OperationHistoryType.GET_CARD, card1));
        Mahjong.CardsData.Builder builder1 = Mahjong.CardsData.newBuilder();
        builder1.addCards(card1);
        Seat operationSeat = null;
        for (Seat seat : seats) {
            if (seat.getSeatNo() == seatNo) {
                seat.getCards().add(card1);
                seat.setCanNotHu(false);
                operationSeat = seat;
                actionResponse.setData(builder1.build().toByteString());
            } else {
                actionResponse.clearData();
            }
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }

        GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(username[0])
                .setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0).build();
        response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
        seats.stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));

        checkSelfGetCard(response, operationSeat, redisService);
    }

    /**
     * 游戏结束
     *
     * @param response
     * @param redisService
     */
    public void gameOver(GameBase.BaseConnection.Builder response, RedisService redisService, int card) {
        Map<Integer, Integer> huCard = new HashMap<>();

        List<Seat> huSeat = new ArrayList<>();
        for (Seat seat : seats) {
            if (0 != seat.getHuCard()) {
                huSeat.add(seat);
            }
        }

        if (count - huSeat.size() > 1) {
            List<ScoreType> scoreTypes = new ArrayList<>();
            Map<Integer, Integer> userId_score = new HashMap<>();
            List<Integer> possibleCard = new ArrayList<>();
            for (Seat seat : seats) {
                possibleCard.addAll(seat.getCards());
                possibleCard.addAll(seat.getAnGangCards());
            }
            List<Seat> loseSeats = new ArrayList<>();
            for (Seat seat : seats) {
                if (0 == seat.getHuCard()) {
                    scoreTypes.clear();
                    if (seat.isJiao()) {
                        scoreTypes.add(ScoreType.BAOJIAO);
                    }
                    System.out.println(seat.getNickname());
                    int score = MahjongUtil.tingScore(seat.getCards(), seat.getPengCards(), seat.getMingGangCards(), seat.getAnGangCards(), possibleCard, scoreTypes);
                    if (0 != score) {
                        userId_score.put(seat.getUserId(), score);
                        seat.setChajiaoCount(seat.getChajiaoCount());
                    } else {
                        seat.setPeijiaoCount(seat.getPeijiaoCount());
                        loseSeats.add(seat);
                    }
                }
            }
            scoreTypes.clear();
            int allScore = 0;
            for (Seat seat : seats) {
                if (userId_score.containsKey(seat.getUserId())) {
                    if (null == seat.getCardResult()) {
                        seat.setCardResult(new GameResult(scoreTypes, 0, userId_score.get(seat.getUserId()) * loseSeats.size()));
                    } else {
                        seat.getCardResult().setScore(seat.getCardResult().getScore() + userId_score.get(seat.getUserId()) * loseSeats.size());
                    }

                    allScore += userId_score.get(seat.getUserId());
                }
            }
            for (Seat seat : loseSeats) {
                if (null == seat.getCardResult()) {
                    seat.setCardResult(new GameResult(scoreTypes, 0, -allScore));
                } else {
                    seat.getCardResult().setScore(seat.getCardResult().getScore() - allScore);
                }

                List<GameResult> mingGangResult = new ArrayList<>();
                for (GameResult gameResult : seat.getMingGangResult()) {
                    if (gameResult.getScore() > 0) {
                        for (Seat seat1 : seats) {
                            if (seat1.getUserId() != seat.getUserId()) {
                                for (GameResult gameResult1 : seat1.getMingGangResult()) {
                                    if (gameResult1.getCard().intValue() == gameResult.getCard()) {
                                        seat1.getMingGangResult().remove(gameResult1);
                                        break;
                                    }
                                }
                            }
                        }
                        mingGangResult.add(gameResult);
                    }
                }
                seat.getMingGangResult().removeAll(mingGangResult);

                List<GameResult> anGangResult = new ArrayList<>();
                for (GameResult gameResult : seat.getAnGangResult()) {
                    if (gameResult.getScore() > 0) {
                        for (Seat seat1 : seats) {
                            if (seat1.getUserId() != seat.getUserId()) {
                                for (GameResult gameResult1 : seat1.getAnGangResult()) {
                                    if (gameResult1.getCard().intValue() == gameResult.getCard()) {
                                        seat1.getAnGangResult().remove(gameResult1);
                                        break;
                                    }
                                }
                            }
                        }
                        anGangResult.add(gameResult);
                    }
                }
                seat.getAnGangResult().removeAll(anGangResult);
            }
        }

        Mahjong.MahjongResultResponse.Builder resultResponse = Mahjong.MahjongResultResponse.newBuilder();
        List<Integer> winSeats = new ArrayList<>();
        for (Seat seat : seats) {
            Mahjong.MahjongUserResult.Builder userResult = Mahjong.MahjongUserResult.newBuilder();
            userResult.setID(seat.getUserId());
            userResult.addAllCards(seat.getCards());
            userResult.addAllPengCards(seat.getPengCards());
            userResult.addAllAnGangCards(seat.getAnGangCards());
            userResult.addAllMingGangCards(seat.getMingGangCards());
            if (null != seat.getCardResult()) {
                userResult.setCardScore(seat.getCardResult().getScore());
                if (seat.getCardResult().getScore() > 0) {
                    winSeats.add(seat.getUserId());
                    userResult.setHuCard(seat.getHuCard());
                    huCard.put(seat.getUserId(), card);
                }
                for (ScoreType scoreType : seat.getCardResult().getScoreTypes()) {
                    userResult.addScoreTypes(Mahjong.ScoreType.forNumber(scoreType.ordinal() - 3));
                }
            }
            int mingGangScore = 0;
            for (GameResult gameResult : seat.getMingGangResult()) {
                mingGangScore += gameResult.getScore();
            }
            int anGangScore = 0;
            for (GameResult gameResult : seat.getAnGangResult()) {
                anGangScore += gameResult.getScore();
            }
            userResult.setMingGangScore(mingGangScore);
            userResult.setAnGangScore(anGangScore);
            resultResponse.addUserResult(userResult);
        }

        if (0 == winSeats.size()) {
            resultResponse.clearMaCard();
            for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
                userResult.setMingGangScore(0);
                userResult.setAnGangScore(0);
            }
        }

        for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
            int win = userResult.getCardScore() + userResult.getMingGangScore() + userResult.getAnGangScore() + userResult.getMaScore();
            userResult.setWinOrLose(win);
            for (Seat seat : seats) {
                if (seat.getUserId() == userResult.getID()) {
                    seat.setScore(seat.getScore() + win);
                    userResult.setScore(seat.getScore());
                    break;
                }
            }
        }

        if (redisService.exists("room_match" + roomNo)) {
            GameBase.ScoreResponse.Builder scoreResponse = GameBase.ScoreResponse.newBuilder();
            for (Mahjong.MahjongUserResult.Builder userResult : resultResponse.getUserResultBuilderList()) {
                if (MahjongTcpService.userClients.containsKey(userResult.getID())) {
                    int win = userResult.getCardScore() + userResult.getMingGangScore() + userResult.getAnGangScore() + userResult.getMaScore();
                    GameBase.MatchResult matchResult;
                    if (gameCount != gameTimes) {
                        matchResult = GameBase.MatchResult.newBuilder().setResult(0).setCurrentScore(win)
                                .setTotalScore(userResult.getScore()).build();
                    } else {
                        matchResult = GameBase.MatchResult.newBuilder().setResult(2).setCurrentScore(win)
                                .setTotalScore(userResult.getScore()).build();
                    }
                    MahjongTcpService.userClients.get(userResult.getID()).send(response.setOperationType(GameBase.OperationType.MATCH_RESULT)
                            .setData(matchResult.toByteString()).build(), userResult.getID());
                }
                scoreResponse.addScoreResult(GameBase.ScoreResult.newBuilder().setID(userResult.getID()).setScore(userResult.getScore()));
            }
            for (Seat seat : seats) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.setOperationType(GameBase.OperationType.MATCH_SCORE)
                            .setData(scoreResponse.build().toByteString()).build(), seat.getUserId());
                }
            }
        } else {
            if (1 == gameCount && aa) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("flowType", 2);
                jsonObject.put("money", 1);
                jsonObject.put("description", "AA支付" + roomNo);
                for (Seat seat : seats) {
                    jsonObject.put("userId", seat.getUserId());
                    ApiResponse moneyDetail = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.moneyDetailedCreate, jsonObject.toJSONString()), new TypeReference<ApiResponse<User>>() {
                    });
                    if (0 != moneyDetail.getCode()) {
                        LoggerFactory.getLogger(this.getClass()).error(Constant.apiUrl + Constant.moneyDetailedCreate + "?" + jsonObject.toJSONString());
                    }
                }
            }
            response.setOperationType(GameBase.OperationType.RESULT).setData(resultResponse.build().toByteString());
            seats.stream().filter(seat -> MahjongTcpService.userClients.containsKey(seat.getUserId()))
                    .forEach(seat -> MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId()));
        }
        clear(huCard);

        if (0 != tempBanker) {
            banker = tempBanker;
            tempBanker = 0;
        }
        //结束房间
        if (gameCount == gameTimes) {
            roomOver(response, redisService);
        } else {
            if (redisService.exists("room_match" + roomNo)) {
                new ReadyTimeout(Integer.valueOf(roomNo), redisService, gameCount).start();
            }
        }
    }

    public void roomOver(GameBase.BaseConnection.Builder response, RedisService redisService) {
        JSONObject jsonObject = new JSONObject();
        //是否竞技场
        if (redisService.exists("room_match" + roomNo)) {
            String matchNo = redisService.getCache("room_match" + roomNo);
            redisService.delete("room_match" + roomNo);
            if (redisService.exists("match_info" + matchNo)) {
                while (!redisService.lock("lock_match_info" + matchNo)) {
                }
                GameBase.MatchResult.Builder matchResult = GameBase.MatchResult.newBuilder();
                MatchInfo matchInfo = JSON.parseObject(redisService.getCache("match_info" + matchNo), MatchInfo.class);
                Arena arena = matchInfo.getArena();

                //移出当前桌
                List<Integer> rooms = matchInfo.getRooms();
                for (Integer integer : rooms) {
                    if (integer == Integer.parseInt(roomNo)) {
                        rooms.remove(integer);
                        break;
                    }
                }

                //等待的人
                List<MatchUser> waitUsers = matchInfo.getWaitUsers();
                if (null == waitUsers) {
                    waitUsers = new ArrayList<>();
                    matchInfo.setWaitUsers(waitUsers);
                }
                //在比赛中的人 重置分数
                List<MatchUser> matchUsers = matchInfo.getMatchUsers();
                for (Seat seat : seats) {
                    redisService.delete("reconnect" + seat.getUserId());
                    for (MatchUser matchUser : matchUsers) {
                        if (seat.getUserId() == matchUser.getUserId()) {
                            matchUser.setScore(seat.getScore());
                        }
                    }
//                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
//                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.setOperationType(GameBase.OperationType.ROOM_INFO).clearData().build(), seat.getUserId());
//                        GameBase.RoomSeatsInfo.Builder roomSeatsInfo = GameBase.RoomSeatsInfo.newBuilder();
//                        GameBase.SeatResponse.Builder seatResponse = GameBase.SeatResponse.newBuilder();
//                        seatResponse.setSeatNo(1);
//                        seatResponse.setID(seat.getUserId());
//                        seatResponse.setScore(seat.getScore());
//                        seatResponse.setReady(false);
//                        seatResponse.setIp(seat.getIp());
//                        seatResponse.setGameCount(seat.getGameCount());
//                        seatResponse.setNickname(seat.getNickname());
//                        seatResponse.setHead(seat.getHead());
//                        seatResponse.setSex(seat.isSex());
//                        seatResponse.setOffline(false);
//                        seatResponse.setIsRobot(seat.isRobot());
//                        roomSeatsInfo.addSeats(seatResponse.build());
//                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString()).build(), seat.getUserId());
//                    }
                }

                //用户对应分数
                Map<Integer, Integer> userIdScore = new HashMap<>();
                for (MatchUser matchUser : matchUsers) {
                    userIdScore.put(matchUser.getUserId(), matchUser.getScore());
                }

                GameBase.MatchData.Builder matchData = GameBase.MatchData.newBuilder();
                switch (matchInfo.getStatus()) {
                    case 1:

                        //根据金币排序
                        seats.sort(new Comparator<Seat>() {
                            @Override
                            public int compare(Seat o1, Seat o2) {
                                return o1.getScore() > o2.getScore() ? 1 : -1;
                            }
                        });

                        //本局未被淘汰的
                        List<MatchUser> thisWait = new ArrayList<>();
                        //循环座位，淘汰
                        for (Seat seat : seats) {
                            for (MatchUser matchUser : matchUsers) {
                                if (matchUser.getUserId() == seat.getUserId()) {
                                    if (seat.getScore() < matchInfo.getMatchEliminateScore() && matchUsers.size() > arena.getCount() / 2) {
                                        matchUsers.remove(matchUser);

                                        matchResult.setResult(3).setTotalScore(seat.getScore()).setCurrentScore(-1);
                                        response.setOperationType(GameBase.OperationType.MATCH_RESULT).setData(matchResult.build().toByteString());
                                        if (MahjongTcpService.userClients.containsKey(matchUser.getUserId())) {
                                            MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                                        }
                                        response.setOperationType(GameBase.OperationType.MATCH_BALANCE).setData(GameBase.MatchBalance.newBuilder()
                                                .setRanking(matchUsers.size()).setTotalScore(matchUser.getScore()).build().toByteString());
                                        if (MahjongTcpService.userClients.containsKey(matchUser.getUserId())) {
                                            MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                                            GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
                                            String uuid = UUID.randomUUID().toString().replace("-", "");
                                            while (redisService.exists(uuid)) {
                                                uuid = UUID.randomUUID().toString().replace("-", "");
                                            }
                                            redisService.addCache("backkey" + uuid, seat.getUserId() + "", 1800);
                                            over.setBackKey(uuid);
                                            over.setDateTime(new Date().getTime());
                                            response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                                            MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                                        }

                                        redisService.delete("reconnect" + seat.getUserId());
                                    } else {
                                        thisWait.add(matchUser);
                                        redisService.addCache("reconnect" + seat.getUserId(), "rongchang_mahjong," + matchNo);
                                    }
                                    break;
                                }
                            }
                        }

                        //淘汰人数以满
                        int count = matchUsers.size();
                        if (count == arena.getCount() / 2 && 0 == rooms.size()) {
                            waitUsers.clear();
                            List<User> users = new ArrayList<>();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (MatchUser matchUser : matchUsers) {
                                stringBuilder.append(",").append(matchUser.getUserId());
                            }
                            jsonObject.clear();
                            jsonObject.put("userIds", stringBuilder.toString().substring(1));
                            ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                    new TypeReference<ApiResponse<List<User>>>() {
                                    });
                            if (0 == usersResponse.getCode()) {
                                users = usersResponse.getData();
                            }

                            //第二轮开始
                            matchInfo.setStatus(2);
                            matchData.setStatus(2);
                            matchData.setCurrentCount(matchUsers.size());
                            matchData.setRound(1);
                            while (4 <= users.size()) {
                                rooms.add(matchInfo.addRoom(matchNo, 2, redisService, users.subList(0, 4), userIdScore, response, matchData));
                            }
                        } else if (count > arena.getCount() / 2) {
                            //满四人继续匹配
                            waitUsers.addAll(thisWait);
                            while (4 <= waitUsers.size()) {
                                //剩余用户
                                List<User> users = new ArrayList<>();
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < 4; i++) {
                                    stringBuilder.append(",").append(waitUsers.remove(0).getUserId());
                                }
                                jsonObject.clear();
                                jsonObject.put("userIds", stringBuilder.toString().substring(1));
                                ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                        new TypeReference<ApiResponse<List<User>>>() {
                                        });
                                if (0 == usersResponse.getCode()) {
                                    users = usersResponse.getData();
                                }
                                matchData.setStatus(1);
                                matchData.setCurrentCount(matchUsers.size());
                                matchData.setRound(1);
                                rooms.add(matchInfo.addRoom(matchNo, 1, redisService, users, userIdScore, response, matchData));
                            }
                        }
                        break;
                    case 2:
                    case 3:
                        for (Seat seat : seats) {
                            redisService.addCache("reconnect" + seat.getUserId(), "rongchang_mahjong," + matchNo);
                        }
                        if (0 == rooms.size()) {
                            matchInfo.setStatus(matchInfo.getStatus() + 1);
                            matchData.setStatus(2);

                            List<User> users = new ArrayList<>();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (MatchUser matchUser : matchUsers) {
                                stringBuilder.append(",").append(matchUser.getUserId());
                            }
                            jsonObject.clear();
                            jsonObject.put("userIds", stringBuilder.toString().substring(1));
                            ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                    new TypeReference<ApiResponse<List<User>>>() {
                                    });
                            if (0 == usersResponse.getCode()) {
                                users = usersResponse.getData();
                            }
                            matchData.setCurrentCount(matchUsers.size());
                            matchData.setRound(matchInfo.getStatus() - 1);
                            while (4 <= users.size()) {
                                rooms.add(matchInfo.addRoom(matchNo, 2, redisService, users.subList(0, 4), userIdScore, response, matchData));
                            }
                        }
                        break;
                    case 4:
                        for (Seat seat : seats) {
                            MatchUser matchUser = new MatchUser();
                            matchUser.setUserId(seat.getUserId());
                            matchUser.setScore(seat.getScore());
                            waitUsers.add(matchUser);
                            redisService.addCache("reconnect" + seat.getUserId(), "rongchang_mahjong," + matchNo);
                        }

                        waitUsers.sort(new Comparator<MatchUser>() {
                            @Override
                            public int compare(MatchUser o1, MatchUser o2) {
                                return o1.getScore() > o2.getScore() ? -1 : 1;
                            }
                        });
                        while (waitUsers.size() > 4) {
                            MatchUser matchUser = waitUsers.remove(waitUsers.size() - 1);

                            response.setOperationType(GameBase.OperationType.MATCH_BALANCE).setData(GameBase.MatchBalance.newBuilder()
                                    .setRanking(matchUsers.size()).setTotalScore(matchUser.getScore()).build().toByteString());
                            if (MahjongTcpService.userClients.containsKey(matchUser.getUserId())) {
                                MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                                GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
                                String uuid = UUID.randomUUID().toString().replace("-", "");
                                while (redisService.exists(uuid)) {
                                    uuid = UUID.randomUUID().toString().replace("-", "");
                                }
                                redisService.addCache("backkey" + uuid, matchUser.getUserId() + "", 1800);
                                over.setBackKey(uuid);
                                over.setDateTime(new Date().getTime());
                                response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                                MahjongTcpService.userClients.get(matchUser.getUserId()).send(response.build(), matchUser.getUserId());
                            }
                            redisService.delete("reconnect" + matchUser.getUserId());
                        }

                        if (0 == rooms.size()) {

                            matchUsers.clear();
                            matchUsers.addAll(waitUsers);
                            waitUsers.clear();

                            matchInfo.setStatus(5);
                            matchData.setStatus(3);

                            List<User> users = new ArrayList<>();
                            StringBuilder stringBuilder = new StringBuilder();
                            for (MatchUser matchUser : matchUsers) {
                                stringBuilder.append(",").append(matchUser.getUserId());
                            }
                            jsonObject.clear();
                            jsonObject.put("userIds", stringBuilder.toString().substring(1));
                            ApiResponse<List<User>> usersResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.userListUrl, jsonObject.toJSONString()),
                                    new TypeReference<ApiResponse<List<User>>>() {
                                    });
                            if (0 == usersResponse.getCode()) {
                                users = usersResponse.getData();
                            }
                            matchData.setCurrentCount(matchUsers.size());
                            matchData.setRound(1);
                            while (4 == users.size()) {
                                rooms.add(matchInfo.addRoom(matchNo, 2, redisService, users, userIdScore, response, matchData));
                            }
                        }
                        break;
                    case 5:
                        matchUsers.sort(new Comparator<MatchUser>() {
                            @Override
                            public int compare(MatchUser o1, MatchUser o2) {
                                return o1.getScore() > o2.getScore() ? -1 : 1;
                            }
                        });
                        for (int i = 0; i < matchUsers.size(); i++) {
                            if (i == 0 && matchInfo.getArena().getArenaType() == 0) {
                                jsonObject.clear();
                                jsonObject.put("flowType", 1);
                                jsonObject.put("money", matchInfo.getArena().getReward());
                                jsonObject.put("description", "比赛获胜" + matchInfo.getArena().getId());
                                jsonObject.put("userId", matchUsers.get(i).getUserId());
                                ApiResponse moneyDetail = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.moneyDetailedCreate, jsonObject.toJSONString()), new TypeReference<ApiResponse<User>>() {
                                });
                                if (0 != moneyDetail.getCode()) {
                                    LoggerFactory.getLogger(this.getClass()).error(Constant.apiUrl + Constant.moneyDetailedCreate + "?" + jsonObject.toJSONString());
                                }
                            }
                            matchResult.setResult(i == 0 ? 1 : 3).setTotalScore(matchUsers.get(i).getScore()).setCurrentScore(-1);
                            response.setOperationType(GameBase.OperationType.MATCH_RESULT).setData(matchResult.build().toByteString());
                            if (MahjongTcpService.userClients.containsKey(matchUsers.get(i).getUserId())) {
                                MahjongTcpService.userClients.get(matchUsers.get(i).getUserId()).send(response.build(), matchUsers.get(i).getUserId());
                            }
                            response.setOperationType(GameBase.OperationType.MATCH_BALANCE).setData(GameBase.MatchBalance.newBuilder()
                                    .setRanking(i + 1).setTotalScore(matchUsers.get(i).getScore()).build().toByteString());
                            if (MahjongTcpService.userClients.containsKey(matchUsers.get(i).getUserId())) {
                                MahjongTcpService.userClients.get(matchUsers.get(i).getUserId()).send(response.build(), matchUsers.get(i).getUserId());
                                GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
                                String uuid = UUID.randomUUID().toString().replace("-", "");
                                while (redisService.exists(uuid)) {
                                    uuid = UUID.randomUUID().toString().replace("-", "");
                                }
                                redisService.addCache("backkey" + uuid, matchUsers.get(i).getUserId() + "", 1800);
                                over.setBackKey(uuid);
                                over.setDateTime(new Date().getTime());
                                response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                                MahjongTcpService.userClients.get(matchUsers.get(i).getUserId()).send(response.build(), matchUsers.get(i).getUserId());
                            }
                        }
                        matchInfo.setStatus(-1);
                        break;
                }
                if (0 < matchInfo.getStatus()) {
                    matchInfo.setMatchUsers(matchUsers);
                    matchInfo.setRooms(rooms);
                    matchInfo.setWaitUsers(waitUsers);
                    redisService.addCache("match_info" + matchNo, JSON.toJSONString(matchInfo));
                }
                redisService.unlock("lock_match_info" + matchNo);
            }
        } else {
            if (0 == gameStatus.compareTo(GameStatus.WAITING) && !aa) {
                jsonObject.clear();
                jsonObject.put("flowType", 1);
                if (8 == gameTimes) {
                    jsonObject.put("money", 2);
                } else {
                    jsonObject.put("money", 3);
                }
                jsonObject.put("description", "开房间退回" + roomNo);
                jsonObject.put("userId", roomOwner);
                ApiResponse moneyDetail = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.moneyDetailedCreate, jsonObject.toJSONString()), new TypeReference<ApiResponse<User>>() {
                });
                if (0 != moneyDetail.getCode()) {
                    LoggerFactory.getLogger(this.getClass()).error(Constant.apiUrl + Constant.moneyDetailedCreate + "?" + jsonObject.toJSONString());
                }
            }

            if (0 != recordList.size()) {
                Mahjong.MahjongBalanceResponse.Builder balance = Mahjong.MahjongBalanceResponse.newBuilder();
                for (Seat seat : seats) {
                    Mahjong.MahjongSeatGameBalance.Builder seatGameOver = Mahjong.MahjongSeatGameBalance.newBuilder()
                            .setID(seat.getUserId()).setMinggang(seat.getMinggang()).setAngang(seat.getAngang())
                            .setZimoCount(seat.getZimoCount()).setHuCount(seat.getHuCount())
                            .setDianpaoCount(seat.getDianpaoCount()).setWinOrLose(seat.getScore())
                            .setChajiao(seat.getChajiaoCount()).setPeijiao(seat.getPeijiaoCount());
                    balance.addGameBalance(seatGameOver);
                }

                for (Seat seat : seats) {
                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                        response.setOperationType(GameBase.OperationType.BALANCE).setData(balance.build().toByteString());
                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                    }

                    jsonObject.clear();
                    jsonObject.put("userId", seat.getUserId());
                    jsonObject.put("dianPao", seat.getDianpaoCount());
                    jsonObject.put("zimo", seat.getZimoCount());
                    jsonObject.put("gameCount", gameCount);
                    ApiResponse apiResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.updateInfoUrl, jsonObject.toJSONString()), ApiResponse.class);
                    if (0 != apiResponse.getCode()) {
                        LoggerFactory.getLogger(this.getClass()).error(Constant.apiUrl + Constant.userInfoUrl + "?" + jsonObject.toJSONString());
                    }
                }

            }

            StringBuilder people = new StringBuilder();

            GameBase.OverResponse.Builder over = GameBase.OverResponse.newBuilder();
            for (Seat seat : seats) {
                people.append(",").append(seat.getUserId());
                redisService.delete("reconnect" + seat.getUserId());
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    String uuid = UUID.randomUUID().toString().replace("-", "");
                    while (redisService.exists(uuid)) {
                        uuid = UUID.randomUUID().toString().replace("-", "");
                    }
                    redisService.addCache("backkey" + uuid, seat.getUserId() + "", 1800);
                    over.setBackKey(uuid);
                    over.setDateTime(new Date().getTime());

                    response.setOperationType(GameBase.OperationType.OVER).setData(over.build().toByteString());
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
            }

            if (0 != recordList.size()) {
                List<TotalScore> totalScores = new ArrayList<>();
                for (Seat seat : seats) {
                    TotalScore totalScore = new TotalScore();
                    totalScore.setHead(seat.getHead());
                    totalScore.setNickname(seat.getNickname());
                    totalScore.setUserId(seat.getUserId());
                    totalScore.setScore(seat.getScore());
                    totalScores.add(totalScore);
                }
                SerializerFeature[] features = new SerializerFeature[]{SerializerFeature.WriteNullListAsEmpty,
                        SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect,
                        SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.WriteNullNumberAsZero,
                        SerializerFeature.WriteNullBooleanAsFalse};
                int feature = SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.WriteEnumUsingName, false);
                jsonObject.clear();
                jsonObject.put("gameType", 0);
                jsonObject.put("roomOwner", roomOwner);
                jsonObject.put("people", people.toString().substring(1));
                jsonObject.put("gameTotal", gameTimes);
                jsonObject.put("gameCount", gameCount);
                jsonObject.put("peopleCount", count);
                jsonObject.put("roomNo", Integer.parseInt(roomNo));
                JSONObject gameRule = new JSONObject();
                gameRule.put("gameRule", gameRules);
                jsonObject.put("gameRule", gameRule.toJSONString());
                jsonObject.put("gameData", JSON.toJSONString(recordList, feature, features).getBytes());
                jsonObject.put("scoreData", JSON.toJSONString(totalScores, feature, features).getBytes());

                ApiResponse apiResponse = JSON.parseObject(HttpUtil.urlConnectionByRsa(Constant.apiUrl + Constant.gamerecordCreateUrl, jsonObject.toJSONString()), ApiResponse.class);
                if (0 != apiResponse.getCode()) {
                    LoggerFactory.getLogger(this.getClass()).error(Constant.apiUrl + Constant.gamerecordCreateUrl + "?" + jsonObject.toJSONString());
                }
            }
        }

        //删除该桌
        redisService.delete("room" + roomNo);
        redisService.delete("room_type" + roomNo);
        roomNo = null;
    }

    /**
     * 摸牌后检测是否可以自摸、暗杠、扒杠
     *
     * @param seat 座位
     */
    public void checkSelfGetCard(GameBase.BaseConnection.Builder response, Seat seat, RedisService redisService) {
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        builder.setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0);
        if (MahjongUtil.checkHu(seat.getCards())) {
            if (1 == (gameRules >> 1) % 2 && surplusCards.size() < 4) {
                seat.setOperation(1);
                hu(seat.getUserId(), response, redisService);
                return;
            }
            builder.addOperationId(GameBase.ActionId.HU);
            if (redisService.exists("room_match" + roomNo)) {
                new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, true).start();
            }
        }
        //暗杠
        if (0 < surplusCards.size()) {
            boolean canGang = false;
            List<Integer> siList = MahjongUtil.get_si(seat.getCards());
            if (siList.size() > 0) {
                if (seat.isJiao()) {
                    for (Integer integer : siList) {
                        List<Integer> tempCards = new ArrayList<>();
                        tempCards.addAll(seat.getCards());
                        Card.remove(tempCards, integer);
                        Card.remove(tempCards, integer);
                        Card.remove(tempCards, integer);
                        Card.remove(tempCards, integer);

                        List<Integer> anGangCards = new ArrayList<>();
                        anGangCards.addAll(seat.getAnGangCards());
                        anGangCards.add(integer);
                        anGangCards.add(integer);
                        anGangCards.add(integer);
                        anGangCards.add(integer);

                        List<Integer> possibleCard = new ArrayList<>();
                        for (Seat seat1 : seats) {
                            possibleCard.addAll(seat1.getCards());
                            possibleCard.addAll(seat1.getAnGangCards());
                        }
                        possibleCard.addAll(surplusCards);
                        if (0 != MahjongUtil.tingScore(tempCards, seat.getPengCards(), seat.getMingGangCards(), anGangCards, possibleCard, new ArrayList<>())) {
                            canGang = true;
                            break;
                        }
                    }
                } else {
                    canGang = true;
                }
            }
            if (canGang) {
                builder.addOperationId(GameBase.ActionId.AN_GANG);
            }
        }
        //扒杠
        if (null != MahjongUtil.checkBaGang(seat.getCards(), seat.getPengCards()) && 0 < surplusCards.size() && !seat.isJiao()) {
            builder.addOperationId(GameBase.ActionId.BA_GANG);
        }
        if (0 != builder.getOperationIdCount()) {
            if (redisService.exists("room_match" + roomNo) && !builder.getOperationIdList().contains(GameBase.ActionId.HU)) {
                new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, false).start();
            }
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                response.clear();
                response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        } else {
            if (seat.isJiao()) {
                playCard(seat.getCards().get(seat.getCards().size() - 1), seat.getUserId(),
                        GameBase.BaseAction.newBuilder().setID(seat.getUserId()), response, redisService);
            }
            if (redisService.exists("room_match" + roomNo)) {
                new PlayCardTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService).start();
            }
        }
    }

    /**
     * 和牌
     *
     * @param userId
     * @param response
     * @param redisService
     */
    public void hu(int userId, GameBase.BaseConnection.Builder response, RedisService redisService) {
        //和牌的人
        final Seat[] huSeat = new Seat[1];
        seats.stream().filter(seat -> seat.getUserId() == userId)
                .forEach(seat -> huSeat[0] = seat);
        boolean ganghou = false;
        //检查是自摸还是点炮,自摸输家是其它三家
        if (MahjongUtil.checkHu(huSeat[0].getCards()) && 1 == huSeat[0].getOperation()) {
            if (0 < historyList.size()) {
                if (0 != historyList.get(historyList.size() - 1).getHistoryType().compareTo(OperationHistoryType.GET_CARD)
                        || historyList.get(historyList.size() - 1).getUserId() != userId) {
                    return;
                }
            }
            historyList.add(new OperationHistory(huSeat[0].getUserId(), OperationHistoryType.HU, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1)));

            if (historyList.size() >= 3) {
                if ((0 == historyList.get(historyList.size() - 3).getHistoryType().compareTo(OperationHistoryType.DIAN_GANG)
                        || 0 == historyList.get(historyList.size() - 3).getHistoryType().compareTo(OperationHistoryType.AN_GANG)
                        || 0 == historyList.get(historyList.size() - 3).getHistoryType().compareTo(OperationHistoryType.BA_GANG))
                        && 0 == historyList.get(historyList.size() - 2).getHistoryType().compareTo(OperationHistoryType.GET_CARD)) {
                    ganghou = true;
                }
            }

            List<Integer> gangCards = new ArrayList<>();
            gangCards.addAll(huSeat[0].getAnGangCards());
            gangCards.addAll(huSeat[0].getMingGangCards());

            List<ScoreType> scoreTypes = MahjongUtil.getHuType(huSeat[0].getCards(), huSeat[0].getPengCards(), gangCards, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1));

            scoreTypes.add(ScoreType.ZIMOHU);
            //天胡
            if (historyList.size() == 1) {
                for (Integer card : huSeat[0].getCards()) {
                    if (4 == Card.containSize(huSeat[0].getCards(), card)) {
                        scoreTypes.add(ScoreType.SHOUGUI);
                        break;
                    }
                }
                scoreTypes.add(ScoreType.TIANHU);
            } else if (huSeat[0].isJiao()) {
                scoreTypes.add(ScoreType.BAOJIAO);
            }
            if (ganghou) {
                scoreTypes.add(ScoreType.GANGSHANGHUA);
            }
            int score = MahjongUtil.getScore(scoreTypes);
            if (1 == score) {
                score += 1;
            }
            int loseScore[] = {0};
            int finalScore = score;
            seats.stream().filter(seat -> seat.getUserId() != userId && 0 == seat.getHuCard()).forEach(seat -> {
                if (seat.isJiao()) {
                    if (2 == finalScore) {
                        if (null != seat.getCardResult()) {
                            seat.getCardResult().setScore(seat.getCardResult().getScore() - 7);
                        } else {
                            seat.setCardResult(new GameResult(new ArrayList<>(), huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), -7));
                        }
                        loseScore[0] += 7;
                    } else {
                        if (null != seat.getCardResult()) {
                            seat.getCardResult().setScore(seat.getCardResult().getScore() - finalScore - 6);
                        } else {
                            seat.setCardResult(new GameResult(new ArrayList<>(), huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), -finalScore - 6));
                        }
                        loseScore[0] += finalScore + 6;
                    }
                } else {
                    if (null != seat.getCardResult()) {
                        seat.getCardResult().setScore(seat.getCardResult().getScore() - finalScore);
                    } else {
                        seat.setCardResult(new GameResult(new ArrayList<>(), huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), -finalScore));
                    }
                    loseScore[0] += finalScore;
                }
            });

            if (null != huSeat[0].getCardResult()) {
                huSeat[0].getCardResult().getScoreTypes().addAll(scoreTypes);
                huSeat[0].getCardResult().setScore(huSeat[0].getCardResult().getScore() + loseScore[0]);
            } else {
                huSeat[0].setCardResult(new GameResult(scoreTypes, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1), loseScore[0]));
            }
            huSeat[0].setZimoCount(huSeat[0].getZimoCount() + 1);
            Mahjong.MahjongHuResponse.Builder mahjongHuResponse = Mahjong.MahjongHuResponse.newBuilder().addCards(huSeat[0].getCards().get(huSeat[0].getCards().size() - 1));
            for (ScoreType scoreType : huSeat[0].getCardResult().getScoreTypes()) {
                mahjongHuResponse.addScoreType(Mahjong.ScoreType.forNumber(scoreType.ordinal() - 3));
            }
            response.setOperationType(GameBase.OperationType.ACTION).setData(GameBase.BaseAction.newBuilder().setOperationId(GameBase.ActionId.HU)
                    .setID(huSeat[0].getUserId()).setData(mahjongHuResponse.build().toByteString()).build().toByteString());
            seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                    .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

            huSeat[0].setHuCard(huSeat[0].getCards().get(huSeat[0].getCards().size() - 1));
            int notHuCount = 0;
            for (Seat seat : seats) {
                if (0 == seat.getHuCard()) {
                    notHuCount++;
                }
            }
            if (tempBanker == 0) {
                tempBanker = huSeat[0].getUserId();
            }
            Card.remove(huSeat[0].getCards(), huSeat[0].getCards().get(huSeat[0].getCards().size() - 1));
            if (notHuCount > 1) {
                lastOperation = 0;
                getCard(response, getNextSeat(huSeat[0].getSeatNo()), redisService);
            } else {
                gameOver(response, redisService, huSeat[0].getCards().get(huSeat[0].getCards().size() - 1));
            }
            return;
        }

        OperationHistory operationHistory = null;
        if (historyList.size() > 0) {
            operationHistory = historyList.get(historyList.size() - 1);
        } else {
            return;
        }
        //找到那张牌
        int card = operationHistory.getCards().get(0);
        Seat loseSeat = null;
        for (Seat seat : seats) {
            if (seat.getSeatNo() == operationSeatNo) {
                loseSeat = seat;
                break;
            }
        }
        int husize = 0;
        for (int i = historyList.size() - 1; i >= 0; i--) {
            if (0 == historyList.get(i).getHistoryType().compareTo(OperationHistoryType.HU)) {
                husize++;
            } else {
                break;
            }
        }
        if (historyList.size() >= 3 + husize) {
            if ((0 == historyList.get(historyList.size() - 3 - husize).getHistoryType().compareTo(OperationHistoryType.DIAN_GANG)
                    || 0 == historyList.get(historyList.size() - 3 - husize).getHistoryType().compareTo(OperationHistoryType.AN_GANG)
                    || 0 == historyList.get(historyList.size() - 3 - husize).getHistoryType().compareTo(OperationHistoryType.BA_GANG))
                    && 0 == historyList.get(historyList.size() - 1 - husize).getHistoryType().compareTo(OperationHistoryType.PLAY_CARD)) {
                ganghou = true;
            }
        }
        boolean qianggang = false;
        if (historyList.size() > 1 + husize) {
            if ((0 == historyList.get(historyList.size() - 1 - husize).getHistoryType().compareTo(OperationHistoryType.BA_GANG))) {
                qianggang = true;
            }
        }

        boolean dihu = historyList.size() == 1;

        int hu = 0;
        for (Seat seat : seats) {
            if (seat.getSeatNo() != operationSeatNo && 1 == seat.getOperation() && 0 == seat.getHuCard()) {
                List<ScoreType> scoreTypes = new ArrayList<>();
                List<Integer> temp = new ArrayList<>();
                temp.addAll(seat.getCards());
                temp.add(card);
                if (MahjongUtil.checkHu(temp)) {
                    List<Integer> gangCards = new ArrayList<>();
                    gangCards.addAll(seat.getAnGangCards());
                    gangCards.addAll(seat.getMingGangCards());
                    scoreTypes = MahjongUtil.getHuType(temp, seat.getPengCards(), gangCards, card);
                    //地胡
                    if (dihu && seat.isJiao()) {
                        scoreTypes.add(ScoreType.DIHU);
                    } else if (ganghou) {
                        scoreTypes.add(ScoreType.GANGSHANGPAO);
                    } else if (qianggang) {
                        scoreTypes.add(ScoreType.QIANGGANG);
                    }
                    if (seat.isJiao()) {
                        scoreTypes.add(ScoreType.BAOJIAO);
                    }

                    int score = MahjongUtil.getScore(scoreTypes);
                    if (0 == score) {
                        score = 1;
                    }
                    if (loseSeat.isJiao()) {
                        if (1 == score) {
                            score = 6;
                        } else {
                            score += 6;
                        }
                    }

                    for (Seat seat1 : seats) {
                        if (seat1.getUserId() == loseSeat.getUserId()) {
                            if (null == seat1.getCardResult()) {
                                seat1.setCardResult(new GameResult(new ArrayList<>(), card, -score));
                            } else {
                                seat1.getCardResult().setScore(seat1.getCardResult().getScore() - score);
                            }
                            break;
                        }
                    }

                    historyList.add(new OperationHistory(huSeat[0].getUserId(), OperationHistoryType.HU, card));

                    banker = huSeat[0].getUserId();

                    if (0 == scoreTypes.size()) {
                        scoreTypes.add(ScoreType.PINGHU);
                    }
                    if (null != seat.getCardResult()) {
                        seat.getCardResult().getScoreTypes().addAll(scoreTypes);
                        seat.getCardResult().setScore(seat.getCardResult().getScore() + score);
                    } else {
                        seat.setCardResult(new GameResult(scoreTypes, card, score));
                    }
                    seat.setHuCount(seat.getHuCount() + 1);

                    if (0 == hu) {
                        if (ganghou) {
                            for (Seat seat1 : seats) {
                                if (seat1.getUserId() == operationHistory.getUserId()) {
                                    if (3 < historyList.size() && 0 == historyList.get(historyList.size() - 4).getHistoryType().compareTo(OperationHistoryType.AN_GANG)) {
                                        for (Seat seat2 : seats) {
                                            if (0 == seat2.getHuCard()) {
                                                seat2.getAnGangResult().remove(seat2.getAnGangResult().size() - 1);
                                            }
                                        }
                                    } else if (3 < historyList.size() && 0 == historyList.get(historyList.size() - 4).getHistoryType().compareTo(OperationHistoryType.BA_GANG)) {
                                        for (Seat seat2 : seats) {
                                            if (0 == seat2.getHuCard()) {
                                                seat2.getMingGangResult().remove(seat2.getMingGangResult().size() - 1);
                                            }
                                        }
                                    } else if (3 < historyList.size() && 0 == historyList.get(historyList.size() - 4).getHistoryType().compareTo(OperationHistoryType.DIAN_GANG)) {
                                        OperationHistory history = historyList.get(historyList.size() - 5);
                                        OperationHistory history1 = historyList.get(historyList.size() - 4);
                                        for (Seat seat2 : seats) {
                                            if (0 == seat2.getHuCard() && (seat2.getUserId() == history.getUserId() || seat2.getUserId() == history1.getUserId())) {
                                                seat2.getMingGangResult().remove(seat2.getMingGangResult().size() - 1);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        } else if (0 == operationHistory.getHistoryType().compareTo(OperationHistoryType.BA_GANG)) {
                            for (Seat seat1 : seats) {
                                if (seat1.getUserId() == operationHistory.getUserId()) {
                                    for (Seat seat2 : seats) {
                                        if (0 == seat2.getHuCard()) {
                                            seat2.getMingGangResult().remove(seat2.getMingGangResult().size() - 1);
                                        }
                                    }
                                    seat1.getMingGangCards().remove(seat1.getMingGangCards().size() - 1);
                                    seat1.getMingGangCards().remove(seat1.getMingGangCards().size() - 1);
                                    seat1.getMingGangCards().remove(seat1.getMingGangCards().size() - 1);
                                    seat1.getMingGangCards().remove(seat1.getMingGangCards().size() - 1);
                                    seat1.getPengCards().add(operationHistory.getCards().get(0));
                                    seat1.getPengCards().add(operationHistory.getCards().get(0));
                                    seat1.getPengCards().add(operationHistory.getCards().get(0));
                                    break;
                                }
                            }
                        }
                    }

                    Mahjong.MahjongHuResponse.Builder mahjongHuResponse = Mahjong.MahjongHuResponse.newBuilder().addCards(card);
                    for (ScoreType scoreType : scoreTypes) {
                        mahjongHuResponse.addScoreType(Mahjong.ScoreType.forNumber(scoreType.ordinal() - 3));
                    }

                    response.setOperationType(GameBase.OperationType.ACTION).setData(GameBase.BaseAction.newBuilder().setOperationId(GameBase.ActionId.HU)
                            .setID(seat.getUserId()).setData(mahjongHuResponse.build().toByteString()).build().toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                    if (tempBanker == 0) {
                        tempBanker = huSeat[0].getUserId();
                    }
                    hu += 1;
                    seat.setHuCard(card);
                }
            }
        }
        if (0 != hu) {
            if (2 == hu) {
                tempBanker = loseSeat.getUserId();
            }
            lastOperation = 0;
            loseSeat.getPlayedCards().remove(loseSeat.getPlayedCards().size() - 1);
            loseSeat.setDianpaoCount(loseSeat.getDianpaoCount() + 1);
            int notHuCount = 0;
            for (Seat seat : seats) {
                if (0 == seat.getHuCard()) {
                    notHuCount++;
                }
            }
            if (notHuCount > 1) {
                getCard(response, getNextSeat(huSeat[0].getSeatNo()), redisService);
            } else {
                gameOver(response, redisService, card);
            }
        }
    }

    /**
     * 暗杠或者扒杠
     *
     * @param actionResponse
     * @param card
     * @param response
     * @param redisService
     */
    public void selfGang(GameBase.BaseAction.Builder actionResponse, List<Integer> card, GameBase.BaseConnection.Builder response, RedisService redisService, int userId) {
        //碰或者杠
        seats.stream().filter(seat -> seat.getSeatNo() == operationSeatNo).forEach(seat -> {
            if (4 == Card.containSize(seat.getCards(), card.get(0))) {//暗杠
                Card.remove(seat.getCards(), card.get(0));
                Card.remove(seat.getCards(), card.get(0));
                Card.remove(seat.getCards(), card.get(0));
                Card.remove(seat.getCards(), card.get(0));

                seat.getAnGangCards().add(card.get(0));
                seat.getAnGangCards().add(card.get(0));
                seat.getAnGangCards().add(card.get(0));
                seat.getAnGangCards().add(card.get(0));

                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.AN_GANG);

                final int[] loseScore = {0};
                seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo() && 0 == seat1.getHuCard())
                        .forEach(seat1 -> {
                            seat1.getAnGangResult().add(new GameResult(scoreTypes, card.get(0), -3));
                            loseScore[0] += 3;
                        });
                seat.getAnGangResult().add(new GameResult(scoreTypes, card.get(0), loseScore[0]));

                seat.setAngang(seat.getAngang() + 1);
                historyList.add(new OperationHistory(seat.getUserId(), OperationHistoryType.AN_GANG, card.get(0)));

                actionResponse.setOperationId(GameBase.ActionId.AN_GANG).setData(Mahjong.CardsData.newBuilder().addAllCards(card).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                getCard(response, seat.getSeatNo(), redisService);
            } else if (3 == Card.containSize(seat.getPengCards(), card.get(0)) && 1 == Card.containSize(seat.getCards(), card.get(0))) {//扒杠
                Card.remove(seat.getCards(), card.get(0));
                Card.remove(seat.getPengCards(), card.get(0));
                Card.remove(seat.getPengCards(), card.get(0));
                Card.remove(seat.getPengCards(), card.get(0));

                seat.getMingGangCards().add(card.get(0));
                seat.getMingGangCards().add(card.get(0));
                seat.getMingGangCards().add(card.get(0));
                seat.getMingGangCards().add(card.get(0));

                List<ScoreType> scoreTypes = new ArrayList<>();
                scoreTypes.add(ScoreType.BA_GANG);

                final int[] loseScore = {0};
                seats.stream().filter(seat1 -> seat1.getSeatNo() != seat.getSeatNo() && 0 == seat1.getHuCard())
                        .forEach(seat1 -> {
                            seat1.getMingGangResult().add(new GameResult(scoreTypes, card.get(0), -1));
                            loseScore[0] += 1;
                        });
                seat.getMingGangResult().add(new GameResult(scoreTypes, card.get(0), loseScore[0]));

                seat.setMinggang(seat.getMinggang() + 1);
                historyList.add(new OperationHistory(seat.getUserId(), OperationHistoryType.BA_GANG, card.get(0)));

                actionResponse.setOperationId(GameBase.ActionId.BA_GANG).setData(Mahjong.CardsData.newBuilder().addAllCards(card).build().toByteString());
                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                        .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                checkCard(response, redisService, seat.getSeatNo());
            }
        });
    }

    /**
     * 出牌后检查是否有人能胡、杠、碰
     *
     * @param response
     * @param redisService
     * @param mopai
     */

    public void checkCard(GameBase.BaseConnection.Builder response, RedisService redisService, int mopai) {
        seats.forEach(seat1 -> seat1.setOperation(0));
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        builder.setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0);
        OperationHistory operationHistory = null;
        if (historyList.size() > 0) {
            operationHistory = historyList.get(historyList.size() - 1);
        }
        int card = operationHistory.getCards().get(0);
        //先检查胡，胡优先
        boolean cannotOperation = false;
        for (Seat seat : seats) {
            if (seat.getSeatNo() != operationSeatNo && 0 == seat.getHuCard()) {
                builder.clearOperationId();
                List<Integer> temp = new ArrayList<>();
                temp.addAll(seat.getCards());

                //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
                int containSize = Card.containSize(temp, card);

                if (3 == containSize) {
                    if (!seat.isJiao()) {
                        builder.addOperationId(GameBase.ActionId.PENG);
                    }
                    boolean baojiaohu = true;
                    if (seat.isJiao()) {
                        List<Integer> tempCards = new ArrayList<>();
                        tempCards.addAll(seat.getCards());
                        Card.remove(tempCards, card);
                        Card.remove(tempCards, card);
                        Card.remove(tempCards, card);

                        List<Integer> mingGangCards = new ArrayList<>();
                        mingGangCards.addAll(seat.getMingGangCards());
                        mingGangCards.add(card);
                        mingGangCards.add(card);
                        mingGangCards.add(card);
                        mingGangCards.add(card);

                        List<Integer> possibleCard = new ArrayList<>();
                        for (Seat seat1 : seats) {
                            possibleCard.addAll(seat1.getCards());
                            possibleCard.addAll(seat1.getAnGangCards());
                        }
                        Card.remove(possibleCard, card);
                        Card.remove(possibleCard, card);
                        Card.remove(possibleCard, card);
                        Card.remove(possibleCard, card);
                        possibleCard.addAll(surplusCards);

                        baojiaohu = 0 != MahjongUtil.tingScore(tempCards, seat.getPengCards(), mingGangCards, seat.getAnGangCards(), possibleCard, new ArrayList<>());
                    }
                    if (0 < surplusCards.size() && baojiaohu) {
                        builder.addOperationId(GameBase.ActionId.DIAN_GANG);
                    }
                } else if (2 == containSize && !seat.isJiao()) {
                    builder.addOperationId(GameBase.ActionId.PENG);
                }
                //当前玩家是否可以胡牌
                temp.add(card);
                if (MahjongUtil.checkHu(temp) && !seat.isOnlyZimo() && !seat.isCanNotHu()) {
                    if (4 > surplusCards.size()) {
                        seat.setOperation(1);
                        hu(seat.getUserId(), response, redisService);
                        return;
                    }
                    builder.addOperationId(GameBase.ActionId.HU);
                }
                if (0 != builder.getOperationIdCount()) {
                    if (redisService.exists("room_match" + roomNo)) {
                        new OperationTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService, false).start();
                    }
                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                        response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                    }
                    cannotOperation = true;
                }
            }
        }

        if (!cannotOperation && 0 != mopai) {
            //如果没有人可以胡、碰、杠，游戏继续，下家摸牌；
            getCard(response, mopai, redisService);
        }
    }

    /**
     * 重连时检查出牌后是否有人能胡、杠、碰
     *
     * @param card 当前出的牌
     * @param date
     */
    public void checkSeatCan(Integer card, GameBase.BaseConnection.Builder response, int userId, Date date, RedisService redisService) {
        GameBase.AskResponse.Builder builder = GameBase.AskResponse.newBuilder();
        int time = 0;
        if (redisService.exists("room_match" + roomNo)) {
            time = 8 - (int) ((new Date().getTime() - date.getTime()) / 1000);
        }
        builder.setTimeCounter(time > 0 ? time : 0);

        for (Seat seat : seats) {
            if (seat.getUserId() == userId && 0 == seat.getHuCard()) {
                builder.clearOperationId();
                List<Integer> temp = new ArrayList<>();
                temp.addAll(seat.getCards());

                //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
                int containSize = Card.containSize(temp, card);
                if (3 == containSize) {

                    if (!seat.isJiao()) {
                        builder.addOperationId(GameBase.ActionId.PENG);
                    }
                    boolean baojiaohu = true;
                    if (seat.isJiao()) {
                        List<Integer> tempCards = new ArrayList<>();
                        tempCards.addAll(seat.getCards());
                        Card.remove(tempCards, card);
                        Card.remove(tempCards, card);
                        Card.remove(tempCards, card);

                        List<Integer> mingGangCards = new ArrayList<>();
                        mingGangCards.addAll(seat.getMingGangCards());
                        mingGangCards.add(card);
                        mingGangCards.add(card);
                        mingGangCards.add(card);
                        mingGangCards.add(card);

                        List<Integer> possibleCard = new ArrayList<>();
                        for (Seat seat1 : seats) {
                            possibleCard.addAll(seat1.getCards());
                            possibleCard.addAll(seat1.getAnGangCards());
                        }
                        Card.remove(possibleCard, card);
                        Card.remove(possibleCard, card);
                        Card.remove(possibleCard, card);
                        Card.remove(possibleCard, card);
                        possibleCard.addAll(surplusCards);

                        baojiaohu = 0 != MahjongUtil.tingScore(tempCards, seat.getPengCards(), mingGangCards, seat.getAnGangCards(), possibleCard, new ArrayList<>());
                    }
                    if (0 < surplusCards.size() && baojiaohu) {
                        builder.addOperationId(GameBase.ActionId.DIAN_GANG);
                    }
                } else if (2 == containSize && !seat.isJiao()) {
                    builder.addOperationId(GameBase.ActionId.PENG);
                }
                //当前玩家是否可以胡牌
                temp.add(card);
                if (MahjongUtil.checkHu(temp) && !seat.isOnlyZimo() && !seat.isCanNotHu()) {
                    if (4 > surplusCards.size()) {
                        seat.setOperation(1);
                        hu(seat.getUserId(), response, redisService);
                        return;
                    }
                    builder.addOperationId(GameBase.ActionId.HU);
                }
                if (0 != builder.getOperationIdCount()) {
                    if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                        response.setOperationType(GameBase.OperationType.ASK).setData(builder.build().toByteString());
                        MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                    }
                }
                break;
            }
        }
    }

    /**
     * 当有人胡、碰、杠后，再次检查是否还有人胡、碰、杠
     */
    public boolean checkSurplus() {

        //找到那张牌
        OperationHistory operationHistory;
        if (historyList.size() > 0) {
            operationHistory = historyList.get(historyList.size() - 1);
        } else {
            return false;
        }
        final int card = operationHistory.getCards().get(0);

        final boolean[] hu = {false};
        //先检查胡，胡优先
        seats.stream().filter(seat -> seat.getSeatNo() != operationSeatNo && 0 == seat.getHuCard()).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card);
            if (MahjongUtil.checkHu(temp) && seat.getOperation() == 0) {
                hu[0] = true;
            }
        });
        return !hu[0];
    }

    /**
     * 检查是否还需要操作
     */
    public boolean passedChecked() {
        //找到那张牌
        OperationHistory operationHistory;
        if (historyList.size() > 0) {
            operationHistory = historyList.get(historyList.size() - 1);
        } else {
            return false;
        }
        final int card = operationHistory.getCards().get(0);
        final boolean[] hasNoOperation = {false};
        //先检查胡，胡优先
        seats.stream().filter(seat -> seat.getSeatNo() != operationSeatNo && 0 == seat.getHuCard()).forEach(seat -> {
            List<Integer> temp = new ArrayList<>();
            temp.addAll(seat.getCards());

            //当前玩家是否可以胡牌
            temp.add(card);
            if (MahjongUtil.checkHu(temp) && !seat.isCanNotHu()) {
                if (seat.getOperation() != 4) {
                    hasNoOperation[0] = true;
                } else if (seat.isJiao()) {
                    seat.setOnlyZimo(true);
                }
            }

            //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
            int containSize = Card.containSize(temp, card);
            if (4 == containSize && 0 < surplusCards.size() && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            } else if (3 <= containSize && seat.getOperation() != 4) {
                hasNoOperation[0] = true;
            }
        });

        return hasNoOperation[0];
    }

    /**
     * 检测单个玩家是否可以碰或者港
     *
     * @param actionResponse
     * @param response
     * @param redisService
     * @param userId
     */
    public void pengOrGang(GameBase.BaseAction.Builder actionResponse, GameBase.BaseConnection.Builder response, RedisService redisService, int userId) {
        //找到那张牌
        final Integer[] card = new Integer[1];
        Seat operationSeat = null;
        for (Seat seat : seats) {
            if (seat.getSeatNo() == operationSeatNo) {
                card[0] = seat.getPlayedCards().get(seat.getPlayedCards().size() - 1);
                operationSeat = seat;
                break;
            }
        }

        for (Seat seat : seats) {
            if (seat.getSeatNo() != operationSeatNo) {
                List<Integer> temp = new ArrayList<>();
                temp.addAll(seat.getCards());

                //当前玩家手里有几张牌，3张可碰可杠，两张只能碰
                int containSize = Card.containSize(temp, card[0]);
                if (3 == containSize && 0 < surplusCards.size() && seat.getOperation() == 2) {//杠牌
                    Card.remove(seat.getCards(), card[0]);
                    Card.remove(seat.getCards(), card[0]);
                    Card.remove(seat.getCards(), card[0]);
                    seat.getMingGangCards().add(card[0]);
                    seat.getMingGangCards().add(card[0]);
                    seat.getMingGangCards().add(card[0]);
                    seat.getMingGangCards().add(card[0]);

                    //添加结算
                    List<ScoreType> scoreTypes = new ArrayList<>();
                    scoreTypes.add(ScoreType.DIAN_GANG);

                    operationSeat.getMingGangResult().add(new GameResult(scoreTypes, card[0], -2));
                    seat.getMingGangResult().add(new GameResult(scoreTypes, card[0], 2));

                    seat.setMinggang(seat.getMinggang() + 1);
                    historyList.add(new OperationHistory(seat.getUserId(), OperationHistoryType.DIAN_GANG, card[0]));

                    operationSeat.getPlayedCards().remove(operationSeat.getPlayedCards().size() - 1);

                    actionResponse.setID(seat.getUserId()).setOperationId(GameBase.ActionId.DIAN_GANG).setData(Mahjong.CardsData.newBuilder().addCards(card[0]).build().toByteString());
                    response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

                    //点杠后需要摸牌
                    getCard(response, seat.getSeatNo(), redisService);
                    return;
                } else if (2 <= containSize && seat.getOperation() == 3) {//碰
                    Card.remove(seat.getCards(), card[0]);
                    Card.remove(seat.getCards(), card[0]);
                    seat.getPengCards().add(card[0]);
                    seat.getPengCards().add(card[0]);
                    seat.getPengCards().add(card[0]);
                    operationSeatNo = seat.getSeatNo();
                    historyList.add(new OperationHistory(seat.getUserId(), OperationHistoryType.PENG, card[0]));

                    operationSeat.getPlayedCards().remove(operationSeat.getPlayedCards().size() - 1);

                    actionResponse.setID(seat.getUserId()).setOperationId(GameBase.ActionId.PENG).setData(Mahjong.CardsData.newBuilder().addCards(card[0]).build().toByteString());
                    response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

                    if (redisService.exists("room_match" + roomNo)) {
                        new PlayCardTimeout(seat.getUserId(), roomNo, historyList.size(), gameCount, redisService).start();
                    }
                    GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(seat.getUserId())
                            .setTimeCounter(redisService.exists("room_match" + roomNo) ? 8 : 0).build();
                    response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());
                    seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                            .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));

                    seat.setCanNotHu(false);
                    return;
                }
            }
        }
    }

    public void start(GameBase.BaseConnection.Builder response, RedisService redisService) {
        gameCount = gameCount + 1;
        if (1 == gameRules % 2) {
            gameStatus = GameStatus.CHANGE_CARD;
        } else {
            gameStatus = GameStatus.BAOJIAO;
        }
        dealCard();
        //骰子
        int dice1 = new Random().nextInt(6) + 1;
        int dice2 = new Random().nextInt(6) + 1;
        dice = new Integer[]{dice1, dice2};
        Mahjong.MahjongStartResponse.Builder dealCard = Mahjong.MahjongStartResponse.newBuilder();
        dealCard.setBanker(banker).addDice(dice1).addDice(dice2);
        dealCard.setSurplusCardsSize(surplusCards.size());
        response.setOperationType(GameBase.OperationType.START);
        for (Seat seat : seats) {
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                dealCard.clearCards();
                dealCard.addAllCards(seat.getCards());
                response.setData(dealCard.build().toByteString());
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }

        if (1 == gameRules % 2) {
            GameBase.ProcessResponse processResponse = GameBase.ProcessResponse.newBuilder().setOperationId(GameBase.ActionId.CHANGE_CARD).build();
            response.clear();
            response.setOperationType(GameBase.OperationType.EXTRA_PROCESS).setData(processResponse.toByteString());
            for (Seat seat : seats) {
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
            }
        } else {
            checkBaojiao(response, redisService);
        }
    }

    public void playCard(Integer card, int userId, GameBase.BaseAction.Builder actionResponse, GameBase.BaseConnection.Builder response, RedisService redisService) {
        actionResponse.setID(userId);
        for (Seat seat : seats) {
            if (seat.getUserId() == userId) {

                if (operationSeatNo == seat.getSeatNo() && lastOperation != userId) {
                    if (seat.getCards().contains(card)) {
                        seat.getCards().remove(card);
                        if (null == seat.getPlayedCards()) {
                            seat.setPlayedCards(new ArrayList<>());
                        }
                        seat.getPlayedCards().add(card);
                        Mahjong.CardsData.Builder builder = Mahjong.CardsData.newBuilder().addCards(card);

                        actionResponse.setOperationId(GameBase.ActionId.PLAY_CARD).setData(builder.build().toByteString());

                        response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                        lastOperation = userId;
                        historyList.add(new OperationHistory(userId, OperationHistoryType.PLAY_CARD, card));
                        seats.stream().filter(seat1 -> MahjongTcpService.userClients.containsKey(seat1.getUserId()))
                                .forEach(seat1 -> MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId()));
                        //先检查其它三家牌，是否有人能胡、杠、碰
                        checkCard(response, redisService, getNextSeat(operationSeatNo));
                    } else {
                        System.out.println("用户手中没有此牌" + userId);
                    }
                } else {
                    System.out.println("不该当前玩家操作" + userId);
                }
                break;
            }
        }
    }

    public void sendRoomInfo(GameBase.RoomCardIntoResponse.Builder roomCardIntoResponseBuilder, GameBase.BaseConnection.Builder response, int userId) {
        Rongchang.RongchangMahjongIntoResponse.Builder intoResponseBuilder = Rongchang.RongchangMahjongIntoResponse.newBuilder();
        intoResponseBuilder.setCount(count);
        intoResponseBuilder.setGameTimes(gameTimes);
        intoResponseBuilder.setGameRules(gameRules);
        intoResponseBuilder.setAa(aa);
        roomCardIntoResponseBuilder.setGameType(GameBase.GameType.MAHJONG_RONGCHANG);
        roomCardIntoResponseBuilder.setError(GameBase.ErrorCode.SUCCESS);
        roomCardIntoResponseBuilder.setData(intoResponseBuilder.build().toByteString());
        response.setOperationType(GameBase.OperationType.ROOM_INFO).setData(roomCardIntoResponseBuilder.build().toByteString());
        if (MahjongTcpService.userClients.containsKey(userId)) {
            MahjongTcpService.userClients.get(userId).send(response.build(), userId);
        }
    }

    public void sendSeatInfo(GameBase.BaseConnection.Builder response) {
        GameBase.RoomSeatsInfo.Builder roomSeatsInfo = GameBase.RoomSeatsInfo.newBuilder();
        for (Seat seat1 : seats) {
            GameBase.SeatResponse.Builder seatResponse = GameBase.SeatResponse.newBuilder();
            seatResponse.setSeatNo(seat1.getSeatNo());
            seatResponse.setID(seat1.getUserId());
            seatResponse.setScore(seat1.getScore());
            seatResponse.setReady(seat1.isReady());
            seatResponse.setNickname(seat1.getNickname());
            seatResponse.setHead(seat1.getHead());
            seatResponse.setSex(seat1.isSex());
            seatResponse.setOffline(seat1.isRobot());
            seatResponse.setIsRobot(seat1.isRobot());
            seatResponse.setIp(seat1.getIp());
            seatResponse.setGameCount(seat1.getGameCount());
            if (null != seat1.getGps()) {
                seatResponse.setGps(seat1.getGps());
            }
            roomSeatsInfo.addSeats(seatResponse.build());
        }
        response.setOperationType(GameBase.OperationType.SEAT_INFO).setData(roomSeatsInfo.build().toByteString());
        for (Seat seat : seats) {
            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
            }
        }
    }

    public void changeCard(int userId, List<Integer> cardsList, GameBase.BaseConnection.Builder response, GameBase.BaseAction.Builder actionResponse, RedisService redisService) {
        if (cardsList.get(0) / 10 == cardsList.get(1) / 10 && cardsList.get(0) / 10 == cardsList.get(2) / 10) {
            boolean allChange = true;
            actionResponse.setOperationId(GameBase.ActionId.CHANGE_CARD);
            for (Seat seat : seats) {
                if (seat.getUserId() == userId && 0 == seat.getChangeOutCards().size() && Card.containAll(seat.getCards(), cardsList)) {
                    seat.getChangeOutCards().addAll(cardsList);
                    for (Seat seat1 : seats) {
                        if (MahjongTcpService.userClients.containsKey(seat1.getUserId())) {
                            if (seat1.getUserId() == userId) {
                                actionResponse.setData(Mahjong.CardsData.newBuilder().addAllCards(cardsList).build().toByteString());
                            } else {
                                actionResponse.setData(Mahjong.CardsData.newBuilder().addCards(0).addCards(0).addCards(0).build().toByteString());
                            }
                            response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                            MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId());
                        }
                    }
                } else if (0 == seat.getChangeOutCards().size()) {
                    allChange = false;
                }
            }
            if (allChange) {
                List<Integer> temps = new ArrayList<>();
                for (Seat seat : seats) {
                    temps.addAll(seat.getChangeOutCards());
                    Card.removeAll(seat.getCards(), seat.getChangeOutCards());
                }
                actionResponse.setOperationId(GameBase.ActionId.CHANGE_CARD_CONFIRM);
                Mahjong.ChangeCardResponse.Builder changeCardResponse = Mahjong.ChangeCardResponse.newBuilder();
                if (new Random().nextBoolean()) {
                    changeDice = 2;
                    for (Seat seat : seats) {
                        List<Integer> changeCard = new ArrayList<>();
                        if (1 == seat.getSeatNo()) {
                            changeCard.addAll(temps.subList(temps.size() - 3, temps.size()));
                            seat.getChangeInCards().addAll(changeCard);
                            temps.remove(temps.size() - 1);
                            temps.remove(temps.size() - 1);
                            temps.remove(temps.size() - 1);
                        } else {
                            changeCard.addAll(temps.subList(0, 3));
                            seat.getChangeInCards().addAll(changeCard);
                            temps.remove(0);
                            temps.remove(0);
                            temps.remove(0);
                        }
                        seat.getCards().addAll(changeCard);
                        if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                            changeCardResponse.setDice(2);
                            changeCardResponse.clearCards();
                            changeCardResponse.addAllCards(changeCard);
                            actionResponse.setID(seat.getUserId()).setData(changeCardResponse.build().toByteString());
                            response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                            MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                        }
                    }
                } else {
                    changeDice = 1;
                    for (Seat seat : seats) {
                        if (temps.size() < 6) {
                            seat.getCards().addAll(temps);
                            seat.getChangeInCards().addAll(temps);
                            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                changeCardResponse.setDice(1);
                                changeCardResponse.clearCards();
                                changeCardResponse.addAllCards(temps);
                                actionResponse.setID(seat.getUserId()).setData(changeCardResponse.build().toByteString());
                                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                            }
                        } else {
                            List<Integer> changeCard = new ArrayList<>();
                            changeCard.addAll(temps.subList(3, 6));
                            seat.getChangeInCards().addAll(changeCard);
                            seat.getCards().addAll(changeCard);
                            temps.remove(3);
                            temps.remove(3);
                            temps.remove(3);
                            if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                                changeCardResponse.setDice(1);
                                changeCardResponse.clearCards();
                                changeCardResponse.addAllCards(changeCard);
                                actionResponse.setID(seat.getUserId()).setData(changeCardResponse.build().toByteString());
                                response.setOperationType(GameBase.OperationType.ACTION).setData(actionResponse.build().toByteString());
                                MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                            }
                        }
                    }
                }
                gameStatus = GameStatus.BAOJIAO;
                checkBaojiao(response, redisService);
            }
        }
    }

    private void checkBaojiao(GameBase.BaseConnection.Builder response, RedisService redisService) {
        boolean hasJiao = false;
        if (0 == gameStatus.compareTo(GameStatus.BAOJIAO)) {
            GameBase.ProcessResponse processResponse = GameBase.ProcessResponse.newBuilder().setOperationId(GameBase.ActionId.BAO_JIAO).build();
            response.clear();
            response.setOperationType(GameBase.OperationType.EXTRA_PROCESS).setData(processResponse.toByteString());
            for (Seat seat : seats) {
                if (seat.getUserId() != banker) {
                    if (MahjongUtil.ting(seat.getCards(), MahjongUtil.getComputePossible(seat.getCards(), 1))) {
                        hasJiao = true;
                        if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                            MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                        }
                    } else {
                        seat.setBaojiao(true);
                    }
                } else {
                    seat.setBaojiao(true);
                }
            }
        }
        if (!hasJiao) {
            gameStatus = GameStatus.PLAYING;
            Seat operationSeat = null;
            GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(banker).build();
            response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());

            for (Seat seat : seats) {
                if (operationSeatNo == seat.getSeatNo()) {
                    operationSeat = seat;
                }
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
            }
            checkSelfGetCard(response, operationSeat, redisService);
        }
    }

    public void baojiao(int userId, boolean bao, GameBase.BaseConnection.Builder response, GameBase.BaseAction actionRequest, RedisService redisService) {
        boolean hasNotBaojiao = false;
        for (Seat seat : seats) {
            if (userId == seat.getUserId() && !seat.isBaojiao()) {
                seat.setBaojiao(true);
                seat.setJiao(bao);
                if (bao) {
                    GameBase.BaseAction.Builder action = GameBase.BaseAction.newBuilder().setID(userId).setOperationId(GameBase.ActionId.BAO_JIAO);
                    response.setOperationType(GameBase.OperationType.ACTION).setData(action.build().toByteString());
                    for (Seat seat1 : seats) {
                        if (MahjongTcpService.userClients.containsKey(seat1.getUserId())) {
                            MahjongTcpService.userClients.get(seat1.getUserId()).send(response.build(), seat1.getUserId());
                        }
                    }
                }
            } else if (!seat.isBaojiao()) {
                hasNotBaojiao = true;
            }
        }
        if (!hasNotBaojiao) {
            gameStatus = GameStatus.PLAYING;
            Seat operationSeat = null;
            GameBase.RoundResponse roundResponse = GameBase.RoundResponse.newBuilder().setID(banker).build();
            response.setOperationType(GameBase.OperationType.ROUND).setData(roundResponse.toByteString());

            for (Seat seat : seats) {
                if (operationSeatNo == seat.getSeatNo()) {
                    operationSeat = seat;
                }
                if (MahjongTcpService.userClients.containsKey(seat.getUserId())) {
                    MahjongTcpService.userClients.get(seat.getUserId()).send(response.build(), seat.getUserId());
                }
            }
            checkSelfGetCard(response, operationSeat, redisService);
        }
    }
}

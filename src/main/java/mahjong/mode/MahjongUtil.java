package mahjong.mode;

import java.util.*;

/**
 * Author pengyi
 * Date 17-2-14.
 */

public class MahjongUtil {

    List<Integer> cards = new ArrayList<>();

    public static List<Integer> dealIntegers(List<Integer> allIntegers) {
        List<Integer> cardList = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            int cardIndex = (int) (Math.random() * allIntegers.size());
            cardList.add(allIntegers.get(cardIndex));
            allIntegers.remove(cardIndex);
        }
        return cardList;
    }

    public static List<Integer> get_dui(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        List<Integer> dui_arr = new ArrayList<>();
        if (cards.size() >= 2) {
            for (int i = 0; i < cards.size() - 1; i++) {
                if (cards.get(i).intValue() == cardList.get(i + 1).intValue()) {
                    dui_arr.add(cards.get(i));
                    dui_arr.add(cards.get(i));
                    i++;
                }
            }
        }
        return dui_arr;
    }

    public static List<Integer> get_san(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        List<Integer> san_arr = new ArrayList<>();
        if (cards.size() >= 3) {
            for (int i = 0; i < cards.size() - 2; i++) {
                if (cards.get(i).intValue() == cards.get(i + 2).intValue()) {
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    i += 2;
                }
            }
        }
        return san_arr;
    }

    public static List<Integer> get_si(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        List<Integer> san_arr = new ArrayList<>();
        if (cards.size() >= 4) {
            cards.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return o1.compareTo(o2);
                }
            });
            for (int i = 0; i < cards.size() - 3; i++) {
                if (cards.get(i).intValue() == cards.get(i + 3).intValue()) {
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    san_arr.add(cards.get(i));
                    i += 3;
                }
            }
        }
        return san_arr;
    }

    public static List<Integer> get_shun(List<Integer> cardList) {
        List<Integer> cards = new ArrayList<>();
        cards.addAll(cardList);
        cards.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        List<Integer> sun_arr = new ArrayList<>();
        List<Integer> temp = new ArrayList<>();
        temp.addAll(cards);
        while (temp.size() > 2) {
            boolean find = false;
            for (int i = 0; i < temp.size() - 2; i++) {
                int start = temp.get(i);
                if (temp.get(i) < 30) {
                    if (temp.contains(start + 1) && temp.contains(start + 2)) {
                        sun_arr.add(start);
                        sun_arr.add(start + 1);
                        sun_arr.add(start + 2);
                        temp.remove(Integer.valueOf(start));
                        temp.remove(Integer.valueOf(start + 1));
                        temp.remove(Integer.valueOf(start + 2));
                        find = true;
                        break;
                    }
                }
            }
            if (!find) {
                break;
            }
        }
        return sun_arr;
    }

    public static boolean ting(List<Integer> userCards, List<Integer> possible) {
        List<Integer> temp = new ArrayList<>();
        temp.addAll(userCards);
        for (Integer card : possible) {
            temp.clear();
            temp.addAll(userCards);
            temp.add(card);
            if (checkHu(temp)) {
                return true;
            }
        }
        return false;
    }

    public static Integer checkGang(List<Integer> cards) {
        List<Integer> cardList = new ArrayList<>();
        cardList.addAll(cards);
        cardList.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        for (int i = 0; i < cardList.size() - 3; i++) {
            if (cardList.get(i).intValue() == cardList.get(i + 3)) {
                return cardList.get(i);
            }
        }
        return null;
    }

    public static Integer checkBaGang(List<Integer> cards, List<Integer> cardList) {
        for (Integer card : cardList) {
            for (Integer card1 : cards) {
                if (card.intValue() == card1) {
                    return card;
                }
            }
        }
        return null;
    }

    /**
     * 牌型
     *
     * @param cards     手牌
     * @param pengCards 碰的牌
     * @param gangCard  杠的牌
     * @return
     */
    public static List<ScoreType> getHuType(List<Integer> cards, List<Integer> pengCards, List<Integer> gangCard, int card) {
        List<ScoreType> scoreTypes = new ArrayList<>();

        List<Integer> cardList = new ArrayList<>();
        cardList.addAll(cards);
        cardList.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });

        //归
        if (3 == Card.containSize(pengCards, card)) {
            scoreTypes.add(ScoreType.GUI);
        }
        //手归
        if (4 == Card.containSize(cards, card)) {
            scoreTypes.add(ScoreType.SHOUGUI);
        }

        List<Integer> allCard = new ArrayList<>();
        allCard.addAll(cards);
        allCard.addAll(pengCards);
        allCard.addAll(gangCard);
        //清一色
        if (!Card.hasSameColor(allCard, 1) || !Card.hasSameColor(allCard, 2)) {
            scoreTypes.add(ScoreType.QINGYISE);
        }
        //七对、拢七对
        if (14 == get_dui(cardList).size()) {
            if (scoreTypes.contains(ScoreType.SHOUGUI)) {
                scoreTypes.remove(ScoreType.SHOUGUI);
                scoreTypes.add(ScoreType.LONGQIDUI);
            } else {
                scoreTypes.add(ScoreType.QIDUI);
            }
        }

        //大对、金勾掉
        if (cardList.size() == 2) {
            scoreTypes.add(ScoreType.JINGOUDIAO);
        } else {
            List<Integer> san = get_san(cardList);
            Card.removeAll(cardList, san);
            if (cardList.size() == 2 && cardList.get(0).intValue() == cardList.get(1)) {
                scoreTypes.add(ScoreType.DADUI);
            }
        }

        return scoreTypes;
    }

    /**
     * 算分
     *
     * @param scoreTypes
     * @return
     */
    public static int getScore(List<ScoreType> scoreTypes) {
        int score = 0;
        for (ScoreType scoreType : scoreTypes) {
            switch (scoreType) {
                case PINGHU:
                    score = 1;
                    break;
                case ZIMOHU:
                    score += 1;
                    break;
                case GUI:
                    score += 4;
                    break;
                case SHOUGUI:
                    score += 6;
                    break;
                case TIANHU:
                    score += 10;
                    break;
                case DIHU:
                    score += 4;
                    break;
                case BAOJIAO:
                    score += 6;
                    break;
                case DADUI:
                    score += 4;
                    break;
                case QINGYISE:
                    score += 6;
                    break;
                case QIDUI:
                    score += 6;
                    break;
                case LONGQIDUI:
                    score += 10;
                    break;
                case JINGOUDIAO:
                    score += 10;
                    break;
                case GANGSHANGPAO:
                    score += 4;
                    break;
                case GANGSHANGHUA:
                    score += 4;
                    break;
                case QIANGGANG:
                    score += 4;
                    break;
            }
        }
        return score;
    }

    public static ArrayList<Integer> getComputePossible(List<Integer> hand_list, int number) {
        Set<Integer> ret = new HashSet<>();
        for (int i = 0; i < hand_list.size(); i++) {
            int mahjong = hand_list.get(i);
            if (!ret.contains(mahjong)) {
                ret.add(mahjong);
            }
            int stepNum = 1;
            do {
                if (!ret.contains(mahjong - stepNum) && Card.legal(mahjong - stepNum)) {
                    ret.add(mahjong - stepNum);
                }
                if (!ret.contains(mahjong + stepNum) && Card.legal(mahjong + stepNum)) {
                    ret.add(mahjong + stepNum);
                }
                stepNum++;
            } while (stepNum <= number);
        }
        ArrayList<Integer> cards = new ArrayList<>();
        cards.addAll(ret);
        return cards;
    }

    /**
     * 传入14张牌，判断是否可胡牌
     *
     * @param cardList
     * @return
     */
    public static boolean checkHu(List<Integer> cardList) {
        List<Integer> handVals = new ArrayList<>();
        handVals.addAll(cardList);
        handVals.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });

        List<Integer> pairs = get_dui(handVals);

        //检查七对
        if (pairs.size() == 14) {
            return true;
        }

        for (int i = 0; i < pairs.size(); i += 2) {
            int md_val = pairs.get(i);
            List<Integer> hand = new ArrayList<>(handVals);
            hand.remove(Integer.valueOf(md_val));
            hand.remove(Integer.valueOf(md_val));
            if (CheckLug(hand)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean CheckLug(List<Integer> handVals) {
        if (handVals.size() == 0) return true;
        int md_val = handVals.get(0);
        handVals.remove(0);
        if (Card.containSize(handVals, md_val) == 2) {
            handVals.remove(Integer.valueOf(md_val));
            handVals.remove(Integer.valueOf(md_val));
            return CheckLug(handVals);
        } else {
            if (handVals.contains(md_val + 1) && handVals.contains(md_val + 2)) {
                handVals.remove(Integer.valueOf(md_val + 1));
                handVals.remove(Integer.valueOf(md_val + 2));
                return CheckLug(handVals);
            }
        }
        return false;
    }

    public static int tingScore(List<Integer> cards, List<Integer> pengCards, List<Integer> mingGangCards,
                                List<Integer> anGangCards, List<Integer> possibleCards, List<ScoreType> scoreTypes) {
        int tingScore = 0;
        List<Integer> possibles = MahjongUtil.getComputePossible(cards, 1);

        List<Integer> myPossibleCards = new ArrayList<>();
        myPossibleCards.addAll(possibleCards);
        Card.removeAll(myPossibleCards, cards);
        Card.removeAll(myPossibleCards, anGangCards);

        List<Integer> cantPossibles = new ArrayList<>();
        for (Integer possible : possibles) {
            if (0 == Card.containSize(myPossibleCards, possible)) {
                cantPossibles.add(possible);
            }
        }
        Card.removeAll(possibles, cantPossibles);
        List<Integer> temp = new ArrayList<>();
        temp.addAll(cards);
        List<Integer> gangCards = new ArrayList<>();
        gangCards.addAll(mingGangCards);
        gangCards.addAll(anGangCards);
        for (Integer i : possibles) {
            System.out.println(i);
            temp.add(i);
            if (checkHu(temp)) {
                System.out.println("可胡" + i);
                List<ScoreType> cardScoreType = new ArrayList<>();
                cardScoreType.addAll(scoreTypes);
                cardScoreType.addAll(getHuType(temp, pengCards, gangCards, i));
                if (0 == cardScoreType.size()) {
                    cardScoreType.add(ScoreType.PINGHU);
                }
                int score = getScore(cardScoreType);
                if (score > tingScore) {
                    tingScore = score;
                }
            }
            Card.remove(temp, i);
        }
        System.out.println("听分" + tingScore);
        return tingScore;
    }
}

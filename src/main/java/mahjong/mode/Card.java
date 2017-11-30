package mahjong.mode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pengyi
 * Date : 16-6-12.
 */
public class Card {
    public static int containSize(List<Integer> cardList, Integer containCard) {
        int size = 0;
        for (Integer card : cardList) {
            if (card.intValue() == containCard) {
                size++;
            }
        }
        return size;
    }

    public static List<Integer> getAllCard() {
        return new ArrayList<>(
                Arrays.asList(
                        11, 11, 11, 11,
                        12, 12, 12, 12,
                        13, 13, 13, 13,
                        14, 14, 14, 14,
                        15, 15, 15, 15,
                        16, 16, 16, 16,
                        17, 17, 17, 17,
                        18, 18, 18, 18,
                        19, 19, 19, 19,
                        21, 21, 21, 21,
                        22, 22, 22, 22,
                        23, 23, 23, 23,
                        24, 24, 24, 24,
                        25, 25, 25, 25,
                        26, 26, 26, 26,
                        27, 27, 27, 27,
                        28, 28, 28, 28,
                        29, 29, 29, 29
                ));
    }

    public static boolean containAll(List<Integer> cardList, List<Integer> cards) {

        for (Integer card : cards) {
            if (!cardList.contains(card)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 有相同颜色的牌
     *
     * @param color
     * @return
     */
    public static boolean hasSameColor(List<Integer> cardList, int color) {

        List<Integer> cards = getAllSameColor(color);
        for (Integer card : cardList) {
            if (cards.contains(card)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取相同颜色的所有牌
     *
     * @param color
     * @return
     */
    public static List<Integer> getAllSameColor(int color) {

        switch (color) {
            case 1:
                return Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19);
            case 2:
                return Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29);
        }
        return null;
    }

    public static boolean legal(int card) {
        return getAllCard().contains(card);
    }

    public static void remove(List<Integer> cards, Integer card) {
        for (Integer card1 : cards) {
            if (card1.intValue() == card) {
                cards.remove(card1);
                return;
            }
        }
    }

    public static void removeAll(List<Integer> cards, List<Integer> removes) {
        for (Integer card : removes) {
            for (Integer card1 : cards) {
                if (card1.intValue() == card) {
                    cards.remove(card1);
                    break;
                }
            }
        }
    }
}

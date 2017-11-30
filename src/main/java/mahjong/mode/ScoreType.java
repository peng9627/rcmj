package mahjong.mode;

/**
 * Created by pengyi
 * Date : 16-6-12.
 */
public enum ScoreType {

    BA_GANG("扒杠", 1),
    AN_GANG("暗杠", 2),
    DIAN_GANG("点杠", 3),
    PINGHU("平胡", 4),
    ZIMOHU("自摸", 5),
    GUI("归", 6),
    SHOUGUI("手归", 7),
    TIANHU("天胡", 8),
    DIHU("地胡", 9),
    BAOJIAO("报叫", 10),
    DADUI("大对", 11),
    QINGYISE("清一色", 12),
    QIDUI("七对", 13),
    LONGQIDUI("拢七对", 14),
    JINGOUDIAO("金勾掉", 15),
    GANGSHANGPAO("杠上炮", 16),
    GANGSHANGHUA("杠上花", 17),
    QIANGGANG("抢杠", 18);

    private String name;
    private Integer values;

    ScoreType(String name, Integer values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getValues() {
        return values;
    }

    public void setValues(Integer values) {
        this.values = values;
    }
}

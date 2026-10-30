package com.example.aichat.util;

/**
 * 精灵养成数值曲线纯函数（算法升级 4）。
 *
 * <p>现状为线性变化，升级为平滑曲线：
 * <ul>
 *   <li>心情 / 饱腹：指数平滑衰减（非每步等量）{@link #smoothDecay}</li>
 *   <li>睡眠恢复：前 2 小时快恢复，之后边际递减（对数曲线）{@link #sleepRecovery}</li>
 *   <li>亲密度：对话次数对数增长（前期快、后期慢）{@link #logarithmicGrowth}</li>
 * </ul>
 * 全部静态纯函数，无 Spring 依赖。
 */
public final class ElfCurve {

    /** 亲密度达到上限所需的交互次数（对数饱和点）。 */
    public static final int INTIMACY_SATURATE_COUNT = 100;
    /** 睡眠恢复饱和所需小时数（8 小时睡满）。 */
    public static final double SLEEP_SATURATE_HOURS = 8.0;

    private ElfCurve() {
    }

    /**
     * 指数平滑衰减：心情/饱腹随时间平滑下降，而非每步等量。
     *
     * @param current 当前值（0~max）
     * @param max     上限
     * @param rate    衰减速率（&gt;0，越大掉得越快）
     * @param time    经过的时间步
     * @return 衰减后的值（夹取在 [0, max]）
     */
    public static double smoothDecay(double current, double max, double rate, double time) {
        double hi = max <= 0 ? current : max;
        double value = Math.max(0.0, current) * Math.exp(-Math.max(0.0, rate) * Math.max(0.0, time));
        return Math.min(hi, value);
    }

    /**
     * 睡眠恢复量（对数曲线）：前 2 小时快恢复，之后边际递减，8 小时恢复满。
     *
     * @param hours 已睡眠小时数
     * @param max   满血/满状态值
     * @return 恢复后的状态值（夹取在 [0, max]）
     */
    public static double sleepRecovery(double hours, double max) {
        double h = Math.max(0.0, hours);
        double ratio = Math.log(1.0 + h) / Math.log(1.0 + SLEEP_SATURATE_HOURS);
        ratio = Math.min(1.0, Math.max(0.0, ratio));
        return (max <= 0 ? 0 : max) * ratio;
    }

    /**
     * 亲密度对数增长：前期对话涨得快，后期边际递减，count=100 次时达到 max。
     *
     * @param current 当前亲密度（保留参数位，便于扩展；累计值由 count 决定）
     * @param max     亲密度上限
     * @param count   累计对话/交互次数
     * @return 该次数下应达到的亲密度（夹取在 [0, max]）
     */
    public static double logarithmicGrowth(double current, double max, int count) {
        double c = Math.max(0, count);
        double ratio = Math.log(1.0 + c) / Math.log(1.0 + INTIMACY_SATURATE_COUNT);
        ratio = Math.min(1.0, Math.max(0.0, ratio));
        double target = (max <= 0 ? 0 : max) * ratio;
        // 不允许回退到比当前更低
        return Math.max(current, target);
    }
}

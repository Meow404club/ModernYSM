package rip.ysm.legacy122;

/**
 * 1.7.10 线 Mth 等价面（legacy1710-l2a-model-load）。
 *
 * 包名说明：共享 molang math 六文件（Clamp/Sin/Cos/Floor/Trunc/HermitBlend）的
 * <1.14 条件分支硬编码 import rip.ysm.legacy122.Mth——包名是共享解析链的既定
 * 契约点（stonecutter 只剥条件不换包名），1710 线在本包名下提供同名 shim。
 *
 * 共享 molang math 包消费五方法：sin/cos 在 1.7.10 vanilla MathHelper 同签名
 *（vanilla-mc-1710 net/minecraft/util/MathHelper.java:9/:13 MCP 实证）；clamp 无
 * 同名泛化面→clamp_float（:74）；floor/ceil 是 Math.floor/ceil 直通（1.12.2 线
 * Mth.java 同语义）。
 */
public final class Mth {

    private Mth() {
    }

    public static float sin(float value) {
        return net.minecraft.util.MathHelper.sin(value);
    }

    public static float cos(float value) {
        return net.minecraft.util.MathHelper.cos(value);
    }

    public static float clamp(float value, float min, float max) {
        return net.minecraft.util.MathHelper.clamp_float(value, min, max);
    }

    public static int floor(double value) {
        return (int) Math.floor(value);
    }

    public static int ceil(double value) {
        return (int) Math.ceil(value);
    }
}

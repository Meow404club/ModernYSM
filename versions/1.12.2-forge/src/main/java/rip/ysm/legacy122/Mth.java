package rip.ysm.legacy122;

/**
 * 1.12.2 线 Mth 等价面（legacy-1222-l2-full）。
 *
 * 共享 molang math 包消费 net.minecraft.util.Mth（mojmap 1.14+）五方法：
 * sin/cos/clamp 在 1.12.2 vanilla MathHelper 同签名（tmp/refs/vanilla-mc/1.12.2
 * net/minecraft/util/math/MathHelper.java:15/:19/:80 MCP 实证）；floor/ceil 是
 * Math.floor/ceil 直通（vanilla Mth.floor=(int)Math.floor 同实现语义）。
 */
public final class Mth {

    private Mth() {
    }

    public static float sin(float value) {
        return net.minecraft.util.math.MathHelper.sin(value);
    }

    public static float cos(float value) {
        return net.minecraft.util.math.MathHelper.cos(value);
    }

    public static float clamp(float value, float min, float max) {
        return net.minecraft.util.math.MathHelper.clamp(value, min, max);
    }

    public static int floor(double value) {
        return (int) Math.floor(value);
    }

    public static int ceil(double value) {
        return (int) Math.ceil(value);
    }
}

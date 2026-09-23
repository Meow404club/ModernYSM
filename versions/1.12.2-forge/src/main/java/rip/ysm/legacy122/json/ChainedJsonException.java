package rip.ysm.legacy122.json;

/**
 * 1.12.2 无 {@code net.minecraft.server.ChainedJsonException}（1.14 起文件 JSON 栈才有）。
 * 同简单名 shim 供 JsonAnimationUtils 的 stonecutter import 缝交换（正文零改动）；
 * 面取最小集=唯一使用点 (String) 构造（JsonAnimationUtils.java:54 实证），
 * checked Exception 对齐原版 throws 语义。
 */
public class ChainedJsonException extends Exception {
    private static final long serialVersionUID = 1L;

    public ChainedJsonException(String message) {
        super(message);
    }
}

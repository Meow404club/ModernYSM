package rip.ysm.util;

/**
 * Java 8 兼容的字符串工具（替代 Java 11+ 的 String.isBlank）。
 * <p>
 * 无分配实现：纯 char 扫描，不产生任何中间对象（注意不要退化成
 * trim().isEmpty()——trim 在空白串场景会分配新 String）。
 */
public final class YsmStrings {
    private YsmStrings() {
    }

    public static boolean isBlank(CharSequence value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** 替代 Java 17 的 HexFormat.of().formatHex(byte[])：小写十六进制、无分隔符。 */
    public static String formatHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = hex[v >>> 4];
            out[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(out);
    }
}

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
}

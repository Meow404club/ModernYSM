package rip.ysm.pinyin;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PinyinMatcher {
    private static final HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
    private static final Map<String, Forms> cache = new ConcurrentHashMap<>();

    static {
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private PinyinMatcher() {
    }

    public static int indexOf(String value, String query) {
        if (query.isEmpty()) {
            return 0;
        }
        int direct = value.toLowerCase(Locale.ENGLISH).indexOf(query);
        if (direct >= 0) {
            return direct;
        }
        if (!containsChinese(value)) {
            return -1;
        }
        Forms forms = formsOf(value);
        int full = forms.full.indexOf(query);
        if (full >= 0) {
            return forms.fullSourceIndex[full];
        }
        int initials = forms.initials.indexOf(query);
        if (initials >= 0) {
            return forms.initialsSourceIndex[initials];
        }
        return -1;
    }

    public static boolean contains(String value, String query) {
        return indexOf(value, query) >= 0;
    }

    private static boolean containsChinese(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) >= 0x4E00 && value.charAt(i) <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }

    private static Forms formsOf(String value) {
        Forms cached = cache.get(value);
        if (cached != null) {
            return cached;
        }
        Forms forms = build(value);
        if (cache.size() >= 4096) {
            cache.clear();
        }
        cache.put(value, forms);
        return forms;
    }

    private static Forms build(String value) {
        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        int[] fullIndex = new int[value.length() * 8];
        int[] initialsIndex = new int[value.length() * 2];
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            String syllable = null;
            if (ch >= 0x4E00 && ch <= 0x9FFF) {
                try {
                    String[] candidates = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                    if (candidates != null && candidates.length > 0) {
                        syllable = candidates[0];
                    }
                } catch (Exception ignored) {
                }
            }
            if (syllable == null || syllable.isEmpty()) {
                syllable = String.valueOf(Character.toLowerCase(ch));
            }
            for (int j = 0; j < syllable.length(); j++) {
                if (full.length() < fullIndex.length) {
                    fullIndex[full.length()] = i;
                    full.append(syllable.charAt(j));
                }
            }
            if (initials.length() < initialsIndex.length) {
                initialsIndex[initials.length()] = i;
                initials.append(syllable.charAt(0));
            }
        }
        return new Forms(full.toString(), initials.toString(), fullIndex, initialsIndex);
    }

    private static final class Forms {
        final String full;
        final String initials;
        final int[] fullSourceIndex;
        final int[] initialsSourceIndex;

        Forms(String full, String initials, int[] fullSourceIndex, int[] initialsSourceIndex) {
            this.full = full;
            this.initials = initials;
            this.fullSourceIndex = fullSourceIndex;
            this.initialsSourceIndex = initialsSourceIndex;
        }

        String full() {
            return this.full;
        }

        String initials() {
            return this.initials;
        }

        int[] fullSourceIndex() {
            return this.fullSourceIndex;
        }

        int[] initialsSourceIndex() {
            return this.initialsSourceIndex;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Forms)) {
                return false;
            }
            Forms other = (Forms) obj;
            return Objects.equals(this.full, other.full) && Objects.equals(this.initials, other.initials)
                    && Objects.equals(this.fullSourceIndex, other.fullSourceIndex)
                    && Objects.equals(this.initialsSourceIndex, other.initialsSourceIndex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.full, this.initials, this.fullSourceIndex, this.initialsSourceIndex);
        }

        @Override
        public String toString() {
            return "Forms[full=" + this.full + ", initials=" + this.initials + ", fullSourceIndex="
                    + this.fullSourceIndex + ", initialsSourceIndex=" + this.initialsSourceIndex + "]";
        }
    }
}

package cn.ninanina.wushan.common.util;

import lombok.SneakyThrows;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import org.apache.commons.lang3.CharUtils;

import java.util.Random;

/**
 * 杂七杂八的工具方法
 */
public class CommonUtil {
    /**
     * 从一串数字、其他字符的混合字符串中解析出整数
     */
    public static int parseInt(String s) {
        StringBuilder builder = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (CharUtils.isAsciiNumeric(c)) builder.append(c);
        }
        return Integer.parseInt(builder.toString());
    }

    @SneakyThrows
    public static String getPinyin(String word) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
                stringBuilder.append(PinyinHelper.toHanyuPinyinStringArray(c, format)[0]);
            } else {
                stringBuilder.append(c);
            }
        }
        return stringBuilder.toString();
    }

    public static boolean videoSrcValid(String src) {
        long currentSeconds = System.currentTimeMillis() / 1000;
        long urlSeconds = Long.parseLong(src.substring(src.indexOf("?e=") + 3, src.indexOf("&h="))) - 1800;
        return currentSeconds < urlSeconds;
    }
}

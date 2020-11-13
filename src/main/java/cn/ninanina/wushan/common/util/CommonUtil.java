package cn.ninanina.wushan.common.util;

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
}

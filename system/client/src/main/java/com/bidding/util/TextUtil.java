package com.bidding.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Shared text helpers for client-side filtering and display.
 */
public final class TextUtil {

    private TextUtil() {
    }

    public static String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }

        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}

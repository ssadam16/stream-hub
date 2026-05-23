package com.technokratos.taskservice.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES = Pattern.compile("(^-+|-+$)");
    private static final int MAX_LEN = 64;

    private SlugUtil() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Cannot slugify blank string");
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("-").toLowerCase(Locale.ROOT);
        slug = EDGES.matcher(slug).replaceAll("");
        if (slug.length() > MAX_LEN) {
            slug = slug.substring(0, MAX_LEN);
            slug = EDGES.matcher(slug).replaceAll("");
        }
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Slugified result is empty for input: " + input);
        }
        return slug;
    }
}

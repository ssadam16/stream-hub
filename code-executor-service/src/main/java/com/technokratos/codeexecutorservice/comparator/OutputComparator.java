package com.technokratos.codeexecutorservice.comparator;

import java.util.Comparator;

public class OutputComparator {

    public static boolean compare(String actual, String expected) {
        return actual.trim().equals(expected.trim());
    }

    public static boolean compareIgnoreWhitespace(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    public static boolean compareWithEpsilon(String actual, String expected, double epsilon) {
        try {
            double a = Double.parseDouble(actual.trim());
            double e = Double.parseDouble(expected.trim());
            return Math.abs(a - e) <= epsilon;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean compareMultiline(String actual, String expected) {
        String[] actualLines = actual.trim().split("\\r?\\n");
        String[] expectedLines = expected.trim().split("\\r?\\n");

        if (actualLines.length != expectedLines.length) {
            return false;
        }

        for (int i = 0; i < actualLines.length; i++) {
            if (!actualLines[i].trim().equals(expectedLines[i].trim())) {
                return false;
            }
        }

        return true;
    }

    private static String normalize(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }
}

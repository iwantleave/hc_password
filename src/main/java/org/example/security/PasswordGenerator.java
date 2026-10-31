package org.example.security;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PasswordGenerator {
    public static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGITS = "0123456789";
    public static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?/~";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate(int length, boolean lowercase, boolean uppercase,
                                  boolean digits, boolean symbols) {
        if (length < 1) {
            throw new IllegalArgumentException("密码长度必须大于 0");
        }

        List<String> sets = new ArrayList<>();
        if (lowercase) {
            sets.add(LOWER);
        }
        if (uppercase) {
            sets.add(UPPER);
        }
        if (digits) {
            sets.add(DIGITS);
        }
        if (symbols) {
            sets.add(SYMBOLS);
        }
        if (sets.isEmpty()) {
            throw new IllegalArgumentException("至少选择一种字符类型");
        }
        if (length < sets.size()) {
            throw new IllegalArgumentException("密码长度不能小于已选择的字符类型数量");
        }

        StringBuilder all = new StringBuilder();
        List<Character> chars = new ArrayList<>(length);
        for (String set : sets) {
            all.append(set);
            chars.add(set.charAt(RANDOM.nextInt(set.length())));
        }

        while (chars.size() < length) {
            chars.add(all.charAt(RANDOM.nextInt(all.length())));
        }
        Collections.shuffle(chars, RANDOM);

        StringBuilder result = new StringBuilder(length);
        for (char c : chars) {
            result.append(c);
        }
        return result.toString();
    }
}

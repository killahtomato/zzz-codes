package dev.zzz.codes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Rewards {

    private static final Pattern CODE_TOKEN = Pattern.compile("\\b[A-Z0-9]{6,16}\\b");
    private static final Pattern QUANTITY_LED = Pattern.compile("[×x]?\\s?\\d[\\d,]{0,10}");
    private static final Pattern BAD = Pattern.compile("(?i)\\b(?:unknown|redeem|redemption|code|codes|rewards?|free|new)\\b");

    private static final Set<String> LEAD_JUNK = Set.of(
            "copy", "code", "codes", "reward", "rewards", "redeem", "redee", "promo",
            "promos", "new", "items", "get");

    private static final Set<String> ABBREV = Set.of("logs", "log", "mods", "mod");

    private Rewards() {
    }

    static String extract(String line, int codeStart, ItemList items) {
        List<int[]> codes = new ArrayList<>();
        Matcher cm = CODE_TOKEN.matcher(line);
        while (cm.find()) {
            codes.add(new int[]{cm.start(), cm.end()});
        }
        int idx = -1;
        for (int i = 0; i < codes.size(); i++) {
            int[] c = codes.get(i);
            if (codeStart >= c[0] && codeStart < c[1]) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return null;
        }
        int[] my = codes.get(idx);
        int prevEnd = idx > 0 ? codes.get(idx - 1)[1] : 0;
        int nextStart = idx + 1 < codes.size() ? codes.get(idx + 1)[0] : line.length();
        String after = line.substring(my[1], nextStart);
        String before = line.substring(prevEnd, my[0]);
        String best = rewardOf(after, items);
        if (best == null) {
            best = rewardOf(before, items);
        }
        if (best == null || best.length() < 3) {
            return null;
        }
        return valid(best) ? best : null;
    }

    static boolean valid(String s) {
        if (s == null || s.length() > 140 || BAD.matcher(s).find()) {
            return false;
        }
        long digits = s.chars().filter(Character::isDigit).count();
        return digits > 0 || s.length() >= 8;
    }

    static String select(String a, String b) {
        boolean va = valid(a);
        boolean vb = valid(b);
        if (va && vb) {
            boolean qa = quantLed(a);
            boolean qb = quantLed(b);
            if (qa != qb) {
                return qa ? a : b;
            }
            return a.length() >= b.length() ? a : b;
        }
        return va ? a : (vb ? b : (a == null ? b : a));
    }

    private static boolean quantLed(String s) {
        char c0 = s.charAt(0);
        return Character.isDigit(c0) || c0 == '×' || c0 == 'x' || c0 == 'X';
    }

    static String display(String reward, String context) {
        String s = reward;
        if (s == null && context != null) {
            s = clean(context);
        }
        if (s == null || s.length() < 3 || s.length() > 200) {
            return null;
        }
        if (BAD.matcher(s).find()) {
            return null;
        }
        return s;
    }

    private static String rewardOf(String region, ItemList items) {
        if (region.isBlank()) {
            return null;
        }
        List<ItemList.Match> matches = items.findIn(region);
        if (matches.isEmpty()) {
            return null;
        }
        int first = matches.get(0).start();
        int lastEnd = extendEnd(region, matches.get(matches.size() - 1).end());
        int start = first;
        while (start > 0) {
            int prevSpace = region.lastIndexOf(' ', start - 1);
            String word = region.substring(prevSpace + 1, start);
            String alpha = word.replaceAll("[^A-Za-z]", "").toLowerCase(Locale.ROOT);
            if (alpha.isEmpty() || alpha.equals("and") || alpha.equals("or") || ABBREV.contains(alpha)) {
                int next = prevSpace + 1;
                if (next >= start) {
                    break;
                }
                start = next;
                continue;
            }
            break;
        }
        return clean(region.substring(start, lastEnd));
    }

    private static int extendEnd(String region, int end) {
        int probe = Math.min(region.length(), end + 8);
        Matcher m = QUANTITY_LED.matcher(region.substring(end, probe));
        if (m.find() && m.start() <= 4 && m.end() > 0) {
            return end + m.end();
        }
        return end;
    }

    static String clean(String s) {
        if (s == null) {
            return null;
        }
        s = CODE_TOKEN.matcher(s).replaceAll(" ");
        s = s.replaceAll("[\\p{Cc}\\s]+", " ").trim();
        s = s.replaceAll("\\s*,\\s*", ", ");
        s = s.replaceAll(", (\\d)", ",$1");
        s = s.replaceAll("^[,.:;]+", "").trim();
        s = s.replaceAll("[,.:;]+$", "").trim();
        s = s.replaceAll("(?i)\\s*(?:and|or)\\s*$", "").trim();
        s = s.replaceAll("[\\p{Cc}\\s]+", " ").trim();
        return s.isEmpty() ? null : s;
    }
}
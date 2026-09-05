package dev.zzz.codes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ItemList {

    private static final String ITEMS_URL = "https://game8.co/games/Zenless-Zone-Zero/archives/451335";

    private static final Pattern ALT = Pattern.compile("alt='([^']{3,70})'");

    private static final Set<String> SKIP = Set.of(
            "back to zenless zone zero wiki front", "arcade", "bosses", "character guides",
            "commissions", "bangboos", "share on reddit", "share on whatsapp", "share on messenger",
            "member benefits illustration", "game8 advertiser recruitment", "sign up", "log in",
            "continue as a guest", "join now", "site interface", "guest", "free member",
            "premium articles", "game tools", "interactive map access", "light/dark theme toggle",
            "article watchlist", "game bookmarks", "cross-device sync", "user profiles",
            "direct feedback", "comment rating", "comment notification",
            "map tool feature illustration", "comment rating feature illustration",
            "premium archive feature illustration", "check list", "event choice checker",
            "deck builder cross-device", "message board notification", "message board cross-device",
            "build planner", "stat calculator", "diagnostic tool", "weapon/armor wishlist",
            "team builder", "character scoring", "tier list maker", "interactive map comments",
            "interactive map pins", "interactive map pins cross-device", "map tool access");

    private static final List<String> BASELINE = List.of(
            "polychrome", "polychromes", "denny", "dennies",
            "senior investigator log", "senior investigator logs",
            "official investigator log", "official investigator logs",
            "trainee investigator log", "trainee investigator logs",
            "investigator log", "investigator logs",
            "w-engine power supply", "w-engine power supplies",
            "w-engine energy module", "w-engine energy modules",
            "w-engine battery", "w-engine batteries",
            "bangboo algorithm module", "bangboo algorithm modules",
            "bangboo system widget", "bangboo system widgets",
            "bangboo software patch", "bangboo software patches",
            "crystallized plating agent", "crystallized plating agents",
            "ether plating agent", "ether plating agents",
            "military plating agent", "military plating agents",
            "molded plating agent", "molded plating agents",
            "hamster cage pass", "hamster cage passes",
            "16mm film", "16mm films",
            "bangbuck", "bangbucks", "boopon",
            "master tape", "master tapes",
            "encrypted master tape", "encrypted master tapes",
            "monochrome", "residual signal", "fading signal",
            "w-engine chip", "w-engine chips", "z-merits",
            "inter-knot credit", "daily performance",
            "battery charge", "backup battery charge");

    record Match(int start, int end) {
    }

    private final Pattern pattern;

    ItemList(boolean fetch) {
        Set<String> names = new LinkedHashSet<>(BASELINE);
        if (fetch) {
            String html = Fetcher.get(ITEMS_URL);
            if (html != null) {
                Matcher m = ALT.matcher(html);
                while (m.find() && names.size() < 500) {
                    String name = m.group(1).trim();
                    if (plausible(name)) {
                        names.add(normalize(name));
                    }
                }
            }
        }
        List<String> phrases = new ArrayList<>(names);
        phrases.sort(Comparator.comparingInt((String s) -> words(s)).reversed()
                .thenComparing(s -> -s.length()));
        StringBuilder sb = new StringBuilder("(?i)\\b(?:");
        for (int i = 0; i < phrases.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(Pattern.quote(phrases.get(i)));
        }
        sb.append(")\\b");
        this.pattern = Pattern.compile(sb.toString());
    }

    List<Match> findIn(String region) {
        List<Match> out = new ArrayList<>();
        Matcher m = pattern.matcher(region);
        while (m.find()) {
            out.add(new Match(m.start(), m.end()));
        }
        return out;
    }

    private static boolean plausible(String name) {
        if (name.length() < 4 || name.length() > 70) {
            return false;
        }
        boolean hasLetter = false;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i))) {
                hasLetter = true;
                break;
            }
        }
        if (!hasLetter) {
            return false;
        }
        String low = name.toLowerCase(Locale.ROOT);
        if (SKIP.contains(low)) {
            return false;
        }
        if (low.startsWith("game8") || low.startsWith("http")) {
            return false;
        }
        return true;
    }

    private static int words(String s) {
        int n = 1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                n++;
            }
        }
        return n;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Cc}]+", " ").trim();
    }
}
package dev.zzz.codes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Extractor {

    record Hit(String code, String context, int score, String reward, boolean urlSourced) {
    }

    private static final Pattern CODE = Pattern.compile("\\b[A-Z0-9]{6,16}\\b");
    private static final Pattern CODE_PARAM = Pattern.compile("[?&](code|redeemCode|coupon)=([A-Za-z0-9]{4,20})");

    private static final Set<String> KEYWORDS = Set.of(
            "redeem", "code", "codes", "gift", "promo", "key", "reward", "copy",
            "valid", "active", "working", "expire", "expires", "expired", "free",
            "claim", "enter", "exchange", "bonus", "polychrome", "hoyoverse", "livestream",
            "new", "denny", "auto-fill");

    private static final List<String> EXPIRED_WORDS = List.of("expired", "no longer", "has expired");

    private static final List<String> ACTIVE_WORDS = List.of(
            "active", "working", "valid", "permanent", "never expires", "currently works");

    private static final Set<String> STOP = Set.of(
            "REDEEM", "REWARD", "REWARDS", "EXPIRED", "EXPIRES", "EXPIRE", "WARNING", "NOTE",
            "NOTES", "NEWS", "UPDATE", "UPDATES", "ACTIVE", "WORKING", "CODES", "COUPON",
            "FOLLOW", "SOCIAL", "TWITTER", "FACEBOOK", "DISCORD", "LINKEDIN", "DETAILS",
            "POLYCHROME", "DENNIES", "DENNY", "MATERIAL", "MATERIALS", "INVESTIGATOR",
            "SUPPLY", "SIGNAL", "SEARCH", "WELCOME", "PLAYER", "PLAYERS", "SUPPORT",
            "PLATFORM", "ALWAYS", "MEMBERS", "REGISTER", "LOGIN", "NOTICE", "REVIEW",
            "CONTENT", "RECENT", "MOBILE", "DEVICE", "SEASON", "VERSION", "NUMBER",
            "BANNER", "WEBSITE", "WEBSTORE", "SYNTAX", "CONSOLE", "CONTACT", "PRIVACY",
            "SERVICE", "SERVICES", "RESULT", "RESULTS", "POINTS", "POINT", "CRYSTAL",
            "CRYSTALS", "WALLET", "REFUND", "REFUNDS", "GIFTING", "DAILY", "WEEKLY",
            "HEALTH", "SAFETY", "SOFTWARE", "MANAGER", "HISTORY", "STORAGE", "ACCOUNT",
            "ACCOUNTS", "PASSWORD", "SETTING", "SETTINGS", "CONTROLS", "DEFAULT",
            "HEADING", "PREFACE", "PREFIX", "POSTFIX", "HEADLINE", "SUBSCRIBE", "SUBSCRIBERS",
            "NEWSLETTER", "CHECKBOX", "DIVIDER", "WIDGET", "FEEDBACK", "TUTORIAL", "GALLERY",
            "OVERVIEW", "METADATA", "READY", "LAUNCHED", "RELEASED", "RELEASE", "ANNIVERSARY",
            "BIRTHDAY", "HAPPY", "HELLO", "THANKS", "THANK", "PLEASE", "POLICY", "TERMS",
            "CONDITIONS", "DISCLOSURE", "ADVERTISEMENT", "SPONSORED", "PATREON", "FEATURED",
            "RECOMMENDED", "POPULAR", "TRENDING", "LATEST", "READING", "ARTICLE", "GUIDE",
            "GUIDES", "REGISTERED", "RIGHTS", "RESERVED", "CHOICE", "OPTION", "OPTIONS",
            "PERMISSION", "EXPLORE", "DISCOVER", "INSTALL", "CLICK", "DOWNLOAD", "RATE",
            "RATINGS", "FAVORITE", "FAVORITES", "BOOKMARK", "SHARE", "SHARING", "INVITE",
            "INVITES", "SERVERS", "CHANNEL", "CHANNELS", "SPECIAL", "GENERAL", "OFFICIAL",
            "COMMUNITY", "COMPANION", "CAMPAIGN", "EVENTS", "EVENT", "RANK", "TIERS",
            "CLASSES", "CHARACTER", "CHARACTERS", "WEAPONS", "ROSTER", "BUILDS", "BUILDING",
            "RUMOR", "LEAK", "LEAKS", "BANNERS", "WINNERS", "SPONSOR", "PUBLISHER",
            "DEVELOPER", "STUDIOS", "MEETING", "DESCRIPT", "SUMMARIES", "SUMMARY",
            "ABSTRACT", "THUMBNAIL", "KEYWORD", "KEYWORDS", "HIGHLIGHT", "HIGHLIGHTS",
            "QUESTION", "QUESTIONS", "ANSWER", "ANSWERS", "COMMENTS", "LIKES", "VIEWS",
            "MEMBER", "JOINED", "FOLLOWERS", "STATUS", "STATUSES", "ALLOWED", "UNBLOCKED",
            "PRACTICE", "METHOD", "METHODS", "MANUAL", "SEPTEMBER", "OCTOBER", "NOVEMBER",
            "DECEMBER", "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAYBE", "JUNE",
            "AUGUST", "CURRENT", "CONFIRMED", "VERIFIED", "AVAILABLE", "UNCLAIMED",
            "SECONDS", "MINUTES", "HOURS", "MONTHS", "YEARS", "EVERYONE", "SOMETHING",
            "SOMETIMES", "WHENEVER", "WHEREVER", "REMEMBER", "WATCHING", "LOOKING",
            "WAITING", "WISHING", "SENDERS", "SIGHTINGS", "WELLFAIR", "SURVIVOR",
            "MELATONIN", "GUARDIANS", "CANDLELIT", "LEISURELY", "PRETTYMUCH", "THEFIVE",
            "LOADING", "FAILED", "TRYAGAIN", "PERMANENT", "ALTERNATIVE", "ALTERNATIVES",
            "MAILBOX", "CONTENTS", "INTERVIEW", "HOYOVERSE", "REDEMPTION", "ZENLESS",
            "COGNOSPHERE", "MASTERCLASS", "PHYSICAL", "SUBSCRIPTION", "LIVESTREAM",
            "STREAM", "STREAMS", "GAMING", "GAMER", "ARCHIVES", "CATEGORY", "CATEGORIES",
            "SCREENSHOT", "SCREENSHOTS", "WIKI", "DIGITAL", "REFRESH", "UPDATED",
            "CURRENTLY", "HOMEPAGE", "INTERKNOT", "NEWERIDU",
            "PENTAVERSE", "GACHA", "RACKUP", "SOMEWHERE", "EVERYWHERE", "ANYWHERE",
            "TESTING", "BETA", "TRAINING", "LEARNING", "SHOWING", "TURNING", "ROLLING",
            "SERIOUSLY", "DEFINITELY", "PROBABLY", "ANOTHER", "BETWEEN", "WITHOUT",
            "WITHIN", "BELOW", "ABOVE", "ABOUT", "OTHERS", "WORDS", "PHRASES",
            "SENTENCE", "PARAGRAPH", "HEADERS", "FOOTERS", "SIDEBAR", "NAVBAR",
            "TOOLBAR", "BREADCRUMB", "BREADCRUMBS");

    private Extractor() {
    }

    static List<Hit> extract(String rawHtml, String text, ItemList items) {
        Set<String> seen = new HashSet<>();
        List<Hit> out = new ArrayList<>();
        String[] lines = text.split("\\n", -1);
        int[] offs = new int[lines.length + 1];
        for (int i = 0; i < lines.length; i++) {
            offs[i + 1] = offs[i] + lines[i].length() + 1;
        }
        Matcher tm = CODE.matcher(text);
        while (tm.find()) {
            String tok = tm.group();
            if (skippable(tok)) {
                continue;
            }
            String context = window(text, tm.start(), tm.end());
            int kw = 0;
            String ctxLow = context.toLowerCase(Locale.ROOT);
            for (String k : KEYWORDS) {
                if (ctxLow.contains(k)) {
                    kw++;
                }
            }
            if (kw == 0 || (kw == 1 && !strong(tok, context))) {
                continue;
            }
            String reward = rewardAt(lines, offs, tm.start(), items);
            if (seen.add(tok)) {
                out.add(new Hit(tok, snippet(context), 2 + kw, reward, false));
            }
        }
        Matcher um = CODE_PARAM.matcher(rawHtml);
        while (um.find()) {
            String code = um.group(2).toUpperCase(Locale.ROOT);
            if (code.length() < 6 || code.length() > 16 || !hasLetters(code)) {
                continue;
            }
            if (seen.add(code)) {
                String ctx = "found in a link on the page (#" + um.group(1) + "=" + code + ")";
                String reward = null;
                int ti = text.toUpperCase(Locale.ROOT).indexOf(code);
                if (ti >= 0) {
                    ctx = window(text, ti, ti + code.length());
                    reward = rewardAt(lines, offs, ti, items);
                }
                out.add(new Hit(code, snippet(ctx), 10, reward, true));
            }
        }
        return out;
    }

    private static String rewardAt(String[] lines, int[] offs, int pos, ItemList items) {
        int li = 0;
        for (int i = 0; i < lines.length; i++) {
            if (pos < offs[i + 1]) {
                li = i;
                break;
            }
        }
        String line = lines[li];
        String reward = Rewards.extract(line, pos - offs[li], items);
        if (reward == null && li + 1 < lines.length) {
            reward = Rewards.extract(line + " " + lines[li + 1], pos - offs[li], items);
        }
        return reward;
    }

    private static boolean skippable(String tok) {
        if (STOP.contains(tok)) {
            return true;
        }
        if (!hasLetters(tok)) {
            return true;
        }
        int letters = 0;
        int digits = 0;
        for (int i = 0; i < tok.length(); i++) {
            if (Character.isDigit(tok.charAt(i))) {
                digits++;
            } else {
                letters++;
            }
        }
        if (letters < 3 || digits > 10) {
            return true;
        }
        return false;
    }

    private static boolean strong(String tok, String context) {
        if (tok.length() >= 8 && hasLetters(tok)) {
            int letters = 0;
            for (int i = 0; i < tok.length(); i++) {
                if (!Character.isDigit(tok.charAt(i))) {
                    letters++;
                }
            }
            return letters >= 3;
        }
        return false;
    }

    private static boolean hasLetters(String tok) {
        for (int i = 0; i < tok.length(); i++) {
            if (!Character.isDigit(tok.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String window(String text, int start, int end) {
        int from = Math.max(0, start - 70);
        int to = Math.min(text.length(), end + 70);
        return text.substring(from, to);
    }

    private static String snippet(String ctx) {
        String t = ctx.trim().replaceAll("[\\s\\p{Cc}]+", " ");
        if (t.length() > 110) {
            t = t.substring(0, 107) + "...";
        }
        return t;
    }

    static boolean expiredSignal(Hit h) {
        String c = h.context().toLowerCase(Locale.ROOT);
        for (String w : EXPIRED_WORDS) {
            if (c.contains(w)) {
                return true;
            }
        }
        return false;
    }

    static boolean activeSignal(Hit h) {
        String c = h.context().toLowerCase(Locale.ROOT);
        for (String w : ACTIVE_WORDS) {
            if (c.contains(w)) {
                return true;
            }
        }
        return false;
    }
}
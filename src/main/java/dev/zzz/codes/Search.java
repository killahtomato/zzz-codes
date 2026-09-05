package dev.zzz.codes;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Search {

    record Result(String url, String title) {
    }

    private static final Pattern DDG_ANCHOR = Pattern.compile(
            "<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>");

    private static final Pattern DDG_LINK = Pattern.compile("[?&]uddg=([^&]+)");

    private static final Pattern BING = Pattern.compile(
            "<li class=\"b_algo\"[^>]*>([\\s\\S]*?)</li>");

    private static final Pattern MOJEEK = Pattern.compile(
            "<a class=\"ob\" href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>");

    private static final Pattern ANCHOR = Pattern.compile(
            "<a[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>");

    private Search() {
    }

    static List<Result> from(String engine, String query, String html) {
        return switch (engine) {
            case "duckduckgo" -> parseDdg(html);
            case "bing" -> parseBing(html);
            case "mojeek" -> parseMojeek(html);
            default -> List.of();
        };
    }

    static String url(String engine, String query) {
        String q = urlencode(query);
        return switch (engine) {
            case "duckduckgo" -> "https://html.duckduckgo.com/html/?q=" + q;
            case "bing" -> "https://www.bing.com/search?q=" + q + "&mkt=en-US";
            case "mojeek" -> "https://www.mojeek.com/search?q=" + q;
            default -> throw new IllegalArgumentException(engine);
        };
    }

    private static String urlencode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static List<Result> parseDdg(String html) {
        List<Result> out = new ArrayList<>();
        Matcher m = DDG_ANCHOR.matcher(html);
        while (m.find()) {
            String href = m.group(1);
            if (href.startsWith("//")) {
                href = "https:" + href;
            }
            String clean = DdgClean.clean(href);
            if (clean.isEmpty()) {
                continue;
            }
            String title = cleanTitle(m.group(2));
            if (title.isEmpty()) {
                continue;
            }
            out.add(new Result(clean, title));
        }
        return firstOnly(out);
    }

    private static List<Result> parseBing(String html) {
        List<Result> out = new ArrayList<>();
        Matcher li = BING.matcher(html);
        while (li.find()) {
            Matcher a = ANCHOR.matcher(li.group(1));
            if (!a.find()) {
                continue;
            }
            String href = HtmlText.decode(a.group(1));
            if (!isGood(href)) {
                continue;
            }
            String title = cleanTitle(a.group(2));
            if (title.isEmpty()) {
                continue;
            }
            out.add(new Result(href, title));
        }
        return firstOnly(out);
    }

    private static List<Result> parseMojeek(String html) {
        List<Result> out = new ArrayList<>();
        Matcher m = MOJEEK.matcher(html);
        while (m.find()) {
            String href = HtmlText.decode(m.group(1));
            if (!isGood(href)) {
                continue;
            }
            String title = cleanTitle(m.group(2));
            if (title.isEmpty()) {
                continue;
            }
            out.add(new Result(href, title));
        }
        return firstOnly(out);
    }

    private static String cleanTitle(String raw) {
        String t = HtmlText.toText(raw);
        return t.trim().replaceAll("[\\s\\p{Cc}]+", " ");
    }

    private static boolean isGood(String url) {
        String low = url.toLowerCase();
        if (!low.startsWith("http")) {
            return false;
        }
        if (low.contains("bing.com") || low.contains("go.microsoft.com") || low.contains("/ck/a")) {
            return false;
        }
        if (low.contains("hoyoverse.com/redemption")) {
            return false;
        }
        return true;
    }

    private static List<Result> firstOnly(List<Result> results) {
        Map<String, Result> seen = new LinkedHashMap<>();
        for (Result r : results) {
            String k = r.url().replaceFirst("/$", "");
            seen.putIfAbsent(k, r);
        }
        return new ArrayList<>(seen.values());
    }

    private static final class DdgClean {
        private DdgClean() {
        }

        private static String clean(String href) {
            try {
                if (!href.startsWith("http://") && !href.startsWith("https://")) {
                    return "";
                }
                if (!href.contains("/l/?uddg=")) {
                    return href;
                }
                Matcher m = DDG_LINK.matcher(href);
                if (!m.find()) {
                    return "";
                }
                String decoded = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
                return isGood(decoded) ? decoded : "";
            } catch (Exception e) {
                return "";
            }
        }
    }
}
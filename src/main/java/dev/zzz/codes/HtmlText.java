package dev.zzz.codes;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HtmlText {

    private static final Pattern JUNK = Pattern.compile(
            "<(script|style|noscript|template|textarea|svg|head)(\\s[^>]*)?>[\\s\\S]*?</\\1>|<!--[\\s\\S]*?-->", Pattern.CASE_INSENSITIVE);

    private static final Pattern BREAK = Pattern.compile(
            "<(br|hr|/p|/div|/li|/tr|/td|/th|/h[1-6]|/table|/section|/article|/footer|/header|/main|/ul|/ol)(\\s[^>]*)?>", Pattern.CASE_INSENSITIVE);

    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    private static final Map<String, String> NAMED = new HashMap<>();

    static {
        NAMED.put("amp", "&");
        NAMED.put("lt", "<");
        NAMED.put("gt", ">");
        NAMED.put("quot", "\"");
        NAMED.put("apos", "'");
        NAMED.put("nbsp", " ");
        NAMED.put("copy", "(c)");
        NAMED.put("reg", "(r)");
        NAMED.put("hellip", "...");
        NAMED.put("mdash", "-");
        NAMED.put("ndash", "-");
        NAMED.put("middot", ".");
        NAMED.put("bull", "*");
        NAMED.put("times", "x");
        NAMED.put("deg", "o");
        NAMED.put("laquo", "<<");
        NAMED.put("raquo", ">>");
        NAMED.put("yen", "Y");
        NAMED.put("euro", "E");
    }

    private static final Pattern ENTITY = Pattern.compile("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);");

    private HtmlText() {
    }

    static String toText(String html) {
        String s = JUNK.matcher(html).replaceAll(" ");
        s = BREAK.matcher(s).replaceAll("\n");
        s = TAGS.matcher(s).replaceAll(" ");
        s = decode(s);
        s = s.replace('\u00a0', ' ');
        s = s.replaceAll("[\\p{Cc}]+", "\n");
        s = s.replaceAll("[ \t\f]+", " ");
        String[] lines = s.split("\\r?\\n");
        StringBuilder sb = new StringBuilder(s.length());
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty()) {
                sb.append(t).append('\n');
            }
        }
        return sb.toString();
    }

    static String decode(String s) {
        Matcher m = ENTITY.matcher(s);
        StringBuffer sb = new StringBuffer(s.length());
        while (m.find()) {
            String body = m.group(1);
            String rep;
            if (body.charAt(0) == '#') {
                rep = numeric(body);
            } else {
                rep = NAMED.getOrDefault(body.toLowerCase(), m.group());
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String numeric(String body) {
        try {
            int cp;
            if (body.charAt(1) == 'x' || body.charAt(1) == 'X') {
                cp = Integer.parseInt(body.substring(2), 16);
            } else {
                cp = Integer.parseInt(body.substring(1));
            }
            if (cp > 0x10FFFF) {
                return "";
            }
            return new String(Character.toChars(cp));
        } catch (RuntimeException e) {
            return "";
        }
    }
}
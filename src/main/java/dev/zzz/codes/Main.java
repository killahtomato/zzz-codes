package dev.zzz.codes;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    private static final String REDEEM_URL = "https://zenless.hoyoverse.com/redemption?code=";

    private static final List<String> DIRECT = List.of(
            "https://www.pcgamesn.com/zenless-zone-zero/codes",
            "https://beebom.com/zenless-zone-zero-codes/",
            "https://mmoculture.com/2026/08/zenless-zone-zero-redeem-codes/",
            "https://www.icy-veins.com/zenless-zone-zero/redemption-codes",
            "https://allthings.how/zenless-zone-zero-codes",
            "https://www.dbltap.com/guides/zenless-zone-zero-codes",
            "https://www.gamesradar.com/games/action-rpg/zenless-zone-zero-codes/",
            "https://gamevika.com/en/zzz/codes",
            "https://game8.co/games/Zenless-Zone-Zero/archives/435683",
            "https://gamerant.com/zenless-zone-zero-zzz-code-livestream-redeem-codes-free-polychrome/",
            "https://zenlesscodes.com/");

    private static final List<String> ENGINES = List.of("duckduckgo", "mojeek", "bing");
    private static final List<String> QUERIES = List.of(
            "Zenless Zone Zero redemption codes",
            "Zenless Zone Zero ZZZ codes September 2026");

    private static final int RESULTS_PER_ENGINE = 4;

    private record PageResult(String url, List<Extractor.Hit> hits) {
    }

    private record Aggregated(String code, int sources, int score, String snippet, String reward,
                              String label, Set<String> sourceHosts) {
    }

    public static void main(String[] args) throws Exception {
        long t0 = System.nanoTime();
        List<String> pages = discover();
        ResultTable table = scrape(pages);
        print(table);
        System.out.printf("scraped %d/%d pages in %.1fs%n",
                table.pagesFetched(), pages.size(), (System.nanoTime() - t0) / 1e9);
    }

    private static List<String> discover() {
        Set<String> urls = new LinkedHashSet<>(DIRECT);
        for (String q : QUERIES) {
            for (String engine : ENGINES) {
                String engineUrl = Search.url(engine, q);
                String html = Fetcher.get(engineUrl);
                if (html == null) {
                    continue;
                }
                int count = 0;
                for (Search.Result r : Search.from(engine, q, html)) {
                    if (count >= RESULTS_PER_ENGINE) {
                        break;
                    }
                    count++;
                    urls.add(r.url());
                }
            }
        }
        return new ArrayList<>(urls);
    }

    private static ResultTable scrape(List<String> pages) throws InterruptedException {
        ItemList items = new ItemList(true);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<PageResult>> futures = new ArrayList<>();
        for (String page : pages) {
            futures.add(pool.submit(() -> {
                String html = Fetcher.get(page);
                if (html == null) {
                    return null;
                }
                String text = HtmlText.toText(html);
                return new PageResult(page, Extractor.extract(html, text, items));
            }));
        }
        pool.shutdown();

        Map<String, Set<String>> sources = new LinkedHashMap<>();
        Map<String, Integer> score = new HashMap<>();
        Map<String, String> snippet = new HashMap<>();
        Map<String, String> rewards = new HashMap<>();
        Map<String, int[]> votes = new HashMap<>();

        int fetched = 0;
        for (Future<PageResult> f : futures) {
            PageResult pr;
            try {
                pr = f.get();
            } catch (ExecutionException e) {
                continue;
            }
            if (pr == null) {
                continue;
            }
            fetched++;
            String host = hostOnly(pr.url());
            for (Extractor.Hit hit : pr.hits()) {
                String code = hit.code();
                sources.computeIfAbsent(code, k -> new LinkedHashSet<>()).add(host);
                score.merge(code, hit.score(), Integer::sum);
                snippet.merge(code, hit.context(), (a, b) -> prefer(a, b));
                if (hit.reward() != null && !hit.reward().isBlank()) {
                    rewards.merge(code, hit.reward(), Rewards::select);
                }
                int[] v = votes.computeIfAbsent(code, k -> new int[2]);
                if (Extractor.expiredSignal(hit)) {
                    v[0]++;
                } else if (Extractor.activeSignal(hit)) {
                    v[1]++;
                }
            }
        }

        List<Aggregated> list = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : sources.entrySet()) {
            String code = e.getKey();
            int[] v = votes.get(code);
            String label;
            if (v[0] > 0 && v[1] == 0) {
                label = "expired?";
            } else if (v[1] > 0 && v[0] == 0) {
                label = "active?";
            } else if (v[0] > 0) {
                label = "mixed?";
            } else {
                label = "";
            }
            list.add(new Aggregated(code, e.getValue().size(), score.get(code), snippet.get(code),
                    rewards.get(code), label, e.getValue()));
        }
        list.sort(Comparator.comparingInt(Aggregated::sources).reversed()
                .thenComparing(Comparator.comparingInt(Aggregated::score).reversed())
                .thenComparing(Aggregated::code));
        return new ResultTable(list, fetched);
    }

    private static void print(ResultTable table) {
        System.out.println();
        System.out.println("== Zenless Zone Zero redemption codes ==");
        String now = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        System.out.println("fetched " + now + "  |  pages " + table.pagesFetched());
        System.out.println("redeem at https://zenless.hoyoverse.com/redemption?code=<CODE>");
        System.out.println("or in-game: Menu > More > Redemption Code  (case-sensitive)");
        System.out.println();
        if (table.codes().isEmpty()) {
            System.out.println("No codes found. Pages may be unreachable right now;");
            System.out.println("check https://zenless.hoyoverse.com/redemption manually.");
            return;
        }
        List<Aggregated> active = new ArrayList<>();
        List<Aggregated> unverified = new ArrayList<>();
        List<Aggregated> expired = new ArrayList<>();
        for (Aggregated a : table.codes()) {
            switch (a.label()) {
                case "active?" -> active.add(a);
                case "expired?", "mixed?" -> expired.add(a);
                default -> unverified.add(a);
            }
        }
        printGroup("ACTIVE (status text found near code)", active);
        printGroup("UNVERIFIED (no status text found near code)", unverified);
        printGroup("POSSIBLY EXPIRED (context mentions expiry)", expired);
    }

    private static void printGroup(String heading, List<Aggregated> codes) {
        if (codes.isEmpty()) {
            return;
        }
        System.out.println("### " + heading);
        System.out.println();
        for (Aggregated a : codes) {
            String shown = Rewards.display(a.reward(), a.snippet());
            if (shown == null) {
                shown = "(no reward info extracted)";
            }
            System.out.printf("%-17s %d source%s%n", a.code(), a.sources(), a.sources() == 1 ? "" : "s");
            System.out.println("  " + shown);
            System.out.println("  " + REDEEM_URL + a.code());
            System.out.println("  from: " + String.join(", ", a.sourceHosts()));
            System.out.println();
        }
    }

    private static String prefer(String a, String b) {
        if (a.toLowerCase(Locale.ROOT).contains("expired") && !b.toLowerCase(Locale.ROOT).contains("expired")) {
            return b;
        }
        return a.toLowerCase(Locale.ROOT).contains("expired") ? b : a;
    }

    private static String hostOnly(String url) {
        try {
            String h = URI.create(url).getHost();
            if (h == null) {
                return url;
            }
            return h.startsWith("www.") ? h.substring(4) : h;
        } catch (Exception e) {
            return url;
        }
    }

    private record ResultTable(List<Aggregated> codes, int pagesFetched) {
    }
}
package dev.zzz.codes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String REDEEM_URL = "https://zenless.hoyoverse.com/redemption?code=";

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--cli")) {
            runCli();
        } else {
            Gui.launch(args);
        }
    }

    private static void runCli() throws Exception {
        long t0 = System.nanoTime();
        Scanner.ResultTable table = Scanner.scan();
        print(table);
        System.out.printf("scraped %d/%d pages in %.1fs%n",
                table.pagesFetched(), table.totalPages(), (System.nanoTime() - t0) / 1e9);
    }

    private static void print(Scanner.ResultTable table) {
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
        List<Scanner.Aggregated> active = new ArrayList<>();
        List<Scanner.Aggregated> unverified = new ArrayList<>();
        List<Scanner.Aggregated> expired = new ArrayList<>();
        for (Scanner.Aggregated a : table.codes()) {
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

    private static void printGroup(String heading, List<Scanner.Aggregated> codes) {
        if (codes.isEmpty()) {
            return;
        }
        System.out.println("### " + heading);
        System.out.println();
        for (Scanner.Aggregated a : codes) {
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
}
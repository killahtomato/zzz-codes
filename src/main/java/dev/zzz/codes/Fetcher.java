package dev.zzz.codes;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Fetcher {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Pattern CT_CHARSET = Pattern.compile("charset=([\\w-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_CHARSET = Pattern.compile("<meta[^>]+charset=['\"]?([\\w-]+)['\"]?", Pattern.CASE_INSENSITIVE);

    private Fetcher() {
    }

    static String get(String url) {
        return get(url, 2);
    }

    private static String get(String url, int attempts) {
        for (int i = 0; i < attempts; i++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", UA)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.8")
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
                int status = resp.statusCode();
                if (status != 200) {
                    System.err.println("warn: " + url + " -> HTTP " + status);
                    return null;
                }
                byte[] body = resp.body();
                return new String(body, charsetOf(resp, body));
            } catch (IllegalArgumentException e) {
                System.err.println("warn: bad url " + url);
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (IOException e) {
                System.err.println("warn: " + url + " -> " + e.getClass().getSimpleName());
            }
        }
        return null;
    }

    private static String charsetOf(HttpResponse<byte[]> resp, byte[] body) {
        String ct = resp.headers().firstValue("content-type").orElse("");
        Matcher m = CT_CHARSET.matcher(ct);
        if (m.find()) {
            return m.group(1);
        }
        int n = Math.min(body.length, 2048);
        m = META_CHARSET.matcher(new String(body, 0, n, StandardCharsets.ISO_8859_1));
        if (m.find()) {
            return m.group(1);
        }
        return "UTF-8";
    }
}
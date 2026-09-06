package dev.zzz.codes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class Redeemed {

    private static final Path FILE = Paths.get(
            System.getProperty("user.home"), ".zzz-codes", "redeemed-codes.txt");

    private Redeemed() {
    }

    static Set<String> load() {
        Set<String> out = new LinkedHashSet<>();
        if (!Files.isRegularFile(FILE)) {
            return out;
        }
        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String code = line.trim();
                if (code.length() >= 6) {
                    out.add(code.toUpperCase(Locale.ROOT));
                }
            }
        } catch (IOException e) {
            System.err.println("warn: could not read " + FILE + " (" + e + ")");
        }
        return out;
    }

    static void save(Set<String> codes) {
        try {
            Files.createDirectories(FILE.getParent());
            String content = codes.stream().sorted().collect(Collectors.joining("\n"));
            Files.write(FILE, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("warn: could not write " + FILE + " (" + e + ")");
        }
    }
}
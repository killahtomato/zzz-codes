package dev.zzz.codes;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

final class Gui {

    private static final String REDEEM_URL = "https://zenless.hoyoverse.com/redemption?code=";
    private static final String ACCENT = "#5b8cff";
    private static final Color BG = new Color(0x14161c);
    private static final Color CARD = new Color(0x1d212b);
    private static final Color TEXT = new Color(0xdfe3ea);
    private static final Color MUTED = new Color(0x99a2b3);
    private static final Color GREEN = new Color(0x4ade80);
    private static final Color AMBER = new Color(0xfbbf24);
    private static final Color RED = new Color(0xf87171);

    private Gui() {
    }

    static void launch(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ZZZ Redemption Codes");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(940, 760);
            frame.setMinimumSize(new Dimension(640, 480));
            frame.setLocationRelativeTo(null);
            frame.getContentPane().setBackground(BG);

            JLabel status = new JLabel("Scraping code tracker pages\u2026", SwingConstants.CENTER);
            status.setForeground(MUTED);
            status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            frame.getContentPane().add(status);

            frame.setVisible(true);
            new ScrapeWorker(frame, status).execute();
        });
    }

    private static final class ScrapeWorker extends SwingWorker<List<String>, Void> {

        private final JFrame frame;
        private final JLabel status;

        ScrapeWorker(JFrame frame, JLabel status) {
            this.frame = frame;
            this.status = status;
        }

        @Override
        protected List<String> doInBackground() throws Exception {
            Scanner.ResultTable table = Scanner.scan();
            return render(table);
        }

        @Override
        protected void done() {
            try {
                List<String> html = get();
                JEditorPane pane = new JEditorPane();
                pane.setContentType("text/html");
                pane.setEditable(false);
                pane.setOpaque(true);
                pane.setBackground(BG);
                pane.setText("<html><body style='font-family:SansSerif;color:#dfe3ea;'>" + String.join("", html) + "</body></html>");
                pane.setCaretPosition(0);
                pane.addHyperlinkListener(new HyperlinkListener() {
                    @Override
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            open(e.getURL().toExternalForm());
                        }
                    }
                });
                frame.getContentPane().removeAll();
                frame.getContentPane().add(new JScrollPane(pane), java.awt.BorderLayout.CENTER);
                frame.getContentPane().setBackground(BG);
                frame.revalidate();
                frame.repaint();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                String msg = e.getCause() == null ? e.toString()
                        : (e.getCause().getMessage() == null ? e.getCause().toString() : e.getCause().getMessage());
                JLabel label = new JLabel("<html><div style='text-align:center;'>Scan failed:<br>"
                        + esc(msg) + "</div></html>", SwingConstants.CENTER);
                label.setForeground(RED);
                frame.getContentPane().removeAll();
                frame.getContentPane().add(label, java.awt.BorderLayout.CENTER);
                frame.setBackground(BG);
                frame.revalidate();
                frame.repaint();
            }
        }
    }

    private static List<String> render(Scanner.ResultTable table) {
        List<String> out = new ArrayList<>();
        out.add(group("ACTIVE", GREEN, table.codes(), "active?"));
        out.add(group("UNVERIFIED", AMBER, table.codes(), ""));
        out.add(group("POSSIBLY EXPIRED", RED, table.codes(), "mixed?", "expired?"));
        if (table.codes().isEmpty()) {
            out.add("<p style='color:#99a2b3;margin-top:24px;'>No codes found.</p>");
        }
        out.add("<p style='color:#99a2b3;font-size:12px;'>scraped "
                + table.pagesFetched() + " pages \u00b7 redeem in-game: Menu \u203a More \u203a Redemption Code</p>");
        return out;
    }

    private static String group(String title, Color color, List<Scanner.Aggregated> all, String... labels) {
        List<Scanner.Aggregated> wants = new ArrayList<>();
        for (Scanner.Aggregated a : all) {
            for (String l : labels) {
                if (a.label().equals(l)) {
                    wants.add(a);
                }
            }
        }
        if (wants.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<h2 style='color:");
        sb.append(css(color)).append(";margin:18px 2px 6px 2px;font-size:16px;'>").append(title).append("</h2>");
        for (Scanner.Aggregated a : wants) {
            sb.append(card(a));
        }
        return sb.toString();
    }

    private static String card(Scanner.Aggregated a) {
        StringBuilder sb = new StringBuilder("<table width='100%' cellpadding='0' cellspacing='0' "
                + "style='background:#1d212b;border:1px solid #2a2f3a;border-radius:8px;margin:6px 0;'><tr><td style='padding:10px 12px;'>");
        String dot = dotColor(a.label());
        sb.append("<b style='font-family:Monospaced;color:").append(dot).append(";font-size:14px;'>")
                .append(esc(a.code())).append("</b>");
        sb.append(" <span style='color:#99a2b3;font-size:12px;'>&mdash; ").append(a.sources())
                .append(a.sources() == 1 ? " source" : " sources")
                .append(a.label().isEmpty() ? "" : " \u00b7 " + esc(a.label()))
                .append("</span><br>");
        String reward = Rewards.display(a.reward(), a.snippet());
        sb.append("<span style='color:#dfe3ea;font-size:13px;'>")
                .append(reward == null ? "(no reward info extracted)" : esc(reward))
                .append("</span>");
        sb.append("<div style='font-size:11px;color:#99a2b3;margin-top:6px;'>")
                .append("<a href='" + REDEEM_URL + esc(a.code()) + "' style='color:" + ACCENT + ";'>Redeem</a>")
                .append(" \u00b7 from: ").append(esc(String.join(", ", a.sourceHosts())))
                .append("</div>");
        sb.append("</td></tr></table>");
        return sb.toString();
    }

    private static String dotColor(String label) {
        if (label.equals("active?")) {
            return css(GREEN);
        }
        if (label.equals("expired?") || label.equals("mixed?")) {
            return css(RED);
        }
        return css(AMBER);
    }

    private static String css(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void open(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ignored) {
        }
    }
}
package dev.zzz.codes;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

final class Gui {

    private static final String REDEEM_URL = "https://zenless.hoyoverse.com/redemption?code=";
    private static final Color CARD_FILL = new Color(0xd91d212b, true);
    private static final Color CARD_EDGE = new Color(0x2a2f3a);
    private static final Color TEXT = new Color(0xdfe3ea);
    private static final Color MUTED = new Color(0xaab3c4);
    private static final Color ACCENT = new Color(0x6ea3ff);
    private static final Color GREEN = new Color(0x4ade80);
    private static final Color AMBER = new Color(0xfbbf24);
    private static final Color RED = new Color(0xf87171);
    private static final Color BG = new Color(0x10121a);

    private Gui() {
    }

    static void launch(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ZZZ Redemption Codes");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(960, 780);
            frame.setLocationRelativeTo(null);

            BgPanel bg = new BgPanel();
            bg.setLayout(new BorderLayout());
            frame.setContentPane(bg);

            JPanel north = new JPanel();
            north.setOpaque(false);
            north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

            JLabel header = headerImage();
            if (header != null) {
                north.add(header);
            }

            JPanel toolbar = new JPanel(new BorderLayout());
            toolbar.setOpaque(false);
            toolbar.setBorder(new EmptyBorder(8, 16, 8, 16));
            JLabel status = new JLabel("Alright, let\u2019s hunt down some fresh codes\u2026");
            status.setForeground(TEXT);
            status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            toolbar.add(status, BorderLayout.CENTER);
            JButton rescan = new JButton("Wanna see me do it again?");
            rescan.setFocusPainted(false);
            rescan.setBackground(new Color(0x3a5b8cff, true));
            rescan.setForeground(Color.WHITE);
            rescan.setOpaque(true);
            toolbar.add(rescan, BorderLayout.EAST);
            north.add(toolbar);
            bg.add(north, BorderLayout.NORTH);

            final JPanel results = new JPanel(new BorderLayout());
            results.setOpaque(false);
            bg.add(results, BorderLayout.CENTER);

            Runnable startScan = () -> {
                setWaiting(results, "Hang tight, sniffing around for new codes\u2026");
                new RescanJob(results, status).execute();
            };

            rescan.addActionListener(e -> startScan.run());

            frame.setVisible(true);
            startScan.run();
        });
    }

    private static JLabel headerImage() {
        BufferedImage raw = load("/zzz/header.png");
        if (raw == null) {
            return null;
        }
        int w = 960;
        int h = raw.getHeight() * w / Math.max(1, raw.getWidth());
        Image img = raw.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        JLabel head = new JLabel(new ImageIcon(img), SwingConstants.CENTER);
        head.setOpaque(false);
        return head;
    }

    private static void setWaiting(JPanel results, String text) {
        results.removeAll();
        JLabel wait = new JLabel(text, SwingConstants.CENTER);
        wait.setForeground(TEXT);
        wait.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        results.add(wait, BorderLayout.CENTER);
        results.revalidate();
        results.repaint();
    }

    private static final class RescanJob extends SwingWorker<Void, Void> {

        private final JPanel results;
        private final JLabel status;

        RescanJob(JPanel results, JLabel status) {
            this.results = results;
            this.status = status;
        }

        @Override
        protected Void doInBackground() throws Exception {
            Scanner.ResultTable table = Scanner.scan();
            cards(results, table);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
                status.setText("");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                String msg = e.getCause() == null ? e.toString()
                        : (e.getCause().getMessage() == null ? e.getCause().toString() : e.getCause().getMessage());
                status.setText("silly us, that hiccuped");
                results.removeAll();
                JLabel label = new JLabel("<html><div style='text-align:center;'>Oops, the hunt glitched:<br>"
                        + esc(msg) + "</div></html>", SwingConstants.CENTER);
                label.setForeground(RED);
                results.add(label, BorderLayout.CENTER);
                results.revalidate();
                results.repaint();
            }
        }
    }

    private static void cards(JPanel results, Scanner.ResultTable table) {
        List<Scanner.Aggregated> active = groupOf(table, "active?");
        List<Scanner.Aggregated> unverified = groupOf(table, "");
        List<Scanner.Aggregated> expired = groupOf(table, "mixed?", "expired?");

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(new EmptyBorder(4, 16, 14, 16));

        addHeader(list, "Fresh & ready to claim", GREEN);
        for (Scanner.Aggregated a : active) {
            addCard(list, a);
        }
        addHeader(list, "Might still work", AMBER);
        for (Scanner.Aggregated a : unverified) {
            addCard(list, a);
        }
        addHeader(list, "Probably let go", RED);
        for (Scanner.Aggregated a : expired) {
            addCard(list, a);
        }

        JScrollPane scroll = new JScrollPane(list,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.setBorder(null);

        SwingUtilities.invokeLater(() -> {
            results.removeAll();
            results.add(scroll, BorderLayout.CENTER);
            results.revalidate();
            results.repaint();
        });
    }

    private static List<Scanner.Aggregated> groupOf(Scanner.ResultTable table, String... labels) {
        List<Scanner.Aggregated> out = new ArrayList<>();
        for (Scanner.Aggregated a : table.codes()) {
            for (String l : labels) {
                if (a.label().equals(l)) {
                    out.add(a);
                }
            }
        }
        return out;
    }

    private static void addHeader(JPanel list, String title, Color color) {
        boolean empty = list.getComponentCount() == 0;
        list.add(Box.createVerticalStrut(empty ? 2 : 14));
        JLabel h = new JLabel(title);
        h.setForeground(color);
        h.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        h.setBorder(new EmptyBorder(0, 2, 4, 0));
        list.add(h);
    }

    private static void addCard(JPanel list, Scanner.Aggregated a) {
        CodeCard card = new CodeCard(a);
        card.setAlignmentX(0f);
        card.setBorder(new EmptyBorder(8, 12, 10, 12));
        card.setMaximumSize(new java.awt.Dimension(card.getMaximumSize().width,
                card.getPreferredSize().height));
        list.add(Box.createVerticalStrut(6));
        list.add(card);
    }

    private static final class CodeCard extends JPanel {

        CodeCard(Scanner.Aggregated a) {
            setOpaque(false);
            setLayout(new BorderLayout());

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            top.setOpaque(false);

            JLabel code = new JLabel(a.code());
            code.setForeground(statusColor(a.label()));
            code.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            top.add(code);

            StringBuilder meta = new StringBuilder();
            meta.append(a.sources()).append(a.sources() == 1 ? " source" : " sources");
            JLabel metaLabel = new JLabel(meta.toString());
            metaLabel.setForeground(MUTED);
            metaLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            metaLabel.setBorder(new EmptyBorder(1, 8, 0, 0));
            top.add(metaLabel);
            add(top, BorderLayout.NORTH);

            String reward = Rewards.display(a.reward(), a.snippet());
            JLabel rewardLabel = new JLabel(reward == null ? "Didn\u2019t catch the rewards for this one" : "<html>" + esc(reward) + "</html>");
            rewardLabel.setForeground(TEXT);
            rewardLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            rewardLabel.setBorder(new EmptyBorder(2, 0, 4, 0));
            add(rewardLabel, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            bottom.setOpaque(false);
            JLabel link = new JLabel("<html><a href='" + REDEEM_URL + esc(a.code()) + "' style='color:" + css(ACCENT) + ";'>Redeem</a></html>");
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    open(REDEEM_URL + a.code());
                }
            });
            bottom.add(link);
            JLabel from = new JLabel(" \u00b7 spotted on: " + String.join(", ", a.sourceHosts()));
            from.setForeground(MUTED);
            from.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            bottom.add(from);
            add(bottom, BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_FILL);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(CARD_EDGE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static Color statusColor(String label) {
        if (label.equals("active?")) {
            return GREEN;
        }
        if (label.equals("expired?") || label.equals("mixed?")) {
            return RED;
        }
        return AMBER;
    }

    private static final class BgPanel extends JPanel {
        private final BufferedImage bg;

        BgPanel() {
            setOpaque(true);
            setBackground(BG);
            bg = load("/zzz/bg.png");
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            if (bg != null) {
                g2.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
                g2.setColor(new Color(0f, 0f, 0f, 0.40f));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    private static BufferedImage load(String path) {
        try (InputStream in = Gui.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("warn: missing embedded image " + path);
                return null;
            }
            return ImageIO.read(in);
        } catch (Exception e) {
            System.err.println("warn: could not load " + path + " (" + e + ")");
            return null;
        }
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
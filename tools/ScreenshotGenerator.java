package tools;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

public class ScreenshotGenerator {

    private static final File OUTPUT_DIR = new File("files/screenshots");

    public static void main(String[] args) throws Exception {
        OUTPUT_DIR.mkdirs();

        generateOverviewDark();
        generateBikeEditDark();
        generateOnboardingDark();
        generateWidgetSingleBikeDark();
        generateWidgetSingleBikeLight();
        generateWidgetAllBikesDark();
        generateWidgetAllBikesLight();
        generateWidgetsShowcaseBanner(OUTPUT_DIR);

        System.out.println("Screenshots generated with crisp custom iconography and clean English typography!");
    }

    private static void setupAntialiasing(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static void drawStatusBar(Graphics2D g, int width, boolean isDark) {
        g.setColor(isDark ? new Color(0xF0, 0xF4, 0xF8) : new Color(0x0E, 0x12, 0x17));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("09:41", 60, 60);

        // Right status glyphs
        int rightX = width - 240;
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.drawString("5G  WiFi", rightX, 60);
        drawPill(g, width - 110, 42, 50, 24, Color.LIGHT_GRAY, null, 6);
        drawPill(g, width - 108, 44, 40, 20, isDark ? new Color(0xD4, 0xFF, 0x00) : new Color(0x43, 0x59, 0x00), null, 4);
    }

    private static void drawNavBar(Graphics2D g, int width, int height) {
        drawPill(g, width / 2 - 120, height - 30, 240, 10, new Color(0x60, 0x70, 0x80), null, 5);
    }

    private static void drawPill(Graphics2D g, int x, int y, int w, int h, Color bg, Color border, int radius) {
        g.setColor(bg);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, radius, radius));
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(x, y, w, h, radius, radius));
        }
    }

    private static void drawCard(Graphics2D g, int x, int y, int w, int h, Color bg, Color border, int radius) {
        drawPill(g, x, y, w, h, bg, border, radius);
    }

    private static void drawMiniBikeIcon(Graphics2D g, int cx, int cy, int size, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f));
        int r = size / 4;
        // Front & Rear Wheels
        g.drawOval(cx - size / 2, cy - r / 2, r * 2, r * 2);
        g.drawOval(cx + size / 2 - r * 2, cy - r / 2, r * 2, r * 2);
        // Frame lines
        int rx = cx - size / 2 + r;
        int ry = cy + r / 2;
        int fx = cx + size / 2 - r;
        int fy = cy + r / 2;
        int bbX = cx - size / 8;
        int bbY = cy + r / 2;
        int seatX = cx - size / 5;
        int seatY = cy - r;
        int barX = cx + size / 5;
        int barY = cy - r;

        g.drawLine(rx, ry, bbX, bbY);
        g.drawLine(bbX, bbY, seatX, seatY);
        g.drawLine(rx, ry, seatX, seatY);
        g.drawLine(bbX, bbY, barX, barY);
        g.drawLine(seatX, seatY, barX, barY);
        g.drawLine(barX, barY, fx, fy);
    }

    private static void drawMiniGearIcon(Graphics2D g, int cx, int cy, int size, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(cx - size / 3, cy - size / 3, size * 2 / 3, size * 2 / 3);
        int r1 = size / 3;
        int r2 = size / 2;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int x1 = (int) (cx + r1 * Math.cos(angle));
            int y1 = (int) (cy + r1 * Math.sin(angle));
            int x2 = (int) (cx + r2 * Math.cos(angle));
            int y2 = (int) (cy + r2 * Math.sin(angle));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private static void drawMiniSunIcon(Graphics2D g, int cx, int cy, int r, Color color) {
        g.setColor(color);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int x1 = (int) (cx + (r + 3) * Math.cos(angle));
            int y1 = (int) (cy + (r + 3) * Math.sin(angle));
            int x2 = (int) (cx + (r + 7) * Math.cos(angle));
            int y2 = (int) (cy + (r + 7) * Math.sin(angle));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private static void drawMiniRainIcon(Graphics2D g, int cx, int cy, int size, Color color) {
        g.setColor(color);
        // Small cloud
        g.fillOval(cx - size / 2, cy - size / 4, size / 2, size / 2);
        g.fillOval(cx - size / 4, cy - size / 2, size * 3 / 5, size * 3 / 5);
        g.fillOval(cx + size / 8, cy - size / 4, size / 2, size / 2);
        // Drops
        g.setStroke(new BasicStroke(2f));
        g.drawLine(cx - 6, cy + size / 4, cx - 10, cy + size / 2);
        g.drawLine(cx + 4, cy + size / 4, cx, cy + size / 2);
        g.drawLine(cx + 14, cy + size / 4, cx + 10, cy + size / 2);
    }

    private static void drawSectionHeader(Graphics2D g, int x, int y, String text) {
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString(text, x, y);
    }

    private static void drawTextField(Graphics2D g, int x, int y, int w, String label, String value) {
        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString(label, x, y);

        drawCard(g, x, y + 10, w, 75, new Color(0x0E, 0x12, 0x17), new Color(0x30, 0x38, 0x46), 14);
        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(value, x + 20, y + 58);
    }

    private static void drawTutorialCard(Graphics2D g, int x, int y, int w, String title, String desc, int step) {
        drawCard(g, x, y, w, 140, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 20);

        // Step number badge
        drawPill(g, x + 25, y + 40, 56, 56, new Color(0x26, 0x33, 0x00), new Color(0x5A, 0x75, 0x00), 28);
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(String.valueOf(step), x + 46, y + 78);

        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(title, x + 105, y + 55);

        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString(desc, x + 105, y + 100);
    }

    private static void drawCenteredString(Graphics2D g, String text, int centerX, int y) {
        FontMetrics metrics = g.getFontMetrics();
        int x = centerX - metrics.stringWidth(text) / 2;
        g.drawString(text, x, y);
    }

    private static void generateOverviewDark() throws Exception {
        int width = 1080;
        int height = 2340;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g);

        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.fillRect(0, 0, width, height);

        drawStatusBar(g, width, true);

        int y = 140;
        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        g.drawString("My Bikes & Pressures", 60, y);

        y += 40;
        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.PLAIN, 30));
        g.drawString("High precision scientific calculator | Frank Berto Model", 60, y);

        y += 60;
        drawPill(g, 60, y, 260, 70, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 16);
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Rider: 75.0 kg", 90, y + 46);

        int unitX = width - 420;
        drawPill(g, unitX, y, 360, 70, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 16);
        drawPill(g, unitX + 6, y + 6, 110, 58, new Color(0xD4, 0xFF, 0x00), null, 14);
        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.drawString("BAR", unitX + 32, y + 46);
        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.drawString("PSI", unitX + 155, y + 46);
        g.drawString("kPa", unitX + 270, y + 46);

        y += 120;
        drawBikeCard(
            g, 50, y, width - 100,
            "Specialized S-Works Tarmac SL7", "ROAD", "7.2 kg",
            List.of(
                new SetupData("Roval Rapide CLX 28mm Slicks", "4.6 bar", "4.9 bar", "4.1 bar", "4.4 bar", true),
                new SetupData("Winter Training 30mm Wheels", "4.1 bar", "4.4 bar", "3.7 bar", "4.0 bar", false)
            )
        );

        y += 660;
        drawBikeCard(
            g, 50, y, width - 100,
            "Canyon Grizl CF SL 8", "GRAVEL", "9.1 kg",
            List.of(
                new SetupData("Gravel 40mm Tubeless Setup", "2.3 bar", "2.5 bar", "2.1 bar", "2.3 bar", true),
                new SetupData("Adventure 45mm Mud Knobbies", "2.0 bar", "2.2 bar", "1.8 bar", "2.0 bar", false)
            )
        );

        int fabY = height - 260;
        int fabX = width - 260;
        drawPill(g, fabX, fabY, 200, 100, new Color(0xD4, 0xFF, 0x00), null, 50);
        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        g.drawString("+ Bike", fabX + 35, fabY + 65);

        drawNavBar(g, width, height);
        g.dispose();
        ImageIO.write(img, "PNG", new File(OUTPUT_DIR, "screenshot_overview_dark.png"));
    }

    private static void generateBikeEditDark() throws Exception {
        int width = 1080;
        int height = 2340;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g) ;

        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.fillRect(0, 0, width, height);

        drawStatusBar(g, width, true);

        int y = 140;
        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 48));
        g.drawString("<-  Edit Bike & Wheelsets", 60, y);

        drawPill(g, width - 260, y - 48, 200, 68, new Color(0xD4, 0xFF, 0x00), null, 34);
        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("Save", width - 190, y - 4);

        y += 80;
        drawSectionHeader(g, 60, y, "1. BIKE SPECIFICATIONS");
        y += 40;
        drawCard(g, 50, y, width - 100, 260, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 24);
        drawTextField(g, 80, y + 30, width - 160, "Bike Name", "Canyon Grizl CF SL 8");
        drawTextField(g, 80, y + 140, width - 160, "Bike Weight (kg)", "9.1");

        y += 310;
        drawSectionHeader(g, 60, y, "2. RIDING DISCIPLINE");
        y += 40;
        drawCard(g, 50, y, width - 100, 160, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 24);
        drawPill(g, 80, y + 45, 180, 65, new Color(0xD4, 0xFF, 0x00), null, 20);
        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("Gravel", 130, y + 87);

        String[] styles = {"Road", "XC MTB", "Trail", "Enduro"};
        int styleX = 280;
        for (String st : styles) {
            drawPill(g, styleX, y + 45, 150, 65, new Color(0x21, 0x27, 0x32), new Color(0x30, 0x38, 0x46), 20);
            g.setColor(new Color(0xF0, 0xF4, 0xF8));
            g.setFont(new Font("SansSerif", Font.PLAIN, 24));
            g.drawString(st, styleX + 35, y + 87);
            styleX += 170;
        }

        y += 210;
        drawSectionHeader(g, 60, y, "3. WHEELSET & TIRE CONFIGURATION");
        y += 40;
        drawCard(g, 50, y, width - 100, 720, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 24);

        drawTextField(g, 80, y + 30, width - 160, "Setup Profile Name", "Gravel 40mm Tubeless Setup");
        drawTextField(g, 80, y + 140, 440, "Front Tire (mm)", "40.0");
        drawTextField(g, 540, y + 140, 440, "Rear Tire (mm)", "40.0");

        drawTextField(g, 80, y + 250, width - 160, "Wheel Diameter", "700c (28\" / 622 mm)");
        drawTextField(g, 80, y + 360, width - 160, "Rim Construction", "Tubeless Hookless (Straight Side)");
        drawTextField(g, 80, y + 470, width - 160, "Inner Rim Width (mm)", "24.0");
        drawTextField(g, 80, y + 580, width - 160, "Tire Casing", "Standard Supple (120 TPI)");

        y += 770;
        drawCard(g, 50, y, width - 100, 240, new Color(0x21, 0x2F, 0x05), new Color(0x5A, 0x75, 0x00), 24);
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("LIVE REAL-TIME PRESSURE CALCULATION", 90, y + 60);

        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        g.drawString("FRONT: 2.3 bar", 90, y + 140);
        g.drawString("REAR: 2.5 bar", 550, y + 140);

        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        g.drawString("DRY Surface Baseline | System Weight: 84.1 kg | Hookless Safety: PASSED", 90, y + 195);

        drawNavBar(g, width, height);
        g.dispose();
        ImageIO.write(img, "PNG", new File(OUTPUT_DIR, "screenshot_bike_edit_dark.png"));
    }

    private static void generateOnboardingDark() throws Exception {
        int width = 1080;
        int height = 2340;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g);

        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.fillRect(0, 0, width, height);

        drawStatusBar(g, width, true);

        int y = 280;
        drawPill(g, width / 2 - 90, y, 180, 180, new Color(0x16, 0x1B, 0x22), new Color(0xD4, 0xFF, 0x00), 90);
        drawMiniBikeIcon(g, width / 2, y + 90, 70, new Color(0xD4, 0xFF, 0x00));

        y += 260;
        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 56));
        drawCenteredString(g, "Welcome to TirePressure", width / 2, y);

        y += 50;
        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.PLAIN, 30));
        drawCenteredString(g, "Scientific tire pressure companion for Road, Gravel & MTB", width / 2, y);

        y += 100;
        drawCard(g, 80, y, width - 160, 380, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 24);

        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString("What is your standard riding weight?", 130, y + 70);

        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        g.drawString("This benchmark will calibrate pressures across all your wheelsets.", 130, y + 120);

        drawCard(g, 130, y + 160, width - 260, 140, new Color(0x0E, 0x12, 0x17), new Color(0xD4, 0xFF, 0x00), 16);
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 72));
        g.drawString("75.0", 380, y + 258);
        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.drawString("kg", 580, y + 258);

        y += 440;
        drawTutorialCard(g, 80, y, width - 160, "Multiple Setups Per Bike", "Save distinct summer, winter, or gravel tire setups for each bike.", 1);
        y += 170;
        drawTutorialCard(g, 80, y, width - 160, "Multi-Surface Pressures", "Real-time readings for Dry tarmac, Wet asphalt, and Mud/Snow.", 2);
        y += 170;
        drawTutorialCard(g, 80, y, width - 160, "ETRTO Hookless Guard", "Active alerts whenever pressures exceed straight-side safety limits.", 3);

        y = height - 280;
        drawPill(g, 80, y, width - 160, 110, new Color(0xD4, 0xFF, 0x00), null, 55);
        g.setColor(new Color(0x0E, 0x12, 0x17));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        drawCenteredString(g, "Get Started  ->", width / 2, y + 68);

        drawNavBar(g, width, height);
        g.dispose();
        ImageIO.write(img, "PNG", new File(OUTPUT_DIR, "screenshot_onboarding_dark.png"));
    }

    private static void generateWidgetSingleBikeDark() throws Exception {
        int width = 800;
        int height = 440;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g);

        drawCard(g, 10, 10, width - 20, height - 20, new Color(0x10, 0x15, 0x1D), new Color(0x26, 0x31, 0x40), 36);

        int y = 65;
        drawPill(g, 40, 38, 54, 54, new Color(0x21, 0x2A, 0x38), null, 27);
        drawMiniBikeIcon(g, 67, 65, 30, new Color(0xD4, 0xFF, 0x00));

        g.setColor(new Color(0xF2, 0xF5, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("Canyon Grizl CF SL", 110, y);

        y += 32;
        drawPill(g, 110, y - 22, 110, 32, new Color(0x23, 0x2C, 0x3B), null, 10);
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("GRAVEL", 126, y - 1);

        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("| 9.1 kg", 235, y);

        drawPill(g, width - 90, 38, 50, 50, new Color(0x21, 0x2A, 0x38), null, 25);
        drawMiniGearIcon(g, width - 65, 63, 20, new Color(0xCB, 0xD5, 0xE1));

        y = 135;
        drawWidgetSetupCard(
            g, 40, y, width - 80, 130,
            "Gravel 40mm Tubeless Setup", "2.3 bar", "2.5 bar",
            "Wet Condition : 2.1 bar | 2.3 bar", true
        );

        y = 280;
        drawWidgetSetupCard(
            g, 40, y, width - 80, 105,
            "Road Training 30mm Wheels", "3.9 bar", "4.2 bar",
            "Wet Condition : 3.5 bar | 3.8 bar", true
        );

        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Rider: 75.0 kg", 45, height - 24);

        drawMiniSunIcon(g, width - 110, height - 30, 8, new Color(0xD4, 0xFF, 0x00));
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("DRY", width - 95, height - 24);

        g.dispose();
        ImageIO.write(img, "PNG", new File(OUTPUT_DIR, "screenshot_widget_single_bike_dark.png"));
    }

    private static void generateWidgetSingleBikeLight() throws Exception {
        int width = 800;
        int height = 440;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g);

        drawCard(g, 10, 10, width - 20, height - 20, new Color(0xFF, 0xFF, 0xFF), new Color(0xE2, 0xE8, 0xF0), 36);

        int y = 65;
        drawPill(g, 40, 38, 54, 54, new Color(0xED, 0xF2, 0xF7), null, 27);
        drawMiniBikeIcon(g, 67, 65, 30, new Color(0x5A, 0x75, 0x00));

        g.setColor(new Color(0x0F, 0x17, 0x2A));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("Canyon Grizl CF SL", 110, y);

        y += 32;
        drawPill(g, 110, y - 22, 110, 32, new Color(0xE2, 0xE8, 0xF0), null, 10);
        g.setColor(new Color(0x47, 0x55, 0x69));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("GRAVEL", 126, y - 1);

        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("| 9.1 kg", 235, y);

        drawPill(g, width - 90, 38, 50, 50, new Color(0xED, 0xF2, 0xF7), null, 25);
        drawMiniGearIcon(g, width - 65, 63, 20, new Color(0x47, 0x55, 0x69));

        y = 135;
        drawWidgetSetupCard(
            g, 40, y, width - 80, 130,
            "Gravel 40mm Tubeless Setup", "2.3 bar", "2.5 bar",
            "Wet Condition : 2.1 bar | 2.3 bar", false
        );

        y = 280;
        drawWidgetSetupCard(
            g, 40, y, width - 80, 105,
            "Road Training 30mm Wheels", "3.9 bar", "4.2 bar",
            "Wet Condition : 3.5 bar | 3.8 bar", false
        );

        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Rider: 75.0 kg", 45, height - 24);

        drawMiniSunIcon(g, width - 110, height - 30, 8, new Color(0x5A, 0x75, 0x00));
        g.setColor(new Color(0x5A, 0x75, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("DRY", width - 95, height - 24);

        g.dispose();
        ImageIO.write(img, "PNG", new File(OUTPUT_DIR, "screenshot_widget_single_bike_light.png"));
    }

    private static void generateWidgetAllBikesDark() throws Exception {
        int width = 800;
        int height = 620;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g);

        drawCard(g, 10, 10, width - 20, height - 20, new Color(0x10, 0x15, 0x1D), new Color(0x26, 0x31, 0x40), 36);

        int y = 65;
        drawPill(g, 40, 38, 54, 54, new Color(0x21, 0x2A, 0x38), null, 27);
        drawMiniBikeIcon(g, 67, 65, 30, new Color(0xD4, 0xFF, 0x00));

        g.setColor(new Color(0xF2, 0xF5, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("TirePressure", 110, y);

        g.setColor(new Color(0x94, 0xA3, 0xB8));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("All Bikes & Profiles Overview", 110, y + 28);

        drawPill(g, width - 90, 38, 50, 50, new Color(0x21, 0x2A, 0x38), null, 25);
        drawMiniGearIcon(g, width - 65, 63, 20, new Color(0xCB, 0xD5, 0xE1));

        y = 125;
        drawAllBikesItem(
            g, 40, y, width - 80, 145,
            "Specialized S-Works Tarmac SL7", "ROAD", "7.2 kg",
            "Roval Rapide CLX 28mm Slicks", "FRONT 4.6 bar   REAR 4.9 bar", "WET: 4.1 / 4.4 bar", true
        );

        y += 160;
        drawAllBikesItem(
            g, 40, y, width - 80, 145,
            "Canyon Grizl CF SL 8", "GRAVEL", "9.1 kg",
            "Gravel 40mm Tubeless Setup", "FRONT 2.3 bar   REAR 2.5 bar", "WET: 2.1 / 2.3 bar", true
        );

        y += 160;
        drawAllBikesItem(
            g, 40, y, width - 80, 145,
            "Santa Cruz Megatower C", "ENDURO MTB", "15.0 kg",
            "Maxxis Assegai 2.5\" DoubleDown", "FRONT 1.6 bar   REAR 1.8 bar", "WET: 1.4 / 1.6 bar", true
        );

        g.dispose();
        ImageIO.write(img, "PNG", new File(OUTPUT_DIR, "screenshot_widget_all_bikes_dark.png"));
    }

    private static void generateWidgetAllBikesLight() throws Exception {
        int width = 800;
        int height = 620;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g);

        drawCard(g, 10, 10, width - 20, height - 20, new Color(0xFF, 0xFF, 0xFF), new Color(0xE2, 0xE8, 0xF0), 36);

        int y = 65;
        drawPill(g, 40, 38, 54, 54, new Color(0xED, 0xF2, 0xF7), null, 27);
        drawMiniBikeIcon(g, 67, 65, 30, new Color(0x5A, 0x75, 0x00));

        g.setColor(new Color(0x0F, 0x17, 0x2A));
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        g.drawString("TirePressure", 110, y);

        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.drawString("All Bikes & Profiles Overview", 110, y + 28);

        drawPill(g, width - 90, 38, 50, 50, new Color(0xED, 0xF2, 0xF7), null, 25);
        drawMiniGearIcon(g, width - 65, 63, 20, new Color(0x47, 0x55, 0x69));

        y = 125;
        drawAllBikesItem(
            g, 40, y, width - 80, 145,
            "Specialized S-Works Tarmac SL7", "ROAD", "7.2 kg",
            "Roval Rapide CLX 28mm Slicks", "FRONT 4.6 bar   REAR 4.9 bar", "WET: 4.1 / 4.4 bar", false
        );

        y += 160;
        drawAllBikesItem(
            g, 40, y, width - 80, 145,
            "Canyon Grizl CF SL 8", "GRAVEL", "9.1 kg",
            "Gravel 40mm Tubeless Setup", "FRONT 2.3 bar   REAR 2.5 bar", "WET: 2.1 / 2.3 bar", false
        );

        y += 160;
        drawAllBikesItem(
            g, 40, y, width - 80, 145,
            "Santa Cruz Megatower C", "ENDURO MTB", "15.0 kg",
            "Maxxis Assegai 2.5\" DoubleDown", "FRONT 1.6 bar   REAR 1.8 bar", "WET: 1.4 / 1.6 bar", false
        );

        g.dispose();
        ImageIO.write(img, "PNG", new File(OUTPUT_DIR, "screenshot_widget_all_bikes_light.png"));
    }

    private static void generateWidgetsShowcaseBanner(File targetDir) throws Exception {
        int width = 1680;
        int height = 720;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        setupAntialiasing(g);

        g.setColor(new Color(0x0A, 0x0D, 0x12));
        g.fillRect(0, 0, width, height);

        RadialGradientPaint glow = new RadialGradientPaint(
            new Point(width / 2, height / 2),
            650f,
            new float[]{0.0f, 1.0f},
            new Color[]{new Color(0x2A, 0x38, 0x05, 0x99), new Color(0x0A, 0x0D, 0x12, 0x00)}
        );
        g.setPaint(glow);
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("OXYGENOS 16/17 & COLOROS 16/17 DESIGN LANGUAGE | HOME SCREEN WIDGETS", 60, 60);

        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 42));
        g.drawString("Native Android Widgets: Single Bike Setups & All Bikes Overview", 60, 110);

        BufferedImage singleDark = ImageIO.read(new File(targetDir, "screenshot_widget_single_bike_dark.png"));
        BufferedImage singleLight = ImageIO.read(new File(targetDir, "screenshot_widget_single_bike_light.png"));
        BufferedImage allDark = ImageIO.read(new File(targetDir, "screenshot_widget_all_bikes_dark.png"));

        g.drawImage(singleDark, 50, 150, 500, 275, null);
        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("Widget 1: Selected Bike (Dark Mode)", 60, 455);

        g.drawImage(singleLight, 580, 150, 500, 275, null);
        g.drawString("Widget 1: Selected Bike (Light Mode)", 590, 455);

        g.drawImage(allDark, 1110, 150, 520, 403, null);
        g.drawString("Widget 2: All Bikes List Overview", 1120, 585);

        int pillY = height - 65;
        drawTagPill(g, 60, pillY, "OxygenOS 16/17 Glass & Pills");
        drawTagPill(g, 450, pillY, "Light, Dark & System Theme Modes");
        drawTagPill(g, 880, pillY, "Instant Live Sync | 100% Offline");

        g.dispose();
        ImageIO.write(img, "PNG", new File(targetDir, "screenshot_widgets_banner.png"));
    }

    private static void drawTagPill(Graphics2D g, int x, int y, String text) {
        int w = text.length() * 11 + 34;
        drawPill(g, x, y, w, 44, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 22);
        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(text, x + 16, y + 28);
    }

    private static void drawBikeCard(
        Graphics2D g, int x, int y, int width,
        String bikeName, String discipline, String bikeWeight,
        List<SetupData> setups
    ) {
        drawCard(g, x, y, width, 620, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 28);

        int curY = y + 60;
        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString(bikeName, x + 40, curY);

        curY += 40;
        drawPill(g, x + 40, curY - 26, 120, 36, new Color(0x26, 0x33, 0x00), new Color(0x5A, 0x75, 0x00), 10);
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(discipline, x + 58, curY);

        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.drawString("| Weight: " + bikeWeight, x + 180, curY);

        curY += 30;
        for (SetupData setup : setups) {
            drawSetupCardInOverview(g, x + 35, curY, width - 70, setup);
            curY += 210;
        }
    }

    private static void drawSetupCardInOverview(Graphics2D g, int x, int y, int w, SetupData setup) {
        drawCard(g, x, y, w, 190, new Color(0x21, 0x27, 0x32), new Color(0x30, 0x38, 0x46), 18);

        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString(setup.name, x + 25, y + 42);

        if (setup.isDefault) {
            drawPill(g, x + w - 160, y + 16, 135, 34, new Color(0x26, 0x33, 0x00), new Color(0x5A, 0x75, 0x00), 8);
            g.setColor(new Color(0xD4, 0xFF, 0x00));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("PRIMARY", x + w - 140, y + 39);
        }

        drawCard(g, x + 25, y + 60, (w - 70) / 2, 75, new Color(0x16, 0x1B, 0x22), new Color(0xD4, 0xFF, 0x00), 12);
        drawMiniSunIcon(g, x + 50, y + 96, 7, new Color(0xD4, 0xFF, 0x00));
        g.setColor(new Color(0xD4, 0xFF, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("DRY", x + 70, y + 105);
        g.setColor(new Color(0xF0, 0xF4, 0xF8));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("F " + setup.frontDry + "  |  R " + setup.rearDry, x + 140, y + 105);

        drawCard(g, x + 35 + (w - 70) / 2, y + 60, (w - 70) / 2, 75, new Color(0x16, 0x1B, 0x22), new Color(0x30, 0x38, 0x46), 12);
        drawMiniRainIcon(g, x + 55 + (w - 70) / 2, y + 96, 18, new Color(0x8B, 0x94, 0x9E));
        g.setColor(new Color(0x8B, 0x94, 0x9E));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("WET", x + 75 + (w - 70) / 2, y + 105);
        g.setColor(new Color(0xCB, 0xD5, 0xE1));
        g.setFont(new Font("SansSerif", Font.BOLD, 26));
        g.drawString("F " + setup.frontWet + "  |  R " + setup.rearWet, x + 195 + (w - 70) / 2, y + 105);

        g.setColor(new Color(0x64, 0x74, 0x8B));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Slippery/Snow: 55% pressure (F 1.2 bar | R 1.4 bar) | ISO / ETRTO Hookless Compliant", x + 25, y + 165);
    }

    private static void drawWidgetSetupCard(
        Graphics2D g, int x, int y, int w, int h,
        String title, String frontPressure, String rearPressure,
        String wetInfo, boolean isDark
    ) {
        Color bg = isDark ? new Color(0x19, 0x20, 0x2B) : new Color(0xF3, 0xF6, 0xFA);
        Color border = isDark ? new Color(0x2B, 0x37, 0x47) : new Color(0xE2, 0xE8, 0xF0);
        drawCard(g, x, y, w, h, bg, border, 18);

        g.setColor(isDark ? new Color(0xF2, 0xF5, 0xF8) : new Color(0x0F, 0x17, 0x2A));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(title, x + 20, y + 32);

        int pillW = (w - 60) / 2;
        Color pillBg = isDark ? new Color(0x12, 0x17, 0x20) : new Color(0xFF, 0xFF, 0xFF);
        Color pillBorder = isDark ? new Color(0x2A, 0x35, 0x46) : new Color(0xCB, 0xD5, 0xE1);

        drawCard(g, x + 20, y + 45, pillW, 46, pillBg, pillBorder, 12);
        g.setColor(isDark ? new Color(0xCB, 0xD5, 0xE1) : new Color(0x47, 0x55, 0x69));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("F", x + 35, y + 74);
        g.setColor(isDark ? new Color(0xD4, 0xFF, 0x00) : new Color(0x43, 0x59, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(frontPressure, x + pillW - 85, y + 75);

        drawCard(g, x + 30 + pillW, y + 45, pillW, 46, pillBg, pillBorder, 12);
        g.setColor(isDark ? new Color(0xCB, 0xD5, 0xE1) : new Color(0x47, 0x55, 0x69));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("R", x + 45 + pillW, y + 74);
        g.setColor(isDark ? new Color(0xD4, 0xFF, 0x00) : new Color(0x43, 0x59, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(rearPressure, x + 20 + 2 * pillW - 85, y + 75);

        if (h > 115) {
            drawMiniRainIcon(g, x + 28, y + 110, 14, isDark ? new Color(0x8B, 0x94, 0x9E) : new Color(0x64, 0x74, 0x8B));
            g.setColor(isDark ? new Color(0x8B, 0x94, 0x9E) : new Color(0x64, 0x74, 0x8B));
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString(wetInfo, x + 45, y + 115);
        }
    }

    private static void drawAllBikesItem(
        Graphics2D g, int x, int y, int w, int h,
        String bikeName, String discipline, String bikeWeight,
        String setupName, String pressures, String wet, boolean isDark
    ) {
        Color bg = isDark ? new Color(0x19, 0x20, 0x2B) : new Color(0xF3, 0xF6, 0xFA);
        Color border = isDark ? new Color(0x2B, 0x37, 0x47) : new Color(0xE2, 0xE8, 0xF0);
        drawCard(g, x, y, w, h, bg, border, 18);

        g.setColor(isDark ? new Color(0xF2, 0xF5, 0xF8) : new Color(0x0F, 0x17, 0x2A));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString(bikeName, x + 20, y + 36);

        drawPill(g, x + w - 240, y + 16, 110, 28, isDark ? new Color(0x23, 0x2C, 0x3B) : new Color(0xE2, 0xE8, 0xF0), null, 8);
        g.setColor(isDark ? new Color(0xD4, 0xFF, 0x00) : new Color(0x47, 0x55, 0x69));
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString(discipline, x + w - 225, y + 35);

        g.setColor(isDark ? new Color(0x94, 0xA3, 0xB8) : new Color(0x64, 0x74, 0x8B));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("| " + bikeWeight, x + w - 115, y + 36);

        Color pillBg = isDark ? new Color(0x12, 0x17, 0x20) : new Color(0xFF, 0xFF, 0xFF);
        Color pillBorder = isDark ? new Color(0x2A, 0x35, 0x46) : new Color(0xCB, 0xD5, 0xE1);
        drawCard(g, x + 20, y + 54, w - 40, 75, pillBg, pillBorder, 12);

        g.setColor(isDark ? new Color(0xCB, 0xD5, 0xE1) : new Color(0x47, 0x55, 0x69));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(setupName, x + 35, y + 80);

        g.setColor(isDark ? new Color(0xD4, 0xFF, 0x00) : new Color(0x43, 0x59, 0x00));
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(pressures, x + 35, y + 112);

        drawMiniRainIcon(g, x + w - 180, y + 107, 14, isDark ? new Color(0x8B, 0x94, 0x9E) : new Color(0x64, 0x74, 0x8B));
        g.setColor(isDark ? new Color(0x8B, 0x94, 0x9E) : new Color(0x64, 0x74, 0x8B));
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString(wet, x + w - 160, y + 112);
    }

    private static class SetupData {
        final String name;
        final String frontDry;
        final String rearDry;
        final String frontWet;
        final String rearWet;
        final boolean isDefault;

        SetupData(String name, String frontDry, String rearDry, String frontWet, String rearWet, boolean isDefault) {
            this.name = name;
            this.frontDry = frontDry;
            this.rearDry = rearDry;
            this.frontWet = frontWet;
            this.rearWet = rearWet;
            this.isDefault = isDefault;
        }
    }
}

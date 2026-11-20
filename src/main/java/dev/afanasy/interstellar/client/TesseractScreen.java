package dev.afanasy.interstellar.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

/** Cinematic timeline made from snapshots of the actual client-visible world. */
public final class TesseractScreen extends Screen {
    private int selectedIndex;
    private long animationTicks;

    public TesseractScreen() {
        super(Text.literal("Interstellar Experience — Tesseract"));
        this.selectedIndex = Math.max(0, TesseractHistory.size() - 1);
    }

    @Override
    public boolean shouldPause() {
        // Time keeps moving behind the Tesseract, just like the scene's time layers.
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        animationTicks++;

        context.fill(0, 0, width, height, 0xE9080D1A);
        drawStarfield(context);
        drawGrid(context);
        drawHeader(context);
        drawGargantua(context, width - 72, 54, 23);

        List<TesseractHistory.Snapshot> snapshots = TesseractHistory.getSnapshots();
        if (!snapshots.isEmpty()) {
            selectedIndex = Math.min(selectedIndex, snapshots.size() - 1);
        } else {
            selectedIndex = 0;
        }

        int margin = 18;
        int gap = 8;
        int gridTop = Math.max(66, height / 8);
        int footerTop = height - 94;
        int usableHeight = Math.max(90, footerTop - gridTop);
        int panelWidth = Math.max(90, (width - margin * 2 - gap * 2) / 3);
        int panelHeight = Math.max(72, (usableHeight - gap * 2) / 3);
        int startX = Math.max(8, (width - (panelWidth * 3 + gap * 2)) / 2);
        int startY = gridTop;

        int emptySlots = 9 - snapshots.size();
        for (int slot = 0; slot < 9; slot++) {
            int row = slot / 3;
            int column = slot % 3;
            int x = startX + column * (panelWidth + gap);
            int y = startY + row * (panelHeight + gap);
            int snapshotIndex = slot - emptySlots;
            if (snapshotIndex >= 0 && snapshotIndex < snapshots.size()) {
                boolean selected = snapshotIndex == selectedIndex;
                drawSnapshotPanel(context, snapshots, snapshotIndex, x, y, panelWidth, panelHeight, selected);
            } else {
                drawEmptyPanel(context, x, y, panelWidth, panelHeight);
            }
        }

        drawFooter(context, snapshots);
    }

    private void drawStarfield(DrawContext context) {
        for (int i = 0; i < 84; i++) {
            int x = Math.floorMod(i * 97 + 31, Math.max(1, width));
            int drift = (int) ((animationTicks / (3 + (i % 5))) % Math.max(1, height));
            int y = Math.floorMod(i * 53 + drift, Math.max(1, height));
            int size = (i % 11 == 0) ? 2 : 1;
            int color = (i % 4 == 0) ? 0x8AB8D8FF : 0x668DA7D8;
            context.fill(x, y, x + size, y + size, color);
        }
    }

    private void drawGrid(DrawContext context) {
        int gridColor = 0x183C79A8;
        int step = 42;
        for (int x = 0; x < width; x += step) {
            context.fill(x, 0, x + 1, height, gridColor);
        }
        for (int y = 0; y < height; y += step) {
            context.fill(0, y, width, y + 1, gridColor);
        }
    }

    private void drawHeader(DrawContext context) {
        context.drawTextWithShadow(textRenderer, "INTERSTELLAR", 20, 14, 0xFFF0E9FF);
        context.drawTextWithShadow(textRenderer, "TESSERACT  //  9 TIME LAYERS", 20, 28, 0xFF9BB5D7);
        context.drawTextWithShadow(textRenderer, "WORLD ECHOES", 20, 41, 0xFF6E8FAE);
        context.drawTextWithShadow(textRenderer, "GARGANTUA", width - 126, 82, 0xFFB9C9E6);
    }

    private void drawGargantua(DrawContext context, int centerX, int centerY, int radius) {
        // Small animated accretion ring, built from thin spans to avoid external shaders.
        int pulse = (int) (Math.sin(animationTicks * 0.045) * 18.0);
        for (int dy = -radius; dy <= radius; dy++) {
            double outer = Math.sqrt(Math.max(0.0, radius * radius - dy * dy));
            double innerRadius = radius * 0.48;
            double inner = Math.abs(dy) < innerRadius
                    ? Math.sqrt(Math.max(0.0, innerRadius * innerRadius - dy * dy)) : 0.0;
            int x1 = centerX - (int) outer;
            int x2 = centerX + (int) outer;
            int leftInner = centerX - (int) inner;
            int rightInner = centerX + (int) inner;
            int warm = clampColor(188 + pulse - Math.abs(dy) * 2, 112, 255);
            int color = 0xFF000000 | (warm << 16) | (clampColor(115 + pulse, 70, 220) << 8) | 210;
            context.fill(x1, centerY + dy, leftInner, centerY + dy + 1, color);
            context.fill(rightInner, centerY + dy, x2 + 1, centerY + dy + 1, color);
        }
        context.fill(centerX - radius / 2, centerY - radius / 2,
                centerX + radius / 2, centerY + radius / 2, 0xFF02030A);
        context.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFF000000);
    }

    private void drawSnapshotPanel(DrawContext context,
                                   List<TesseractHistory.Snapshot> snapshots,
                                   int index,
                                   int x,
                                   int y,
                                   int panelWidth,
                                   int panelHeight,
                                   boolean selected) {
        TesseractHistory.Snapshot snapshot = snapshots.get(index);
        int background = selected ? 0xE51A2940 : 0xC90A1324;
        int border = selected ? 0xFFE5C78A : 0x775E7EAB;
        context.fill(x, y, x + panelWidth, y + panelHeight, background);
        context.fill(x, y, x + panelWidth, y + 2, border);
        context.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, border);
        context.fill(x, y, x + 1, y + panelHeight, border);
        context.fill(x + panelWidth - 1, y, x + panelWidth, y + panelHeight, border);

        String age = timeLabel(snapshots, index);
        context.drawTextWithShadow(textRenderer, age, x + 9, y + 7,
                selected ? 0xFFFFE8B6 : 0xFFB5C7E0);

        int mapCell = Math.max(2, Math.min(5,
                Math.min((panelHeight - 38) / TesseractHistory.MAP_SIZE,
                        (panelWidth - 155) / TesseractHistory.MAP_SIZE)));
        int mapX = x + 9;
        int mapY = y + 24;
        drawWorldMap(context, snapshot, mapX, mapY, mapCell);

        int infoX = mapX + TesseractHistory.MAP_SIZE * mapCell + 11;
        int infoY = y + 30;
        int infoColor = selected ? 0xFFE2E9F5 : 0xFFA6B5CC;
        context.drawTextWithShadow(textRenderer, "X  " + snapshot.centerX(), infoX, infoY, infoColor);
        context.drawTextWithShadow(textRenderer, "Y  " + snapshot.centerY(), infoX, infoY + 12, infoColor);
        context.drawTextWithShadow(textRenderer, "Z  " + snapshot.centerZ(), infoX, infoY + 24, infoColor);
        context.drawTextWithShadow(textRenderer, shortDimension(snapshot.dimension()), infoX, infoY + 39,
                0xFF7E9CBF);

        if (selected) {
            context.fill(x + panelWidth - 7, y + 7, x + panelWidth - 4, y + 10, 0xFFFFE5A6);
        }
    }

    private void drawWorldMap(DrawContext context, TesseractHistory.Snapshot snapshot,
                              int x, int y, int cell) {
        for (int mapZ = 0; mapZ < TesseractHistory.MAP_SIZE; mapZ++) {
            for (int mapX = 0; mapX < TesseractHistory.MAP_SIZE; mapX++) {
                int color = snapshot.colorAt(mapX, mapZ);
                int tileX = x + mapX * cell;
                int tileY = y + mapZ * cell;
                if (color == 0) {
                    color = 0xFF08101E;
                } else {
                    color = shade(color, snapshot.heightAt(mapX, mapZ));
                }
                context.fill(tileX, tileY, tileX + cell - 1, tileY + cell - 1, color);
            }
        }
        context.fill(x - 2, y - 2, x + TesseractHistory.MAP_SIZE * cell + 2, y - 1, 0x667DA3CC);
        context.fill(x - 2, y + TesseractHistory.MAP_SIZE * cell + 1,
                x + TesseractHistory.MAP_SIZE * cell + 2, y + TesseractHistory.MAP_SIZE * cell + 2, 0x667DA3CC);
    }

    private void drawEmptyPanel(DrawContext context, int x, int y, int panelWidth, int panelHeight) {
        context.fill(x, y, x + panelWidth, y + panelHeight, 0x88060B15);
        context.fill(x, y, x + panelWidth, y + 1, 0x333C5B7F);
        context.drawTextWithShadow(textRenderer, "ОЖИДАНИЕ СЛЕДУЮЩЕГО МОМЕНТА",
                x + 10, y + Math.max(10, panelHeight / 2 - 4), 0xFF526783);
    }

    private void drawFooter(DrawContext context, List<TesseractHistory.Snapshot> snapshots) {
        int quoteY = height - 77;
        context.fill(18, quoteY - 8, width - 18, quoteY - 7, 0x665E7DA6);
        context.drawCenteredTextWithShadow(textRenderer,
                "У них есть доступ к бесконечности времени и пространству,",
                width / 2, quoteY, 0xFFE9E6F1);
        context.drawCenteredTextWithShadow(textRenderer,
                "но они не связаны ими, как мы.",
                width / 2, quoteY + 13, 0xFFBCC9DC);

        String status = snapshots.isEmpty()
                ? "СОБИРАЕМ ПЕРВЫЙ ОТПЕЧАТОК МИРА"
                : "СЛОЙ " + (selectedIndex + 1) + "/" + snapshots.size()
                    + "   •   A/D или ←/→ — перемещение во времени";
        context.drawCenteredTextWithShadow(textRenderer, status,
                width / 2, height - 35, 0xFF91A9C8);
        context.drawCenteredTextWithShadow(textRenderer,
                "O / ESC — закрыть     P — сохранить момент     R — повторить голос",
                width / 2, height - 21, 0xFF7187A4);
    }

    private static String timeLabel(List<TesseractHistory.Snapshot> snapshots, int index) {
        if (index == snapshots.size() - 1) {
            return "NOW  //  ТЕКУЩИЙ МОМЕНТ";
        }
        long ageSeconds = Math.max(0L,
                (snapshots.get(snapshots.size() - 1).worldTime() - snapshots.get(index).worldTime()) / 20L);
        return String.format(Locale.ROOT, "T − %02d:%02d", ageSeconds / 60L, ageSeconds % 60L);
    }

    private static String shortDimension(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return "UNKNOWN SPACE";
        }
        int colon = dimension.indexOf(':');
        return (colon >= 0 ? dimension.substring(colon + 1) : dimension).toUpperCase(Locale.ROOT);
    }

    private static int shade(int argb, int height) {
        int multiplier = clampColor(135 + height * 2, 85, 180);
        int red = (((argb >> 16) & 255) * multiplier) / 150;
        int green = (((argb >> 8) & 255) * multiplier) / 150;
        int blue = ((argb & 255) * multiplier) / 150;
        return 0xFF000000 | (clampColor(red, 0, 255) << 16)
                | (clampColor(green, 0, 255) << 8) | clampColor(blue, 0, 255);
    }

    private static int clampColor(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        MinecraftClient client = MinecraftClient.getInstance();
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_O) {
            client.setScreen(null);
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) {
            selectedIndex = Math.max(0, selectedIndex - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) {
            selectedIndex = Math.min(Math.max(0, TesseractHistory.size() - 1), selectedIndex + 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_P) {
            if (TesseractHistory.capture(client)) {
                selectedIndex = Math.max(0, TesseractHistory.size() - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_R) {
            InterstellarExperienceClient.playQuote();
            return true;
        }
        return super.keyPressed(input);
    }
}

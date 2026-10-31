package com.example.gentoo.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The Gentoo desktop: a real, typable terminal in a window, plus the achievement toast. */
public class DesktopScreen extends Screen {

    private static final String PROMPT = "mint@gentoo ~ $ ";
    private static final String PASSWORD = "[sudo] password for mint: ";

    private static final int TEXT = 0xFFD6D6D6;
    private static final int GREEN = 0xFF7BE07B;
    private static final int DIM = 0xFF8A8A8A;
    private static final int WARN = 0xFFD8C06A;
    private static final int ERR = 0xFFE06C6C;
    private static final int HINT = 0xFF6FB8E0;
    private static final int PINK = 0xFFFF9EC7;
    private static final int PINK_LIGHT = 0xFFFFC6DD;
    private static final int PINK_DARK = 0xFFD96C9C;
    private static final int TRUNK = 0xFF8A5A3B;
    private static final int TRUNK_DARK = 0xFF6B4227;

    private static final int SAK_W = 34;
    private static final int SAK_H = 30;

    private static final String GLYPHS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$+-*/=%\"'#&_(),.;:?!\\|{}<>[]^~";

    private static int[] sakuraCache;

    private static final String[] NEOFETCH = {
            "       .vir.        mint@gentoo",
            "      mFmFmFmF        OS: Gentoo Linux x86_64",
            "     mFmFmFmFmF       Kernel: 6.6.21-gentoo",
            "    mFmFmFmFmFmF      Uptime: 1 min",
            "   mFmFmFmFmFmFmF     Shell: bash 5.2.21",
            "   mFmFmFmFmFmFmF     DE: Xfce 4.18",
            "   mFmFmFmFmFmFmF     WM: xfwm4",
            "    mFmFmFmFmFmF      CPU: QEMU Virtual CPU",
            "     mFmFmFmFmF       Memory: 412MiB / 16GiB",
    };

    private static final String[] NEOFETCH_SMALL = {
            "mint@gentoo",
            "--------------",
            "OS: Gentoo Linux x86_64",
            "Kernel: 6.6.21-gentoo",
            "Uptime: 1 min",
            "Shell: bash 5.2.21",
            "DE: Xfce 4.18",
            "WM: xfwm4",
            "CPU: QEMU Virtual CPU",
            "Memory: 412MiB / 16GiB",
    };

    private final List<String> lines = new ArrayList<>();
    private String input = "";
    private boolean awaitPassword;
    private String pendingCmd;

    private boolean matrix;
    private long matrixAt;
    private boolean sakura;

    private long achAt = System.currentTimeMillis() + 400;

    public DesktopScreen() {
        super(Component.literal("Gentoo desktop"));
    }

    @Override
    protected void init() {
        playAchievementSound();
        if (lines.isEmpty()) {
            lines.add("mint@gentoo ~ $ neofetch");
            for (String l : NEOFETCH) {
                lines.add(l);
            }
            lines.add("");
            lines.add("type 'help' to see what this terminal can do.");
        }
    }

    private void playAchievementSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
    }

    // ---------------------------------------------------------------- terminal

    private void say(String... ls) {
        for (String l : ls) {
            lines.add(l);
        }
        while (lines.size() > 300) {
            lines.remove(0);
        }
    }

    private void submit() {
        String cmd = input.trim();
        input = "";

        if (awaitPassword) {
            awaitPassword = false;
            String pending = pendingCmd;
            pendingCmd = null;
            if (pending != null) {
                runAsRoot(pending);
            }
            return;
        }

        say(PROMPT + cmd);
        if (cmd.isEmpty()) {
            return;
        }
        run(cmd);
    }

    private void run(String cmd) {
        String low = cmd.toLowerCase(Locale.ROOT).trim();

        switch (low) {
            case "help", "man", "?" -> {
                say("help        - this list",
                        "neofetch    - system info",
                        "cmatrix     - matrix rain (q to quit)",
                        "larp        - cmatrix + neofetch + sakura",
                        "sakura      - the tree",
                        "ls / pwd / whoami / date / uname -a",
                        "cat /etc/os-release",
                        "sudo <cmd>  - run as root (any password)",
                        "achievement - show the toast again",
                        "clear       - wipe the screen",
                        "exit        - back to the world");
                return;
            }
            case "clear" -> {
                lines.clear();
                return;
            }
            case "exit", "logout", "q!" -> {
                onClose();
                return;
            }
            case "neofetch", "fastfetch" -> {
                for (String l : NEOFETCH) {
                    lines.add(l);
                }
                return;
            }
            case "cmatrix", "matrix" -> {
                startMatrix();
                return;
            }
            case "sakura" -> {
                sakura = true;
                startMatrix();
                say("sakura: in bloom");
                return;
            }
            case "larp" -> {
                larp();
                return;
            }
            case "achievement", "ach" -> {
                achAt = System.currentTimeMillis();
                playAchievementSound();
                say("achievement: I use Gentoo, btw");
                return;
            }
            case "ls" -> {
                say("Desktop    Documents  Downloads  Music  Pictures",
                        "Public     Templates  Videos     sakura.png  .xinitrc");
                return;
            }
            case "pwd" -> {
                say("/home/mint");
                return;
            }
            case "whoami" -> {
                say("mint");
                return;
            }
            case "date" -> {
                say(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " EET");
                return;
            }
            case "neofetch --small" -> {
                for (String l : NEOFETCH_SMALL) {
                    lines.add(l);
                }
                return;
            }
            default -> {
            }
        }

        if (low.startsWith("uname")) {
            say("Linux gentoo 6.6.21-gentoo #1 SMP PREEMPT_DYNAMIC x86_64 GNU/Linux");
            return;
        }
        if (low.startsWith("cat /etc/os-release")) {
            say("NAME=Gentoo", "ID=gentoo", "PRETTY_NAME=\"Gentoo Linux\"", "HOME_URL=\"https://gentoo.org\"");
            return;
        }
        if (low.equals("sudo") || low.equals("sudo -s") || low.equals("sudo su")) {
            awaitPassword = true;
            pendingCmd = "";
            say("usage: sudo <command>");
            return;
        }
        if (low.startsWith("sudo ")) {
            String rest = cmd.substring(5).trim();
            awaitPassword = true;
            pendingCmd = rest;
            say(PASSWORD);
            return;
        }
        if (low.startsWith("su ") || low.equals("su")) {
            awaitPassword = true;
            pendingCmd = "id";
            say("Password:");
            return;
        }
        if (low.startsWith("rm") && (low.contains("-rf") || low.contains("-fr") || low.contains("-r"))) {
            rm(cmd);
            return;
        }
        if (low.startsWith("rm ")) {
            String target = cmd.substring(3).trim();
            say("rm: cannot remove '" + target + "': No such file or directory");
            return;
        }
        if (low.startsWith("emerge")) {
            say(">>> Calculating dependencies... done!",
                    ">>> Nothing to merge: the system is up to date.");
            return;
        }
        if (low.startsWith("echo ")) {
            say(cmd.substring(5));
            return;
        }

        String head = cmd.split(" ")[0];
        say("bash: " + head + ": command not found");
    }

    private void runAsRoot(String cmd) {
        String low = cmd.toLowerCase(Locale.ROOT).trim();
        if (low.isEmpty()) {
            say("root@gentoo # id", "uid=0(root) gid=0(root) groups=0(root)");
            return;
        }
        if (low.startsWith("rm")) {
            rm(cmd);
            return;
        }
        if (low.startsWith("id") || low.equals("whoami")) {
            say("uid=0(root) gid=0(root) groups=0(root)");
            return;
        }
        if (low.startsWith("ls")) {
            say("bin  boot  dev  etc  home  lib64  mnt  opt  proc  root  run  sys  usr  var");
            return;
        }
        if (low.startsWith("shutdown") || low.startsWith("poweroff") || low.startsWith("halt")) {
            say(" * Shutting down ...", " * Unmounting filesystems ... [ ok ]",
                    " * Power down. (you can close the window)");
            return;
        }
        say("root@gentoo # " + cmd);
        say("(ran as root)");
    }

    private void rm(String cmd) {
        String low = cmd.toLowerCase(Locale.ROOT);
        boolean preserve = low.contains("--no-preserve-root") || low.contains("/*") || low.contains("/ *");
        if (!low.contains("/")) {
            String target = cmd.replaceFirst("(?i)^\\s*(sudo\\s+)?rm\\s+(-[a-z]+\\s+)*", "").trim();
            say("rm: cannot remove '" + target + "': No such file or directory");
            return;
        }
        if (!preserve) {
            say("rm: it is dangerous to operate recursively on '/'",
                    "rm: use --no-preserve-root to override this failsafe",
                    "hint: good. the disk stays alive this time. btw");
            return;
        }
        say("rm: descending into '/'",
                "rm: cannot remove '/proc': Device or resource busy",
                "rm: cannot remove '/sys': Device or resource busy",
                "rm: cannot remove '/dev': Device or resource busy",
                "rm: cannot remove '/': Read-only file system",
                "hint: / is mounted read-only. gentoo survives. btw");
    }

    private void startMatrix() {
        matrix = true;
        matrixAt = System.currentTimeMillis();
    }

    private void larp() {
        say("larp: loading the aesthetic ...");
        for (String l : NEOFETCH_SMALL) {
            lines.add(l);
        }
        sakura = true;
        startMatrix();
        say("");
        say("hint: q - leave the matrix");
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (matrix) {
            if (key == GLFW.GLFW_KEY_Q || key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_SPACE) {
                matrix = false;
                return true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (awaitPassword) {
                awaitPassword = false;
                pendingCmd = null;
                say("^C", "sudo: aborted");
                return true;
            }
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_F1) {
            Minecraft.getInstance().setScreen(new TerminalScreen());
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            return true;
        }
        if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_V) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                input += clip.replace("\n", " ").replace("\r", " ");
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (matrix || Screen.hasControlDown()) {
            return false;
        }
        if (c >= 32 && input.length() < 120) {
            input += c;
            return true;
        }
        return super.charTyped(c, mods);
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        long now = System.currentTimeMillis();

        for (int y = 0; y < height; y++) {
            g.fill(0, y, width, y + 1, lerp(0xFF16233F, 0xFF2B1733, y / (float) Math.max(1, height)));
        }

        // top bar
        g.fill(0, 0, width, 20, 0xC0000000);
        g.drawString(font, "Activities", 10, 6, 0xFFE8E8E8, false);
        g.drawString(font, "gentoo", 78, 6, 0xFFB9A6FF, false);
        String clock = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        g.drawString(font, clock, width - 10 - font.width(clock), 6, 0xFFE8E8E8, false);

        // desktop icon
        int ix = 16, iy = 36;
        g.fill(ix, iy, ix + 34, iy + 34, 0xB0000000);
        g.drawString(font, ">_", ix + 8, iy + 5, 0xFF7BE07B, false);
        g.drawString(font, "Terminal", ix - 4, iy + 38, 0xFFD8D8D8, false);

        int w = Math.min(470, width - 30);
        int h = Math.min(235, height - 64);
        int x = (width - w) / 2;
        int y = (height - h) / 2 + 4;

        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xE60C0C0C);
        g.fill(x, y, x + w, y + 16, 0xFF1E1E1E);
        g.drawString(font, "mint@gentoo: ~", x + 6, y + 4, 0xFFB9A6FF, false);

        int cx0 = x + 6;
        int cy0 = y + 20;
        int cx1 = x + w - 6;
        int cy1 = y + h - 6;

        g.enableScissor(cx0, cy0, cx1, cy1);
        if (matrix) {
            drawMatrix(g, cx0, cy0, cx1 - cx0, cy1 - cy0, now);
        }

        int visible = Math.max(1, (cy1 - cy0) / 10 - 1);
        int from = Math.max(0, lines.size() - visible);

        // a larp wall: readable output on the left, rain + sakura on the right
        int panelRight = cx0;
        if (matrix && sakura) {
            int maxW = font.width(PROMPT) + 80;
            for (int i = from; i < lines.size(); i++) {
                maxW = Math.max(maxW, font.width(lines.get(i)));
            }
            panelRight = Math.min(cx1 - 10, cx0 + maxW + 8);
            g.fill(cx0 - 4, cy0 - 2, panelRight, cy1 + 2, 0xE00A0A0A);
        }

        int ly = cy0;
        for (int i = from; i < lines.size(); i++) {
            String line = lines.get(i);
            g.drawString(font, line, cx0, ly, colorOf(line), matrix);
            ly += 10;
        }

        if (sakura) {
            drawSakura(g, cx1 - 4, cy1 - 8);
            if (matrix) {
                drawPetals(g, panelRight, cy0, cx1 - panelRight, cy1 - cy0, now);
            }
        }

        // prompt
        String p = awaitPassword ? PASSWORD : PROMPT;
        g.drawString(font, p, cx0, cy1 - 10, awaitPassword ? WARN : GREEN, matrix);
        if (!awaitPassword) {
            g.drawString(font, input, cx0 + font.width(p), cy1 - 10, 0xFFD6FFD6, matrix);
        }
        if (!matrix && (now / 500) % 2 == 0) {
            int px = cx0 + font.width(p) + (awaitPassword ? 0 : font.width(input));
            g.fill(px + 1, cy1 - 10, px + 4, cy1 - 2, 0xFF9BE39B);
        }
        g.disableScissor();

        drawAchievement(g, now);

        String hint = matrix
                ? "q — выйти из matrix    ESC — в мир    F1 — установка"
                : "ESC — в мир    F1 — установка    help — команды";
        g.drawString(font, hint, (width - font.width(hint)) / 2, height - 12, 0x80FFFFFF, false);
    }

    private void drawMatrix(GuiGraphics g, int x0, int y0, int w, int h, long now) {
        int step = 8;
        int cols = Math.max(1, w / step);
        int rows = Math.max(1, h / 10);
        double secs = (now - matrixAt) / 1000.0;

        for (int c = 0; c < cols; c++) {
            int seed = c * 7919 + 13;
            double speed = 3.0 + (Math.abs(seed) % 5);        // rows per second
            int offset = Math.abs(seed / 11) % (rows + 8);
            int head = (int) (secs * speed) + offset;
            head %= (rows + 8);

            for (int k = 0; k < 9; k++) {
                int row = head - k;
                if (row < 0 || row >= rows) {
                    continue;
                }
                char ch = GLYPHS.charAt(Math.abs(c * 31 + row * 17 + (int) (now / 110)) % GLYPHS.length());
                int color;
                if (k == 0) {
                    color = 0xFFD9FFD9;
                } else if (k < 3) {
                    color = 0xFF6FE36F;
                } else if (k < 6) {
                    color = 0x9A2E9A2E;
                } else {
                    color = 0x70155C15;
                }
                g.drawString(font, String.valueOf(ch), x0 + c * step, y0 + row * 10, color, false);
            }
        }
    }

    /** Draws a small pixel-art cherry tree; built once and cached. */
    private void drawSakura(GuiGraphics g, int right, int bottom) {
        int cell = 3;
        int w = SAK_W * cell;
        int h = SAK_H * cell;
        int x0 = right - w;
        int y0 = bottom - h;

        g.fill(x0 - 5, y0 - 5, right + 5, bottom + 5, 0xE80A0A0A);

        int[] px = buildSakura();
        for (int row = 0; row < SAK_H; row++) {
            int start = 0;
            int color = px[row * SAK_W];
            for (int col = 1; col <= SAK_W; col++) {
                int c = col < SAK_W ? px[row * SAK_W + col] : 0;
                if (c != color) {
                    if (color != 0) {
                        g.fill(x0 + start * cell, y0 + row * cell, x0 + col * cell, y0 + (row + 1) * cell, color);
                    }
                    start = col;
                    color = c;
                }
            }
        }
    }

    private static int[] buildSakura() {
        if (sakuraCache != null) {
            return sakuraCache;
        }
        int[] px = new int[SAK_W * SAK_H];

        // trunk, slightly curved, thicker at the base
        for (int row = 14; row < SAK_H; row++) {
            int cx = 17 + (int) Math.round(Math.sin((row - 14) * 0.16) * 1.5);
            int half = row >= 25 ? 2 : 1;
            for (int dx = -half; dx <= half; dx++) {
                put(px, cx + dx, row, TRUNK);
            }
            put(px, cx + half, row, TRUNK_DARK);
        }
        // roots
        line(px, 16, 27, 12, 29, TRUNK_DARK);
        line(px, 18, 27, 23, 29, TRUNK_DARK);
        line(px, 19, 29, 25, 29, TRUNK);

        // branches reaching into the crown
        line(px, 17, 22, 10, 14, TRUNK);
        line(px, 17, 20, 24, 13, TRUNK);

        // blossom "clouds": dark rim, main pink, light top-left
        puff(px, 6, 11, 3);
        puff(px, 10, 8, 4);
        puff(px, 28, 11, 3);
        puff(px, 24, 8, 4);
        puff(px, 17, 5, 5);
        puff(px, 17, 10, 5);
        puff(px, 10, 14, 2);
        puff(px, 24, 13, 2);

        // blossom texture: little light speckles inside the crown
        for (int i = 0; i < 130; i++) {
            int x = 2 + (i * 37) % 30;
            int y = 1 + (i * 23) % 15;
            int cur = px[y * SAK_W + x];
            if (cur == PINK || cur == PINK_LIGHT) {
                px[y * SAK_W + x] = i % 6 == 0 ? 0xFFFFE6F0 : PINK_LIGHT;
            }
        }

        // fallen petals on the ground
        int[] ground = {2, 6, 10, 14, 18, 23, 27, 31, 5, 12, 21, 29};
        for (int i = 0; i < ground.length; i++) {
            put(px, ground[i], SAK_H - 1 - (i % 2), i % 3 == 0 ? PINK_LIGHT : PINK_DARK);
        }

        sakuraCache = px;
        return px;
    }

    private static void puff(int[] px, int cx, int cy, int r) {
        blob(px, cx + 1, cy + 1, r, PINK_DARK);
        blob(px, cx, cy, r - 1, PINK);
        if (r >= 4) {
            blob(px, cx - 1, cy - 1, r - 3, PINK_LIGHT);
        }
    }

    private static void blob(int[] px, int cx, int cy, int r, int color) {
        if (r <= 0) {
            return;
        }
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            for (int x = cx - dx; x <= cx + dx; x++) {
                put(px, x, cy + dy, color);
            }
        }
    }

    private static void line(int[] px, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps);
            int y = y1 + (y2 - y1) * i / Math.max(1, steps);
            put(px, x, y, color);
        }
    }

    private static void put(int[] px, int x, int y, int color) {
        if (x >= 0 && x < SAK_W && y >= 0 && y < SAK_H) {
            px[y * SAK_W + x] = color;
        }
    }

    /** Falling petals over the rain, so the tree does not look like a still image. */
    private void drawPetals(GuiGraphics g, int x0, int y0, int w, int h, long now) {
        if (w <= 6) {
            return;
        }
        for (int i = 0; i < 16; i++) {
            int seed = i * 977 + 31;
            int px = x0 + Math.abs(seed * 13) % (w - 3);
            int drift = (int) (Math.sin(now / 700.0 + i * 1.7) * 5);
            int py = y0 + (int) (((now / 14 + Math.abs(seed) % 500) % (h + 30)) - 16);
            int color = i % 3 == 0 ? PINK_LIGHT : PINK;
            int size = i % 4 == 0 ? 2 : 1;
            g.fill(px + drift, py, px + drift + size, py + size, color);
        }
    }

    private int colorOf(String line) {
        if (line.startsWith("bash:") || line.startsWith("rm:")) return ERR;
        if (line.startsWith("hint:") || line.startsWith("larp:") || line.startsWith(PASSWORD)) return WARN;
        if (line.startsWith(PROMPT)) return GREEN;
        if (line.startsWith("achievement:") || line.startsWith("sakura:")) return HINT;
        if (line.contains("mFmF")) return GREEN;
        return TEXT;
    }

    private void drawAchievement(GuiGraphics g, long now) {
        long age = now - achAt;
        if (age < 0 || age > 9600) {
            return;
        }
        float in = Math.min(1F, age / 300F);
        float out = age > 9000 ? Math.min(1F, (age - 9000) / 300F) : 0F;
        float slide = in - out;
        if (slide <= 0F) {
            return;
        }
        int tw = 172, th = 40;
        int tx = (int) (width - (tw + 12) * slide);
        int ty = 24;

        g.fill(tx, ty, tx + tw, ty + th, 0xF0212121);
        g.fill(tx, ty, tx + 3, ty + th, 0xFF6FE36F);
        g.fill(tx, ty, tx + tw, ty + 1, 0xFF3A3A3A);
        g.drawString(font, "Advancement Made!", tx + 10, ty + 8, 0xFFE8D44D, false);
        g.drawString(font, "I use Gentoo, btw", tx + 10, ty + 22, 0xFFFFFFFF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int lerp(int a, int b, float f) {
        int aa = (int) (((a >> 24) & 0xFF) + (((b >> 24) & 0xFF) - ((a >> 24) & 0xFF)) * f);
        int ar = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * f);
        int ag = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * f);
        int ab = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * f);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }
}

package com.example.gentoo.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** The live-cd terminal. Everything is typed by the player; nothing is simulated for them. */
public class TerminalScreen extends Screen {

    private static final int GREEN = 0xFF8FE388;
    private static final int DIM = 0xFF5F6B5F;
    private static final int TEXT = 0xFFD4D4D4;
    private static final int WARN = 0xFFD8C06A;
    private static final int ERR = 0xFFE06C6C;
    private static final int HINT = 0xFF6FB8E0;
    private static final int BAR = 0xFF141A14;
    private static final int BG = 0xFF0B0D0B;

    private final GentooState st = GentooState.get();
    private final Deque<String> pending = new ArrayDeque<>();

    private String input = "";
    private int histIdx = -1;
    private int speed = 1;
    private int delay;
    private int scroll;
    private int fails;
    private int openDesktopIn = -1;
    private int autoPace = 20;

    public TerminalScreen() {
        super(Component.literal("Gentoo"));
    }

    @Override
    protected void init() {
        if (st.log.isEmpty()) {
            banner();
        }
    }

    private void banner() {
        st.say(
            "",
            "Gentoo Linux 2024.1 x86_64 - LiveDVD (openrc)",
            "tty1  kernel 6.6.21-gentoo  8 CPUs  ~16 GiB RAM",
            "",
            "You are root on the live system. The disk /dev/sda is 64 GiB and empty.",
            "Type 'help' to see the next command, 'clear' to wipe the screen.",
            "");
    }

    // ---------------------------------------------------------------- logic

    private String hint() {
        if (st.done()) {
            return "installed. 'reboot' restarts into Gentoo, 'reset' wipes the disk and starts over.";
        }
        Stage s = Flow.STAGES.get(st.stage);
        if (s.chroot && !st.chroot) {
            return "you are not in the chroot yet - run 'chroot /mnt/gentoo /bin/bash' first";
        }
        return s.hint;
    }

    private void submit() {
        String raw = input.trim();
        input = "";
        scroll = 0;
        if (raw.isEmpty()) {
            st.say(st.prompt());
            return;
        }
        st.history.add(raw);
        if (st.history.size() > 200) {
            st.history.remove(0);
        }
        histIdx = -1;
        st.say(st.prompt() + raw);
        run(raw);
        st.save();
    }

    private void run(String raw) {
        String cmd = raw.replaceAll("\\s+", " ").trim();
        String low = cmd.toLowerCase(Locale.ROOT);

        if (low.equals("clear")) {
            st.log.clear();
            return;
        }
        if (low.equals("help") || low.equals("hint") || low.equals("man")) {
            st.say("hint: " + hint());
            return;
        }
        if (low.equals("reset")) {
            st.reset();
            banner();
            return;
        }
        if (low.equals("autopilot")) {
            st.autopilot = !st.autopilot;
            autoPace = 10;
            st.say(st.autopilot ? "autopilot: on - дальше команды печатаются сами"
                                : "autopilot: off - дальше печатаешь сам");
            st.save();
            return;
        }
        if (flavour(low)) {
            return;
        }
        if (st.done()) {
            if (low.equals("reboot")) {
                st.say(" * Rebooting into your new Gentoo system ...");
                openDesktopIn = 20;
            } else if (low.equals("desktop")) {
                Minecraft.getInstance().setScreen(new DesktopScreen());
            } else {
                st.say("Стоит свежая Gentoo. 'desktop' - загрузиться, 'reset' - переустановить.");
            }
            return;
        }

        Stage stage = Flow.STAGES.get(st.stage);
        Pattern p = Pattern.compile(stage.expect, Pattern.CASE_INSENSITIVE);

        if (stage.chroot && !st.chroot) {
            unknown(cmd, stage);
            return;
        }
        if (p.matcher(cmd).matches()) {
            advance(stage, cmd);
            return;
        }
        String stripped = cmd.replace(" --ask", "").replace(" -a ", " ");
        if (p.matcher(stripped).matches()) {
            advance(stage, cmd);
            return;
        }
        unknown(cmd, stage);
    }

    private boolean flavour(String low) {
        switch (low) {
            case "ls":
                st.say("bin   dev  home  lib64  mnt  proc  run   sys  usr",
                       "boot  etc  lib   media  opt  root  sbin  tmp  var");
                return true;
            case "ls /mnt/gentoo", "ls /mnt/gentoo/":
                st.say(st.stage > 15 ? "bin  boot  dev  etc  home  lib  root  usr  var"
                                     : "");
                if (st.stage <= 15) {
                    st.say("(пусто - stage3 ещё не распакован)");
                }
                return true;
            case "pwd":
                st.say(st.cwd);
                return true;
            case "whoami":
                st.say("root");
                return true;
            case "uname -a":
                st.say("Linux livecd 6.6.21-gentoo #1 SMP x86_64 GNU/Linux");
                return true;
            case "date":
                st.say(new java.text.SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy").format(new java.util.Date()));
                return true;
            case "cat /etc/os-release":
                st.say("NAME=Gentoo", "ID=gentoo", "PRETTY_NAME=\"Gentoo Linux\"");
                return true;
            case "neofetch", "fastfetch":
                st.say("       .vir.          root@livecd",
                       "      mFmFmFmF         OS: Gentoo Linux x86_64 (live)",
                       "     mFmFmFmFmF        Kernel: 6.6.21-gentoo",
                       "    mFmFmFmFmFmF       Uptime: 4 mins",
                       "   mFmFmFmFmFmFmF      Shell: bash 5.2.21",
                       "   mFmFmFmFmFmFmF      Packages: 0 (0 installed)");
                return true;
            default:
                return false;
        }
    }

    private void unknown(String cmd, Stage stage) {
        String head = cmd.split(" ")[0];
        st.say("bash: " + (head.isEmpty() ? cmd : head) + ": command not found");
        fails++;
        if (fails % 3 == 0) {
            st.say("hint: " + (stage.chroot && !st.chroot
                    ? "сначала chroot /mnt/gentoo /bin/bash"
                    : stage.hint));
        }
    }

    private void advance(Stage stage, String cmd) {
        fails = 0;
        st.stage++;

        if (stage.expect.contains("chroot")) {
            st.chroot = true;
            st.cwd = "/";
        } else if (stage.expect.equals("exit")) {
            st.chroot = false;
            st.cwd = "/mnt/gentoo";
        } else if (cmd.startsWith("cd ")) {
            String target = cmd.substring(3).trim();
            if (target.length() > 1 && target.endsWith("/")) {
                target = target.substring(0, target.length() - 1);
            }
            st.cwd = target;
        }

        if (st.done()) {
            st.finished = true;
            st.autopilot = false;
        }
        pending.addAll(Arrays.asList(stage.out));
        speed = Math.max(1, stage.speed);
        delay = speed;
        st.save();
    }

    // ---------------------------------------------------------------- ticking

    /** Types the next install command on its own, but only while the shell is idle. */
    private void autopilot() {
        if (!st.autopilot || st.done() || !pending.isEmpty()) {
            return;
        }
        if (--autoPace > 0) {
            return;
        }
        autoPace = 10;
        Stage stage = Flow.STAGES.get(st.stage);
        if (stage.chroot && !st.chroot) {
            return;
        }
        input = "";
        for (char c : stage.hint.toCharArray()) {
            charTyped(c, 0);
        }
        submit();
    }

    @Override
    public void tick() {
        if (openDesktopIn < 0) {
            autopilot();
        }
        if (openDesktopIn > 0) {
            openDesktopIn--;
            if (openDesktopIn == 0) {
                Minecraft.getInstance().setScreen(new DesktopScreen());
                return;
            }
        }
        if (pending.isEmpty()) {
            return;
        }
        if (--delay > 0) {
            return;
        }
        st.log.add(pending.poll());
        while (st.log.size() > 600) {
            st.log.remove(0);
        }
        scroll = 0;
        delay = speed;
        if (pending.isEmpty() && st.finished && openDesktopIn < 0) {
            openDesktopIn = 30;
        }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, BG);
        g.fill(0, 0, width, 20, BAR);

        String left = "gentoo-install@livecd";
        g.drawString(font, left, 8, 6, GREEN, false);
        String step = st.done() ? "installed" : (st.stage + 1) + "/" + Flow.STAGES.size();
        String right = "F1 hint   ESC close   " + step;
        g.drawString(font, right, width - 8 - font.width(right), 6, DIM, false);
        if (st.autopilot) {
            String ap = "autopilot";
            g.drawString(font, ap, width - 16 - font.width(right) - font.width(ap), 6, WARN, false);
        }

        int lineH = 10;
        int top = 26;
        int bottom = height - 22;
        int maxLines = Math.max(1, (bottom - top) / lineH);

        List<String> lines = wrap(st.log, width - 16);
        int end = Math.max(0, lines.size() - scroll);
        int start = Math.max(0, end - maxLines);
        end = Math.min(lines.size(), start + maxLines);

        int y = top;
        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            g.drawString(font, line, 8, y, colorOf(line), false);
            y += lineH;
        }

        g.fill(0, height - 20, width, height, BAR);
        String prompt = st.prompt() + input;
        if (font.width(prompt) > width - 16) {
            prompt = "..." + prompt.substring(Math.max(0, prompt.length() - 60));
        }
        g.drawString(font, prompt, 8, height - 14, 0xFFD6FFD6, false);
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = 8 + font.width(prompt) + 1;
            g.fill(cx, height - 14, cx + 5, height - 5, 0xFF9BE39B);
        }
    }

    private int colorOf(String line) {
        if (line.startsWith(">>>")) return GREEN;
        if (line.startsWith("*")) return GREEN;
        if (line.startsWith("[ebuild")) return WARN;
        if (line.startsWith("bash:")) return ERR;
        if (line.startsWith("hint:")) return HINT;
        if (line.startsWith("make[")) return DIM;
        if (line.startsWith("./")) return DIM;
        return TEXT;
    }

    private List<String> wrap(List<String> src, int maxWidth) {
        List<String> out = new ArrayList<>();
        for (String line : src) {
            if (line.isEmpty() || font.width(line) <= maxWidth) {
                out.add(line);
                continue;
            }
            StringBuilder cur = new StringBuilder();
            for (String word : line.split(" ")) {
                String test = cur.length() == 0 ? word : cur + " " + word;
                if (font.width(test) > maxWidth && cur.length() > 0) {
                    out.add(cur.toString());
                    cur = new StringBuilder(word);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            out.add(cur.toString());
        }
        return out;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_F1) {
            st.say("hint: " + hint());
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (st.autopilot) {
                st.autopilot = false;
                st.save();
                st.say("autopilot: off - дальше печатаешь сам ('autopilot' включит обратно)");
                return true;
            }
            submit();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            history(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            history(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_UP) {
            scroll = Math.min(scroll + 5, 200);
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            scroll = Math.max(0, scroll - 5);
            return true;
        }
        if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_V) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                input += clip.replace("\n", " ").replace("\r", " ");
            }
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (Screen.hasControlDown()) {
            return false;
        }
        if (c >= 32 && input.length() < 200) {
            input += c;
            return true;
        }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        scroll = (int) Math.max(0, Math.min(scroll + (dy > 0 ? 2 : -2), 200));
        return true;
    }

    private void history(int dir) {
        if (st.history.isEmpty()) {
            return;
        }
        if (histIdx == -1) {
            histIdx = st.history.size();
        }
        histIdx = Math.max(0, Math.min(st.history.size(), histIdx + dir));
        input = histIdx >= st.history.size() ? "" : st.history.get(histIdx);
    }

    @Override
    public void onClose() {
        st.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package com.example.gentoo.terminal;

/** One step of the install: what the player must type, what the shell answers, how fast it prints. */
public final class Stage {
    public final String expect;
    public final String hint;
    public final String[] out;
    public final int speed;
    public final boolean chroot;

    public Stage(String expect, String hint, int speed, boolean chroot, String... out) {
        this.expect = expect;
        this.hint = hint;
        this.speed = speed;
        this.chroot = chroot;
        this.out = out;
    }
}

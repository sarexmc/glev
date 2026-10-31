package com.supernova;

/** Tiempos (en ticks) compartidos por servidor y cliente. 20 ticks = 1 segundo. */
public final class Timeline {
    public static final int P1_MSG60 = 200;
    public static final int P1_BLAST = P1_MSG60 + 1200;
    public static final int P1_WAVE = P1_BLAST + 240;
    public static final int P1_END = P1_BLAST + 2400;
    public static final int P2_MSG60 = 100;
    public static final int P2_BLAST = P2_MSG60 + 1200;
    public static final int P2_SHIP_MSG = P2_BLAST + 160;

    private Timeline() {}
}

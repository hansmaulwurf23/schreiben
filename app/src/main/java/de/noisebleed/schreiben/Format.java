package de.noisebleed.schreiben;

import java.util.Locale;

/** Anzeigetexte, die an mehreren Stellen gebraucht werden. */
public final class Format {

    private Format() {
    }

    /** Dauer als m:ss, z. B. "1:05". */
    public static String duration(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.GERMANY, "%d:%02d", seconds / 60, seconds % 60);
    }
}

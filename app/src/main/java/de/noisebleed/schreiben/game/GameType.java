package de.noisebleed.schreiben.game;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;

import java.util.List;
import java.util.Random;

/**
 * Ein Spiel auf dem Startbildschirm. Neue Spiele entstehen als Unterklasse, die {@link #createTasks}
 * implementiert, und einen Eintrag in {@link GameTypes} — Startbildschirm, Spiel, Ergebnis und
 * Bestenliste richten sich danach.
 */
public abstract class GameType {

    private final String id;
    @StringRes
    private final int title;
    @StringRes
    private final int description;
    private final String symbol;
    @ColorRes
    private final int color;

    /**
     * @param id          dauerhafte Kennung, steht in der Bestenliste und im JSON-Export — nie ändern
     * @param description kurzes Beispiel für die Kachel, z. B. "der Apfel → die Äpfel"
     * @param symbol      kurzes Zeichen für die Kachel, z. B. "Ä"
     */
    protected GameType(String id, @StringRes int title, @StringRes int description, String symbol,
                       @ColorRes int color) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.symbol = symbol;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    @StringRes
    public int getTitle() {
        return title;
    }

    @StringRes
    public int getDescription() {
        return description;
    }

    public String getSymbol() {
        return symbol;
    }

    @ColorRes
    public int getColor() {
        return color;
    }

    /** {@code count} verschiedene, zufällig gewählte Aufgaben in zufälliger Reihenfolge. */
    public abstract List<Task> createTasks(int count, Random random);
}

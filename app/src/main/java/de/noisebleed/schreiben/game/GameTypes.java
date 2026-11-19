package de.noisebleed.schreiben.game;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Alle Spiele in der Reihenfolge des Startbildschirms. Ein neues Spiel wird hier eingetragen — mehr
 * braucht es nicht, damit es auf dem Startbildschirm und in der Bestenliste erscheint.
 */
public final class GameTypes {

    private static final List<GameType> ALL = Collections.unmodifiableList(List.of(
            new PluralType()
    ));

    private GameTypes() {
    }

    public static List<GameType> all() {
        return ALL;
    }

    /** Null, wenn die Kennung unbekannt ist (etwa aus dem Export einer neueren Version). */
    @Nullable
    public static GameType byId(@Nullable String id) {
        for (GameType type : ALL) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }
}

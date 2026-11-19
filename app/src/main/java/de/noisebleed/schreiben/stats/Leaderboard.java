package de.noisebleed.schreiben.stats;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.noisebleed.schreiben.game.GameType;

/**
 * Alle gespielten Spiele. Dasselbe JSON-Format dient zum Speichern auf dem Gerät und zum Export:
 * {@code {"format": "schreiben-bestenliste", "version": 1, "games": [...]}}.
 */
public final class Leaderboard {

    static final String FORMAT = "schreiben-bestenliste";
    static final int VERSION = 1;

    /** Mehr Punkte zuerst, bei Gleichstand die schnellere Zeit, dann das frühere Spiel. */
    public static final Comparator<GameRecord> RANKING = (a, b) -> {
        if (a.points != b.points) {
            return Integer.compare(b.points, a.points);
        }
        if (a.millis != b.millis) {
            return Long.compare(a.millis, b.millis);
        }
        return Long.compare(a.timestamp, b.timestamp);
    };

    private final List<GameRecord> games = new ArrayList<>();

    public List<GameRecord> all() {
        return Collections.unmodifiableList(games);
    }

    public void add(GameRecord record) {
        games.add(record);
    }

    /** Rangliste, wahlweise nur eines Spiels ({@code null} = alle). */
    public List<GameRecord> ranked(@Nullable GameType type) {
        List<GameRecord> result = new ArrayList<>();
        for (GameRecord game : games) {
            if (type == null || type.getId().equals(game.typeId)) {
                result.add(game);
            }
        }
        Collections.sort(result, RANKING);
        return result;
    }

    /** Bestes Spiel einer Art, null wenn es noch keins gibt. */
    @Nullable
    public GameRecord best(GameType type) {
        List<GameRecord> ranked = ranked(type);
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    /** Ergebnis eines Imports. */
    public static final class MergeResult {
        public final int added;
        public final int duplicates;

        MergeResult(int added, int duplicates) {
            this.added = added;
            this.duplicates = duplicates;
        }
    }

    /**
     * Spiele aus einer anderen Liste übernehmen. Was schon da ist (gleiche id), wird übersprungen —
     * so schadet es nicht, denselben Export zweimal zu importieren.
     */
    public MergeResult merge(Leaderboard other) {
        Set<String> known = new HashSet<>();
        for (GameRecord game : games) {
            known.add(game.id);
        }
        int added = 0, duplicates = 0;
        for (GameRecord game : other.games) {
            if (known.add(game.id)) {
                games.add(game);
                added++;
            } else {
                duplicates++;
            }
        }
        return new MergeResult(added, duplicates);
    }

    public JSONObject toJson() throws JSONException {
        JSONArray array = new JSONArray();
        for (GameRecord game : games) {
            array.put(game.toJson());
        }
        return new JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("games", array);
    }

    /**
     * Liest eine Liste ein. Einzelne kaputte Einträge werden übersprungen, eine Datei, die gar keine
     * Bestenliste ist, führt zur Ausnahme.
     */
    public static Leaderboard fromJson(String text) throws JSONException {
        JSONObject json = new JSONObject(text);
        if (!FORMAT.equals(json.optString("format"))) {
            throw new JSONException("Keine Schreiben-Bestenliste");
        }
        Leaderboard board = new Leaderboard();
        JSONArray array = json.getJSONArray("games");
        for (int i = 0; i < array.length(); i++) {
            try {
                board.games.add(GameRecord.fromJson(array.getJSONObject(i)));
            } catch (JSONException ignored) {
                // Einen kaputten Eintrag auslassen statt die ganze Liste zu verwerfen.
            }
        }
        return board;
    }
}

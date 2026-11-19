package de.noisebleed.schreiben.stats;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import de.noisebleed.schreiben.game.GameType;
import de.noisebleed.schreiben.game.GameTypes;
import de.noisebleed.schreiben.game.Round;

/** Ein gespieltes Spiel in der Bestenliste. */
public final class GameRecord {

    /** Eindeutig, damit ein erneuter Import dasselbe Spiel nicht doppelt einträgt. */
    public final String id;
    /** Ende des Spiels, Millisekunden seit 1970. */
    public final long timestamp;
    /** Kennung des Spiels; kann aus einer neueren Version stammen und hier unbekannt sein. */
    public final String typeId;
    public final int correct;
    public final int total;
    public final int points;
    public final long millis;

    public GameRecord(String id, long timestamp, String typeId, int correct,
                      int total, int points, long millis) {
        this.id = id;
        this.timestamp = timestamp;
        this.typeId = typeId;
        this.correct = correct;
        this.total = total;
        this.points = points;
        this.millis = millis;
    }

    public static GameRecord of(Round round, long timestamp) {
        return new GameRecord(UUID.randomUUID().toString(), timestamp, round.getType().getId(),
                round.correctCount(), round.getTasks().size(),
                round.points(), round.totalMillis());
    }

    @Nullable
    public GameType type() {
        return GameTypes.byId(typeId);
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("timestamp", timestamp)
                .put("type", typeId)
                .put("correct", correct)
                .put("total", total)
                .put("points", points)
                .put("millis", millis);
    }

    public static GameRecord fromJson(JSONObject json) throws JSONException {
        GameRecord record = new GameRecord(
                json.getString("id"),
                json.getLong("timestamp"),
                json.getString("type"),
                json.getInt("correct"),
                json.getInt("total"),
                json.getInt("points"),
                json.getLong("millis"));
        if (record.id.isEmpty() || record.correct < 0 || record.correct > record.total
                || record.points < 0 || record.millis < 0) {
            throw new JSONException("Ungültiger Eintrag " + record.id);
        }
        return record;
    }
}

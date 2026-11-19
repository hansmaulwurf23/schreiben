package de.noisebleed.schreiben.stats;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Die Bestenliste als JSON-Datei im privaten Speicher der App. Ein Update der App lässt sie
 * unberührt; zum Gerätewechsel lässt sie sich exportieren und wieder importieren.
 */
public final class LeaderboardStore {

    private static final String TAG = "LeaderboardStore";
    private static final String FILE = "leaderboard.json";

    private LeaderboardStore() {
    }

    public static synchronized Leaderboard load(Context context) {
        AtomicFile file = file(context);
        try (InputStream in = file.openRead()) {
            return Leaderboard.fromJson(readText(in));
        } catch (FileNotFoundException e) {
            return new Leaderboard();
        } catch (IOException | JSONException e) {
            // Lieber mit leerer Liste weiter als die App unbenutzbar machen; die kaputte Datei bleibt
            // liegen, bis das nächste Spiel sie überschreibt.
            Log.e(TAG, "Bestenliste nicht lesbar", e);
            return new Leaderboard();
        }
    }

    public static synchronized void save(Context context, Leaderboard board) throws IOException {
        AtomicFile file = file(context);
        FileOutputStream out = file.startWrite();
        try {
            out.write(toBytes(board));
            file.finishWrite(out);
        } catch (IOException | RuntimeException e) {
            file.failWrite(out);
            throw e;
        }
    }

    public static synchronized void add(Context context, GameRecord record) throws IOException {
        Leaderboard board = load(context);
        board.add(record);
        save(context, board);
    }

    public static void export(Context context, OutputStream out) throws IOException {
        out.write(toBytes(load(context)));
        out.flush();
    }

    /** Spiele aus einem Export übernehmen; vorhandene bleiben, Doppelte werden übersprungen. */
    public static synchronized Leaderboard.MergeResult importFrom(Context context, InputStream in)
            throws IOException, JSONException {
        Leaderboard imported = Leaderboard.fromJson(readText(in));
        Leaderboard board = load(context);
        Leaderboard.MergeResult result = board.merge(imported);
        if (result.added > 0) {
            save(context, board);
        }
        return result;
    }

    private static byte[] toBytes(Leaderboard board) throws IOException {
        try {
            return board.toJson().toString(2).getBytes(StandardCharsets.UTF_8);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private static String readText(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), FILE));
    }
}

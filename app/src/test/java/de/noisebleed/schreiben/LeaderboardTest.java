package de.noisebleed.schreiben;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.junit.Test;

import java.util.List;

import de.noisebleed.schreiben.game.GameTypes;
import de.noisebleed.schreiben.game.PluralType;
import de.noisebleed.schreiben.stats.GameRecord;
import de.noisebleed.schreiben.stats.Leaderboard;

public class LeaderboardTest {

    private static GameRecord game(String id, String type, int correct, long millis) {
        return new GameRecord(id, 1000 + id.hashCode(), type, correct, 5, correct, millis);
    }

    private static Leaderboard sample() {
        Leaderboard board = new Leaderboard();
        board.add(game("a", PluralType.ID, 3, 30_000));
        board.add(game("b", PluralType.ID, 5, 50_000));
        board.add(game("c", PluralType.ID, 5, 40_000));
        board.add(game("d", "neu", 4, 60_000));
        return board;
    }

    @Test
    public void ranksByPointsThenTime() {
        List<GameRecord> ranked = sample().ranked(null);
        assertEquals("c", ranked.get(0).id);
        assertEquals("b", ranked.get(1).id);
        assertEquals("d", ranked.get(2).id);
        assertEquals("a", ranked.get(3).id);
    }

    @Test
    public void filtersByType() {
        Leaderboard board = sample();
        assertEquals(3, board.ranked(GameTypes.byId(PluralType.ID)).size());
        assertEquals("c", board.best(GameTypes.byId(PluralType.ID)).id);
        assertNull(new Leaderboard().best(GameTypes.byId(PluralType.ID)));
    }

    @Test
    public void exportImportRoundTripSkipsDuplicates() throws Exception {
        String exported = sample().toJson().toString(2);
        Leaderboard other = new Leaderboard();
        other.add(game("x", PluralType.ID, 2, 20_000));
        other.add(game("a", PluralType.ID, 3, 30_000));

        Leaderboard.MergeResult result = other.merge(Leaderboard.fromJson(exported));
        assertEquals(3, result.added);
        assertEquals(1, result.duplicates);
        assertEquals(5, other.all().size());

        result = other.merge(Leaderboard.fromJson(exported));
        assertEquals(0, result.added);
        assertEquals(4, result.duplicates);
    }

    @Test
    public void keepsUnknownTypesAndSkipsBrokenEntries() throws Exception {
        String json = "{\"format\":\"schreiben-bestenliste\",\"version\":2,\"games\":["
                + "{\"id\":\"n\",\"timestamp\":5,\"type\":\"neu\","
                + "\"correct\":3,\"total\":5,\"points\":3,\"millis\":100},"
                + "{\"id\":\"kaputt\",\"timestamp\":5}"
                + "]}";
        Leaderboard board = Leaderboard.fromJson(json);
        assertEquals(1, board.all().size());
        assertNull(board.all().get(0).type());
    }

    @Test(expected = JSONException.class)
    public void rejectsOtherFiles() throws Exception {
        Leaderboard.fromJson("{\"games\":[]}");
    }
}

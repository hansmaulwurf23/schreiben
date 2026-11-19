package de.noisebleed.schreiben;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;

import de.noisebleed.schreiben.game.PluralType;
import de.noisebleed.schreiben.game.Round;
import de.noisebleed.schreiben.game.Task;

public class RoundTest {

    @Test
    public void roundHasFiveDifferentWords() {
        for (int seed = 0; seed < 200; seed++) {
            Round round = Round.create(new PluralType(), new Random(seed));
            assertEquals(5, Round.SIZE);
            assertEquals(Round.SIZE, round.getTasks().size());
            assertEquals(Round.SIZE, new HashSet<>(round.getTasks()).size());
        }
    }

    @Test
    public void onePointPerCorrectWord() {
        for (int correct = 0; correct <= Round.SIZE; correct++) {
            assertEquals(correct, play(correct).points());
        }
    }

    /** Spielt eine Runde mit genau {@code correct} richtigen Antworten, jede dauert 1,5 s. */
    private static Round play(int correct) {
        Round round = Round.create(new PluralType(), new Random(9));
        for (int i = 0; i < Round.SIZE; i++) {
            Task task = round.current();
            String answer = i < correct ? task.solution() : task.solution() + "x";
            assertEquals(i < correct, round.answer(answer, 1500));
        }
        assertTrue(round.isFinished());
        assertNull(round.current());
        assertEquals(correct, round.correctCount());
        assertEquals(7_500, round.totalMillis());
        return round;
    }

    @Test
    public void survivesJsonRoundTrip() throws Exception {
        Round round = Round.create(new PluralType(), new Random(3));
        round.answer(round.current().solution(), 1200);
        round.answer("quatsch", 800);
        Round copy = Round.fromJson(round.toJson());
        assertEquals(round.getTasks(), copy.getTasks());
        assertEquals(2, copy.getDone());
        assertEquals(1, copy.correctCount());
        assertEquals("quatsch", copy.getAnswer(1));
        assertEquals(2000, copy.totalMillis());
        assertFalse(copy.isFinished());
    }
}

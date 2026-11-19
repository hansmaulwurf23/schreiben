package de.noisebleed.schreiben;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import de.noisebleed.schreiben.game.Task;

public class TaskTest {

    private final Task apfel = new Task("der Apfel", "die", List.of("Äpfel"));
    private final Task wort = new Task("das Wort", "die", List.of("Wörter", "Worte"));

    @Test
    public void comparesExactlyButIgnoresSurroundingSpaces() {
        assertTrue(apfel.isCorrect("Äpfel"));
        assertTrue(apfel.isCorrect("  Äpfel "));
        assertFalse(apfel.isCorrect("Apfel"));
        assertFalse(apfel.isCorrect("Apfeln"));
        assertFalse(apfel.isCorrect("äpfel"));
        assertFalse(apfel.isCorrect("ÄPFEL"));
    }

    @Test
    public void recognisesCaseSlips() {
        assertTrue(apfel.isCaseSlip("äpfel"));
        assertTrue(apfel.isCaseSlip("ÄPFEL"));
        assertFalse(apfel.isCaseSlip("Äpfel"));
        assertFalse(apfel.isCaseSlip("apfel"));
    }

    @Test
    public void acceptsEveryListedPlural() {
        assertTrue(wort.isCorrect("Wörter"));
        assertTrue(wort.isCorrect("Worte"));
        assertEquals("Wörter", wort.solution());
        assertEquals("das Wort → die Wörter", wort.solved());
    }

    @Test
    public void survivesJson() throws Exception {
        assertEquals(wort, Task.fromJson(new JSONObject(wort.toJson().toString())));
    }
}

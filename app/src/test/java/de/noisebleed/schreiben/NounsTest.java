package de.noisebleed.schreiben;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import de.noisebleed.schreiben.game.Nouns;
import de.noisebleed.schreiben.game.PluralType;
import de.noisebleed.schreiben.game.Round;
import de.noisebleed.schreiben.game.Task;

public class NounsTest {

    @Test
    public void listIsClean() {
        Set<String> singulars = new HashSet<>();
        for (Nouns.Noun noun : Nouns.ALL) {
            assertTrue(noun.singular, Set.of("der", "die", "das").contains(noun.article));
            assertTrue("doppelt: " + noun.singular, singulars.add(noun.singular));
            assertTrue(noun.singular, Character.isUpperCase(noun.singular.charAt(0)));
            assertFalse(noun.plurals.isEmpty());
            for (String plural : noun.plurals) {
                assertTrue(plural, plural.matches("[A-ZÄÖÜ][a-zäöüß]+"));
                assertTrue("passt nicht auf die Tastatur: " + plural, plural.length() <= 14);
            }
        }
        assertTrue(Nouns.ALL.size() > 100);
    }

    @Test
    public void detectsUmlautPlurals() {
        assertTrue(find("Apfel").umlaut());
        assertTrue(find("Haus").umlaut());
        assertTrue(find("Fuß").umlaut());
        assertFalse(find("Hund").umlaut());
        // Schon die Einzahl hat den Umlaut: kein Wechsel.
        assertFalse(find("Löwe").umlaut());
        assertFalse(find("Mädchen").umlaut());
    }

    @Test
    public void everyRoundHasUmlautWords() {
        for (int seed = 0; seed < 500; seed++) {
            Round round = Round.create(new PluralType(), new Random(seed));
            int umlaut = 0;
            for (Task task : round.getTasks()) {
                if (find(task.getPrompt().substring(4)).umlaut()) {
                    umlaut++;
                }
            }
            assertTrue("Runde " + seed, umlaut >= PluralType.MIN_UMLAUT);
        }
    }

    @Test
    public void showsArticleAndAsksForPlural() {
        Task task = find("Apfel").toTask();
        assertEquals("der Apfel", task.getPrompt());
        assertEquals("die", task.getPrefix());
        assertEquals("Äpfel", task.solution());
    }

    @Test
    public void everyWordComesUpEventually() {
        Set<Task> seen = new HashSet<>();
        for (int seed = 0; seed < 2000; seed++) {
            seen.addAll(Round.create(new PluralType(), new Random(seed)).getTasks());
        }
        assertEquals(Nouns.ALL.size(), seen.size());
    }

    private static Nouns.Noun find(String singular) {
        for (Nouns.Noun noun : Nouns.ALL) {
            if (noun.singular.equals(singular)) {
                return noun;
            }
        }
        throw new AssertionError("nicht in der Liste: " + singular);
    }
}

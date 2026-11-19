package de.noisebleed.schreiben.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.noisebleed.schreiben.R;

/**
 * Mehrzahl: "der Apfel" wird gezeigt, das Kind schreibt "die Äpfel". Jede Runde enthält mindestens
 * {@link #MIN_UMLAUT} Wörter, deren Mehrzahl einen Umlaut bekommt — gerade die sind schwierig.
 */
public final class PluralType extends GameType {

    public static final String ID = "mehrzahl";
    /** So viele Umlaut-Wörter (Apfel – Äpfel) kommen in jeder Runde mindestens vor. */
    public static final int MIN_UMLAUT = 2;

    private final List<Nouns.Noun> nouns;

    public PluralType() {
        this(Nouns.ALL);
    }

    PluralType(List<Nouns.Noun> nouns) {
        super(ID, R.string.type_plural, R.string.type_plural_description, "Ä", R.color.type_plural);
        this.nouns = nouns;
    }

    @Override
    public List<Task> createTasks(int count, Random random) {
        List<Nouns.Noun> umlaut = new ArrayList<>();
        List<Nouns.Noun> rest = new ArrayList<>();
        for (Nouns.Noun noun : nouns) {
            (noun.umlaut() ? umlaut : rest).add(noun);
        }
        Collections.shuffle(umlaut, random);
        List<Nouns.Noun> chosen = new ArrayList<>(umlaut.subList(0, Math.min(MIN_UMLAUT, umlaut.size())));
        // Der Rest kommt aus allen übrigen Wörtern, Umlaut oder nicht.
        rest.addAll(umlaut.subList(chosen.size(), umlaut.size()));
        Collections.shuffle(rest, random);
        for (int i = 0; chosen.size() < count && i < rest.size(); i++) {
            chosen.add(rest.get(i));
        }
        Collections.shuffle(chosen, random);
        List<Task> tasks = new ArrayList<>();
        for (Nouns.Noun noun : chosen) {
            tasks.add(noun.toTask());
        }
        return tasks;
    }
}

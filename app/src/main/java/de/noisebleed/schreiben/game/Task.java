package de.noisebleed.schreiben.game;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Eine einzelne Schreibaufgabe: Das Kind sieht {@link #getPrompt()} ("der Apfel") und schreibt hinter
 * {@link #getPrefix()} ("die") das gesuchte Wort. Es können mehrere Schreibweisen richtig sein
 * (Wörter, Worte); die erste ist die, die als Lösung angezeigt wird.
 *
 * Verglichen wird genau, auch Groß- und Kleinschreibung — beim Schreibenlernen gehört sie dazu.
 * Nur Leerzeichen am Rand zählen nicht.
 */
public final class Task {

    private final String prompt;
    private final String prefix;
    private final List<String> answers;

    public Task(String prompt, String prefix, List<String> answers) {
        if (answers.isEmpty()) {
            throw new IllegalArgumentException("Aufgabe ohne Lösung: " + prompt);
        }
        this.prompt = prompt;
        this.prefix = prefix;
        this.answers = Collections.unmodifiableList(new ArrayList<>(answers));
    }

    /** Was gezeigt wird, z. B. "der Apfel". */
    public String getPrompt() {
        return prompt;
    }

    /** Steht vor dem Eingabefeld, z. B. "die"; darf leer sein. */
    public String getPrefix() {
        return prefix;
    }

    public List<String> getAnswers() {
        return answers;
    }

    /** Die Lösung, die angezeigt wird. */
    public String solution() {
        return answers.get(0);
    }

    public boolean isCorrect(String given) {
        return answers.contains(normalize(given));
    }

    /**
     * Richtig bis auf Groß- und Kleinschreibung ("äpfel"). Zählt trotzdem als falsch, bekommt aber
     * einen eigenen Hinweis.
     */
    public boolean isCaseSlip(String given) {
        if (isCorrect(given)) {
            return false;
        }
        String lower = normalize(given).toLowerCase(Locale.GERMAN);
        for (String answer : answers) {
            if (answer.toLowerCase(Locale.GERMAN).equals(lower)) {
                return true;
            }
        }
        return false;
    }

    /** Aufgabe mit Lösung, z. B. "der Apfel → die Äpfel". */
    public String solved() {
        return prompt + " → " + withPrefix(solution());
    }

    /** "die Äpfel"; ohne Präfix nur das Wort. */
    public String withPrefix(String word) {
        return prefix.isEmpty() ? word : prefix + " " + word;
    }

    static String normalize(String given) {
        return given.trim().replaceAll("\\s+", " ");
    }

    @NonNull
    @Override
    public String toString() {
        return prompt + " → " + withPrefix("?");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Task)) {
            return false;
        }
        Task t = (Task) o;
        return t.prompt.equals(prompt) && t.prefix.equals(prefix) && t.answers.equals(answers);
    }

    @Override
    public int hashCode() {
        return (31 * prompt.hashCode() + prefix.hashCode()) * 31 + answers.hashCode();
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("prompt", prompt)
                .put("prefix", prefix)
                .put("answers", new JSONArray(answers));
    }

    public static Task fromJson(JSONObject json) throws JSONException {
        JSONArray array = json.getJSONArray("answers");
        List<String> answers = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            answers.add(array.getString(i));
        }
        return new Task(json.getString("prompt"), json.optString("prefix", ""), answers);
    }
}

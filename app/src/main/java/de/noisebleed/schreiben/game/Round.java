package de.noisebleed.schreiben.game;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Eine Runde aus {@link #SIZE} zufälligen Aufgaben eines Spiels. Hält die gegebenen Antworten und die
 * Bearbeitungszeit je Aufgabe; die Zeit misst der Aufrufer, damit die Klasse ohne Android-Uhr
 * testbar bleibt. Jede richtige Antwort bringt einen Punkt.
 */
public final class Round {

    public static final int SIZE = 5;

    private final GameType type;
    private final List<Task> tasks;
    private final List<String> answers = new ArrayList<>();
    private final List<Long> millis = new ArrayList<>();

    private Round(GameType type, List<Task> tasks) {
        this.type = type;
        this.tasks = Collections.unmodifiableList(tasks);
    }

    public static Round create(GameType type, Random random) {
        return new Round(type, new ArrayList<>(type.createTasks(SIZE, random)));
    }

    public GameType getType() {
        return type;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    /** Wie viele Aufgaben schon beantwortet sind. */
    public int getDone() {
        return answers.size();
    }

    public boolean isFinished() {
        return answers.size() == tasks.size();
    }

    @Nullable
    public Task current() {
        return isFinished() ? null : tasks.get(answers.size());
    }

    /** Antwort auf die aktuelle Aufgabe verbuchen; true, wenn sie stimmt. */
    public boolean answer(String value, long durationMillis) {
        Task task = current();
        if (task == null) {
            throw new IllegalStateException("Runde ist schon vorbei");
        }
        answers.add(Task.normalize(value));
        millis.add(Math.max(0, durationMillis));
        return task.isCorrect(value);
    }

    /** Die Antwort auf Aufgabe {@code index}, null wenn noch nicht beantwortet. */
    @Nullable
    public String getAnswer(int index) {
        return index < answers.size() ? answers.get(index) : null;
    }

    public boolean isCorrect(int index) {
        String answer = getAnswer(index);
        return answer != null && tasks.get(index).isCorrect(answer);
    }

    public int correctCount() {
        int correct = 0;
        for (int i = 0; i < answers.size(); i++) {
            if (isCorrect(i)) {
                correct++;
            }
        }
        return correct;
    }

    /** Ein Punkt je richtige Antwort. */
    public int points() {
        return correctCount();
    }

    /** Summe der Bearbeitungszeiten; die Pausen für die Rückmeldung zählen nicht mit. */
    public long totalMillis() {
        long total = 0;
        for (long m : millis) {
            total += m;
        }
        return total;
    }

    // ---------------------------------------------------------------- Zwischenspeichern

    public JSONObject toJson() throws JSONException {
        JSONArray taskArray = new JSONArray();
        for (Task task : tasks) {
            taskArray.put(task.toJson());
        }
        return new JSONObject()
                .put("type", type.getId())
                .put("tasks", taskArray)
                .put("answers", new JSONArray(answers))
                .put("millis", new JSONArray(millis));
    }

    public static Round fromJson(JSONObject json) throws JSONException {
        GameType type = GameTypes.byId(json.getString("type"));
        if (type == null) {
            throw new JSONException("Unbekanntes Spiel");
        }
        JSONArray taskArray = json.getJSONArray("tasks");
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < taskArray.length(); i++) {
            tasks.add(Task.fromJson(taskArray.getJSONObject(i)));
        }
        Round round = new Round(type, tasks);
        JSONArray answerArray = json.getJSONArray("answers");
        JSONArray millisArray = json.getJSONArray("millis");
        for (int i = 0; i < answerArray.length(); i++) {
            round.answers.add(answerArray.getString(i));
            round.millis.add(millisArray.getLong(i));
        }
        return round;
    }
}

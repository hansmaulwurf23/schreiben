package de.noisebleed.schreiben;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

import de.noisebleed.schreiben.game.GameType;
import de.noisebleed.schreiben.game.GameTypes;
import de.noisebleed.schreiben.game.Round;
import de.noisebleed.schreiben.game.Task;
import de.noisebleed.schreiben.stats.GameRecord;
import de.noisebleed.schreiben.stats.Leaderboard;
import de.noisebleed.schreiben.stats.LeaderboardStore;
import de.noisebleed.schreiben.widget.KeyboardView;

/**
 * Die Aufgaben einer Runde nacheinander. Eingabe über die eigene Buchstabentastatur (oder eine
 * Hardware-Tastatur), nach jeder Antwort eine kurze Rückmeldung. Gemessen wird nur die Zeit, in der
 * eine Aufgabe zu sehen ist und auf eine Antwort wartet — die Rückmeldung und eine Pause der App
 * zählen nicht.
 */
public class QuizActivity extends AppCompatActivity implements KeyboardView.Listener {

    private static final String TAG = "QuizActivity";

    public static final String EXTRA_TYPE = "type";

    private static final String STATE_ROUND = "round";
    private static final String STATE_INPUT = "input";
    private static final String STATE_ELAPSED = "elapsed";

    private static final int MAX_CHARS = 14;
    private static final long FEEDBACK_CORRECT_MS = 900;
    private static final long FEEDBACK_WRONG_MS = 3000;
    private static final long TICK_MS = 250;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StringBuilder input = new StringBuilder();

    private Round round;

    /** Bearbeitungszeit der aktuellen Aufgabe bis zur letzten Pause. */
    private long elapsedBefore;
    /** Seit wann die Uhr läuft (elapsedRealtime), 0 = angehalten. */
    private long runningSince;
    /** Rückmeldung zur letzten Antwort wird gezeigt; Eingaben außer Fertig sind gesperrt. */
    private boolean showingFeedback;

    private TextView progressText;
    private TextView timer;
    private LinearProgressIndicator progress;
    private TextView prompt;
    private TextView prefix;
    private TextView answerBox;
    private TextView feedback;
    private KeyboardView keyboard;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            timer.setText(Format.duration(round.totalMillis() + currentElapsed()));
            handler.postDelayed(this, TICK_MS);
        }
    };

    private final Runnable next = this::nextTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        round = restoreRound(savedInstanceState);
        if (round == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_quiz);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(round.getType().getTitle());
        toolbar.setNavigationOnClickListener(v -> confirmAbort());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmAbort();
            }
        });

        progressText = findViewById(R.id.progress_text);
        timer = findViewById(R.id.timer);
        progress = findViewById(R.id.progress);
        progress.setMax(round.getTasks().size());
        progress.setIndicatorColor(ContextCompat.getColor(this, round.getType().getColor()));
        prompt = findViewById(R.id.prompt);
        prefix = findViewById(R.id.prefix);
        answerBox = findViewById(R.id.answer);
        answerBox.setBackground(ContextCompat.getDrawable(this, R.drawable.answer_box).mutate());
        feedback = findViewById(R.id.feedback);
        keyboard = findViewById(R.id.keyboard);
        keyboard.setListener(this);

        if (savedInstanceState != null) {
            input.append(savedInstanceState.getString(STATE_INPUT, ""));
            elapsedBefore = savedInstanceState.getLong(STATE_ELAPSED, 0);
        }
        showTask();
    }

    private Round restoreRound(Bundle state) {
        if (state != null && state.containsKey(STATE_ROUND)) {
            try {
                return Round.fromJson(new JSONObject(state.getString(STATE_ROUND)));
            } catch (JSONException e) {
                Log.e(TAG, "Runde nicht wiederherstellbar", e);
            }
        }
        GameType type = GameTypes.byId(getIntent().getStringExtra(EXTRA_TYPE));
        return type == null ? null : Round.create(type, new Random());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!showingFeedback) {
            runningSince = SystemClock.elapsedRealtime();
        }
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseClock();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            outState.putString(STATE_ROUND, round.toJson().toString());
        } catch (JSONException e) {
            Log.e(TAG, "Runde nicht speicherbar", e);
        }
        outState.putString(STATE_INPUT, showingFeedback ? "" : input.toString());
        outState.putLong(STATE_ELAPSED, showingFeedback ? 0 : currentElapsed());
    }

    // ---------------------------------------------------------------- Uhr

    private long currentElapsed() {
        return elapsedBefore + (runningSince > 0 ? SystemClock.elapsedRealtime() - runningSince : 0);
    }

    private void pauseClock() {
        elapsedBefore = currentElapsed();
        runningSince = 0;
    }

    // ---------------------------------------------------------------- Anzeige

    private void showTask() {
        Task task = round.current();
        if (task == null) {
            finishRound();
            return;
        }
        int done = round.getDone();
        progressText.setText(getString(R.string.task_progress, done + 1, round.getTasks().size()));
        progress.setProgressCompat(done, true);
        feedback.setText("");
        prompt.setText(task.getPrompt());
        prefix.setText(task.getPrefix());
        setBoxColors(R.color.answer_box_bg, R.color.answer_box_border);
        updateInput();
    }

    private void updateInput() {
        answerBox.setText(input);
        // Am Wortanfang groß: Nomen schreibt man groß.
        keyboard.setShift(input.length() == 0);
    }

    private void setBoxColors(@ColorRes int fill, @ColorRes int stroke) {
        GradientDrawable box = (GradientDrawable) answerBox.getBackground();
        box.setColor(ContextCompat.getColor(this, fill));
        box.setStroke(Math.round(3 * getResources().getDisplayMetrics().density),
                ContextCompat.getColor(this, stroke));
    }

    // ---------------------------------------------------------------- Eingabe

    @Override
    public void onLetter(char letter) {
        if (showingFeedback || input.length() >= MAX_CHARS) {
            return;
        }
        input.append(letter);
        updateInput();
    }

    @Override
    public void onDelete() {
        if (showingFeedback || input.length() == 0) {
            return;
        }
        input.setLength(input.length() - 1);
        updateInput();
    }

    @Override
    public void onEnter() {
        if (showingFeedback) {
            // Während der Rückmeldung weiter zur nächsten Aufgabe, statt zu warten.
            handler.removeCallbacks(next);
            nextTask();
            return;
        }
        if (input.toString().trim().isEmpty()) {
            return;
        }
        Task task = round.current();
        String given = input.toString();
        long millis = currentElapsed();
        runningSince = 0;
        elapsedBefore = 0;
        boolean correct = round.answer(given, millis);
        showingFeedback = true;
        progress.setProgressCompat(round.getDone(), true);

        if (correct) {
            setBoxColors(R.color.answer_correct_bg, R.color.answer_correct);
            feedback.setTextColor(ContextCompat.getColor(this, R.color.answer_correct));
            feedback.setText(R.string.feedback_correct);
        } else {
            setBoxColors(R.color.answer_wrong_bg, R.color.answer_wrong);
            feedback.setTextColor(ContextCompat.getColor(this, R.color.answer_wrong));
            String solution = task.withPrefix(task.solution());
            feedback.setText(task.isCaseSlip(given)
                    ? getString(R.string.feedback_case, solution)
                    : getString(R.string.feedback_wrong, solution));
        }
        handler.postDelayed(next, correct ? FEEDBACK_CORRECT_MS : FEEDBACK_WRONG_MS);
    }

    private void nextTask() {
        showingFeedback = false;
        input.setLength(0);
        elapsedBefore = 0;
        runningSince = SystemClock.elapsedRealtime();
        showTask();
    }

    private void finishRound() {
        handler.removeCallbacksAndMessages(null);
        GameRecord record = GameRecord.of(round, System.currentTimeMillis());
        GameRecord best = LeaderboardStore.load(this).best(round.getType());
        boolean newRecord = record.points > 0
                && (best == null || Leaderboard.RANKING.compare(record, best) < 0);
        String saveError = null;
        try {
            LeaderboardStore.add(this, record);
        } catch (IOException e) {
            Log.e(TAG, "Ergebnis nicht gespeichert", e);
            saveError = e.getMessage();
        }
        Intent result = new Intent(this, ResultActivity.class)
                .putExtra(ResultActivity.EXTRA_RECORD_ID, record.id)
                .putExtra(ResultActivity.EXTRA_NEW_RECORD, newRecord)
                .putExtra(ResultActivity.EXTRA_SAVE_ERROR, saveError);
        try {
            result.putExtra(ResultActivity.EXTRA_ROUND, round.toJson().toString());
        } catch (JSONException e) {
            Log.e(TAG, "Runde nicht übergebbar", e);
        }
        startActivity(result);
        finish();
    }

    private void confirmAbort() {
        if (round.getDone() == 0 && input.length() == 0) {
            finish();
            return;
        }
        Dialogs.builder(this)
                .setTitle(R.string.abort_title)
                .setMessage(R.string.abort_text)
                .setPositiveButton(R.string.abort_confirm, (d, w) -> finish())
                .setNegativeButton(R.string.abort_continue, null)
                .show();
    }

    /** Hardware- oder Bluetooth-Tastatur, praktisch im Emulator. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        Runnable action = null;
        int unicode = event.getUnicodeChar(event.getMetaState());
        if (unicode != 0 && Character.isLetter(unicode)) {
            action = () -> onLetter((char) unicode);
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            action = this::onDelete;
        } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            action = this::onEnter;
        }
        if (action == null) {
            return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            action.run();
        }
        return true;
    }
}

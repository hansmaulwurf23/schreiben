package de.noisebleed.schreiben;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import de.noisebleed.schreiben.game.Round;
import de.noisebleed.schreiben.game.Task;

/** Nach der Runde: Punkte, Zeit und alle Wörter mit Lösung; weiter geht es zur Bestenliste. */
public class ResultActivity extends AppCompatActivity {

    private static final String TAG = "ResultActivity";

    public static final String EXTRA_ROUND = "round";
    public static final String EXTRA_RECORD_ID = "record_id";
    public static final String EXTRA_NEW_RECORD = "new_record";
    public static final String EXTRA_SAVE_ERROR = "save_error";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Round round;
        try {
            round = Round.fromJson(new JSONObject(getIntent().getStringExtra(EXTRA_ROUND)));
        } catch (JSONException | NullPointerException e) {
            Log.e(TAG, "Keine Runde übergeben", e);
            finish();
            return;
        }
        setContentView(R.layout.activity_result);

        int correct = round.correctCount();
        int total = round.getTasks().size();
        String emoji;
        int title;
        if (correct == total) {
            emoji = "🏆";
            title = R.string.result_title_perfect;
        } else if (correct >= total * 8 / 10) {
            emoji = "🌟";
            title = R.string.result_title_great;
        } else if (correct >= total / 2) {
            emoji = "👍";
            title = R.string.result_title_good;
        } else {
            emoji = "💪";
            title = R.string.result_title_practice;
        }
        ((TextView) findViewById(R.id.emoji)).setText(emoji);
        ((TextView) findViewById(R.id.title)).setText(title);
        ((TextView) findViewById(R.id.subtitle)).setText(getString(R.string.result_correct, correct, total));
        ((TextView) findViewById(R.id.points)).setText(String.valueOf(round.points()));
        ((TextView) findViewById(R.id.time)).setText(Format.duration(round.totalMillis()));
        if (getIntent().getBooleanExtra(EXTRA_NEW_RECORD, false)) {
            findViewById(R.id.record).setVisibility(View.VISIBLE);
        }

        LinearLayout review = findViewById(R.id.review);
        for (int i = 0; i < total; i++) {
            Task task = round.getTasks().get(i);
            View row = getLayoutInflater().inflate(R.layout.item_review, review, false);
            TextView mark = row.findViewById(R.id.mark);
            boolean right = round.isCorrect(i);
            mark.setText(right ? "✓" : "✗");
            mark.setTextColor(ContextCompat.getColor(this, right ? R.color.answer_correct : R.color.answer_wrong));
            ((TextView) row.findViewById(R.id.task)).setText(task.solved());
            if (!right) {
                // Die eigene, falsche Antwort durchgestrichen daneben.
                TextView given = row.findViewById(R.id.given);
                given.setText(round.getAnswer(i));
                given.setPaintFlags(given.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            review.addView(row);
        }

        String recordId = getIntent().getStringExtra(EXTRA_RECORD_ID);
        findViewById(R.id.to_leaderboard).setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class)
                    .putExtra(LeaderboardActivity.EXTRA_HIGHLIGHT, recordId)
                    .putExtra(LeaderboardActivity.EXTRA_TYPE, round.getType().getId()));
            finish();
        });
        findViewById(R.id.play_again).setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class)
                    .putExtra(QuizActivity.EXTRA_TYPE, round.getType().getId()));
            finish();
        });

        String saveError = getIntent().getStringExtra(EXTRA_SAVE_ERROR);
        if (saveError != null && savedInstanceState == null) {
            Snackbar.make(review, getString(R.string.result_save_failed, saveError),
                    Snackbar.LENGTH_LONG).show();
        }
    }
}

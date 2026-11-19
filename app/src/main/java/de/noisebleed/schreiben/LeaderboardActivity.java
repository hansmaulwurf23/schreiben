package de.noisebleed.schreiben;

import android.content.ActivityNotFoundException;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.noisebleed.schreiben.stats.GameRecord;
import de.noisebleed.schreiben.stats.Leaderboard;
import de.noisebleed.schreiben.stats.LeaderboardStore;
import de.noisebleed.schreiben.game.GameType;
import de.noisebleed.schreiben.game.GameTypes;

/**
 * Bestenliste aller gespielten Runden, filterbar nach Spiel. Lässt sich als JSON
 * exportieren und auf einem anderen Gerät wieder importieren.
 */
public class LeaderboardActivity extends AppCompatActivity {

    private static final String TAG = "LeaderboardActivity";

    /** Diesen Eintrag hervorheben und hinscrollen (gerade gespieltes Spiel). */
    public static final String EXTRA_HIGHLIGHT = "highlight";
    /** Anfangs gewählte Filter. */
    public static final String EXTRA_TYPE = "type";

    private static final String STATE_TYPE = "type";

    private Leaderboard board;
    @Nullable
    private GameType filterType;
    @Nullable
    private String highlight;

    private ChipGroup typeFilter;
    private TextView summary;
    private TextView empty;
    private RecyclerView list;
    private final GameAdapter adapter = new GameAdapter();

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), this::exportTo);
    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::importFrom);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        typeFilter = findViewById(R.id.type_filter);
        summary = findViewById(R.id.summary);
        empty = findViewById(R.id.empty);
        list = findViewById(R.id.games);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        Bundle source = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        if (source != null) {
            filterType = GameTypes.byId(source.getString(savedInstanceState != null ? STATE_TYPE : EXTRA_TYPE));
        }
        highlight = getIntent().getStringExtra(EXTRA_HIGHLIGHT);

        board = LeaderboardStore.load(this);
        buildTypeChips();
        refresh(true);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TYPE, filterType != null ? filterType.getId() : null);
    }

    // ---------------------------------------------------------------- Filter

    private void buildTypeChips() {
        typeFilter.removeAllViews();
        addChip(typeFilter, getString(R.string.filter_all), R.color.neutral, filterType == null,
                () -> setTypeFilter(null));
        for (GameType type : GameTypes.all()) {
            addChip(typeFilter, type.getSymbol() + "  " + getString(type.getTitle()), type.getColor(),
                    type == filterType, () -> setTypeFilter(type));
        }
    }

    private void addChip(ChipGroup group, CharSequence text, int color, boolean checked, Runnable onSelect) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setTextSize(16);
        int c = ContextCompat.getColor(this, color);
        int[][] states = {{android.R.attr.state_checked}, {}};
        chip.setChipBackgroundColor(new ColorStateList(states,
                new int[]{c, ContextCompat.getColor(this, R.color.surface)}));
        chip.setTextColor(new ColorStateList(states,
                new int[]{Color.WHITE, ContextCompat.getColor(this, R.color.text_primary)}));
        chip.setChipStrokeColor(ColorStateList.valueOf(c));
        chip.setChipStrokeWidth(2 * getResources().getDisplayMetrics().density);
        group.addView(chip);
        chip.setChecked(checked);
        chip.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                onSelect.run();
            }
        });
    }

    private void setTypeFilter(@Nullable GameType type) {
        filterType = type;
        refresh(false);
    }

    private void refresh(boolean scrollToHighlight) {
        List<GameRecord> games = board.ranked(filterType);
        adapter.setGames(games);
        empty.setVisibility(games.isEmpty() ? View.VISIBLE : View.GONE);

        int points = 0, correct = 0, total = 0;
        for (GameRecord game : games) {
            points += game.points;
            correct += game.correct;
            total += game.total;
        }
        summary.setText(getResources().getQuantityString(R.plurals.leaderboard_summary, games.size(),
                games.size(), getResources().getQuantityString(R.plurals.points_short, points, points),
                total == 0 ? 0 : Math.round(100f * correct / total)));
        summary.setVisibility(games.isEmpty() ? View.GONE : View.VISIBLE);

        if (scrollToHighlight && highlight != null) {
            for (int i = 0; i < games.size(); i++) {
                if (games.get(i).id.equals(highlight)) {
                    int position = i;
                    list.post(() -> ((LinearLayoutManager) list.getLayoutManager())
                            .scrollToPositionWithOffset(position, list.getHeight() / 3));
                    break;
                }
            }
        }
    }

    // ---------------------------------------------------------------- Liste

    private final class GameAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final DateFormat dateFormat =
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.GERMANY);
        private List<GameRecord> games = new ArrayList<>();

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        void setGames(List<GameRecord> games) {
            this.games = games;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
            return new RecyclerView.ViewHolder(view) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            GameRecord game = games.get(position);
            View view = holder.itemView;

            TextView rank = view.findViewById(R.id.rank);
            rank.setText(String.valueOf(position + 1));
            int rankColor = position == 0 ? R.color.gold : position == 1 ? R.color.silver
                    : position == 2 ? R.color.bronze : R.color.divider;
            rank.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(LeaderboardActivity.this, rankColor)));
            rank.setTextColor(position < 3 ? Color.WHITE
                    : ContextCompat.getColor(LeaderboardActivity.this, R.color.text_primary));

            GameType type = game.type();
            TextView symbol = view.findViewById(R.id.symbol);
            symbol.setText(type != null ? type.getSymbol() : "?");
            symbol.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(LeaderboardActivity.this,
                    type != null ? type.getColor() : R.color.disabled)));

            CharSequence typeName = type != null ? getString(type.getTitle())
                    : getString(R.string.type_unknown, game.typeId);
            ((TextView) view.findViewById(R.id.title)).setText(typeName);
            ((TextView) view.findViewById(R.id.detail)).setText(getString(R.string.leaderboard_detail,
                    game.correct, game.total, Format.duration(game.millis)));
            ((TextView) view.findViewById(R.id.date)).setText(dateFormat.format(new Date(game.timestamp)));
            ((TextView) view.findViewById(R.id.points)).setText(getResources().getQuantityString(R.plurals.points_short, game.points, game.points));

            boolean highlighted = game.id.equals(highlight);
            MaterialCardView card = view.findViewById(R.id.card);
            card.setCardBackgroundColor(ContextCompat.getColor(LeaderboardActivity.this,
                    highlighted ? R.color.highlight : R.color.surface));
            card.setStrokeWidth(highlighted ? Math.round(2 * getResources().getDisplayMetrics().density) : 0);
        }
    }

    // ---------------------------------------------------------------- Export / Import

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_leaderboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            if (item.getItemId() == R.id.export) {
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
                exportLauncher.launch("schreiben-bestenliste-" + date + ".json");
                return true;
            }
            if (item.getItemId() == R.id.import_) {
                // Manche Dateimanager kennen .json nicht als application/json.
                importLauncher.launch(new String[]{"application/json", "text/plain", "application/octet-stream"});
                return true;
            }
        } catch (ActivityNotFoundException e) {
            // Kein Dateiauswahl-Dialog, z. B. in einem eingeschränkten Kinderprofil.
            message(R.string.no_file_picker);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportTo(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
                throw new IOException("Datei nicht beschreibbar");
            }
            LeaderboardStore.export(this, out);
            message(R.string.export_done);
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Export fehlgeschlagen", e);
            message(getString(R.string.export_failed, e.getMessage()));
        }
    }

    private void importFrom(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IOException("Datei nicht lesbar");
            }
            Leaderboard.MergeResult result = LeaderboardStore.importFrom(this, in);
            board = LeaderboardStore.load(this);
            refresh(false);
            message(getResources().getQuantityString(R.plurals.import_done, result.added,
                    result.added, result.duplicates));
        } catch (IOException | JSONException | SecurityException e) {
            Log.e(TAG, "Import fehlgeschlagen", e);
            message(getString(R.string.import_failed, e.getMessage()));
        }
    }

    private void message(@StringRes int text) {
        message(getString(text));
    }

    private void message(String text) {
        Snackbar.make(list, text, Snackbar.LENGTH_LONG).show();
    }
}

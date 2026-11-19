package de.noisebleed.schreiben;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import de.noisebleed.schreiben.game.GameType;
import de.noisebleed.schreiben.game.GameTypes;
import de.noisebleed.schreiben.update.AppUpdater;

/** Startbildschirm: eine Kachel je Spiel aus {@link GameTypes}. */
public class MainActivity extends AppCompatActivity {

    private RecyclerView types;

    /** Pro Start nur einmal von selbst auf ein Update hinweisen, nicht bei jedem onResume. */
    private boolean updateOffered;
    /** Nach der Freigabe "Apps aus dieser Quelle" die Installation erneut anstoßen. */
    private boolean installAfterPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        types = findViewById(R.id.types);
        // Solange es nur ein Spiel gibt, nimmt seine Kachel die ganze Breite ein.
        int columns = Math.min(getResources().getInteger(R.integer.type_columns), GameTypes.all().size());
        GridLayoutManager layout = new GridLayoutManager(this, columns);
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? columns : 1;
            }
        });
        types.setLayoutManager(layout);
        types.setAdapter(new TypeAdapter(GameTypes.all()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (installAfterPermission) {
            installAfterPermission = !AppUpdater.install(this);
            return;
        }
        if (AppUpdater.readyVersionName(this) != null) {
            offerUpdate();
        } else {
            AppUpdater.checkIfDue(this, (version, error) -> {
                if (version != null) {
                    offerUpdate();
                }
            });
        }
    }

    private void open(GameType type) {
        startActivity(new Intent(this, QuizActivity.class)
                .putExtra(QuizActivity.EXTRA_TYPE, type.getId()));
    }

    /** Überschrift als erste, volle Zeile, danach die Kacheln. */
    private final class TypeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int HEADING = 0;
        private static final int TYPE = 1;

        private final List<GameType> items;

        TypeAdapter(List<GameType> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? HEADING : TYPE;
        }

        @Override
        public int getItemCount() {
            return items.size() + 1;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout = viewType == HEADING ? R.layout.item_heading : R.layout.item_task_type;
            return new RecyclerView.ViewHolder(inflater.inflate(layout, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                return;
            }
            GameType type = items.get(position - 1);
            View view = holder.itemView;
            TextView symbol = view.findViewById(R.id.symbol);
            symbol.setText(type.getSymbol());
            symbol.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(MainActivity.this, type.getColor())));
            ((TextView) view.findViewById(R.id.title)).setText(type.getTitle());
            ((TextView) view.findViewById(R.id.limits)).setText(type.getDescription());
            view.setOnClickListener(v -> open(type));
        }
    }

    // ---------------------------------------------------------------- Menü

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.dark_theme).setChecked(Settings.isDark(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.leaderboard) {
            startActivity(new Intent(this, LeaderboardActivity.class));
            return true;
        }
        if (id == R.id.dark_theme) {
            Settings.setDark(this, !item.isChecked());
            return true;
        }
        if (id == R.id.check_update) {
            checkForUpdate();
            return true;
        }
        if (id == R.id.about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------- Update

    private void checkForUpdate() {
        if (!AppUpdater.isAllowed(this)) {
            Snackbar.make(types, R.string.update_not_allowed, Snackbar.LENGTH_LONG).show();
            return;
        }
        Snackbar.make(types, R.string.update_checking, Snackbar.LENGTH_SHORT).show();
        AppUpdater.check(this, (version, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (version != null) {
                updateOffered = false;
                offerUpdate();
            } else {
                String message = error != null
                        ? getString(R.string.update_failed, error)
                        : getString(R.string.update_none, BuildConfig.VERSION_NAME);
                Snackbar.make(types, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void offerUpdate() {
        String version = AppUpdater.readyVersionName(this);
        if (version == null || updateOffered || isFinishing() || isDestroyed()) {
            return;
        }
        updateOffered = true;
        Dialogs.builder(this)
                .setTitle(R.string.update_available_title)
                .setMessage(getString(R.string.update_available_text, version, BuildConfig.VERSION_NAME))
                .setPositiveButton(R.string.update_install,
                        (d, w) -> installAfterPermission = !AppUpdater.install(this))
                .setNegativeButton(R.string.update_later, null)
                .show();
    }

    private void showAbout() {
        View content = getLayoutInflater().inflate(R.layout.dialog_about, null, false);
        TextView version = content.findViewById(R.id.about_version);
        version.setText(getString(R.string.about_version,
                BuildConfig.VERSION_NAME, BuildConfig.GIT_SHA));
        Dialogs.builder(this)
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}

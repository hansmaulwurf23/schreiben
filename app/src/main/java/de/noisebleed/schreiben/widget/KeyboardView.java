package de.noisebleed.schreiben.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.noisebleed.schreiben.R;

/**
 * Bildschirmtastatur im QWERTZ-Layout mit Ä, Ö, Ü und ß, Umschalttaste und einer breiten
 * Fertig-Taste. Eine eigene Tastatur statt der des Systems, weil die mit Wortvorschlägen und
 * Autokorrektur die Lösung verraten würde.
 *
 * Die Umschalttaste gilt für einen Buchstaben, wie am Handy. Ob sie anfangs gedrückt ist, entscheidet
 * der Aufrufer über {@link #setShift} (am Wortanfang ja, denn Nomen schreibt man groß).
 */
public class KeyboardView extends View {

    public interface Listener {
        void onLetter(char letter);

        void onEnter();

        void onDelete();
    }

    private static final char ENTER = '\n';
    private static final char DELETE = '\b';
    private static final char SHIFT = '\u0001';
    private static final String[] ROWS = {"QWERTZUIOPÜ", "ASDFGHJKLÖÄ", SHIFT + "YXCVBNMß" + DELETE, "\n"};
    private static final int COLUMNS = 11;
    /** Umschalten und Löschen füllen zusammen mit acht Buchstaben die dritte Reihe. */
    private static final float WIDE = (COLUMNS - 8) / 2f;
    /** Tasten sind etwas höher als breit. */
    private static final float KEY_ASPECT = 1.4f;
    /** Abstand zwischen den Tasten relativ zur Tastenbreite. */
    private static final float GAP_RATIO = 0.12f;

    private static final class Key {
        final char code;
        final RectF rect = new RectF();

        Key(char code) {
            this.code = code;
        }
    }

    private final List<Key> keys = new ArrayList<>();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path backspace = new Path();
    private final Path shiftArrow = new Path();
    private final Path check = new Path();

    private final int colorKey;
    private final int colorKeyPressed;
    private final int colorText;
    private final int colorOk;
    private final int colorShiftActive;
    private final float maxKeyHeight;
    private float gap;
    private float corner;

    private boolean shift;
    private Listener listener;
    private Key pressed;

    public KeyboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        colorKey = ContextCompat.getColor(context, R.color.key_bg);
        colorKeyPressed = ContextCompat.getColor(context, R.color.key_pressed);
        colorText = ContextCompat.getColor(context, R.color.key_text);
        colorOk = ContextCompat.getColor(context, R.color.key_ok);
        colorShiftActive = ContextCompat.getColor(context, R.color.key_shift_active);
        maxKeyHeight = getResources().getDimension(R.dimen.letter_key_max_height);
        float density = getResources().getDisplayMetrics().density;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.5f * density);
        stroke.setColor(ContextCompat.getColor(context, R.color.key_stroke));
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        icon.setStyle(Paint.Style.STROKE);
        icon.setStrokeWidth(2.5f * density);
        icon.setStrokeJoin(Paint.Join.ROUND);
        icon.setStrokeCap(Paint.Cap.ROUND);
        for (String row : ROWS) {
            for (char c : row.toCharArray()) {
                keys.add(new Key(c));
            }
        }
        setHapticFeedbackEnabled(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Nächster Buchstabe groß? */
    public void setShift(boolean shift) {
        if (this.shift != shift) {
            this.shift = shift;
            invalidate();
        }
    }

    public boolean isShift() {
        return shift;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float keyWidth = keyWidthFor(width);
        float keyHeight = Math.min(keyWidth * KEY_ASPECT, maxKeyHeight);
        int height = Math.round(ROWS.length * keyHeight + (ROWS.length - 1) * keyWidth * GAP_RATIO)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float avail = w - getPaddingLeft() - getPaddingRight();
        float keyWidth = keyWidthFor(w);
        gap = keyWidth * GAP_RATIO;
        corner = keyWidth * 0.22f;
        float rowHeight = (h - getPaddingTop() - getPaddingBottom() - gap * (ROWS.length - 1)) / ROWS.length;
        int index = 0;
        for (int r = 0; r < ROWS.length; r++) {
            String row = ROWS[r];
            float rowWidth = -gap;
            for (char c : row.toCharArray()) {
                rowWidth += widthOf(c, keyWidth) + gap;
            }
            float x = getPaddingLeft() + (avail - rowWidth) / 2;
            float y = getPaddingTop() + r * (rowHeight + gap);
            for (char c : row.toCharArray()) {
                float kw = widthOf(c, keyWidth);
                keys.get(index++).rect.set(x, y, x + kw, y + rowHeight);
                x += kw + gap;
            }
        }
        text.setTextSize(Math.min(keyWidth * 0.62f, rowHeight * 0.5f));
        buildIcons(rowHeight * 0.36f);
    }

    /** Elf Tasten und zehn Lücken füllen die Breite, die Lücke ist ein fester Bruchteil der Taste. */
    private float keyWidthFor(int width) {
        float avail = width - getPaddingLeft() - getPaddingRight();
        return avail / (COLUMNS + GAP_RATIO * (COLUMNS - 1));
    }

    private float widthOf(char c, float keyWidth) {
        float units = c == ENTER ? COLUMNS : c == DELETE || c == SHIFT ? WIDE : 1;
        return keyWidth * units + gap * (units - 1);
    }

    /** Symbole um (0,0) zentriert, {@code h} hoch. */
    private void buildIcons(float h) {
        float w = h * 1.4f;
        backspace.reset();
        backspace.moveTo(-w / 2, 0);
        backspace.lineTo(-w / 2 + h / 2, -h / 2);
        backspace.lineTo(w / 2, -h / 2);
        backspace.lineTo(w / 2, h / 2);
        backspace.lineTo(-w / 2 + h / 2, h / 2);
        backspace.close();
        float cx = w / 8, s = h / 5;
        backspace.moveTo(cx - s, -s);
        backspace.lineTo(cx + s, s);
        backspace.moveTo(cx + s, -s);
        backspace.lineTo(cx - s, s);

        // Pfeil nach oben mit Schaft, wie ⇧.
        float a = h * 0.55f, stem = h * 0.25f;
        shiftArrow.reset();
        shiftArrow.moveTo(0, -h / 2);
        shiftArrow.lineTo(a, 0);
        shiftArrow.lineTo(stem, 0);
        shiftArrow.lineTo(stem, h / 2);
        shiftArrow.lineTo(-stem, h / 2);
        shiftArrow.lineTo(-stem, 0);
        shiftArrow.lineTo(-a, 0);
        shiftArrow.close();

        check.reset();
        check.moveTo(-h * 0.5f, 0);
        check.lineTo(-h * 0.15f, h * 0.35f);
        check.lineTo(h * 0.55f, -h * 0.4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float textOffset = -(text.descent() + text.ascent()) / 2;
        for (Key key : keys) {
            float cx = key.rect.centerX(), cy = key.rect.centerY();
            int background = key.code == ENTER ? colorOk
                    : key.code == SHIFT && shift ? colorShiftActive : colorKey;
            fill.setColor(background);
            canvas.drawRoundRect(key.rect, corner, corner, fill);
            if (key.code != ENTER) {
                canvas.drawRoundRect(key.rect, corner, corner, stroke);
            }
            if (key == pressed) {
                fill.setColor(colorKeyPressed);
                canvas.drawRoundRect(key.rect, corner, corner, fill);
            }
            icon.setColor(key.code == ENTER || (key.code == SHIFT && shift) ? 0xFFFFFFFF : colorText);
            if (key.code == DELETE || key.code == SHIFT || key.code == ENTER) {
                canvas.save();
                canvas.translate(cx, cy);
                if (key.code == SHIFT) {
                    icon.setStyle(shift ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
                    canvas.drawPath(shiftArrow, icon);
                    icon.setStyle(Paint.Style.STROKE);
                } else if (key.code == ENTER) {
                    canvas.drawPath(check, icon);
                } else {
                    canvas.drawPath(backspace, icon);
                }
                canvas.restore();
            } else {
                text.setColor(colorText);
                canvas.drawText(label(key.code), cx, cy + textOffset, text);
            }
        }
    }

    /** Wie die Taste beschriftet ist und was sie schreibt: groß nur mit Umschalten; ß gibt es nur klein. */
    private String label(char code) {
        String letter = String.valueOf(code);
        return shift || code == 'ß' ? letter : letter.toLowerCase(Locale.GERMAN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                Key key = keyAt(event.getX(), event.getY());
                if (key != pressed) {
                    pressed = key;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                Key released = keyAt(event.getX(), event.getY());
                pressed = null;
                invalidate();
                if (released != null) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    dispatch(released.code);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = null;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void dispatch(char code) {
        if (code == SHIFT) {
            setShift(!shift);
            return;
        }
        if (listener == null) {
            return;
        }
        if (code == ENTER) {
            listener.onEnter();
        } else if (code == DELETE) {
            listener.onDelete();
        } else {
            char letter = label(code).charAt(0);
            setShift(false);
            listener.onLetter(letter);
        }
    }

    /** Die Taste unter dem Finger; Lücken zählen zur nächstgelegenen Taste derselben Zeile. */
    @Nullable
    private Key keyAt(float x, float y) {
        Key best = null;
        float bestDist = Float.MAX_VALUE;
        for (Key key : keys) {
            RectF r = key.rect;
            if (y < r.top - gap / 2 || y > r.bottom + gap / 2) {
                continue;
            }
            float dx = x < r.left ? r.left - x : x > r.right ? x - r.right : 0;
            if (dx < bestDist && dx <= gap) {
                bestDist = dx;
                best = key;
            }
        }
        return best;
    }
}

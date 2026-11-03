package de.marcel.zeitwaechter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Der große Ring auf „Heute“.
 * Er füllt sich vom Einstempeln bis zur spätesten Ausstempelzeit.
 * Farbe: rot = noch im Minus, grün = im Plus.
 * Die kleine Markierung zeigt, ab wo es ins Plus geht.
 */
public class RingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();

    private float progress = 0f;
    private float marker = -1f;
    private final float stroke;

    public RingView(Context c) {
        this(c, null);
    }

    public RingView(Context c, AttributeSet attrs) {
        super(c, attrs);
        float d = getResources().getDisplayMetrics().density;
        stroke = 16 * d;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(c.getColor(R.color.ring_track));
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(c.getColor(R.color.danger));
        markFill.setStyle(Paint.Style.FILL);
        markFill.setColor(c.getColor(R.color.card));
        markStroke.setStyle(Paint.Style.STROKE);
        markStroke.setStrokeWidth(3.5f * d);
        markStroke.setColor(c.getColor(R.color.ok));
    }

    /**
     * @param progress  0..1, wie weit der Ring gefüllt ist
     * @param color     Farbe des gefüllten Teils
     * @param marker    0..1 Position der Plus-Markierung, oder &lt; 0 für keine
     */
    public void set(float progress, int color, float marker) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        this.marker = marker;
        arc.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float pad = stroke / 2f + 4 * getResources().getDisplayMetrics().density;
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        box.set(left + pad, top + pad, left + size - pad, top + size - pad);

        canvas.drawArc(box, 0, 360, false, track);
        if (progress > 0.002f) {
            canvas.drawArc(box, -90, 360 * progress, false, arc);
        }
        if (marker >= 0 && marker <= 1) {
            double a = Math.toRadians(-90 + 360 * marker);
            float r = box.width() / 2f;
            float cx = box.centerX() + (float) (r * Math.cos(a));
            float cy = box.centerY() + (float) (r * Math.sin(a));
            float mr = stroke / 2f + 3 * getResources().getDisplayMetrics().density;
            canvas.drawCircle(cx, cy, mr, markFill);
            canvas.drawCircle(cx, cy, mr, markStroke);
        }
    }
}

package de.marcel.zeitwaechter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Farbiger Balken: Überstunden des Monats nach Zuschlagsart. */
public class SegmentBarView extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF r = new RectF();
    private final float d;
    private long[] values = new long[0];
    private int[] colors = new int[0];

    public SegmentBarView(Context c) {
        this(c, null);
    }

    public SegmentBarView(Context c, AttributeSet attrs) {
        super(c, attrs);
        d = getResources().getDisplayMetrics().density;
    }

    public void set(long[] values, int[] colors) {
        this.values = values;
        this.colors = colors;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (10 * d));
    }

    @Override
    protected void onDraw(Canvas c) {
        float h = getHeight();
        r.set(0, 0, getWidth(), h);
        clip.reset();
        clip.addRoundRect(r, h / 2f, h / 2f, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        long total = 0;
        for (long v : values) total += Math.max(0, v);
        if (total <= 0) {
            p.setColor(getContext().getColor(R.color.ring_track));
            c.drawRect(r, p);
        } else {
            float gap = 2 * d;
            float x = 0;
            float usable = getWidth();
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float w = usable * values[i] / (float) total;
                p.setColor(colors[i]);
                c.drawRect(x, 0, x + w, h, p);
                x += w;
                if (x < getWidth() - 1) { // Trennfuge, außer am Ende
                    p.setColor(getContext().getColor(R.color.card));
                    c.drawRect(x - gap / 2f, 0, x + gap / 2f, h, p);
                }
            }
        }
        c.restore();
    }
}

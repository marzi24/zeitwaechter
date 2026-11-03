package de.marcel.zeitwaechter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Balken Mo–So: Netto-Arbeitszeit der aktuellen Woche. */
public class WeekBarsView extends View {

    private static final String[] DAYS = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};

    private final Paint done = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint today = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint empty = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sollLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelToday = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float d;

    private long[] net = new long[7];
    private int todayIdx = -1;
    private long soll = 8 * Work.HOUR;

    public WeekBarsView(Context c) {
        this(c, null);
    }

    public WeekBarsView(Context c, AttributeSet attrs) {
        super(c, attrs);
        d = getResources().getDisplayMetrics().density;
        done.setColor(c.getColor(R.color.primary));
        today.setColor(c.getColor(R.color.primary));
        today.setAlpha(140);
        empty.setColor(c.getColor(R.color.primary_soft));
        sollLine.setColor(c.getColor(R.color.text_secondary));
        sollLine.setAlpha(90);
        sollLine.setStrokeWidth(1.2f * d);
        sollLine.setPathEffect(new android.graphics.DashPathEffect(new float[]{4 * d, 4 * d}, 0));
        label.setColor(c.getColor(R.color.text_secondary));
        label.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        labelToday.set(label);
        labelToday.setColor(c.getColor(R.color.primary));
    }

    /** @param net Netto je Tag Mo..So in ms, todayIdx 0..6 oder -1, soll Sollzeit Mo–Fr in ms */
    public void set(long[] net, int todayIdx, long soll) {
        this.net = net;
        this.todayIdx = todayIdx;
        this.soll = soll;
        invalidate();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), (int) (96 * d));
    }

    @Override
    protected void onDraw(Canvas c) {
        float labelH = 18 * d;
        float chartH = getHeight() - labelH - 4 * d;
        float slot = getWidth() / 7f;
        float bw = Math.min(30 * d, slot * 0.62f);
        long max = Math.max(10 * Work.HOUR, soll);
        for (long v : net) max = Math.max(max, v);

        if (soll > 0) {
            float y = chartH - chartH * soll / (float) max;
            c.drawLine(0, y, getWidth(), y, sollLine);
        }
        for (int i = 0; i < 7; i++) {
            float cx = slot * i + slot / 2f;
            float h = net[i] > 0 ? Math.max(6 * d, chartH * net[i] / (float) max) : 5 * d;
            r.set(cx - bw / 2f, chartH - h, cx + bw / 2f, chartH);
            Paint p = net[i] <= 0 ? empty : (i == todayIdx ? today : done);
            c.drawRoundRect(r, 8 * d, 8 * d, p);
            c.drawText(DAYS[i], cx, getHeight() - 4 * d, i == todayIdx ? labelToday : label);
        }
    }
}

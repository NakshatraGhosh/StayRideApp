package com.example.stayride.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    public static class Segment {
        public String label;
        public int value;
        public int color;

        public Segment(String label, int value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Segment> segments = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        centerPaint.setColor(0xFF16232B);
        centerPaint.setTextAlign(Paint.Align.CENTER);
        legendPaint.setColor(0xFF6B7A83);
    }

    public void setSegments(List<Segment> data) {
        segments.clear();
        segments.addAll(data);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (segments.isEmpty()) return;

        float density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        textPaint.setTextSize(13 * scaled);
        centerPaint.setTextSize(18 * scaled);
        legendPaint.setTextSize(12 * scaled);

        int total = 0;
        for (Segment s : segments) total += s.value;
        if (total == 0) return;

        float size = Math.min(getWidth(), getHeight());
        float donut = size * 0.78f;
        float left = (getWidth() - donut) / 2f;
        float top = 0;
        oval.set(left, top, left + donut, top + donut);

        float start = -90f;
        paint.setStyle(Paint.Style.FILL);
        for (Segment s : segments) {
            float sweep = 360f * s.value / total;
            paint.setColor(s.color);
            canvas.drawArc(oval, start, sweep, true, paint);
            start += sweep;
        }

        float hole = donut * 0.62f;
        float cx = getWidth() / 2f;
        float cy = donut / 2f;
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, cy, hole / 2f, paint);
        centerPaint.setColor(0xFF16232B);
        canvas.drawText(String.valueOf(total), cx, cy + 4 * density, centerPaint);

        float legendTop = donut + 14 * density;
        float x = 10 * density;
        for (Segment s : segments) {
            dotPaint.setColor(s.color);
            canvas.drawCircle(x + 6 * density, legendTop - 4 * density, 6 * density, dotPaint);
            String text = s.label + "  " + s.value;
            canvas.drawText(text, x + 18 * density, legendTop, legendPaint);
            legendTop += 22 * density;
        }
    }
}

package com.example.stayride.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.stayride.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarChartView extends View {

    public static class Bar {
        public String label;
        public int value;

        public Bar(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }

    private final List<Bar> bars = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaintAlt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private boolean compact = false;

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        barPaint.setColor(ContextCompat.getColor(context, R.color.brand_primary));
        barPaintAlt.setColor(ContextCompat.getColor(context, R.color.brand_accent));
        gridPaint.setColor(ContextCompat.getColor(context, R.color.divider));
        gridPaint.setStrokeWidth(2f);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
        invalidate();
    }

    public void setBars(List<Bar> data) {
        bars.clear();
        bars.addAll(data);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) return;

        float density = getResources().getDisplayMetrics().density;
        labelPaint.setTextSize(10 * density * getResources().getDisplayMetrics().scaledDensity / 16f);
        valuePaint.setTextSize(10 * density * getResources().getDisplayMetrics().scaledDensity / 16f);

        float labelSpace = 22 * density;
        float valueSpace = 16 * density;
        float padTop = valueSpace + 4 * density;
        float padBottom = labelSpace;
        float chartH = getHeight() - padTop - padBottom;
        if (chartH <= 0) return;

        int max = 1;
        for (Bar b : bars) max = Math.max(max, b.value);

        int lines = compact ? 3 : 4;
        for (int i = 0; i <= lines; i++) {
            float y = padTop + chartH * i / lines;
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }

        float slot = getWidth() / (float) bars.size();
        float barWidth = Math.min(slot * 0.55f, 44 * density);
        float radius = Math.min(barWidth / 2f, 8 * density);

        for (int i = 0; i < bars.size(); i++) {
            Bar b = bars.get(i);
            float cx = slot * i + slot / 2f;
            float h = chartH * b.value / (float) max;
            float top = padTop + chartH - h;
            rect.set(cx - barWidth / 2f, top, cx + barWidth / 2f, padTop + chartH);
            canvas.drawRoundRect(rect, radius, radius, (i % 2 == 0) ? barPaint : barPaintAlt);

            String value = b.value >= 1000
                    ? String.format(Locale.getDefault(), "%.0fK", b.value / 1000f)
                    : String.valueOf(b.value);
            canvas.drawText(value, cx, top - 5 * density, valuePaint);
            canvas.drawText(b.label, cx, getHeight() - 6 * density, labelPaint);
        }
    }
}

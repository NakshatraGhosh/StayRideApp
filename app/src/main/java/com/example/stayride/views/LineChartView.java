package com.example.stayride.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.stayride.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LineChartView extends View {

    public static class Point {
        public String label;
        public int value;

        public Point(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }

    private final List<Point> points = new ArrayList<>();
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint areaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint peakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path areaPath = new Path();

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.divider));
        gridPaint.setStrokeWidth(2f);
        linePaint.setColor(ContextCompat.getColor(context, R.color.brand_primary));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        dotPaint.setColor(ContextCompat.getColor(context, R.color.brand_primary_dark));
        dotPaint.setStyle(Paint.Style.FILL);
        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setColor(ContextCompat.getColor(context, R.color.brand_primary_dark));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
        peakPaint.setColor(ContextCompat.getColor(context, R.color.brand_primary_dark));
        peakPaint.setTextAlign(Paint.Align.CENTER);
        peakPaint.setFakeBoldText(true);
    }

    public void setPoints(List<Point> data) {
        points.clear();
        points.addAll(data);
        invalidate();
    }

    public static String compact(int value) {
        if (Math.abs(value) >= 10000000) return String.format(Locale.getDefault(), "%.1fCr", value / 10000000f);
        if (Math.abs(value) >= 100000) return String.format(Locale.getDefault(), "%.1fL", value / 100000f);
        if (Math.abs(value) >= 1000) return String.format(Locale.getDefault(), "%.1fK", value / 1000f);
        return String.valueOf(value);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points.isEmpty()) return;

        float density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        labelPaint.setTextSize(10 * scaled);
        valuePaint.setTextSize(11 * scaled);
        peakPaint.setTextSize(11 * scaled);

        float labelSpace = 20 * density;
        float padTop = 24 * density;
        float padBottom = labelSpace;
        float padLeft = 4 * density;
        float padRight = 4 * density;
        float chartW = getWidth() - padLeft - padRight;
        float chartH = getHeight() - padTop - padBottom;
        if (chartH <= 0 || chartW <= 0) return;

        int max = 1;
        int peakIndex = 0;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).value > max) max = points.get(i).value;
            if (points.get(i).value > points.get(peakIndex).value) peakIndex = i;
        }
        boolean hasData = points.get(peakIndex).value > 0;

        int lines = 3;
        for (int i = 0; i <= lines; i++) {
            float y = padTop + chartH * i / lines;
            canvas.drawLine(padLeft, y, getWidth() - padRight, y, gridPaint);
        }

        int n = points.size();
        float stepX = n == 1 ? chartW : chartW / (n - 1f);
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padLeft + (n == 1 ? chartW / 2f : stepX * i);
            ys[i] = padTop + chartH - (chartH * points.get(i).value / (float) max);
        }

        int lineColor = ContextCompat.getColor(getContext(), R.color.brand_primary);
        LinearGradient gradient = new LinearGradient(0, padTop, 0, padTop + chartH,
                (lineColor & 0x00FFFFFF) | 0x66000000, 0x00000000, Shader.TileMode.CLAMP);
        areaPaint.setShader(gradient);
        areaPaint.setStyle(Paint.Style.FILL);

        linePath.reset();
        areaPath.reset();
        linePath.moveTo(xs[0], ys[0]);
        areaPath.moveTo(xs[0], padTop + chartH);
        areaPath.lineTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            linePath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
            areaPath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }
        areaPath.lineTo(xs[n - 1], padTop + chartH);
        areaPath.close();
        canvas.drawPath(areaPath, areaPaint);
        canvas.drawPath(linePath, linePaint);

        float dotRadius = 4f * density;
        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], dotRadius, dotPaint);
        }

        if (hasData && n <= 12) {
            float textY = Math.max(ys[peakIndex] - 10 * density, padTop + 10 * density);
            canvas.drawText(compact(points.get(peakIndex).value), xs[peakIndex], textY, peakPaint);
        }

        float labelY = getHeight() - 5 * density;
        int skip = n > 8 ? 2 : 1;
        for (int i = 0; i < n; i++) {
            if (i % skip != 0 && i != n - 1) continue;
            canvas.drawText(points.get(i).label, xs[i], labelY, labelPaint);
        }
    }
}

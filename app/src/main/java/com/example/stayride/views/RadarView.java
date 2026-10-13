package com.example.stayride.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.example.stayride.R;
import com.example.stayride.util.Ui;

/**
 * Animated "radio waves" radar: concentric rings expanding outwards from a
 * central location marker. Used on the Find-a-Driver searching screen.
 */
public class RadarView extends View {

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;
    private ValueAnimator animator;

    public RadarView(Context c) {
        super(c);
        init();
    }

    public RadarView(Context c, @Nullable AttributeSet a) {
        super(c, a);
        init();
    }

    public RadarView(Context c, @Nullable AttributeSet a, int d) {
        super(c, a, d);
        init();
    }

    private void init() {
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(dp(2.5f));
        ring.setColor(Ui.color(this, R.color.brand_primary));

        dot.setStyle(Paint.Style.FILL);
        dot.setColor(Ui.color(this, R.color.brand_primary_dark));

        halo.setStyle(Paint.Style.FILL);
        halo.setColor(Ui.color(this, R.color.brand_primary_light));

        tick.setStyle(Paint.Style.FILL);
        tick.setColor(Ui.color(this, R.color.brand_primary));
        tick.setAlpha(120);
    }

    public void start() {
        if (animator != null) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void stop() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float maxR = Math.min(w, h) / 2f - dp(6f);

        canvas.drawCircle(cx, cy, maxR, halo);

        final int rings = 3;
        for (int i = 0; i < rings; i++) {
            float p = (phase + (float) i / rings) % 1f;
            float r = dp(8f) + p * maxR;
            ring.setAlpha((int) (200 * (1f - p)));
            canvas.drawCircle(cx, cy, r, ring);
        }

        canvas.drawCircle(cx, cy, dp(9f), tick);
        canvas.drawCircle(cx, cy, dp(6f), dot);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}

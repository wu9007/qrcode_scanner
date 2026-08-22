package com.shinow.qrscan;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class ScanOverlayView extends View {

    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint hintPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF hole = new RectF();
    private ValueAnimator animator;
    private float lineT = 0f;
    private String hint;

    public ScanOverlayView(Context context) {
        super(context);
        init();
    }

    public ScanOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        maskPaint.setColor(0x99000000);
        cornerPaint.setColor(0xFF12C4FF);
        cornerPaint.setStrokeWidth(dp(4));
        cornerPaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(0xFF12C4FF);
        linePaint.setStrokeWidth(dp(2));
        hintPaint.setColor(0xFFFFFFFF);
        hintPaint.setTextAlign(Paint.Align.CENTER);
        hintPaint.setTextSize(dp(14));
    }

    void setStyle(@Nullable Integer color, @Nullable String hint) {
        if (color != null) {
            cornerPaint.setColor(color);
            linePaint.setColor(color);
        }
        this.hint = (hint == null || hint.trim().isEmpty()) ? null : hint.trim();
        invalidate();
    }

    void start() {
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            lineT = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    void stop() {
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float topBar = dp(56);
        float bottomReserve = dp(128);
        float availableH = Math.max(dp(120), h - topBar - bottomReserve);
        float size = Math.min(w * 0.72f, availableH * 0.92f);
        float left = (w - size) / 2f;
        float top = topBar + Math.max(0f, (availableH - size) / 2f);
        hole.set(left, top, left + size, top + size);

        canvas.drawRect(0, 0, w, hole.top, maskPaint);
        canvas.drawRect(0, hole.bottom, w, h, maskPaint);
        canvas.drawRect(0, hole.top, hole.left, hole.bottom, maskPaint);
        canvas.drawRect(hole.right, hole.top, w, hole.bottom, maskPaint);

        float len = dp(22);
        float t = hole.top;
        float b = hole.bottom;
        float l = hole.left;
        float r = hole.right;
        canvas.drawLine(l, t, l + len, t, cornerPaint);
        canvas.drawLine(l, t, l, t + len, cornerPaint);
        canvas.drawLine(r, t, r - len, t, cornerPaint);
        canvas.drawLine(r, t, r, t + len, cornerPaint);
        canvas.drawLine(l, b, l + len, b, cornerPaint);
        canvas.drawLine(l, b, l, b - len, cornerPaint);
        canvas.drawLine(r, b, r - len, b, cornerPaint);
        canvas.drawLine(r, b, r, b - len, cornerPaint);

        float y = hole.top + hole.height() * lineT;
        linePaint.setAlpha(200);
        canvas.drawLine(hole.left + dp(8), y, hole.right - dp(8), y, linePaint);

        if (hint != null) {
            float textY = Math.min(h - dp(140), hole.bottom + dp(28));
            canvas.drawText(hint, w / 2f, textY, hintPaint);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}

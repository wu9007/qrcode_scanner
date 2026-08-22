package com.shinow.qrscan;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class ScanOverlayView extends View {

    private static final int DEFAULT_ACCENT = 0xFF12C4FF;

    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint hintPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF hole = new RectF();
    private ValueAnimator animator;
    private float lineT = 0f;
    private String hint;
    private String title;
    private int accent = DEFAULT_ACCENT;

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
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeCap(Paint.Cap.SQUARE);
        cornerPaint.setStrokeJoin(Paint.Join.MITER);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        hintPaint.setColor(0xE6FFFFFF);
        hintPaint.setTextAlign(Paint.Align.LEFT);
        hintPaint.setTextSize(dp(14));
        titlePaint.setColor(0xFFFFFFFF);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(dp(18));
        titlePaint.setFakeBoldText(true);
        applyAccent(DEFAULT_ACCENT);
    }

    void setStyle(@Nullable Integer color, @Nullable String hint, @Nullable String title) {
        applyAccent(color == null ? DEFAULT_ACCENT : color);
        this.hint = (hint == null || hint.trim().isEmpty()) ? null : hint.trim();
        this.title = (title == null || title.trim().isEmpty()) ? null : title.trim();
        invalidate();
    }

    private void applyAccent(int color) {
        accent = color | 0xFF000000;
        cornerPaint.setColor(accent);
        cornerPaint.setStrokeWidth(dp(5));
        glowPaint.setStrokeWidth(dp(10));
        linePaint.setStrokeWidth(dp(2.5f));
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

        if (title != null) {
            canvas.drawText(title, w / 2f, Math.max(dp(36), hole.top - dp(20)), titlePaint);
        }

        float len = dp(24);
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
        float x0 = hole.left + dp(10);
        float x1 = hole.right - dp(10);
        int rgb = accent & 0x00FFFFFF;
        glowPaint.setShader(new LinearGradient(x0, y, x1, y,
                new int[]{0x00000000, (0x66 << 24) | rgb, 0x00000000},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawLine(x0, y, x1, y, glowPaint);
        linePaint.setShader(new LinearGradient(x0, y, x1, y,
                new int[]{0x00000000, accent, 0x00000000},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawLine(x0, y, x1, y, linePaint);

        if (hint != null) {
            int width = Math.max(1, (int) hole.width());
            StaticLayout layout = new StaticLayout(
                    hint, hintPaint, width,
                    Layout.Alignment.ALIGN_CENTER, 1.15f, 0, false);
            canvas.save();
            canvas.translate(hole.centerX() - width / 2f, hole.bottom + dp(18));
            layout.draw(canvas);
            canvas.restore();
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}

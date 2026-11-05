package org.futo.inputmethod.keyboard.internal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;

import org.futo.inputmethod.keyboard.PointerTracker;

import java.util.ArrayList;
import java.util.Random;

import javax.annotation.Nonnull;

/**
 * Draws a small, colorful firework burst whenever a key is pressed.
 * Coordinates are in the coordinate system of the MainKeyboardView.
 */
public final class FireworksDrawingPreview extends AbstractDrawingPreview {
    private static final int PARTICLES_PER_BURST = 28;
    private static final int MAX_PARTICLES = 600;
    private static final long PARTICLE_LIFETIME_MS = 750;

    private static final class Particle {
        float x, y;
        float prevX, prevY;
        float vx, vy; // px per ms
        int color;
        float radius;
        long birth;
        long lifetime;
    }

    private final ArrayList<Particle> mParticles = new ArrayList<>();
    private final Random mRandom = new Random();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float mDensity;
    private long mLastFrameTime = 0;

    public FireworksDrawingPreview(final float density) {
        mDensity = density;
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * Spawns a firework burst at the given position.
     * @param hue base hue (0..360) of the burst
     */
    public void burst(final float x, final float y, final float hue) {
        if (!isPreviewEnabled()) return;

        final long now = SystemClock.uptimeMillis();
        if (mParticles.isEmpty()) mLastFrameTime = now;

        final float[] hsv = new float[3];
        for (int i = 0; i < PARTICLES_PER_BURST; i++) {
            if (mParticles.size() >= MAX_PARTICLES) mParticles.remove(0);

            final Particle p = new Particle();
            final double angle = (Math.PI * 2.0 * i) / PARTICLES_PER_BURST
                    + (mRandom.nextFloat() - 0.5f) * 0.4f;
            final float speed = (0.12f + mRandom.nextFloat() * 0.22f) * mDensity;

            p.x = p.prevX = x;
            p.y = p.prevY = y;
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed - 0.08f * mDensity;
            p.radius = (1.5f + mRandom.nextFloat() * 1.8f) * mDensity;
            p.birth = now;
            p.lifetime = (long) (PARTICLE_LIFETIME_MS * (0.6f + mRandom.nextFloat() * 0.5f));

            if (mRandom.nextFloat() < 0.15f) {
                // Some white/golden sparkles
                p.color = mRandom.nextBoolean() ? Color.WHITE : Color.rgb(255, 230, 120);
            } else {
                hsv[0] = (hue + (mRandom.nextFloat() - 0.5f) * 120f + 360f) % 360f;
                hsv[1] = 0.75f + mRandom.nextFloat() * 0.25f;
                hsv[2] = 1.0f;
                p.color = Color.HSVToColor(hsv);
            }

            mParticles.add(p);
        }

        invalidateDrawingView();
    }

    @Override
    public void onDeallocateMemory() {
        mParticles.clear();
    }

    @Override
    public void drawPreview(@Nonnull final Canvas canvas) {
        if (mParticles.isEmpty()) return;

        final long now = SystemClock.uptimeMillis();
        final float dt = Math.min(Math.max(now - mLastFrameTime, 0), 50);
        mLastFrameTime = now;

        final float gravity = 0.0006f * mDensity; // px per ms^2
        final float drag = (float) Math.pow(0.996, dt);

        for (int i = mParticles.size() - 1; i >= 0; i--) {
            final Particle p = mParticles.get(i);
            final long age = now - p.birth;
            if (age >= p.lifetime) {
                mParticles.remove(i);
                continue;
            }

            p.prevX = p.x;
            p.prevY = p.y;
            p.vx *= drag;
            p.vy = p.vy * drag + gravity * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;

            final float life = 1.0f - (float) age / p.lifetime;
            // Twinkle a little near the end
            final float twinkle = life < 0.35f && mRandom.nextFloat() < 0.3f ? 0.4f : 1.0f;
            final int alpha = (int) (255 * life * twinkle);

            mPaint.setColor(p.color);
            mPaint.setAlpha(alpha);

            // Streak from previous position
            mPaint.setStrokeWidth(p.radius * life * 1.2f);
            canvas.drawLine(p.prevX - p.vx * 18f, p.prevY - p.vy * 18f, p.x, p.y, mPaint);
            canvas.drawCircle(p.x, p.y, p.radius * (0.4f + 0.6f * life), mPaint);
        }

        if (!mParticles.isEmpty()) {
            invalidateDrawingView();
        }
    }

    @Override
    public void setPreviewPosition(@Nonnull final PointerTracker tracker) {
        // Bursts are triggered explicitly via burst()
    }
}

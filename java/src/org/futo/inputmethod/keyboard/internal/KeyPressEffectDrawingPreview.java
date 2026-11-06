package org.futo.inputmethod.keyboard.internal;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;

import org.futo.inputmethod.keyboard.PointerTracker;

import java.util.ArrayList;
import java.util.Random;

import javax.annotation.Nonnull;

/**
 * Draws a small animated effect (fireworks, confetti, hearts, ...) whenever a key is pressed.
 * Coordinates are in the coordinate system of the MainKeyboardView.
 */
public final class KeyPressEffectDrawingPreview extends AbstractDrawingPreview {
    // Effect ids, stored in the preferences. Do not change the values.
    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_FIREWORKS = 1;
    public static final int EFFECT_CONFETTI = 2;
    public static final int EFFECT_HEARTS = 3;
    public static final int EFFECT_STARS = 4;
    public static final int EFFECT_BUBBLES = 5;
    public static final int EFFECT_SNOW = 6;
    public static final int EFFECT_RIPPLE = 7;
    public static final int EFFECT_RANDOM = 8;

    private static final int[] RANDOM_CHOICES = {
            EFFECT_FIREWORKS, EFFECT_CONFETTI, EFFECT_HEARTS, EFFECT_STARS,
            EFFECT_BUBBLES, EFFECT_SNOW, EFFECT_RIPPLE
    };

    private static final int MAX_PARTICLES = 600;

    private static final int SHAPE_SPARK = 0;
    private static final int SHAPE_CONFETTI = 1;
    private static final int SHAPE_HEART = 2;
    private static final int SHAPE_STAR = 3;
    private static final int SHAPE_BUBBLE = 4;
    private static final int SHAPE_SNOWFLAKE = 5;
    private static final int SHAPE_RING = 6;

    private static final class Particle {
        int shape;
        float x, y;
        float vx, vy; // px per ms
        float gravity; // px per ms^2
        float drag; // velocity multiplier per ms
        float rotation, rotationSpeed; // degrees, degrees per ms
        float size;
        float wobble, wobbleSpeed; // sideways sway for floating shapes
        int color;
        long birth;
        long lifetime;
    }

    private final ArrayList<Particle> mParticles = new ArrayList<>();
    private final Random mRandom = new Random();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mHeartPath = new Path();
    private final Path mStarPath = new Path();
    private final float[] mHsv = new float[3];
    private final float mDensity;
    private long mLastFrameTime = 0;
    private int mEffect = EFFECT_NONE;

    public KeyPressEffectDrawingPreview(final float density) {
        mDensity = density;
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        buildPaths();
    }

    private void buildPaths() {
        // Unit heart, roughly 2x2 centered on the origin
        mHeartPath.reset();
        mHeartPath.moveTo(0f, 0.9f);
        mHeartPath.cubicTo(-1.4f, -0.1f, -0.9f, -1.3f, 0f, -0.45f);
        mHeartPath.cubicTo(0.9f, -1.3f, 1.4f, -0.1f, 0f, 0.9f);
        mHeartPath.close();

        // Unit five pointed star
        mStarPath.reset();
        for (int i = 0; i < 10; i++) {
            final double angle = -Math.PI / 2.0 + i * Math.PI / 5.0;
            final float r = (i % 2 == 0) ? 1.0f : 0.45f;
            final float px = (float) Math.cos(angle) * r;
            final float py = (float) Math.sin(angle) * r;
            if (i == 0) mStarPath.moveTo(px, py); else mStarPath.lineTo(px, py);
        }
        mStarPath.close();
    }

    public void setEffect(final int effect) {
        mEffect = effect;
        setPreviewEnabled(effect != EFFECT_NONE);
        if (effect == EFFECT_NONE) mParticles.clear();
    }

    public int getEffect() {
        return mEffect;
    }

    private float rnd(final float min, final float max) {
        return min + mRandom.nextFloat() * (max - min);
    }

    private int hsv(final float hue, final float saturation, final float value) {
        mHsv[0] = ((hue % 360f) + 360f) % 360f;
        mHsv[1] = saturation;
        mHsv[2] = value;
        return Color.HSVToColor(mHsv);
    }

    private Particle newParticle(final int shape, final float x, final float y, final long now) {
        if (mParticles.size() >= MAX_PARTICLES) mParticles.remove(0);
        final Particle p = new Particle();
        p.shape = shape;
        p.x = x;
        p.y = y;
        p.birth = now;
        p.drag = 1.0f;
        mParticles.add(p);
        return p;
    }

    /**
     * Spawns the current effect at the given position.
     * @param hue base hue (0..360) of the effect
     */
    public void burst(final float x, final float y, final float hue) {
        if (!isPreviewEnabled() || mEffect == EFFECT_NONE) return;

        final long now = SystemClock.uptimeMillis();
        if (mParticles.isEmpty()) mLastFrameTime = now;

        int effect = mEffect;
        if (effect == EFFECT_RANDOM) {
            effect = RANDOM_CHOICES[mRandom.nextInt(RANDOM_CHOICES.length)];
        }

        final float d = mDensity;
        switch (effect) {
            case EFFECT_FIREWORKS: {
                final int count = 28;
                for (int i = 0; i < count; i++) {
                    final Particle p = newParticle(SHAPE_SPARK, x, y, now);
                    final double angle = (Math.PI * 2.0 * i) / count + rnd(-0.2f, 0.2f);
                    final float speed = rnd(0.12f, 0.34f) * d;
                    p.vx = (float) Math.cos(angle) * speed;
                    p.vy = (float) Math.sin(angle) * speed - 0.08f * d;
                    p.gravity = 0.0006f * d;
                    p.drag = 0.996f;
                    p.size = rnd(1.5f, 3.3f) * d;
                    p.lifetime = (long) (750 * rnd(0.6f, 1.1f));
                    if (mRandom.nextFloat() < 0.15f) {
                        p.color = mRandom.nextBoolean() ? Color.WHITE : Color.rgb(255, 230, 120);
                    } else {
                        p.color = hsv(hue + rnd(-60f, 60f), rnd(0.75f, 1f), 1f);
                    }
                }
                break;
            }
            case EFFECT_CONFETTI: {
                for (int i = 0; i < 22; i++) {
                    final Particle p = newParticle(SHAPE_CONFETTI, x, y, now);
                    final double angle = -Math.PI / 2.0 + rnd(-1.1f, 1.1f);
                    final float speed = rnd(0.2f, 0.42f) * d;
                    p.vx = (float) Math.cos(angle) * speed;
                    p.vy = (float) Math.sin(angle) * speed;
                    p.gravity = 0.0011f * d;
                    p.drag = 0.994f;
                    p.rotation = rnd(0f, 360f);
                    p.rotationSpeed = rnd(-0.6f, 0.6f);
                    p.wobble = rnd(0f, 6.28f);
                    p.wobbleSpeed = rnd(0.01f, 0.025f);
                    p.size = rnd(3f, 5f) * d;
                    p.lifetime = (long) rnd(900f, 1300f);
                    p.color = hsv(rnd(0f, 360f), rnd(0.7f, 0.95f), 1f);
                }
                break;
            }
            case EFFECT_HEARTS: {
                final int count = 4 + mRandom.nextInt(3);
                for (int i = 0; i < count; i++) {
                    final Particle p = newParticle(SHAPE_HEART, x + rnd(-12f, 12f) * d, y, now);
                    p.vx = rnd(-0.03f, 0.03f) * d;
                    p.vy = -rnd(0.06f, 0.11f) * d;
                    p.wobble = rnd(0f, 6.28f);
                    p.wobbleSpeed = rnd(0.006f, 0.012f);
                    p.rotation = rnd(-15f, 15f);
                    p.size = rnd(5f, 9f) * d;
                    p.lifetime = (long) rnd(1000f, 1400f);
                    p.color = hsv(rnd(330f, 375f), rnd(0.55f, 0.9f), 1f);
                }
                break;
            }
            case EFFECT_STARS: {
                final int count = 9;
                for (int i = 0; i < count; i++) {
                    final Particle p = newParticle(SHAPE_STAR, x, y, now);
                    final double angle = (Math.PI * 2.0 * i) / count + rnd(-0.3f, 0.3f);
                    final float speed = rnd(0.07f, 0.16f) * d;
                    p.vx = (float) Math.cos(angle) * speed;
                    p.vy = (float) Math.sin(angle) * speed;
                    p.drag = 0.995f;
                    p.rotation = rnd(0f, 72f);
                    p.rotationSpeed = rnd(-0.4f, 0.4f);
                    p.size = rnd(4f, 7f) * d;
                    p.lifetime = (long) rnd(700f, 1000f);
                    p.color = mRandom.nextFloat() < 0.6f
                            ? hsv(rnd(42f, 55f), rnd(0.5f, 0.9f), 1f)
                            : hsv(hue + rnd(-30f, 30f), 0.6f, 1f);
                }
                break;
            }
            case EFFECT_BUBBLES: {
                final int count = 5 + mRandom.nextInt(3);
                for (int i = 0; i < count; i++) {
                    final Particle p = newParticle(SHAPE_BUBBLE, x + rnd(-14f, 14f) * d, y, now);
                    p.vx = rnd(-0.02f, 0.02f) * d;
                    p.vy = -rnd(0.04f, 0.09f) * d;
                    p.wobble = rnd(0f, 6.28f);
                    p.wobbleSpeed = rnd(0.005f, 0.01f);
                    p.size = rnd(4f, 9f) * d;
                    p.lifetime = (long) rnd(1100f, 1600f);
                    p.color = hsv(hue + rnd(-80f, 80f), rnd(0.25f, 0.5f), 1f);
                }
                break;
            }
            case EFFECT_SNOW: {
                for (int i = 0; i < 10; i++) {
                    final Particle p = newParticle(SHAPE_SNOWFLAKE,
                            x + rnd(-22f, 22f) * d, y + rnd(-14f, 4f) * d, now);
                    p.vx = rnd(-0.015f, 0.015f) * d;
                    p.vy = rnd(0.025f, 0.06f) * d;
                    p.wobble = rnd(0f, 6.28f);
                    p.wobbleSpeed = rnd(0.004f, 0.009f);
                    p.rotation = rnd(0f, 60f);
                    p.rotationSpeed = rnd(-0.08f, 0.08f);
                    p.size = rnd(2.5f, 5f) * d;
                    p.lifetime = (long) rnd(1300f, 1900f);
                    p.color = mRandom.nextFloat() < 0.7f ? Color.WHITE : Color.rgb(190, 225, 255);
                }
                break;
            }
            case EFFECT_RIPPLE: {
                for (int i = 0; i < 2; i++) {
                    // The second ring starts a little later by being born "in the future"
                    final Particle p = newParticle(SHAPE_RING, x, y, now + i * 120L);
                    p.size = rnd(34f, 44f) * d; // final radius
                    p.lifetime = 600;
                    p.color = hsv(hue + i * 40f, 0.8f, 1f);
                }
                break;
            }
            default:
                break;
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

        for (int i = mParticles.size() - 1; i >= 0; i--) {
            final Particle p = mParticles.get(i);
            final long age = now - p.birth;
            if (age < 0) continue; // not started yet
            if (age >= p.lifetime) {
                mParticles.remove(i);
                continue;
            }

            final float prevX = p.x;
            final float prevY = p.y;
            if (p.drag != 1.0f) {
                final float drag = (float) Math.pow(p.drag, dt);
                p.vx *= drag;
                p.vy *= drag;
            }
            p.vy += p.gravity * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.rotation += p.rotationSpeed * dt;
            p.wobble += p.wobbleSpeed * dt;

            final float progress = (float) age / p.lifetime;
            final float life = 1.0f - progress;
            drawParticle(canvas, p, progress, life, prevX, prevY);
        }

        if (!mParticles.isEmpty()) {
            invalidateDrawingView();
        }
    }

    private void drawParticle(final Canvas canvas, final Particle p, final float progress,
                              final float life, final float prevX, final float prevY) {
        final float d = mDensity;
        mPaint.setColor(p.color);

        switch (p.shape) {
            case SHAPE_SPARK: {
                final float twinkle = life < 0.35f && mRandom.nextFloat() < 0.3f ? 0.4f : 1.0f;
                mPaint.setAlpha((int) (255 * life * twinkle));
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(p.size * life * 1.2f);
                canvas.drawLine(prevX - p.vx * 18f, prevY - p.vy * 18f, p.x, p.y, mPaint);
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(p.x, p.y, p.size * (0.4f + 0.6f * life), mPaint);
                break;
            }
            case SHAPE_CONFETTI: {
                mPaint.setAlpha((int) (255 * Math.min(1f, life * 2.5f)));
                mPaint.setStyle(Paint.Style.FILL);
                // Fluttering: the piece appears to flip around its axis
                final float flip = (float) Math.cos(p.wobble * 3.0f);
                final float w = p.size * Math.max(0.15f, Math.abs(flip));
                final float h = p.size * 0.55f;
                canvas.save();
                canvas.translate(p.x + (float) Math.sin(p.wobble) * 3f * d, p.y);
                canvas.rotate(p.rotation);
                canvas.drawRect(-w / 2f, -h / 2f, w / 2f, h / 2f, mPaint);
                canvas.restore();
                break;
            }
            case SHAPE_HEART: {
                // Pop in, then float up and fade out
                final float grow = Math.min(1f, progress * 6f);
                final float scale = p.size * (0.4f + 0.6f * grow);
                mPaint.setAlpha((int) (255 * Math.min(1f, life * 1.8f)));
                mPaint.setStyle(Paint.Style.FILL);
                canvas.save();
                canvas.translate(p.x + (float) Math.sin(p.wobble) * 6f * d, p.y);
                canvas.rotate(p.rotation + (float) Math.sin(p.wobble) * 10f);
                canvas.scale(scale, scale);
                canvas.drawPath(mHeartPath, mPaint);
                canvas.restore();
                break;
            }
            case SHAPE_STAR: {
                final float twinkle = 0.65f + 0.35f * (float) Math.sin(progress * 25f + p.rotation);
                final float scale = p.size * (0.5f + 0.5f * (float) Math.sin(Math.PI * Math.min(1f, progress * 1.3f)));
                mPaint.setAlpha((int) (255 * life * twinkle));
                mPaint.setStyle(Paint.Style.FILL);
                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate(p.rotation);
                canvas.scale(scale, scale);
                canvas.drawPath(mStarPath, mPaint);
                canvas.restore();
                break;
            }
            case SHAPE_BUBBLE: {
                // Bubbles pop at the end: they expand quickly and vanish
                final boolean popping = progress > 0.88f;
                final float popProgress = popping ? (progress - 0.88f) / 0.12f : 0f;
                final float radius = p.size * (1f + popProgress * 0.6f) * Math.min(1f, 0.3f + progress * 5f);
                final float cx = p.x + (float) Math.sin(p.wobble) * 5f * d;
                final int alpha = (int) (230 * (popping ? 1f - popProgress : 1f));

                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setAlpha(alpha / 5);
                canvas.drawCircle(cx, p.y, radius, mPaint);

                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(1.2f * d);
                mPaint.setAlpha(alpha);
                canvas.drawCircle(cx, p.y, radius, mPaint);

                // Highlight
                mPaint.setColor(Color.WHITE);
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setAlpha((int) (alpha * 0.8f));
                canvas.drawCircle(cx - radius * 0.35f, p.y - radius * 0.35f, radius * 0.18f, mPaint);
                break;
            }
            case SHAPE_SNOWFLAKE: {
                mPaint.setAlpha((int) (255 * Math.min(1f, Math.min(progress * 6f, life * 2f))));
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(Math.max(1f, p.size * 0.22f));
                canvas.save();
                canvas.translate(p.x + (float) Math.sin(p.wobble) * 8f * d, p.y);
                canvas.rotate(p.rotation);
                for (int arm = 0; arm < 3; arm++) {
                    canvas.drawLine(-p.size, 0, p.size, 0, mPaint);
                    canvas.rotate(60f);
                }
                canvas.restore();
                break;
            }
            case SHAPE_RING: {
                // Ease out: fast at first, then slowing down
                final float eased = 1f - (1f - progress) * (1f - progress);
                mPaint.setAlpha((int) (220 * life));
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(Math.max(1f, 4f * d * life));
                canvas.drawCircle(p.x, p.y, p.size * eased, mPaint);
                break;
            }
            default:
                break;
        }
    }

    @Override
    public void setPreviewPosition(@Nonnull final PointerTracker tracker) {
        // Effects are triggered explicitly via burst()
    }
}

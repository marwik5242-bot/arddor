package com.thirteenag.menustudio;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.WeakHashMap;

/**
 * All decorative drawing. Runs in Fabric's afterRender event, so it never touches
 * vanilla widgets, never changes renderer state and only uses DrawContext.fill.
 * Decorations stay out of the central button column so buttons are never covered.
 */
public final class Atmosphere {
    private static final Text TAG = Text.translatable("ag13.tag");

    // {side (0 = left, 1 = right), y fraction, cycle seconds, phase}
    private static final float[][] EYE_PAIRS = {
            {0, 0.30f, 13f, 0.05f},
            {1, 0.42f, 16f, 0.45f},
            {0, 0.70f, 19f, 0.75f},
            {1, 0.78f, 11f, 0.25f}
    };

    private static DrawContext lastCtx;
    private static Screen lastScreen;
    private static long lastNanos;
    private static long openedNanos;
    private static double time;
    private static float dt;
    private static float fade = 1f;

    private static int forestW = -1;
    private static int forestH = -1;
    private static int[] treeH = new int[0];

    private static final WeakHashMap<ClickableWidget, Float> HOVER = new WeakHashMap<>();

    private Atmosphere() {
    }

    public static void renderTitle(Screen screen, DrawContext ctx, int mx, int my) {
        MenuStudioConfig c = MenuStudioConfig.get();
        if (!beginFrame(screen, ctx, c)) return;
        int w = screen.width;
        int h = screen.height;
        boolean low = c.lowPerformanceMode;

        if (c.darkOverlay || c.fogIntensity > 0.01f) {
            vignette(ctx, w, h, c, 1f);
        }
        if (c.forestSilhouette && !low) {
            forest(ctx, w, h);
        }
        if (c.animatedStars && !low) {
            stars(ctx, w, h);
        }
        if (c.animatedEyes) {
            eyes(ctx, w, h, c, low);
        }
        if (c.grainEffect && !low) {
            grain(ctx, w, h, c);
        }
        if (c.creatorTag) {
            tag(ctx, w, c);
        }
        glow(screen, ctx, mx, my, c);
    }

    public static void renderOther(Screen screen, DrawContext ctx, int mx, int my) {
        MenuStudioConfig c = MenuStudioConfig.get();
        if (!beginFrame(screen, ctx, c)) return;
        if (c.screenTint) {
            vignette(ctx, screen.width, screen.height, c, 0.6f);
        }
        glow(screen, ctx, mx, my, c);
    }

    // ---------------------------------------------------------------- frame state

    private static boolean beginFrame(Screen screen, DrawContext ctx, MenuStudioConfig c) {
        if (ctx == lastCtx) return false; // already drawn this frame (duplicate listener)
        lastCtx = ctx;
        long now = System.nanoTime();
        if (lastNanos == 0L) lastNanos = now;
        dt = Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        if (screen != lastScreen) {
            lastScreen = screen;
            openedNanos = now;
        }
        time += dt * c.animationSpeed;
        if (c.smoothTransitions && c.transitionSeconds > 0.01f) {
            float t = Math.min(1f, (now - openedNanos) / 1.0e9f / c.transitionSeconds);
            fade = t * t * (3f - 2f * t);
        } else {
            fade = 1f;
        }
        return true;
    }

    // ---------------------------------------------------------------- helpers

    private static int argb(int rgb, float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static boolean visible(float alpha) {
        return alpha >= 0.008f;
    }

    static int accentRgb(String accent) {
        switch (accent) {
            case "pink": return 0xE05C9A;
            case "white": return 0xE8E8E8;
            case "green": return 0x3FB56A;
            default: return 0xC0242E;
        }
    }

    private static int themeRgb(String theme) {
        switch (theme) {
            case "blood": return 0x1C0000;
            case "ash": return 0x101012;
            default: return 0x000000;
        }
    }

    private static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    // ---------------------------------------------------------------- layers

    /** Soft dark edges (left/right) and top; the centre column stays clear. */
    private static void vignette(DrawContext ctx, int w, int h, MenuStudioConfig c, float scale) {
        float breathe = 1f + 0.08f * (float) Math.sin(time * 0.3);
        float base = (c.darkOverlay ? c.overlayOpacity * 0.45f : 0f) + c.fogIntensity * 0.25f;
        float strength = base * breathe * scale * fade;
        if (!visible(strength)) return;
        int rgb = themeRgb(c.theme);
        int band = Math.max(12, w / 6);
        for (int i = 0; i < band; i += 2) {
            float t = 1f - i / (float) band;
            int col = argb(rgb, strength * t * t);
            if ((col >>> 24) == 0) continue;
            ctx.fill(i, 0, i + 2, h, col);
            ctx.fill(w - i - 2, 0, w - i, h, col);
        }
        int top = Math.max(6, h / 12);
        for (int i = 0; i < top; i += 2) {
            float t = 1f - i / (float) top;
            int col = argb(rgb, strength * 0.8f * t * t);
            if ((col >>> 24) == 0) continue;
            ctx.fill(0, i, w, i + 2, col);
        }
    }

    private static void ensureForest(int w, int h) {
        if (w == forestW && h == forestH) return;
        forestW = w;
        forestH = h;
        treeH = new int[Math.max(1, w)];
        int maxH = Math.max(8, (int) (h * 0.07f));
        long seed = 1313L;
        int x = 0;
        while (x < w) {
            seed = mix(seed);
            int tw = 6 + (int) ((seed >>> 8) & 7);
            seed = mix(seed);
            float r = ((seed >>> 8) & 0xFFFF) / 65535f;
            int th = (int) (maxH * (0.45f + 0.55f * r));
            for (int i = 0; i < tw && x + i < w; i++) {
                float tri = 1f - Math.abs(i - tw / 2f) / (tw / 2f);
                int hh = (int) (th * (0.30f + 0.70f * tri));
                hh = (hh / 2) * 2;
                if (hh > treeH[x + i]) treeH[x + i] = hh;
            }
            x += Math.max(3, (int) (tw * 0.7f));
        }
    }

    private static void forest(DrawContext ctx, int w, int h) {
        int maxH = Math.max(8, (int) (h * 0.07f));
        // Never overlap the lowest vanilla button row on small windows.
        if (h / 4 + 156 > h - maxH) return;
        ensureForest(w, h);
        int col = argb(0x000000, 0.92f * fade);
        if ((col >>> 24) == 0) return;
        // Keep the two bottom text lines (version / copyright) readable.
        int xMin = 120;
        int xMax = w - 210;
        int x = xMin;
        while (x < xMax && x < treeH.length) {
            int hh = treeH[x];
            int start = x;
            while (x < xMax && x < treeH.length && treeH[x] == hh) x++;
            if (hh > 0) ctx.fill(start, h - hh, x, h, col);
        }
    }

    private static void stars(DrawContext ctx, int w, int h) {
        int cx = w / 2;
        for (int i = 0; i < 70; i++) {
            long hs = mix(i * 7919L + 17L);
            int x = (int) (((hs >>> 8) & 0xFFFF) / 65535f * (w - 1));
            int y = (int) (((hs >>> 28) & 0xFFFF) / 65535f * (h * 0.7f));
            if (Math.abs(x - cx) < 130) continue;
            float speed = 0.6f + (hs & 7) * 0.15f;
            float tw = 0.5f + 0.5f * (float) Math.sin(time * speed + i);
            float a = (0.12f + 0.45f * tw) * fade;
            if (!visible(a)) continue;
            ctx.fill(x, y, x + 1, y + 1, argb(0xFFFFFF, a));
        }
    }

    private static void grain(DrawContext ctx, int w, int h, MenuStudioConfig c) {
        float a = c.grainIntensity * 0.30f * fade;
        if (!visible(a)) return;
        int n = Math.min(160, (int) (c.grainIntensity * 160f));
        long step = System.nanoTime() / 100_000_000L;
        int col = argb(0xB0B0B0, a);
        for (int i = 0; i < n; i++) {
            long hs = mix(step * 31L + i);
            int x = (int) ((hs >>> 8) & 0xFFFF) % Math.max(1, w);
            int y = (int) ((hs >>> 32) & 0xFFFF) % Math.max(1, h);
            ctx.fill(x, y, x + 1, y + 1, col);
        }
    }

    private static void tag(DrawContext ctx, int w, MenuStudioConfig c) {
        float a = 0.75f * fade;
        if (!visible(a)) return;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int tw = tr.getWidth(TAG);
        int x = (w - tw) / 2;
        ctx.drawTextWithShadow(tr, TAG, x, 10, argb(0xD8D8D8, a));
        ctx.fill(x, 21, x + tw, 22, argb(accentRgb(c.accent), a));
    }

    // ---------------------------------------------------------------- eyes

    private static void eyes(DrawContext ctx, int w, int h, MenuStudioConfig c, boolean low) {
        int cx = w / 2;
        int avail = cx - 130; // free width on each side of the button column
        if (avail < 56) return;
        float s = Math.min(Math.min(avail / 4.6f, h * 0.055f), 22f);
        if (s < 5f) return;
        int accent = accentRgb(c.accent);
        int pairs = low ? 2 : EYE_PAIRS.length;
        for (int p = 0; p < pairs; p++) {
            float[] d = EYE_PAIRS[p];
            double cyc = (time / d[2] + d[3]) % 1.0;
            float vis = (float) Math.sin(Math.PI * cyc);
            vis *= vis;
            float alpha = vis * c.eyeIntensity * fade * 0.85f;
            if (!visible(alpha)) continue;

            int zoneX = d[0] == 0 ? avail / 2 : w - avail / 2;
            int y = (int) (h * d[1]);
            float gap = s * 2.3f;

            // blink
            double bp = (time + d[3] * 7.0) % 5.5;
            float open = 1f;
            if (bp < 0.2) {
                float x = (float) (bp / 0.2);
                open = Math.max(0.08f, Math.abs(2f * x - 1f));
            }
            float b = s * 0.55f * open;
            float gx = (float) Math.sin(time * 0.35 + d[3] * 6.0) * s * 0.18f;
            float gy = (float) Math.sin(time * 0.27 + d[3] * 3.0) * b * 0.15f;

            drawEye(ctx, Math.round(zoneX - gap / 2f), y, s, b, alpha, gx, gy, accent);
            drawEye(ctx, Math.round(zoneX + gap / 2f), y, s, b, alpha, gx, gy, accent);
        }
    }

    private static void drawEye(DrawContext ctx, int cx, int cy, float a, float b, float alpha,
                                float gx, float gy, int accent) {
        // faint accent halo
        float bh = b * 1.35f;
        float ah = a * 1.2f;
        int ih = (int) Math.ceil(bh);
        int haloCol = argb(accent, alpha * 0.10f);
        if ((haloCol >>> 24) != 0) {
            for (int dy = -ih; dy < ih; dy++) {
                float ty = (dy + 0.5f) / bh;
                if (Math.abs(ty) >= 1f) continue;
                int span = Math.round(ah * (float) Math.sqrt(1f - ty * ty));
                ctx.fill(cx - span, cy + dy, cx + span, cy + dy + 1, haloCol);
            }
        }

        int scleraCol = argb(0xC8C8C8, alpha);
        int irisCol = argb(0x050505, alpha);
        float irisA = a * 0.42f;
        float irisB = Math.max(0.5f, b * 0.92f);
        int ib = (int) Math.ceil(b);
        for (int dy = -ib; dy < ib; dy++) {
            float ty = (dy + 0.5f) / b;
            if (Math.abs(ty) >= 1f) continue;
            float span = a * (float) Math.sqrt(1f - ty * ty);
            int x0 = Math.round(cx - span);
            int x1 = Math.round(cx + span);
            if (x1 <= x0) continue;
            ctx.fill(x0, cy + dy, x1, cy + dy + 1, scleraCol);

            float iy = ((dy + 0.5f) - gy) / irisB;
            if (Math.abs(iy) < 1f) {
                float is = irisA * (float) Math.sqrt(1f - iy * iy);
                int i0 = Math.max(x0, Math.round(cx + gx - is));
                int i1 = Math.min(x1, Math.round(cx + gx + is));
                if (i1 > i0) ctx.fill(i0, cy + dy, i1, cy + dy + 1, irisCol);
            }
        }
        if (b > 3f) {
            int hx = Math.round(cx + gx - irisA * 0.35f);
            int hy = Math.round(cy + gy - irisB * 0.4f);
            ctx.fill(hx, hy, hx + 1, hy + 1, argb(0xFFFFFF, alpha));
        }
    }

    // ---------------------------------------------------------------- button glow

    /** Accent outline that fades in on hover or keyboard focus. Buttons themselves are untouched. */
    private static void glow(Screen screen, DrawContext ctx, int mx, int my, MenuStudioConfig c) {
        if (!c.buttonGlow) {
            HOVER.clear();
            return;
        }
        int rgb = accentRgb(c.accent);
        for (ClickableWidget wd : Screens.getButtons(screen)) {
            if (!wd.visible) continue;
            boolean on = wd.active && (wd.isMouseOver(mx, my) || wd.isFocused());
            float cur = HOVER.containsKey(wd) ? HOVER.get(wd) : 0f;
            cur += ((on ? 1f : 0f) - cur) * Math.min(1f, dt * 12f);
            if (cur < 0.01f) {
                HOVER.remove(wd);
                continue;
            }
            HOVER.put(wd, cur);
            int col = argb(rgb, cur * 0.9f * fade);
            int x = wd.getX();
            int y = wd.getY();
            int bw = wd.getWidth();
            int bh = wd.getHeight();
            ctx.fill(x, y, x + bw, y + 1, col);
            ctx.fill(x, y + bh - 1, x + bw, y + bh, col);
            ctx.fill(x, y + 1, x + 1, y + bh - 1, col);
            ctx.fill(x + bw - 1, y + 1, x + bw, y + bh - 1, col);
        }
    }
}

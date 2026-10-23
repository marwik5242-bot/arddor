package com.thirteenag.menustudio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/** Persistent settings. File: config/13ag-menu-studio.json (same name as in 0.1.x). */
public final class MenuStudioConfig {
    public static final String[] ACCENTS = {"red", "pink", "white", "green"};
    public static final String[] THEMES = {"eyes", "blood", "ash"};

    public boolean creatorTag = true;
    public boolean darkOverlay = true;
    public boolean animatedStars = true;
    public boolean grainEffect = true;
    public boolean animatedEyes = true;
    public boolean forestSilhouette = true;
    public boolean screenTint = true;
    public boolean customBackgroundEnabled = false;
    public boolean smoothTransitions = true;
    public boolean buttonGlow = true;
    public boolean lowPerformanceMode = false;
    public boolean blurEffect = false;

    public float overlayOpacity = 0.55f;
    public float buttonOpacity = 0.80f;
    public float animationSpeed = 1.0f;
    public float eyeIntensity = 0.70f;
    public float fogIntensity = 0.50f;
    public float grainIntensity = 0.30f;
    public float transitionSeconds = 0.60f;

    public String accent = "red";
    public String theme = "eyes";
    public String backgroundImagePath = "";
    public String language = "auto";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MenuStudioConfig instance;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("13ag-menu-studio.json");
    }

    public static synchronized MenuStudioConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static MenuStudioConfig load() {
        Path f = file();
        MenuStudioConfig c = null;
        boolean existed = Files.isRegularFile(f);
        if (existed) {
            try {
                c = GSON.fromJson(Files.readString(f), MenuStudioConfig.class);
            } catch (Exception e) {
                System.err.println("[13ag Menu Studio] Config invalid, using defaults: " + e.getMessage());
                try {
                    Files.move(f, f.resolveSibling("13ag-menu-studio.json.broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {
                    // best effort
                }
                existed = false;
            }
        }
        if (c == null) {
            c = new MenuStudioConfig();
        }
        c.sanitize();
        if (!existed) {
            c.save();
        }
        return c;
    }

    public synchronized void save() {
        sanitize();
        Path f = file();
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling("13ag-menu-studio.json.tmp");
            Files.writeString(tmp, GSON.toJson(this));
            try {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("[13ag Menu Studio] Could not save config: " + e.getMessage());
        }
    }

    public void resetToDefaults() {
        MenuStudioConfig d = new MenuStudioConfig();
        creatorTag = d.creatorTag;
        darkOverlay = d.darkOverlay;
        animatedStars = d.animatedStars;
        grainEffect = d.grainEffect;
        animatedEyes = d.animatedEyes;
        forestSilhouette = d.forestSilhouette;
        screenTint = d.screenTint;
        customBackgroundEnabled = d.customBackgroundEnabled;
        smoothTransitions = d.smoothTransitions;
        buttonGlow = d.buttonGlow;
        lowPerformanceMode = d.lowPerformanceMode;
        blurEffect = d.blurEffect;
        overlayOpacity = d.overlayOpacity;
        buttonOpacity = d.buttonOpacity;
        animationSpeed = d.animationSpeed;
        eyeIntensity = d.eyeIntensity;
        fogIntensity = d.fogIntensity;
        grainIntensity = d.grainIntensity;
        transitionSeconds = d.transitionSeconds;
        accent = d.accent;
        theme = d.theme;
        backgroundImagePath = d.backgroundImagePath;
        language = d.language;
    }

    /** Clamp everything to safe ranges; NaN / infinity / garbage never reaches the renderer. */
    public void sanitize() {
        overlayOpacity = clamp(overlayOpacity, 0f, 1f, 0.55f);
        buttonOpacity = clamp(buttonOpacity, 0f, 1f, 0.80f);
        animationSpeed = clamp(animationSpeed, 0.1f, 4f, 1f);
        eyeIntensity = clamp(eyeIntensity, 0f, 1f, 0.70f);
        fogIntensity = clamp(fogIntensity, 0f, 1f, 0.50f);
        grainIntensity = clamp(grainIntensity, 0f, 1f, 0.30f);
        transitionSeconds = clamp(transitionSeconds, 0f, 3f, 0.60f);
        accent = oneOf(accent, ACCENTS, "red");
        theme = oneOf(theme, THEMES, "eyes");
        if (backgroundImagePath == null) backgroundImagePath = "";
        if (language == null) language = "auto";
    }

    private static float clamp(float v, float min, float max, float def) {
        if (Float.isNaN(v) || Float.isInfinite(v)) return def;
        return Math.max(min, Math.min(max, v));
    }

    private static String oneOf(String v, String[] allowed, String def) {
        if (v == null) return def;
        String l = v.toLowerCase(Locale.ROOT);
        for (String a : allowed) {
            if (a.equals(l)) return a;
        }
        return def;
    }
}

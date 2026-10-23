package com.thirteenag.menustudio;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** In-game editor. Changes apply immediately; Save / Done / Esc write them to disk. */
public final class MenuStudioScreen extends Screen {
    private static final String[] TABS = {"atmosphere", "interface", "animations", "appearance", "performance"};

    private final Screen parent;
    private int page = 0;

    public MenuStudioScreen(Screen parent) {
        super(Text.translatable("ag13.editor.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MenuStudioConfig c = MenuStudioConfig.get();

        // category tabs
        int tabW = Math.min(90, (width - 12) / TABS.length);
        int tabsX = (width - tabW * TABS.length) / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            ButtonWidget tab = ButtonWidget.builder(Text.translatable("ag13.tab." + TABS[i]), b -> {
                page = idx;
                clearAndInit();
            }).dimensions(tabsX + i * tabW, 28, tabW, 20).build();
            tab.active = i != page;
            addDrawableChild(tab);
        }

        int slot = 0;
        switch (page) {
            case 0:
                toggle(slot++, "darkOverlay", () -> c.darkOverlay, v -> c.darkOverlay = v);
                toggle(slot++, "forestSilhouette", () -> c.forestSilhouette, v -> c.forestSilhouette = v);
                toggle(slot++, "animatedStars", () -> c.animatedStars, v -> c.animatedStars = v);
                toggle(slot++, "animatedEyes", () -> c.animatedEyes, v -> c.animatedEyes = v);
                toggle(slot++, "grainEffect", () -> c.grainEffect, v -> c.grainEffect = v);
                toggle(slot++, "screenTint", () -> c.screenTint, v -> c.screenTint = v);
                break;
            case 1:
                toggle(slot++, "buttonGlow", () -> c.buttonGlow, v -> c.buttonGlow = v);
                toggle(slot++, "smoothTransitions", () -> c.smoothTransitions, v -> c.smoothTransitions = v);
                toggle(slot++, "creatorTag", () -> c.creatorTag, v -> c.creatorTag = v);
                slider(slot++, "overlayOpacity", 0f, 1f, c.overlayOpacity, v -> c.overlayOpacity = v);
                break;
            case 2:
                slider(slot++, "animationSpeed", 0.1f, 4f, c.animationSpeed, v -> c.animationSpeed = v);
                slider(slot++, "eyeIntensity", 0f, 1f, c.eyeIntensity, v -> c.eyeIntensity = v);
                slider(slot++, "fogIntensity", 0f, 1f, c.fogIntensity, v -> c.fogIntensity = v);
                slider(slot++, "grainIntensity", 0f, 1f, c.grainIntensity, v -> c.grainIntensity = v);
                slider(slot++, "transitionSeconds", 0f, 3f, c.transitionSeconds, v -> c.transitionSeconds = v);
                break;
            case 3:
                cycle(slot++, "theme", MenuStudioConfig.THEMES, () -> c.theme, v -> c.theme = v);
                cycle(slot++, "accent", MenuStudioConfig.ACCENTS, () -> c.accent, v -> c.accent = v);
                break;
            default:
                toggle(slot++, "lowPerformanceMode", () -> c.lowPerformanceMode, v -> c.lowPerformanceMode = v);
                break;
        }

        // footer
        int fw = 100;
        int fy = height - 28;
        int gap = 6;
        int fx = (width - (fw * 3 + gap * 2)) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("ag13.button.reset"), b -> {
            c.resetToDefaults();
            clearAndInit();
        }).dimensions(fx, fy, fw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ag13.button.save"), b -> c.save())
                .dimensions(fx + fw + gap, fy, fw, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("ag13.button.done"), b -> close())
                .dimensions(fx + (fw + gap) * 2, fy, fw, 20).build());
    }

    private int colWidth() {
        return width >= 330 ? 150 : Math.max(100, width - 40);
    }

    private int slotX(int slot) {
        if (width >= 330) {
            return slot % 2 == 0 ? width / 2 - colWidth() - 4 : width / 2 + 4;
        }
        return (width - colWidth()) / 2;
    }

    private int slotY(int slot) {
        int row = width >= 330 ? slot / 2 : slot;
        return 62 + row * 24;
    }

    private Text toggleText(String key, boolean on) {
        return Text.translatable("ag13.opt." + key).append(Text.literal(": "))
                .append(Text.translatable(on ? "options.on" : "options.off"));
    }

    private void toggle(int slot, String key, BooleanSupplier get, Consumer<Boolean> set) {
        addDrawableChild(ButtonWidget.builder(toggleText(key, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(toggleText(key, get.getAsBoolean()));
        }).dimensions(slotX(slot), slotY(slot), colWidth(), 20).build());
    }

    private void cycle(int slot, String key, String[] values, java.util.function.Supplier<String> get,
                       Consumer<String> set) {
        addDrawableChild(ButtonWidget.builder(cycleText(key, get.get()), b -> {
            int idx = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(get.get())) idx = i;
            }
            set.accept(values[(idx + 1) % values.length]);
            b.setMessage(cycleText(key, get.get()));
        }).dimensions(slotX(slot), slotY(slot), colWidth(), 20).build());
    }

    private Text cycleText(String key, String value) {
        return Text.translatable("ag13.opt." + key).append(Text.literal(": "))
                .append(Text.translatable("ag13." + key + "." + value));
    }

    private void slider(int slot, String key, float min, float max, float current, Consumer<Float> set) {
        addDrawableChild(new FloatSlider(slotX(slot), slotY(slot), colWidth(), 20,
                Text.translatable("ag13.opt." + key), min, max, current, set));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        if (page == 4) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("ag13.hint.performance"),
                    width / 2, 92, 0xFFA0A0A0);
        }
    }

    @Override
    public void close() {
        MenuStudioConfig.get().save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private static final class FloatSlider extends SliderWidget {
        private final Text label;
        private final float min;
        private final float max;
        private final Consumer<Float> setter;

        FloatSlider(int x, int y, int w, int h, Text label, float min, float max, float current,
                    Consumer<Float> setter) {
            super(x, y, w, h, Text.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private float currentValue() {
            return (float) (min + (max - min) * value);
        }

        @Override
        protected void updateMessage() {
            setMessage(label.copy().append(Text.literal(": " + String.format(Locale.ROOT, "%.2f", currentValue()))));
        }

        @Override
        protected void applyValue() {
            setter.accept(currentValue());
        }
    }
}

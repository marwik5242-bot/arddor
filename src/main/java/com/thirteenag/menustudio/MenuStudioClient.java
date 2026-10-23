package com.thirteenag.menustudio;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class MenuStudioClient implements ClientModInitializer {
    public static KeyBinding openEditor;

    @Override
    public void onInitializeClient() {
        MenuStudioConfig.get();

        openEditor = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ag13_menu_studio.open_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, "key.categories.misc"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openEditor.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MenuStudioScreen(null));
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                // Editor button, top-left corner, free area on the vanilla title screen.
                Screens.getButtons(screen).add(ButtonWidget.builder(Text.translatable("ag13.button.editor"),
                        b -> client.setScreen(new MenuStudioScreen(screen))).dimensions(6, 6, 48, 20).build());
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, dt) ->
                        Atmosphere.renderTitle(s, ctx, mx, my));
            } else if (client.world == null && !(screen instanceof GameMenuScreen)) {
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, dt) ->
                        Atmosphere.renderOther(s, ctx, mx, my));
            }
        });
    }
}

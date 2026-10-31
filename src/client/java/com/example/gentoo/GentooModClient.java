package com.example.gentoo;

import com.example.gentoo.terminal.DesktopScreen;
import com.example.gentoo.terminal.GentooState;
import com.example.gentoo.terminal.TerminalScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class GentooModClient implements ClientModInitializer {

    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.gentoo-mod.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "key.categories.misc"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                if (client.screen == null) {
                    // once the install is done, G leads straight to the desktop with the achievement
                    client.setScreen(GentooState.get().finished ? new DesktopScreen() : new TerminalScreen());
                }
            }
        });
    }
}

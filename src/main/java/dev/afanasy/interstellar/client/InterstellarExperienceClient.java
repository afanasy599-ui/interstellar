package dev.afanasy.interstellar.client;

import dev.afanasy.interstellar.InterstellarExperience;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class InterstellarExperienceClient implements ClientModInitializer {
    private static KeyBinding openTesseractKey;
    private static KeyBinding captureMomentKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(
                Identifier.of(InterstellarExperience.MOD_ID, "tesseract")
        );

        openTesseractKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.interstellar_experience.open_tesseract",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                category
        ));

        captureMomentKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.interstellar_experience.capture_moment",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(InterstellarExperienceClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        TesseractHistory.tick(client);

        while (openTesseractKey.wasPressed()) {
            if (client.player != null && client.world != null) {
                openTesseract(client);
            }
        }

        while (captureMomentKey.wasPressed()) {
            if (TesseractHistory.capture(client) && client.player != null) {
                client.player.sendMessage(Text.literal("§5[INTERSTELLAR] §fМомент сохранён в памяти Тессеракта."), true);
            }
        }
    }

    private static void openTesseract(MinecraftClient client) {
        client.setScreen(new TesseractScreen());
        client.getSoundManager().play(PositionedSoundInstance.master(
                InterstellarExperience.TESSERACT_QUOTE,
                0.96f,
                0.88f
        ));
    }

    public static void playQuote() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().play(PositionedSoundInstance.master(
                InterstellarExperience.TESSERACT_QUOTE,
                0.96f,
                0.88f
        ));
    }
}

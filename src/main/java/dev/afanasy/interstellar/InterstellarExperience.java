package dev.afanasy.interstellar;

import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Registers the client-only cinematic narration sound. */
public final class InterstellarExperience implements ModInitializer {
    public static final String MOD_ID = "interstellar_experience";
    public static final Identifier QUOTE_ID = Identifier.of(MOD_ID, "tesseract_quote");
    public static final SoundEvent TESSERACT_QUOTE = SoundEvent.of(QUOTE_ID);

    @Override
    public void onInitialize() {
        Registry.register(Registries.SOUND_EVENT, QUOTE_ID, TESSERACT_QUOTE);
    }
}

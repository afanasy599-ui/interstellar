package dev.afanasy.interstellar;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initial playable build of Interstellar Experience.
 * The Tesseract dimension uses this save's seed/world generator; if the bundled
 * world package is installed, its saved region files are visible in this dimension.
 */
public final class InterstellarExperience implements ModInitializer {
    public static final String MOD_ID = "interstellar_experience";

    public static final RegistryKey<World> TESSERACT_WORLD = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("interstellar", "tesseract")
    );

    private static final String QUOTE = "У них есть доступ к бесконечности времени и пространству, " +
            "но они не связаны ими, как мы.";

    // Position is retained for the current server session so /tesseract exit
    // returns the player to exactly where they entered from.
    private static final Map<UUID, SavedPosition> RETURN_POINTS = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));

        ServerTickEvents.END_WORLD_TICK.register(InterstellarExperience::tickTesseract);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("tesseract")
                .then(CommandManager.literal("enter").executes(context -> enter(context.getSource())))
                .then(CommandManager.literal("exit").executes(context -> exit(context.getSource())))
                .then(CommandManager.literal("quote").executes(context -> showQuote(context.getSource())))
                .then(CommandManager.literal("info").executes(context -> showInfo(context.getSource())))
                .executes(context -> showInfo(context.getSource())));
    }

    private static int enter(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        if (player.getEntityWorld().getRegistryKey().equals(TESSERACT_WORLD)) {
            player.sendMessage(Text.literal("Ты уже находишься в Тессеракте. Используй /tesseract exit."), false);
            return 0;
        }

        MinecraftServer server = source.getServer();
        ServerWorld destination = server.getWorld(TESSERACT_WORLD);
        if (destination == null) {
            player.sendMessage(Text.literal("Измерение Тессеракт не загрузилось. Проверь, что мод установлен и мир был перезапущен."), false);
            return 0;
        }

        ServerWorld origin = player.getEntityWorld();
        RETURN_POINTS.put(player.getUuid(), new SavedPosition(
                origin.getRegistryKey(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch()
        ));

        boolean moved = player.teleport(destination,
                player.getX(), player.getY(), player.getZ(), Set.of(),
                player.getYaw(), player.getPitch(), true);

        if (!moved) {
            RETURN_POINTS.remove(player.getUuid());
            player.sendMessage(Text.literal("Не удалось открыть переход в Тессеракт."), false);
            return 0;
        }

        destination.spawnParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(), 90,

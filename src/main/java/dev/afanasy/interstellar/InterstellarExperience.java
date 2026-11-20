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
                1.0, 1.1, 1.0, 0.08);
        destination.spawnParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0, player.getZ(), 28,
                0.8, 1.0, 0.8, 0.025);

        player.sendMessage(Text.literal("§5§lINTERSTELLAR  §7—  ТЕССЕРАКТ ОТКРЫТ"), false);
        player.sendMessage(Text.literal("§f" + QUOTE), false);
        player.sendMessage(Text.literal("§7Чтобы вернуться, используй §f/tesseract exit§7."), false);
        return 1;
    }

    private static int exit(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Эту команду может использовать только игрок."));
            return 0;
        }

        if (!player.getEntityWorld().getRegistryKey().equals(TESSERACT_WORLD)) {
            player.sendMessage(Text.literal("Ты не находишься в Тессеракте."), false);
            return 0;
        }

        SavedPosition saved = RETURN_POINTS.remove(player.getUuid());
        MinecraftServer server = source.getServer();
        ServerWorld destination = saved == null ? server.getOverworld() : server.getWorld(saved.worldKey());
        if (destination == null) destination = server.getOverworld();

        double x = saved == null ? destination.getSpawnPoint().getPos().getX() + 0.5 : saved.x();
        double y = saved == null ? destination.getSpawnPoint().getPos().getY() + 1.0 : saved.y();
        double z = saved == null ? destination.getSpawnPoint().getPos().getZ() + 0.5 : saved.z();
        float yaw = saved == null ? 0.0f : saved.yaw();
        float pitch = saved == null ? 0.0f : saved.pitch();

        player.teleport(destination, x, y, z, Set.of(), yaw, pitch, true);
        player.sendMessage(Text.literal("§7Ты вернулся из Тессеракта. Пространство снова подчиняется привычным законам."), false);
        return 1;
    }

    private static int showQuote(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Эту команду может использовать только игрок."));
            return 0;
        }
        player.sendMessage(Text.literal("§5§o" + QUOTE), false);
        return 1;
    }

    private static int showInfo(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§5§lINTERSTELLAR EXPERIENCE"), false);
        source.sendFeedback(() -> Text.literal("§7/tesseract enter §f— войти в измерение"), false);
        source.sendFeedback(() -> Text.literal("§7/tesseract exit §f— вернуться обратно"), false);
        source.sendFeedback(() -> Text.literal("§7/tesseract quote §f— показать фразу"), false);
        return 1;
    }

    private static void tickTesseract(ServerWorld world) {
        if (!world.getRegistryKey().equals(TESSERACT_WORLD)) return;

        // Keep the Tesseract in a perpetual midnight state.
        if (world.getTime() % 20L == 0L) {
            world.setTimeOfDay(18000L);
        }

        if (world.getTime() % 4L != 0L) return;
        long tick = world.getTime();

        for (ServerPlayerEntity player : world.getPlayers()) {
            double cx = player.getX();
            double cy = player.getY() + 1.0;
            double cz = player.getZ();

            // A moving orbital ring of portal particles makes the player feel
            // surrounded by a slowly rotating, four-dimensional space.
            for (int i = 0; i < 10; i++) {
                double angle = (tick * 0.018) + (Math.PI * 2.0 * i / 10.0);
                double radius = 3.2 + Math.sin(angle * 2.0) * 0.25;
                double px = cx + Math.cos(angle) * radius;
                double pz = cz + Math.sin(angle) * radius;
                double py = cy + Math.sin(angle * 1.7) * 0.7;
                world.spawnParticles(ParticleTypes.PORTAL, px, py, pz,
                        1, 0.015, 0.025, 0.015, 0.005);
            }

            // Small points of light form a second, tilted orbit.
            for (int i = 0; i < 4; i++) {
                double angle = -(tick * 0.025) + (Math.PI * 2.0 * i / 4.0);
                double radius = 2.0;
                world.spawnParticles(ParticleTypes.END_ROD,
                        cx + Math.cos(angle) * radius,
                        cy + 0.6 + Math.sin(angle * 1.4) * 1.1,
                        cz + Math.sin(angle) * radius,
                        1, 0.0, 0.0, 0.0, 0.0);
            }
        }

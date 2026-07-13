package dev.franwdev.kmccore.server;

import dev.franwdev.kmccore.KmcCore;
import dev.franwdev.kmccore.config.KmcCoreConfig;
import dev.franwdev.kmccore.network.NetworkHandler;
import dev.franwdev.kmccore.network.SyncConfigPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ServerSetupHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getCommandSenderWorld().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            // 1. Sync Configs
            syncConfigsToPlayer(player);

            // 2. Force Resource Pack
            forceResourcePackToPlayer(player);
        }
    }

    @SubscribeEvent
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Path sourceDir = FMLPaths.CONFIGDIR.get().resolve("kmccore").resolve("datapacks");
        if (!Files.exists(sourceDir)) {
            try {
                Files.createDirectories(sourceDir);
            } catch (IOException e) {
                KmcCore.LOGGER.error("KMC Core: Failed to create local datapacks directory", e);
            }
            return;
        }

        Path targetDir = server.getWorldPath(LevelResource.DATAPACK_DIR);
        try (Stream<Path> stream = Files.list(sourceDir)) {
            stream.forEach(path -> {
                Path destPath = targetDir.resolve(path.getFileName().toString());
                try {
                    if (Files.isDirectory(path)) {
                        copyDirectory(path, destPath);
                    } else {
                        Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    KmcCore.LOGGER.info("KMC Core: Successfully injected datapack: {}", path.getFileName().toString());
                } catch (IOException e) {
                    KmcCore.LOGGER.error("KMC Core: Failed to copy datapack {}", path.getFileName().toString(), e);
                }
            });
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core: Failed to list local datapacks directory", e);
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(path -> {
                Path dest = target.resolve(source.relativize(path));
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        enforceDatapacks(event.getServer());
    }

    private void syncConfigsToPlayer(ServerPlayer player) {
        Path syncDir = FMLPaths.CONFIGDIR.get().resolve("kmccore").resolve("sync");
        if (!Files.exists(syncDir)) {
            try {
                Files.createDirectories(syncDir);
                Path readme = syncDir.resolve("README.txt");
                Files.writeString(readme, "Place configuration files (e.g. soulslikeregen-common.toml) here to sync them to joining clients.");
            } catch (IOException e) {
                KmcCore.LOGGER.error("KMC Core: Failed to create sync config directory", e);
            }
            return;
        }

        Map<String, String> configsToSync = new HashMap<>();
        try (Stream<Path> stream = Files.list(syncDir)) {
            stream.filter(Files::isRegularFile)
                  .forEach(path -> {
                      String fileName = path.getFileName().toString();
                      if (!fileName.equals("README.txt")) {
                          try {
                              String content = Files.readString(path);
                              configsToSync.put(fileName, content);
                          } catch (IOException e) {
                              KmcCore.LOGGER.error("KMC Core: Failed to read sync config file {}", fileName, e);
                          }
                      }
                  });
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core: Failed to list sync config directory", e);
        }

        if (!configsToSync.isEmpty()) {
            SyncConfigPacket packet = new SyncConfigPacket(configsToSync);
            NetworkHandler.sendToClient(player, packet);
            KmcCore.LOGGER.info("KMC Core: Sent {} synchronized configurations to player {}", configsToSync.size(), player.getName().getString());
        }
    }

    private void forceResourcePackToPlayer(ServerPlayer player) {
        String url = KmcCoreConfig.FORCED_RESOURCE_PACK_URL.get();
        if (url != null && !url.isEmpty()) {
            String hash = KmcCoreConfig.FORCED_RESOURCE_PACK_HASH.get();
            if (hash == null) {
                hash = "";
            }
            Component prompt = Component.literal("This server requires a custom resource pack.");
            ClientboundResourcePackPacket packPacket = new ClientboundResourcePackPacket(url, hash, true, prompt);
            player.connection.send(packPacket);
            KmcCore.LOGGER.info("KMC Core: Sent forced resource pack packet to player {}", player.getName().getString());
        }
    }

    private void enforceDatapacks(MinecraftServer server) {
        List<? extends String> forcedPacks = KmcCoreConfig.FORCED_DATAPACKS.get();
        if (forcedPacks == null || forcedPacks.isEmpty()) {
            return;
        }

        PackRepository repository = server.getPackRepository();
        repository.reload();

        Collection<String> selected = new ArrayList<>(repository.getSelectedIds());
        boolean changed = false;

        for (String forced : forcedPacks) {
            if (repository.getAvailableIds().contains(forced)) {
                if (!selected.contains(forced)) {
                    selected.add(forced);
                    changed = true;
                    KmcCore.LOGGER.info("KMC Core: Datapack {} is forced, enabling it.", forced);
                }
            } else {
                KmcCore.LOGGER.warn("KMC Core: Forced datapack {} was not found in repository available packs.", forced);
            }
        }

        if (changed) {
            KmcCore.LOGGER.info("KMC Core: Reloading resources to apply forced datapacks...");
            server.reloadResources(selected);
        }
    }
}

package dev.franwdev.kmccore.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.franwdev.kmccore.KmcCore;
import dev.franwdev.kmccore.client.handler.SpellcastingFirstPersonHandler;
import dev.franwdev.kmccore.config.KmcCoreConfig;
import dev.franwdev.kmccore.network.SyncConfigPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraftforge.event.TickEvent;

public class ClientSetupHandler {

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetupHandler::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(ClientSetupHandler::onLoggingOut);
        MinecraftForge.EVENT_BUS.addListener(ClientSetupHandler::onClientTick);

        // Suppress efiscompat's full-body model render in first-person during casting.
        // Registered whenever both Iron's Spellbooks and Epic Fight are present.
        if (ModList.get().isLoaded("ironsspellbooks") && ModList.get().isLoaded("epicfight")) {
            MinecraftForge.EVENT_BUS.register(new SpellcastingFirstPersonHandler());
            KmcCore.LOGGER.info("KMC Core: Registered SpellcastingFirstPersonHandler (ironsspellbooks + epicfight detected).");
        }
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(new ClientSetupRunner());
    }

    private static class ClientSetupRunner implements Runnable {
        @Override
        public void run() {
            applyDefaultControlsAndOptions();
            injectLocalResourcePacks();
        }
    }

    private static void applyDefaultControlsAndOptions() {
        Path markerFile = FMLPaths.CONFIGDIR.get().resolve("kmccore").resolve("first_run_done.marker");
        if (Files.exists(markerFile)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        Options options = mc.options;
        if (options == null) return;

        KmcCore.LOGGER.info("KMC Core: First run detected. Initializing default controls from bundled options.txt...");

        boolean changed = false;

        try (java.io.InputStream in = ClientSetupHandler.class.getResourceAsStream("/assets/kmccore/defaults/options.txt")) {
            if (in != null) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("key_") && line.contains(":")) {
                            String[] parts = line.split(":", 2);
                            if (parts.length == 2) {
                                String rawKey = parts[0].trim();
                                String rawVal = parts[1].trim();
                                String keybindName = rawKey.substring(4); // Remove "key_"

                                for (KeyMapping keyMapping : options.keyMappings) {
                                    if (keyMapping.getName().equals(keybindName)) {
                                        try {
                                            InputConstants.Key currentKey = keyMapping.getKey();
                                            InputConstants.Key targetKey = InputConstants.getKey(rawVal);
                                            if (!currentKey.equals(targetKey)) {
                                                keyMapping.setKey(targetKey);
                                                changed = true;
                                                KmcCore.LOGGER.info("KMC Core Defaults: Set key {} to {}", keybindName, rawVal);
                                            }
                                        } catch (Exception e) {
                                            KmcCore.LOGGER.error("KMC Core Defaults: Failed to set keybind {} to {}", keybindName, rawVal, e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                KmcCore.LOGGER.warn("KMC Core: Bundled options.txt was not found in mod resources.");
            }
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core: Failed to read bundled options.txt", e);
        }

        if (changed) {
            options.save();
        }

        // Copy bundled configuration defaults
        try (java.io.InputStream configIn = ClientSetupHandler.class.getResourceAsStream("/assets/kmccore/defaults/config/alexscaves-client.toml")) {
            if (configIn != null) {
                Path targetConfig = FMLPaths.CONFIGDIR.get().resolve("alexscaves-client.toml");
                Files.copy(configIn, targetConfig, StandardCopyOption.REPLACE_EXISTING);
                KmcCore.LOGGER.info("KMC Core Defaults: Copied bundled alexscaves-client.toml to config folder");
            }
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core Defaults: Failed to copy bundled configuration files", e);
        }

        try {
            Files.createDirectories(markerFile.getParent());
            Files.writeString(markerFile, "done");
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core: Failed to write first_run_done.marker", e);
        }
    }

    public static void handleConfigSync(SyncConfigPacket packet) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Map<String, String> configs = packet.getConfigs();

        KmcCore.LOGGER.info("KMC Core: Received {} configurations to synchronize from server.", configs.size());

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();

            Path targetFile = configDir.resolve(fileName).toAbsolutePath();
            Path backupFile = configDir.resolve(fileName + ".bak").toAbsolutePath();

            try {
                // If a backup doesn't exist, create it from the current file
                if (Files.exists(targetFile) && !Files.exists(backupFile)) {
                    Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
                    KmcCore.LOGGER.info("KMC Core: Backed up config {} to {}", fileName, backupFile.getFileName());
                }

                // Write the synchronized content
                Files.createDirectories(targetFile.getParent());
                Files.writeString(targetFile, content);
                KmcCore.LOGGER.info("KMC Core: Synchronized configuration file {}", fileName);
            } catch (IOException e) {
                KmcCore.LOGGER.error("KMC Core: Failed to synchronize configuration file {}", fileName, e);
            }
        }

        // Force Forge to reload configs from disk
        try {
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, configDir);
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, configDir);
            KmcCore.LOGGER.info("KMC Core: Triggered config reload for synchronized options.");
        } catch (Exception e) {
            KmcCore.LOGGER.error("KMC Core: Error reloading configs reflectively", e);
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        KmcCore.LOGGER.info("KMC Core: Disconnecting from server. Reverting synchronized configurations...");

        try {
            Files.list(configDir).forEach(path -> {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".bak")) {
                    String originalName = fileName.substring(0, fileName.length() - 4);
                    Path originalPath = configDir.resolve(originalName);
                    try {
                        Files.copy(path, originalPath, StandardCopyOption.REPLACE_EXISTING);
                        Files.delete(path);
                        KmcCore.LOGGER.info("KMC Core: Restored and cleaned up backup for {}", originalName);
                    } catch (IOException e) {
                        KmcCore.LOGGER.error("KMC Core: Failed to restore backup for {}", originalName, e);
                    }
                }
            });

            // Reload original configs
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, configDir);
            ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, configDir);
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core: Failed to list configuration directory during disconnect restoration", e);
        }
    }

    private static void injectLocalResourcePacks() {
        Path sourceDir = FMLPaths.CONFIGDIR.get().resolve("kmccore").resolve("resourcepacks");
        if (!Files.exists(sourceDir)) {
            try {
                Files.createDirectories(sourceDir);
            } catch (IOException e) {
                KmcCore.LOGGER.error("KMC Core: Failed to create local resourcepacks directory", e);
            }
            return;
        }

        Path targetDir = FMLPaths.GAMEDIR.get().resolve("resourcepacks");
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core: Failed to create target resourcepacks directory", e);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Options options = mc.options;
        if (options == null) return;

        boolean changed = false;
        try (Stream<Path> stream = Files.list(sourceDir)) {
            List<Path> paths = stream.toList();
            for (Path path : paths) {
                String fileName = path.getFileName().toString();
                Path destPath = targetDir.resolve(fileName);
                try {
                    if (Files.isDirectory(path)) {
                        copyDirectory(path, destPath);
                    } else {
                        Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    KmcCore.LOGGER.info("KMC Core: Injected resource pack: {}", fileName);

                    // Add to selected resource packs in option list
                    String packId = "file/" + fileName;
                    if (!options.resourcePacks.contains(packId)) {
                        options.resourcePacks.add(packId);
                        changed = true;
                        KmcCore.LOGGER.info("KMC Core: Enabled resource pack: {}", packId);
                    }
                } catch (IOException e) {
                    KmcCore.LOGGER.error("KMC Core: Failed to copy resource pack {}", fileName, e);
                }
            }
        } catch (IOException e) {
            KmcCore.LOGGER.error("KMC Core: Failed to list local resourcepacks directory", e);
        }

        if (changed) {
            options.save();
            mc.reloadResourcePacks();
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
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

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            checkXray();
        }
    }

    private static void checkXray() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }

        ResourceLocation[] checks = new ResourceLocation[] {
            new ResourceLocation("minecraft", "blockstates/stone.json"),
            new ResourceLocation("minecraft", "blockstates/deepslate.json"),
            new ResourceLocation("minecraft", "blockstates/netherrack.json"),
            new ResourceLocation("minecraft", "blockstates/dirt.json"),
            new ResourceLocation("minecraft", "blockstates/sand.json"),
            new ResourceLocation("minecraft", "blockstates/gravel.json")
        };

        for (Pack pack : mc.getResourcePackRepository().getSelectedPacks()) {
            String originalId = pack.getId();
            // Only inspect custom resource packs placed by the user (IDs start with "file/")
            if (!originalId.startsWith("file/")) {
                continue;
            }

            String id = originalId.toLowerCase();
            String desc = pack.getDescription().getString().toLowerCase();

            if (id.contains("xray") || id.contains("x-ray") || id.contains("x_ray") ||
                desc.contains("xray") || desc.contains("x-ray") || desc.contains("x_ray")) {
                
                KmcCore.LOGGER.error("KMC Core: X-Ray detected by name/description in pack '{}'!", originalId);
                triggerXrayShutdown(mc, originalId, "name/description");
            }

            PackResources packResources = pack.open();
            if (packResources != null) {
                int overriddenCount = 0;
                for (ResourceLocation loc : checks) {
                    if (packResources.getResource(PackType.CLIENT_RESOURCES, loc) != null) {
                        overriddenCount++;
                    }
                }
                if (overriddenCount >= 3) {
                    KmcCore.LOGGER.error("KMC Core: X-Ray detected via blockstate overrides (count={}) in pack '{}'!", overriddenCount, originalId);
                    triggerXrayShutdown(mc, originalId, "blockstate overrides");
                }
            }
        }
    }

    private static void triggerXrayShutdown(Minecraft mc, String packId, String reason) {
        if (mc.player != null && mc.player.connection != null) {
            try {
                mc.player.connection.sendChat("Soy un tramposo patético y he estado usando X-Ray todo este tiempo porque soy incapaz de jugar limpio. Me siento humillado al admitir que necesito trampas para ser alguien en este server... Adiós, me voy porque no merezco estar aquí.");
            } catch (Exception e) {
                KmcCore.LOGGER.error("KMC Core: Failed to send self-callout chat message", e);
            }
        }

        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            // Ignore
        }

        mc.stop();
        System.exit(0);
    }
}

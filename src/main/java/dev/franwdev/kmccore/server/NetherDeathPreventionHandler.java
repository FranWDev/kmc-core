package dev.franwdev.kmccore.server;

import dev.franwdev.kmccore.config.KmcCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class NetherDeathPreventionHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (!KmcCoreConfig.NETHER_DEATH_PREVENTION_ENABLED.get()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level().dimension() == Level.NETHER) {
                event.setCanceled(true);
                player.setHealth(1.0F);
                player.getFoodData().setFoodLevel(10);
                player.getFoodData().setSaturation(0.0F);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));

                boolean teleported = false;

                if (KmcCoreConfig.NETHER_DEATH_PREVENTION_TO_SPAWN.get()) {
                    DimensionTransition destination = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
                    if (destination != null) {
                        player.changeDimension(destination);
                        teleported = true;
                    }

                    if (!teleported) {
                        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
                        if (overworld != null) {
                            BlockPos worldSpawn = overworld.getSharedSpawnPos();
                            player.teleportTo(overworld, worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, player.getYRot(), player.getXRot());
                            teleported = true;
                        }
                    }
                }

                if (!teleported) {
                    ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
                    if (overworld != null) {
                        double x = KmcCoreConfig.NETHER_DEATH_PREVENTION_X.get();
                        double y = KmcCoreConfig.NETHER_DEATH_PREVENTION_Y.get();
                        double z = KmcCoreConfig.NETHER_DEATH_PREVENTION_Z.get();
                        player.teleportTo(overworld, x, y, z, player.getYRot(), player.getXRot());
                    }
                }

                MinecraftServer server = player.getServer();
                if (server != null) {
                    String name = player.getGameProfile().getName();
                    String cmd1 = String.format("temperature set %s 100", name);
                    String cmd2 = String.format("slregen player %s fatigue set 9999", name);
                    String cmd3 = String.format("xp set %s 0 levels", name);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd1);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd2);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd3);
                }
            }
        }
    }
}


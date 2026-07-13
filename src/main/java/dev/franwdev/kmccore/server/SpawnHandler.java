package dev.franwdev.kmccore.server;

import dev.franwdev.kmccore.config.KmcCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnHandler {

    @SubscribeEvent
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (!KmcCoreConfig.DISABLE_BED_SPAWN.get()) {
            return;
        }

        BlockPos pos = event.getNewSpawn();
        if (pos != null) {
            BlockState state = event.getEntity().level().getBlockState(pos);
            if (state.getBlock() instanceof BedBlock) {
                event.setCanceled(true);
            }
        }
    }
}

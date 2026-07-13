package dev.franwdev.kmccore.server;

import dev.franwdev.kmccore.config.KmcCoreConfig;
import net.blay09.mods.waystones.api.WaystoneActivatedEvent;
import net.blay09.mods.waystones.block.WaystoneBlockBase;
import net.blay09.mods.waystones.block.PortstoneBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WaystoneSpawnHandler {

    private void setSpawnAtPlayerPos(ServerPlayer player) {
        if (!KmcCoreConfig.WAYSTONE_SET_SPAWN.get()) {
            return;
        }
        player.setRespawnPosition(player.level().dimension(), player.blockPosition(), player.getYRot(), true, true);
    }

    @SubscribeEvent
    public void onWaystoneActivated(WaystoneActivatedEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            setSpawnAtPlayerPos(player);
        }
    }

    @SubscribeEvent
    public void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isServer() && event.getEntity() instanceof ServerPlayer player) {
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
                if (block instanceof WaystoneBlockBase || block instanceof PortstoneBlock) {
                    setSpawnAtPlayerPos(player);
                }
            }
        }
    }
}

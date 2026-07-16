package dev.franwdev.kmccore;

import dev.franwdev.kmccore.client.ClientSetupHandler;
import dev.franwdev.kmccore.config.KmcCoreConfig;
import dev.franwdev.kmccore.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.franwdev.kmccore.server.ServerSetupHandler;
import dev.franwdev.kmccore.server.SpawnHandler;
import dev.franwdev.kmccore.server.WaystoneSpawnHandler;
import dev.franwdev.kmccore.server.NetherDeathPreventionHandler;
import dev.franwdev.kmccore.server.LoginInvulnerabilityHandler;
import net.minecraftforge.fml.ModList;

import java.util.function.Supplier;

@Mod(KmcCore.MODID)
public class KmcCore {

    public static final String MODID = "kmccore";
    public static final Logger LOGGER = LogManager.getLogger(KmcCore.class);

    public KmcCore() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, KmcCoreConfig.CLIENT_SPEC, "kmccore-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, KmcCoreConfig.SERVER_SPEC, "kmccore-server.toml");

        // FML events
        modBus.addListener(this::setup);

        // Client initialization safely guarded
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                new ClientInitSupplier(modBus)
        );

        // Register server events
        MinecraftForge.EVENT_BUS.register(new ServerSetupHandler());
        MinecraftForge.EVENT_BUS.register(new SpawnHandler());
        MinecraftForge.EVENT_BUS.register(new NetherDeathPreventionHandler());
        MinecraftForge.EVENT_BUS.register(new LoginInvulnerabilityHandler());
        if (ModList.get().isLoaded("waystones")) {
            MinecraftForge.EVENT_BUS.register(new WaystoneSpawnHandler());
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }

    private static class ClientInitSupplier implements Supplier<Runnable> {
        private final IEventBus modBus;

        public ClientInitSupplier(IEventBus modBus) {
            this.modBus = modBus;
        }

        @Override
        public Runnable get() {
            return new ClientInitRunner(modBus);
        }
    }

    private static class ClientInitRunner implements Runnable {
        private final IEventBus modBus;

        public ClientInitRunner(IEventBus modBus) {
            this.modBus = modBus;
        }

        @Override
        public void run() {
            ClientSetupHandler.init(modBus);
        }
    }
}

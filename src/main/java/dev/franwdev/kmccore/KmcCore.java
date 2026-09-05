package dev.franwdev.kmccore;

import dev.franwdev.kmccore.client.ClientSetupHandler;
import dev.franwdev.kmccore.config.KmcCoreConfig;
import dev.franwdev.kmccore.network.NetworkHandler;
import dev.franwdev.kmccore.server.LoginInvulnerabilityHandler;
import dev.franwdev.kmccore.server.NetherDeathPreventionHandler;
import dev.franwdev.kmccore.server.ServerSetupHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(KmcCore.MODID)
public class KmcCore {

    public static final String MODID = "kmccore";
    public static final Logger LOGGER = LoggerFactory.getLogger(KmcCore.class);

    public KmcCore(IEventBus modBus, ModContainer modContainer) {
        // Register configs
        modContainer.registerConfig(Type.CLIENT, KmcCoreConfig.CLIENT_SPEC, "kmccore-client.toml");
        modContainer.registerConfig(Type.SERVER, KmcCoreConfig.SERVER_SPEC, "kmccore-server.toml");

        // FML events
        modBus.addListener(this::setup);

        // Client initialization safely guarded
        if (FMLEnvironment.dist.isClient()) {
            ClientSetupHandler.init(modBus);
        }

        // Register server events
        NeoForge.EVENT_BUS.register(new ServerSetupHandler());
        NeoForge.EVENT_BUS.register(new NetherDeathPreventionHandler());
        NeoForge.EVENT_BUS.register(new LoginInvulnerabilityHandler());
    }

    private void setup(final FMLCommonSetupEvent event) {
        NetworkHandler.register();
    }
}

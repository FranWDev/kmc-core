package dev.franwdev.kmccore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;

import java.util.ArrayList;
import java.util.List;

public class KmcCoreConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec SERVER_SPEC;



    // Server-side options
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> FORCED_DATAPACKS;
    public static ForgeConfigSpec.ConfigValue<String> FORCED_RESOURCE_PACK_URL;
    public static ForgeConfigSpec.ConfigValue<String> FORCED_RESOURCE_PACK_HASH;
    public static BooleanValue DISABLE_BED_SPAWN;
    public static BooleanValue WAYSTONE_SET_SPAWN;

    static {
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.comment("KMC Core Client Configurations").push("client");
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();

        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();
        serverBuilder.comment("KMC Core Server Configurations").push("server");

        FORCED_DATAPACKS = serverBuilder
                .comment("List of datapack names that the server must enforce to be enabled and loaded.")
                .defineListAllowEmpty(List.of("forcedDatapacks"), ArrayList::new, o -> o instanceof String);

        FORCED_RESOURCE_PACK_URL = serverBuilder
                .comment("Resource pack URL to send to clients upon connecting.")
                .define("forcedResourcePackUrl", "");

        FORCED_RESOURCE_PACK_HASH = serverBuilder
                .comment("Resource pack SHA-1 hash to verify integrity (optional).")
                .define("forcedResourcePackHash", "");

        DISABLE_BED_SPAWN = serverBuilder
                .comment("Cancel players from setting their spawn point when they sleep in a bed.")
                .define("disableBedSpawn", true);

        WAYSTONE_SET_SPAWN = serverBuilder
                .comment("Set player spawn point at their position when clicking a Waystone.")
                .define("waystoneSetSpawn", true);

        serverBuilder.pop();
        SERVER_SPEC = serverBuilder.build();
    }
}

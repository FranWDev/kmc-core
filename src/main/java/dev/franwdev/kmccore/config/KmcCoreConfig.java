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
    public static BooleanValue NETHER_DEATH_PREVENTION_ENABLED;
    public static BooleanValue NETHER_DEATH_PREVENTION_TO_SPAWN;
    public static ForgeConfigSpec.DoubleValue NETHER_DEATH_PREVENTION_X;
    public static ForgeConfigSpec.DoubleValue NETHER_DEATH_PREVENTION_Y;
    public static ForgeConfigSpec.DoubleValue NETHER_DEATH_PREVENTION_Z;

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
                .define("disableBedSpawn", false);

        WAYSTONE_SET_SPAWN = serverBuilder
                .comment("Set player spawn point at their position when clicking a Waystone.")
                .define("waystoneSetSpawn", false);

        NETHER_DEATH_PREVENTION_ENABLED = serverBuilder
                .comment("Enable Nether lethal damage death prevention.")
                .define("netherDeathPreventionEnabled", true);

        NETHER_DEATH_PREVENTION_TO_SPAWN = serverBuilder
                .comment("If true, send players to their spawn point. If false (or if invalid), send to configured coordinates.")
                .define("netherDeathPreventionToSpawn", true);

        NETHER_DEATH_PREVENTION_X = serverBuilder
                .comment("Nether death prevention teleport X coordinate.")
                .defineInRange("netherDeathPreventionX", 0.0, -30000000.0, 30000000.0);

        NETHER_DEATH_PREVENTION_Y = serverBuilder
                .comment("Nether death prevention teleport Y coordinate.")
                .defineInRange("netherDeathPreventionY", 80.0, -64.0, 320.0);

        NETHER_DEATH_PREVENTION_Z = serverBuilder
                .comment("Nether death prevention teleport Z coordinate.")
                .defineInRange("netherDeathPreventionZ", 0.0, -30000000.0, 30000000.0);

        serverBuilder.pop();
        SERVER_SPEC = serverBuilder.build();
    }
}

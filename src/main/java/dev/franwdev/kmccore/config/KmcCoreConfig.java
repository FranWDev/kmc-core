package dev.franwdev.kmccore.config;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;

public class KmcCoreConfig {

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;

    // Server-side options
    public static ConfigValue<List<? extends String>> FORCED_DATAPACKS;
    public static ConfigValue<String> FORCED_RESOURCE_PACK_URL;
    public static ConfigValue<String> FORCED_RESOURCE_PACK_HASH;
    public static BooleanValue NETHER_DEATH_PREVENTION_ENABLED;
    public static BooleanValue NETHER_DEATH_PREVENTION_TO_SPAWN;
    public static DoubleValue NETHER_DEATH_PREVENTION_X;
    public static DoubleValue NETHER_DEATH_PREVENTION_Y;
    public static DoubleValue NETHER_DEATH_PREVENTION_Z;

    static {
        Builder clientBuilder = new Builder();
        clientBuilder.comment("KMC Core Client Configurations").push("client");
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();

        Builder serverBuilder = new Builder();
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

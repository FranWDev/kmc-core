package dev.franwdev.kmccore.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin for KMC-CORE. Prevents client-side mixins from loading on
 * dedicated servers to avoid ClassNotFoundException for client-only classes
 * (e.g., Epic Fight's RenderEngine, LocalPlayerPatch).
 */
public class KmcCoreMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            if (mixinClassName.contains("SpellcastingRenderEngineMixin")
                    || mixinClassName.contains("SpellcastingLocalPlayerPatchMixin")
                    || mixinClassName.contains("SpellcastingControlEngineMixin")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}

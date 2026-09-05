package dev.franwdev.kmccore.client.handler;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.Layer.Priority;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks.Render;
import yesman.epicfight.api.client.event.types.render.ValidatePlayerModelEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * Prevents Epic Fight from rendering the full player model in first-person
 * during Iron's Spellbooks casting animations (including GTBC spell lib spells).
 */
@OnlyIn(Dist.CLIENT)
public class SpellcastingFirstPersonHandler {

    private static final String EFISCOMPAT_NAMESPACE = "efiscompat";

    public static void init() {
        Render.VALIDATE_PLAYER_MODEL_TO_RENDER.registerEvent(SpellcastingFirstPersonHandler::onValidatePlayerModel);
    }

    public static void onValidatePlayerModel(ValidatePlayerModelEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) {
            return;
        }

        AbstractClientPlayerPatch<?> patch = event.getPlayerPatch();
        if (patch == null) {
            return;
        }
        if (!(patch.getOriginal() instanceof LocalPlayer eventPlayer)) {
            return;
        }
        if (!eventPlayer.getStringUUID().equals(localPlayer.getStringUUID())) {
            return;
        }

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
        if (playerPatch == null || !playerPatch.isEpicFightMode()) {
            return;
        }

        // Respect inaction (rolls, dashes) – don't suppress those.
        EntityState state = playerPatch.getEntityState();
        if (state.inaction()) {
            return;
        }

        // Condition 1: Spellbooks network state says we are casting.
        if (ClientMagicData.isCasting()) {
            event.setShouldRender(false);
            return;
        }

        // Condition 2: The active EpicFight animation belongs to efiscompat's
        // casting namespace. This covers the 1-2 tick lag before the sync packet
        // arrives — the animation starts before isCasting() becomes true.
        if (isPlayingEfiscompatCastAnimation(playerPatch)) {
            event.setShouldRender(false);
        }
    }

    private static boolean isPlayingEfiscompatCastAnimation(LocalPlayerPatch playerPatch) {
        Animator animator = playerPatch.getAnimator();
        if (!(animator instanceof ClientAnimator clientAnimator)) {
            return false;
        }

        Layer layer = clientAnimator.baseLayer.getLayer(Priority.HIGHEST);
        if (layer == null || layer.isOff()) {
            return false;
        }

        AnimationPlayer animationPlayer = layer.animationPlayer;
        if (animationPlayer == null) {
            return false;
        }

        AssetAccessor<? extends StaticAnimation> accessor = animationPlayer.getRealAnimation();
        if (accessor == null || !accessor.isPresent()) {
            return false;
        }

        ResourceLocation registryName = accessor.registryName();
        return registryName != null
                && EFISCOMPAT_NAMESPACE.equals(registryName.getNamespace());
    }
}


package dev.franwdev.kmccore.mixin.irons;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/**
 * While Iron's Spells 'n Spellbooks is casting a spell, Epic Fight must not
 * control either the first-person camera angles or the hand renderer.
 *
 * <p><b>Problem 1 – Camera inside head:</b> Epic Fight's {@code epicfight$computeCameraAngles}
 * rotates/translates the viewport to match its animation poses. When Spellbooks
 * plays a casting animation, Epic Fight still applies its camera transform,
 * pushing the viewpoint inside the player's skull. Cancelling this event during
 * casting restores the vanilla camera.</p>
 *
 * <p><b>Problem 2 – Hand render override:</b> Epic Fight's {@code epicfight$renderHand}
 * replaces the vanilla {@code ItemInHandRenderer}, hiding the Spellbooks
 * first-person casting animation. Cancelling it yields control back to
 * Spellbooks.</p>
 *
 * <p>Exception: during Epic Fight "inaction" states (rolls, dodge animations)
 * we let Epic Fight keep full control so those full-body animations play
 * correctly.</p>
 */
@Mixin(value = RenderEngine.class, remap = false)
public abstract class SpellcastingRenderEngineMixin {

    /**
     * Returns {@code true} when Epic Fight should yield control to Spellbooks
     * (i.e., the local player is casting AND is not in an inaction state such
     * as a roll or dash).
     */
    private static boolean kmccore$shouldYieldToSpellbooks(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        // Only intervene in first-person. In third-person the EpicFight model
        // should render normally so other players and the local player see it.
        if (!mc.options.getCameraType().isFirstPerson()) {
            return false;
        }
        if (!ClientMagicData.isCasting()) {
            return false;
        }

        LocalPlayer localPlayer = mc.player;
        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(localPlayer);
        if (playerPatch == null || !playerPatch.isEpicFightMode()) {
            return false;
        }

        // Inaction = roll / dodge / etc. – keep Epic Fight in charge.
        EntityState state = playerPatch.getEntityState();
        return !state.inaction();
    }

    /**
     * Cancels Epic Fight's camera angle override during casting.
     * Without this, the camera rotates into the player model, causing the
     * "inside the head" view in first-person.
     */
    @Inject(method = "epicfight$computeCameraAngles", at = @At("HEAD"), cancellable = true)
    private void kmccore$cancelCameraDuringCast(ComputeCameraAngles event, CallbackInfo ci) {
        if (kmccore$shouldYieldToSpellbooks(Minecraft.getInstance())) {
            ci.cancel();
        }
    }

    /**
     * Cancels Epic Fight's first-person hand renderer override during casting
     * so that Spellbooks' vanilla-based hand animations are shown instead.
     */
    @Inject(method = "epicfight$renderHand", at = @At("HEAD"), cancellable = true)
    private void kmccore$cancelHandRenderDuringCast(RenderHandEvent event, CallbackInfo ci) {
        if (kmccore$shouldYieldToSpellbooks(Minecraft.getInstance())) {
            ci.cancel();
        }
    }
}


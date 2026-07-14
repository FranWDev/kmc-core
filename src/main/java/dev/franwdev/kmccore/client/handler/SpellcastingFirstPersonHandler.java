package dev.franwdev.kmccore.client.handler;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.forgeevent.RenderEpicFightPlayerEvent;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Prevents Epic Fight's {@link yesman.epicfight.client.renderer.FirstPersonRenderer}
 * from rendering the full player model in first-person during Iron's Spellbooks
 * casting animations (including GTBC spell lib spells).
 *
 * <h3>Why two conditions?</h3>
 * <ul>
 *   <li><b>{@code ClientMagicData.isCasting()}</b> – set server-side and synced
 *       via packet. May arrive 1-2 ticks late, causing a brief flicker where the
 *       EpicFight model appears before the flag is set.</li>
 *   <li><b>Active animation namespace "efiscompat"</b> – efiscompat registers
 *       EpicFight casting animations under its own namespace. If the current
 *       HIGHEST-priority layer animation belongs to efiscompat, it is a casting
 *       animation — even if the network packet has not arrived yet.</li>
 * </ul>
 * Either condition alone is sufficient to suppress the render.
 *
 * <p>Exception: inaction states (rolls, dashes) bypass both checks so Epic Fight
 * keeps control of those full-body animations.</p>
 */
@OnlyIn(Dist.CLIENT)
public class SpellcastingFirstPersonHandler {

    private static final String EFISCOMPAT_NAMESPACE = "efiscompat";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderEpicFightPlayer(RenderEpicFightPlayerEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) {
            return;
        }

        // Only act on the local player's patch.
        PlayerPatch<?> patch = event.getPlayerPatch();
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

    /**
     * Returns {@code true} if the HIGHEST-priority base layer in the player's
     * Epic Fight animator is currently playing an animation registered by
     * efiscompat (namespace = "efiscompat"), which means a casting animation
     * is in progress even if {@code ClientMagicData.isCasting()} is not yet
     * {@code true} due to network latency.
     */
    private static boolean isPlayingEfiscompatCastAnimation(LocalPlayerPatch playerPatch) {
        Animator animator = playerPatch.getAnimator();
        if (!(animator instanceof ClientAnimator clientAnimator)) {
            return false;
        }

        Layer layer = clientAnimator.baseLayer.getLayer(Layer.Priority.HIGHEST);
        if (layer == null || layer.isOff()) {
            return false;
        }

        var animationPlayer = layer.animationPlayer;
        if (animationPlayer == null) {
            return false;
        }

        var accessor = animationPlayer.getRealAnimation();
        if (accessor == null || !accessor.isPresent()) {
            return false;
        }

        var registryName = accessor.get().getRegistryName();
        return registryName != null
                && EFISCOMPAT_NAMESPACE.equals(registryName.getNamespace());
    }
}

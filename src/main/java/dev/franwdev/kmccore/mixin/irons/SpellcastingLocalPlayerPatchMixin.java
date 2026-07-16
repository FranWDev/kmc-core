package dev.franwdev.kmccore.mixin.irons;

import io.redspace.ironsspellbooks.api.item.ISpellbook;
import io.redspace.ironsspellbooks.api.item.weapons.MagicSwordItem;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/**
 * Prevents Epic Fight's first-person player model from rendering during any
 * Iron's Spellbooks casting animation (including GTBC spell lib spells).
 *
 * <p>The check in {@code overrideRender} uses three independent conditions, any
 * of which is sufficient to suppress the render:</p>
 * <ol>
 *   <li><b>isCasting()</b> – the standard Spellbooks network-synced cast flag.</li>
 *   <li><b>isUsingItem() with a staff/spellbook</b> – covers instant-cast spells
 *       and the early frames before the sync packet arrives.</li>
 *   <li><b>Active efiscompat animation</b> – if Epic Fight is already playing a
 *       casting animation registered by efiscompat, the render must be hidden
 *       regardless of the other two flags.</li>
 * </ol>
 */
@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class SpellcastingLocalPlayerPatchMixin {

    private static final String EFISCOMPAT_NAMESPACE = "efiscompat";

    /**
     * Forces {@code overrideRender()} to return {@code false} during any
     * Spellbooks cast, preventing Epic Fight's FirstPersonRenderer from
     * drawing the full player model over the vanilla hand animation.
     */
    @Inject(method = "overrideRender", at = @At("HEAD"), cancellable = true)
    private void kmccore$suppressFirstPersonModelDuringCast(CallbackInfoReturnable<Boolean> cir) {
        if (kmccore$shouldSuppressFirstPerson((LocalPlayerPatch) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canPlayAttackAnimation", at = @At("HEAD"), cancellable = true)
    private void kmccore$blockAttackDuringCast(CallbackInfoReturnable<Boolean> cir) {
        if (ClientMagicData.isCasting()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "playAnimationSynchronized", at = @At("HEAD"), cancellable = true)
    private void kmccore$blockAttackAnimationDuringCast(
            AssetAccessor<? extends StaticAnimation> animation,
            float transitionTimeModifier,
            CallbackInfo ci) {
        if (ClientMagicData.isCasting()) {
            StaticAnimation anim = animation.get();
            if (anim instanceof AttackAnimation) {
                ci.cancel();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when Epic Fight should yield first-person rendering
     * to Iron's Spellbooks. Uses three independent detection paths to handle
     * network latency and instant-cast edge cases.
     */
    private static boolean kmccore$shouldSuppressFirstPerson(LocalPlayerPatch patch) {
        // Only suppress in first-person view. Third-person must still show the
        // EpicFight model so other players (and the local player in 3rd person)
        // see the casting animation correctly.
        var mc = Minecraft.getInstance();
        if (!mc.options.getCameraType().isFirstPerson()) {
            return false;
        }

        // Allow inaction (rolls, dashes) to render normally.
        EntityState state = patch.getEntityState();
        if (state != null && state.inaction()) {
            return false;
        }

        // Condition 1: Spellbooks reports an active cast (server-synced).
        if (ClientMagicData.isCasting()) {
            return true;
        }

        // Condition 2: Player is actively using a staff, spellbook, or magic
        // sword. Covers instant-cast spells and the 1-2 tick lag before
        // isCasting() becomes true via network sync.
        if (mc.player != null && mc.player.isUsingItem()) {
            ItemStack using = mc.player.getUseItem();
            var item = using.getItem();
            if (item instanceof StaffItem
                    || item instanceof ISpellbook
                    || item instanceof MagicSwordItem) {
                return true;
            }
        }

        // Condition 3: An efiscompat casting animation is currently playing in
        // Epic Fight's HIGHEST-priority layer.
        return kmccore$isEfiscompatAnimationActive(patch);
    }

    private static boolean kmccore$isEfiscompatAnimationActive(LocalPlayerPatch patch) {
        Animator animator = patch.getAnimator();
        if (!(animator instanceof ClientAnimator clientAnimator)) {
            return false;
        }
        Layer layer = clientAnimator.baseLayer.getLayer(Layer.Priority.HIGHEST);
        if (layer == null || layer.isOff()) {
            return false;
        }
        var animPlayer = layer.animationPlayer;
        if (animPlayer == null) {
            return false;
        }
        var accessor = animPlayer.getRealAnimation();
        if (accessor == null || !accessor.isPresent()) {
            return false;
        }
        var registryName = accessor.get().getRegistryName();
        return registryName != null
                && EFISCOMPAT_NAMESPACE.equals(registryName.getNamespace());
    }
}

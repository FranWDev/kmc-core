package dev.franwdev.kmccore.mixin.irons;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/**
 * Prevents Epic Fight from interfering with Iron's Spellbooks casting
 * animations in first-person.
 *
 * <ul>
 *   <li>{@code overrideRender} – Epic Fight returns {@code true} here to signal
 *       that its {@link yesman.epicfight.client.renderer.FirstPersonRenderer}
 *       should draw the full player model in first-person. During casting we
 *       force {@code false} so only the vanilla hand (driven by Spellbooks) is
 *       shown, eliminating the "double model" artifact.</li>
 *   <li>{@code canPlayAttackAnimation} – returns {@code false} during casting
 *       so Epic Fight's combo/attack system stays inactive.</li>
 *   <li>{@code playAnimationSynchronized} – cancels any AttackAnimation that
 *       Epic Fight attempts to force while a spell is being cast.</li>
 * </ul>
 */
@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class SpellcastingLocalPlayerPatchMixin {

    /**
     * When {@code overrideRender()} returns {@code true}, Epic Fight renders its
     * own first-person player model, causing the "double model" bug during
     * Spellbooks casts. Force it to {@code false} while casting.
     */
    @Inject(method = "overrideRender", at = @At("HEAD"), cancellable = true)
    private void kmccore$suppressFirstPersonModelDuringCast(CallbackInfoReturnable<Boolean> cir) {
        if (ClientMagicData.isCasting()) {
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
}

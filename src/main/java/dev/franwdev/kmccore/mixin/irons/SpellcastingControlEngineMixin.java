package dev.franwdev.kmccore.mixin.irons;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.ControlEngine;

/**
 * Suppresses Epic Fight's melee-attack input handling while a spell is being
 * cast via Iron's Spells 'n Spellbooks.
 *
 * <p>Without this, Epic Fight intercepts the left-click and tries to trigger a
 * melee combo, overriding the spell-cast animation. Cancelling
 * {@code maybeAttack} during casting keeps Epic Fight from competing with
 * Spellbooks for input control.</p>
 */
@Mixin(value = ControlEngine.class, remap = false)
public abstract class SpellcastingControlEngineMixin {

    @Inject(method = "maybeAttack", at = @At("HEAD"), cancellable = true)
    private void kmccore$suppressAttackDuringCast(CallbackInfo ci) {
        if (Minecraft.getInstance().player != null && ClientMagicData.isCasting()) {
            ci.cancel();
        }
    }
}

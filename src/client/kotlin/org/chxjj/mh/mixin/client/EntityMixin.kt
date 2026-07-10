package org.chxjj.mh.mixin.client

import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.Entity
import org.chxjj.mh.murdermystery.MurderMysteryMod
import org.chxjj.mh.config.MurderMysteryConfig
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Entity::class)
abstract class EntityMixin {

    @Inject(method = ["isCurrentlyGlowing"], at = [At("HEAD")], cancellable = true)
    private fun modifyGlowing(ci: CallbackInfoReturnable<Boolean>) {
        if (!MurderMysteryConfig.enabled) return

        val entity = this as? Entity ?: return
        if (entity !is AbstractClientPlayer) return

        val playerType = MurderMysteryMod.getPlayerType(entity)
        val shouldGlow = when (playerType) {
            MurderMysteryMod.PlayerType.MURDERER -> MurderMysteryConfig.highlightMurderer
            MurderMysteryMod.PlayerType.DETECTIVE_LIKE -> MurderMysteryConfig.highlightDetective
            else -> false
        }

        if (shouldGlow) {
            ci.returnValue = true
        }
    }
}

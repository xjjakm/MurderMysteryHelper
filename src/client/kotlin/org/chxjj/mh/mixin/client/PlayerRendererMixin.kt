package org.chxjj.mh.mixin.client

import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.network.chat.Component
import org.chxjj.mh.murdermystery.MurderMysteryMod
import org.chxjj.mh.config.MurderMysteryConfig
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(PlayerRenderer::class)
class PlayerRendererMixin {

    @Inject(method = ["getNameTagRenderText"], at = [At("RETURN")], cancellable = true)
    private fun modifyNameTag(player: AbstractClientPlayer, cir: CallbackInfoReturnable<Component>) {
        if (!MurderMysteryConfig.enabled) return

        val playerType = MurderMysteryMod.getPlayerType(player)
        val originalName = cir.returnValue

        val modifiedName = when (playerType) {
            MurderMysteryMod.PlayerType.MURDERER -> {
                if (MurderMysteryConfig.highlightMurderer) {
                    Component.literal("§c[MURD] §f").append(originalName)
                } else {
                    originalName
                }
            }
            MurderMysteryMod.PlayerType.DETECTIVE_LIKE -> {
                if (MurderMysteryConfig.highlightDetective) {
                    Component.literal("§9[BOW] §f").append(originalName)
                } else {
                    originalName
                }
            }
            else -> originalName
        }

        cir.returnValue = modifiedName
    }
}

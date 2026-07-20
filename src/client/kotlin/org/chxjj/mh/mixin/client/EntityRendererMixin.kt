package org.chxjj.mh.mixin.client

import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import org.chxjj.mh.config.MurderMysteryConfigHandler
import org.chxjj.mh.murdermystery.MurderMysteryMod
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(EntityRenderer::class)
abstract class EntityRendererMixin {

    @Inject(method = ["extractRenderState"], at = [At("TAIL")])
    private fun modifyOutlineColor(entity: Entity, state: EntityRenderState, partialTicks: Float, ci: CallbackInfo) {
        if (!MurderMysteryConfigHandler.instance.enabled) return
        if (state.outlineColor == 0) return

        if (entity is AbstractClientPlayer) {
            val playerType = MurderMysteryMod.getPlayerType(entity)
            val shouldGlow = when (playerType) {
                MurderMysteryMod.PlayerType.MURDERER -> MurderMysteryConfigHandler.instance.highlightMurderer
                MurderMysteryMod.PlayerType.DETECTIVE_LIKE -> MurderMysteryConfigHandler.instance.highlightDetective
                else -> false
            }
            if (shouldGlow) {
                val color = when (playerType) {
                    MurderMysteryMod.PlayerType.MURDERER -> rgbToArgb(
                        MurderMysteryConfigHandler.instance.murdererRed.toInt(),
                        MurderMysteryConfigHandler.instance.murdererGreen.toInt(),
                        MurderMysteryConfigHandler.instance.murdererBlue.toInt()
                    )
                    MurderMysteryMod.PlayerType.DETECTIVE_LIKE -> rgbToArgb(
                        MurderMysteryConfigHandler.instance.detectiveRed.toInt(),
                        MurderMysteryConfigHandler.instance.detectiveGreen.toInt(),
                        MurderMysteryConfigHandler.instance.detectiveBlue.toInt()
                    )
                    else -> null
                }
                color?.let { state.outlineColor = it }
            }
        }

        if (entity is ArmorStand && MurderMysteryConfigHandler.instance.showDroppedBow) {
            val mainHandItem = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
            if (mainHandItem.item is net.minecraft.world.item.BowItem && entity.isInvisible) {
                state.outlineColor = rgbToArgb(0, 255, 255)
            }
        }
    }

    private fun rgbToArgb(r: Int, g: Int, b: Int): Int {
        return (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
    }
}

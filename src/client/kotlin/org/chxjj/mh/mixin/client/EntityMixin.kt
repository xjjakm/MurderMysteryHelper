package org.chxjj.mh.mixin.client

import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import org.chxjj.mh.config.MurderMysteryConfigHandler
import org.chxjj.mh.murdermystery.MurderMysteryMod
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Entity::class)
abstract class EntityMixin {

    @Inject(method = ["isCurrentlyGlowing"], at = [At("HEAD")], cancellable = true)
    private fun modifyGlowing(ci: CallbackInfoReturnable<Boolean>) {
        if (!MurderMysteryConfigHandler.instance.enabled) return

        val entity = (this as? Entity) ?: return

        if (entity is AbstractClientPlayer) {
            val playerType = MurderMysteryMod.getPlayerType(entity)
            val shouldGlow = when (playerType) {
                MurderMysteryMod.PlayerType.MURDERER -> MurderMysteryConfigHandler.instance.highlightMurderer
                MurderMysteryMod.PlayerType.DETECTIVE_LIKE -> MurderMysteryConfigHandler.instance.highlightDetective
                else -> false
            }
            if (shouldGlow) {
                ci.returnValue = true
            }
        }

        if (entity is ArmorStand && MurderMysteryConfigHandler.instance.showDroppedBow) {
            val mainHandItem = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
            if (mainHandItem.item is net.minecraft.world.item.BowItem && entity.isInvisible) {
                ci.returnValue = true
            }
        }
    }


}

package org.chxjj.mh.mixin.client

import net.minecraft.network.syncher.SynchedEntityData
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.ModifyVariable

@Mixin(SynchedEntityData::class)
class SynchedEntityDataMixin {

    @Shadow
    @Suppress("unused")
    private val itemsById: Array<SynchedEntityData.DataItem<*>?>? = null

    @ModifyVariable(method = ["assignValues"], at = At("HEAD"), argsOnly = true)
    private fun filterItems(items: List<SynchedEntityData.DataValue<*>>): List<SynchedEntityData.DataValue<*>> {
        val array = itemsById ?: return items
        val maxId = array.size - 1
        return if (items.any { it.id > maxId }) {
            items.filter { it.id <= maxId }
        } else {
            items
        }
    }
}

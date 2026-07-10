package org.chxjj.mh.mixin.client

import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import org.chxjj.mh.murdermystery.MurderMysteryMod
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ClientPacketListener::class)
class ClientPacketListenerMixin {

    @Inject(method = ["handleSetEquipment"], at = [At("HEAD")])
    private fun onHandleSetEquipment(packet: ClientboundSetEquipmentPacket, ci: CallbackInfo) {
        MurderMysteryMod.handleEquipmentPacket(packet)
    }
}

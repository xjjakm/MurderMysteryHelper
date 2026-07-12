package org.chxjj.mh.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import org.chxjj.mh.config.MurderMysteryConfig
import org.chxjj.mh.murdermystery.MurderMysteryMod
import org.lwjgl.glfw.GLFW

class murdermysteryhelperClient : ClientModInitializer {

    companion object {
        lateinit var configKeyMapping: KeyMapping
    }

    override fun onInitializeClient() {
        configKeyMapping = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.murdermysteryhelper.openconfig",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC
            )
        )

        MurderMysteryMod.initialize()

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            while (configKeyMapping.consumeClick()) {
                client.setScreen(MurderMysteryConfig.generateConfigScreen(client.screen))
            }
        })

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("mh")
                    .then(
                        ClientCommands.literal("reset")
                            .executes {
                                MurderMysteryMod.reset()
                                it.source.sendFeedback(Component.translatable("mh.command.reset.success"))
                                1
                            }
                    )
            )
        }
    }
}

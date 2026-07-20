package org.chxjj.mh.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import org.chxjj.mh.config.MurderMysteryConfigHandler

class config : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            MurderMysteryConfigHandler.generateConfigScreen(parent)
        }
    }
}

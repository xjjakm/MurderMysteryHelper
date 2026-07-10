package org.chxjj.mh.client

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import org.chxjj.mh.config.MurderMysteryConfig

class config : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return ConfigScreenFactory { parent ->
            MurderMysteryConfig.generateConfigScreen(parent)
        }
    }
}

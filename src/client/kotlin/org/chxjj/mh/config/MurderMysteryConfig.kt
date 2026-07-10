package org.chxjj.mh.config

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

enum class GameMode {
    CLASSIC,
    ASSASSINATION;

    fun getTranslationKey(): String {
        return "mh.config.gamemode.${name.lowercase()}"
    }
}

object MurderMysteryConfig {
    var enabled = true
    var mode = GameMode.CLASSIC
    var highlightMurderer = true
    var highlightDetective = true
    var playSoundOnMurderer = true
    var playSoundOnDetective = true
    var chatMessageOnMurderer = true
    var chatMessageOnDetective = true
    var showDroppedBow = true
    var murdererRed = 203f
    var murdererGreen = 9f
    var murdererBlue = 9f
    var detectiveRed = 0f
    var detectiveGreen = 144f
    var detectiveBlue = 255f

    private fun boolOption(nameKey: String, descKey: String, default: Boolean, getter: () -> Boolean, setter: (Boolean) -> Unit): Option<Boolean> {
        return Option.createBuilder<Boolean>()
            .name(Component.translatable(nameKey))
            .description(OptionDescription.of(Component.translatable(descKey)))
            .binding(default, getter, setter)
            .controller { option: Option<Boolean> -> BooleanControllerBuilder.create(option) }
            .build()
    }

    private fun floatOption(nameKey: String, descKey: String, default: Float, getter: () -> Float, setter: (Float) -> Unit): Option<Float> {
        return Option.createBuilder<Float>()
            .name(Component.translatable(nameKey))
            .description(OptionDescription.of(Component.translatable(descKey)))
            .binding(default, getter, setter)
            .controller { option: Option<Float> -> FloatFieldControllerBuilder.create(option).range(0f, 255f) }
            .build()
    }

    fun generateConfigScreen(parent: Screen?): Screen {
        val generalCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("mh.config.category.general"))
            .option(boolOption("mh.config.general.enabled", "mh.config.general.enabled.desc", true, { enabled }, { enabled = it }))
            .option(
                Option.createBuilder<GameMode>()
                    .name(Component.translatable("mh.config.general.mode"))
                    .description(OptionDescription.of(Component.translatable("mh.config.general.mode.desc")))
                    .binding(GameMode.CLASSIC, { mode }, { mode = it })
                    .controller { option: Option<GameMode> ->
                        EnumControllerBuilder.create(option)
                            .enumClass(GameMode::class.java)
                            .formatValue { value -> Component.translatable(value.getTranslationKey()) }
                    }
                    .build()
            )
            .build()

        val highlightCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("mh.config.category.highlight"))
            .option(boolOption("mh.config.highlight.murderer", "mh.config.highlight.murderer.desc", true, { highlightMurderer }, { highlightMurderer = it }))
            .option(boolOption("mh.config.highlight.detective", "mh.config.highlight.detective.desc", true, { highlightDetective }, { highlightDetective = it }))
            .option(boolOption("mh.config.highlight.droppedBow", "mh.config.highlight.droppedBow.desc", true, { showDroppedBow }, { showDroppedBow = it }))
            .build()

        val colorCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("mh.config.category.colors"))
            .option(floatOption("mh.config.colors.murdererRed", "mh.config.colors.murdererRed.desc", 203f, { murdererRed }, { murdererRed = it }))
            .option(floatOption("mh.config.colors.murdererGreen", "mh.config.colors.murdererGreen.desc", 9f, { murdererGreen }, { murdererGreen = it }))
            .option(floatOption("mh.config.colors.murdererBlue", "mh.config.colors.murdererBlue.desc", 9f, { murdererBlue }, { murdererBlue = it }))
            .option(floatOption("mh.config.colors.detectiveRed", "mh.config.colors.detectiveRed.desc", 0f, { detectiveRed }, { detectiveRed = it }))
            .option(floatOption("mh.config.colors.detectiveGreen", "mh.config.colors.detectiveGreen.desc", 144f, { detectiveGreen }, { detectiveGreen = it }))
            .option(floatOption("mh.config.colors.detectiveBlue", "mh.config.colors.detectiveBlue.desc", 255f, { detectiveBlue }, { detectiveBlue = it }))
            .build()

        val notifyCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("mh.config.category.notifications"))
            .option(boolOption("mh.config.notify.soundMurderer", "mh.config.notify.soundMurderer.desc", true, { playSoundOnMurderer }, { playSoundOnMurderer = it }))
            .option(boolOption("mh.config.notify.soundDetective", "mh.config.notify.soundDetective.desc", true, { playSoundOnDetective }, { playSoundOnDetective = it }))
            .option(boolOption("mh.config.notify.chatMurderer", "mh.config.notify.chatMurderer.desc", true, { chatMessageOnMurderer }, { chatMessageOnMurderer = it }))
            .option(boolOption("mh.config.notify.chatDetective", "mh.config.notify.chatDetective.desc", true, { chatMessageOnDetective }, { chatMessageOnDetective = it }))
            .build()

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("mh.config.title"))
            .category(generalCategory)
            .category(highlightCategory)
            .category(colorCategory)
            .category(notifyCategory)
            .build()
            .generateScreen(parent)
    }
}

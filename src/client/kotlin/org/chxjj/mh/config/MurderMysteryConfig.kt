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
    INFECTION,
    ASSASSINATION
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

    private fun boolOption(name: String, desc: String, default: Boolean, getter: () -> Boolean, setter: (Boolean) -> Unit): Option<Boolean> {
        return Option.createBuilder<Boolean>()
            .name(Component.literal(name))
            .description(OptionDescription.of(Component.literal(desc)))
            .binding(default, getter, setter)
            .controller { option: Option<Boolean> -> BooleanControllerBuilder.create(option) }
            .build()
    }

    private fun floatOption(name: String, default: Float, getter: () -> Float, setter: (Float) -> Unit): Option<Float> {
        return Option.createBuilder<Float>()
            .name(Component.literal(name))
            .binding(default, getter, setter)
            .controller { option: Option<Float> -> FloatFieldControllerBuilder.create(option).range(0f, 255f) }
            .build()
    }

    fun generateConfigScreen(parent: Screen?): Screen {
        val generalCategory = ConfigCategory.createBuilder()
            .name(Component.literal("General"))
            .option(boolOption("Enabled", "Enable the Murder Mystery helper", true, { enabled }, { enabled = it }))
            .option(
                Option.createBuilder<GameMode>()
                    .name(Component.literal("Game Mode"))
                    .description(OptionDescription.of(Component.literal("Select the game mode")))
                    .binding(GameMode.CLASSIC, { mode }, { mode = it })
                    .controller { option: Option<GameMode> -> EnumControllerBuilder.create(option).enumClass(GameMode::class.java) }
                    .build()
            )
            .build()

        val highlightCategory = ConfigCategory.createBuilder()
            .name(Component.literal("Highlight"))
            .option(boolOption("Highlight Murderer", "Highlight the murderer with a glow effect", true, { highlightMurderer }, { highlightMurderer = it }))
            .option(boolOption("Highlight Detective", "Highlight the detective with a glow effect", true, { highlightDetective }, { highlightDetective = it }))
            .option(boolOption("Show Dropped Bow", "Show a glow effect around dropped bows", true, { showDroppedBow }, { showDroppedBow = it }))
            .build()

        val colorCategory = ConfigCategory.createBuilder()
            .name(Component.literal("Colors"))
            .option(floatOption("Murderer Red", 203f, { murdererRed }, { murdererRed = it }))
            .option(floatOption("Murderer Green", 9f, { murdererGreen }, { murdererGreen = it }))
            .option(floatOption("Murderer Blue", 9f, { murdererBlue }, { murdererBlue = it }))
            .option(floatOption("Detective Red", 0f, { detectiveRed }, { detectiveRed = it }))
            .option(floatOption("Detective Green", 144f, { detectiveGreen }, { detectiveGreen = it }))
            .option(floatOption("Detective Blue", 255f, { detectiveBlue }, { detectiveBlue = it }))
            .build()

        val notifyCategory = ConfigCategory.createBuilder()
            .name(Component.literal("Notifications"))
            .option(boolOption("Sound on Murderer", "Play a sound when a murderer is detected", true, { playSoundOnMurderer }, { playSoundOnMurderer = it }))
            .option(boolOption("Sound on Detective", "Play a sound when a detective is detected", true, { playSoundOnDetective }, { playSoundOnDetective = it }))
            .option(boolOption("Chat Message on Murderer", "Send a chat message when a murderer is detected", true, { chatMessageOnMurderer }, { chatMessageOnMurderer = it }))
            .option(boolOption("Chat Message on Detective", "Send a chat message when a detective is detected", true, { chatMessageOnDetective }, { chatMessageOnDetective = it }))
            .build()

        return YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Murder Mystery Helper"))
            .category(generalCategory)
            .category(highlightCategory)
            .category(colorCategory)
            .category(notifyCategory)
            .build()
            .generateScreen(parent)
    }
}

package org.chxjj.mh.config

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

enum class GameMode {
    CLASSIC,
    ASSASSINATION;

    fun getTranslationKey(): String {
        return "mh.config.gamemode.${name.lowercase()}"
    }
}

class MurderMysteryConfig {
    @SerialEntry
    var enabled = true

    @SerialEntry
    var mode = GameMode.CLASSIC

    @SerialEntry
    var highlightMurderer = true

    @SerialEntry
    var highlightDetective = true

    @SerialEntry
    var playSoundOnMurderer = true

    @SerialEntry
    var playSoundOnDetective = true

    @SerialEntry
    var chatMessageOnMurderer = true

    @SerialEntry
    var chatMessageOnDetective = true

    @SerialEntry
    var showDroppedBow = true

    @SerialEntry
    var murdererRed = 203f

    @SerialEntry
    var murdererGreen = 9f

    @SerialEntry
    var murdererBlue = 9f

    @SerialEntry
    var detectiveRed = 0f

    @SerialEntry
    var detectiveGreen = 144f

    @SerialEntry
    var detectiveBlue = 255f
}

object MurderMysteryConfigHandler {
    val HANDLER: ConfigClassHandler<MurderMysteryConfig> = ConfigClassHandler.createBuilder(MurderMysteryConfig::class.java)
        .id(Identifier.fromNamespaceAndPath("mh", "config"))
        .serializer { config ->
            GsonConfigSerializerBuilder.create(config)
                .setPath(FabricLoader.getInstance().configDir.resolve("mh.json"))
                .build()
        }
        .build()

    val instance: MurderMysteryConfig
        get() = HANDLER.instance()

    fun load() {
        HANDLER.load()
    }

    fun save() {
        HANDLER.save()
    }

    fun generateConfigScreen(parent: Screen?): Screen {
        val cfg = instance

        val boolOption = { nameKey: String, descKey: String, default: Boolean, getter: () -> Boolean, setter: (Boolean) -> Unit ->
            Option.createBuilder<Boolean>()
                .name(Component.translatable(nameKey))
                .description(OptionDescription.of(Component.translatable(descKey)))
                .binding(default, getter, setter)
                .controller { option: Option<Boolean> -> BooleanControllerBuilder.create(option) }
                .build()
        }

        val floatOption = { nameKey: String, descKey: String, default: Float, getter: () -> Float, setter: (Float) -> Unit ->
            Option.createBuilder<Float>()
                .name(Component.translatable(nameKey))
                .description(OptionDescription.of(Component.translatable(descKey)))
                .binding(default, getter, setter)
                .controller { option: Option<Float> -> FloatFieldControllerBuilder.create(option).range(0f, 255f) }
                .build()
        }

        val generalCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("mh.config.category.general"))
            .option(boolOption("mh.config.general.enabled", "mh.config.general.enabled.desc", true, { cfg.enabled }, { cfg.enabled = it }))
            .option(
                Option.createBuilder<GameMode>()
                    .name(Component.translatable("mh.config.general.mode"))
                    .description(OptionDescription.of(Component.translatable("mh.config.general.mode.desc")))
                    .binding(GameMode.CLASSIC, { cfg.mode }, { cfg.mode = it })
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
            .option(boolOption("mh.config.highlight.murderer", "mh.config.highlight.murderer.desc", true, { cfg.highlightMurderer }, { cfg.highlightMurderer = it }))
            .option(boolOption("mh.config.highlight.detective", "mh.config.highlight.detective.desc", true, { cfg.highlightDetective }, { cfg.highlightDetective = it }))
            .option(boolOption("mh.config.highlight.droppedBow", "mh.config.highlight.droppedBow.desc", true, { cfg.showDroppedBow }, { cfg.showDroppedBow = it }))
            .build()

        val colorCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("mh.config.category.colors"))
            .option(floatOption("mh.config.colors.murdererRed", "mh.config.colors.murdererRed.desc", 203f, { cfg.murdererRed }, { cfg.murdererRed = it }))
            .option(floatOption("mh.config.colors.murdererGreen", "mh.config.colors.murdererGreen.desc", 9f, { cfg.murdererGreen }, { cfg.murdererGreen = it }))
            .option(floatOption("mh.config.colors.murdererBlue", "mh.config.colors.murdererBlue.desc", 9f, { cfg.murdererBlue }, { cfg.murdererBlue = it }))
            .option(floatOption("mh.config.colors.detectiveRed", "mh.config.colors.detectiveRed.desc", 0f, { cfg.detectiveRed }, { cfg.detectiveRed = it }))
            .option(floatOption("mh.config.colors.detectiveGreen", "mh.config.colors.detectiveGreen.desc", 144f, { cfg.detectiveGreen }, { cfg.detectiveGreen = it }))
            .option(floatOption("mh.config.colors.detectiveBlue", "mh.config.colors.detectiveBlue.desc", 255f, { cfg.detectiveBlue }, { cfg.detectiveBlue = it }))
            .build()

        val notifyCategory = ConfigCategory.createBuilder()
            .name(Component.translatable("mh.config.category.notifications"))
            .option(boolOption("mh.config.notify.soundMurderer", "mh.config.notify.soundMurderer.desc", true, { cfg.playSoundOnMurderer }, { cfg.playSoundOnMurderer = it }))
            .option(boolOption("mh.config.notify.soundDetective", "mh.config.notify.soundDetective.desc", true, { cfg.playSoundOnDetective }, { cfg.playSoundOnDetective = it }))
            .option(boolOption("mh.config.notify.chatMurderer", "mh.config.notify.chatMurderer.desc", true, { cfg.chatMessageOnMurderer }, { cfg.chatMessageOnMurderer = it }))
            .option(boolOption("mh.config.notify.chatDetective", "mh.config.notify.chatDetective.desc", true, { cfg.chatMessageOnDetective }, { cfg.chatMessageOnDetective = it }))
            .build()

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("mh.config.title"))
            .category(generalCategory)
            .category(highlightCategory)
            .category(colorCategory)
            .category(notifyCategory)
            .save { save() }
            .build()
            .generateScreen(parent)
    }
}

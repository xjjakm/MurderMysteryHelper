package org.chxjj.mh.murdermystery

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.ItemStack
import org.chxjj.mh.MurderMysterySwordDetection
import org.chxjj.mh.config.GameMode
import org.chxjj.mh.config.MurderMysteryConfigHandler
import java.util.*

object MurderMysteryMod {
    private val mc = Minecraft.getInstance()

    private val murdererPlayers = ObjectOpenHashSet<UUID>()
    private val bowPlayers = ObjectOpenHashSet<UUID>()

    private var playHurtSound = false
    private var playBowSound = false

    private var currentPlayerType = PlayerType.NEUTRAL

    /** 玩家实体 ID 变化检测（多世界插件换房间/重生时实体重建，但不会触发 JOIN 事件） */
    private var lastPlayerEntityId = -1

    enum class PlayerType {
        NEUTRAL,
        DETECTIVE_LIKE,
        MURDERER
    }

    fun initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { _ ->
            onTick()
        })

        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(LevelRenderEvents.AfterTranslucentFeatures {
            onLevelRender()
        })

        ClientPlayConnectionEvents.JOIN.register(ClientPlayConnectionEvents.Join { _: ClientPacketListener, _, _: Minecraft ->
            reset()
        })

        ClientPlayConnectionEvents.DISCONNECT.register(ClientPlayConnectionEvents.Disconnect { _: ClientPacketListener, _: Minecraft ->
            reset()
        })

        // 多世界插件切换房间 = 客户端换世界（不触发 JOIN/DISCONNECT）
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(ClientLevelEvents.AfterClientLevelChange { _: Minecraft, _: net.minecraft.client.multiplayer.ClientLevel ->
            reset()
        })
    }

    private fun onTick() {
        if (!MurderMysteryConfigHandler.instance.enabled) return
        if (mc.player == null) return
        if (mc.level == null) return

        val player = mc.player!!
        currentPlayerType = getPlayerTypeFromHand(player)

        // 玩家实体被服务器重建（同维度重开房间/重生）时重置检测数据
        val entityId = player.id
        if (lastPlayerEntityId != -1 && entityId != lastPlayerEntityId) {
            reset()
        }
        lastPlayerEntityId = entityId

        checkWorldPlayers()
    }

    private fun checkWorldPlayers() {
        val level = mc.level ?: return

        for (entity in level.entitiesForRendering()) {
            if (entity is AbstractClientPlayer && entity != mc.player) {
                checkPlayerEquipment(entity)
            }
        }
    }

    private fun checkPlayerEquipment(player: AbstractClientPlayer) {
        for (slot in EquipmentSlot.entries) {
            if (slot.type != EquipmentSlot.Type.HAND) continue
            val itemStack = player.getItemBySlot(slot)
            if (itemStack.isEmpty) continue
            handleItem(itemStack, player)
        }
    }

    fun handleEquipmentPacket(packet: ClientboundSetEquipmentPacket) {
        if (!MurderMysteryConfigHandler.instance.enabled) return
        val world = mc.level ?: return
        val entity = world.getEntity(packet.entity) ?: return

        packet.slots
            .filter { !it.second.isEmpty && it.first.type == EquipmentSlot.Type.HAND }
            .forEach { handleItem(it.second, entity) }
    }

    private fun handleItem(itemStack: ItemStack, entity: Entity?) {
        if (entity !is AbstractClientPlayer) return

        val isSword = MurderMysterySwordDetection.isSword(itemStack)
        val isBow = itemStack.item is BowItem

        when {
            isSword -> handleHasSword(entity)
            isBow -> handleHasBow(entity)
        }
    }

    private fun handleHasSword(entity: AbstractClientPlayer) {
        if (MurderMysteryConfigHandler.instance.mode == GameMode.ASSASSINATION) return

        if (murdererPlayers.add(entity.gameProfile.id)) {
            if (MurderMysteryConfigHandler.instance.chatMessageOnMurderer) {
                sendChatMessage(Component.translatable("mh.notify.murderer", entity.gameProfile.name))
            }
            if (MurderMysteryConfigHandler.instance.playSoundOnMurderer) {
                playHurtSound = true
            }
        }
    }

    private fun handleHasBow(entity: AbstractClientPlayer) {
        if (MurderMysteryConfigHandler.instance.mode == GameMode.ASSASSINATION) return

        if (bowPlayers.add(entity.gameProfile.id)) {
            if (MurderMysteryConfigHandler.instance.chatMessageOnDetective) {
                sendChatMessage(Component.translatable("mh.notify.detective", entity.gameProfile.name))
            }
            if (MurderMysteryConfigHandler.instance.playSoundOnDetective) {
                playBowSound = true
            }
        }
    }

    private fun getPlayerTypeFromHand(player: AbstractClientPlayer): PlayerType {
        for (slot in EquipmentSlot.entries) {
            if (slot.type != EquipmentSlot.Type.HAND) continue
            val itemStack = player.getItemBySlot(slot)
            if (itemStack.isEmpty) continue

            when {
                itemStack.item is BowItem -> return PlayerType.DETECTIVE_LIKE
                MurderMysterySwordDetection.isSword(itemStack) -> return PlayerType.MURDERER
            }
        }
        return PlayerType.NEUTRAL
    }

    fun getPlayerType(player: AbstractClientPlayer): PlayerType {
        return when (player.gameProfile.id) {
            in murdererPlayers -> PlayerType.MURDERER
            in bowPlayers -> PlayerType.DETECTIVE_LIKE
            else -> PlayerType.NEUTRAL
        }
    }

    private fun onLevelRender() {
        if (!MurderMysteryConfigHandler.instance.enabled) return
        if (mc.player == null) return
        if (mc.level == null) return

        if (playHurtSound) {
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_HURT, 1F))
            playHurtSound = false
        }

        if (playBowSound) {
            mc.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.CROSSBOW_SHOOT, 1F))
            playBowSound = false
        }
    }

    private fun sendChatMessage(message: Component) {
        mc.execute {
            mc.player?.sendSystemMessage(message)
        }
    }

    fun reset() {
        murdererPlayers.clear()
        bowPlayers.clear()
        currentPlayerType = PlayerType.NEUTRAL
        playHurtSound = false
        playBowSound = false
        lastPlayerEntityId = -1
        org.chxjj.mh.hud.MurderMysteryHud.clear()
    }
}

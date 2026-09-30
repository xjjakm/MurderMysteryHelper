package org.chxjj.mh.hud

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import org.chxjj.mh.config.MurderMysteryConfigHandler
import org.chxjj.mh.murdermystery.MurderMysteryMod
import java.util.Locale

/**
 * 密室谋杀游戏 - 可拖动敌人信息窗口
 * 移植自 1.8.9 版本的 MurderMysteryHUD / HUDRenderHandler
 *
 * 窗口：皮肤头像 + 名字 + 角色 + 武器图标/状态 + 距离威胁 + 飞刀/箭矢警告 + 坐标
 * 右侧垂直透明度滑块；聊天界面按住左键拖动窗口/滑块，拖完自动保存配置
 */
object MurderMysteryHud {
    private val mc = Minecraft.getInstance()

    private const val MIN_WIDTH = 120
    private const val WINDOW_HEIGHT = 110
    private const val SLIDER_WIDTH = 10

    // 距离阈值
    private const val SAFE_DISTANCE = 50.0
    private const val ALERT_DISTANCE = 25.0
    private const val DANGER_DISTANCE = 10.0

    // 拖动状态
    private var dragging = false
    private var dragOffsetX = 0
    private var dragOffsetY = 0
    private var draggingSlider = false

    fun initialize() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("mh", "hud")) { graphics, _ ->
            render(graphics)
        }

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { _ ->
            WeaponTracker.tick()
            handleMouse()
        })
    }

    fun clear() {
        WeaponTracker.clear()
        dragging = false
        draggingSlider = false
    }

    // ==================== 目标查找 ====================

    private fun findClosestEnemy(): AbstractClientPlayer? {
        val player = mc.player ?: return null
        val level = mc.level ?: return null

        var closest: AbstractClientPlayer? = null
        var closestDistance = Double.MAX_VALUE

        for (entity in level.entitiesForRendering()) {
            if (entity !is AbstractClientPlayer || entity === player) continue
            // 只检查在 Tab 列表中的玩家（过滤 NPC 和死亡玩家）
            if (mc.connection?.getPlayerInfo(entity.uuid) == null) continue
            if (MurderMysteryMod.getPlayerType(entity) == MurderMysteryMod.PlayerType.NEUTRAL) continue

            val distance = player.distanceTo(entity).toDouble()
            if (distance < closestDistance) {
                closestDistance = distance
                closest = entity
            }
        }

        return closest
    }

    // ==================== 渲染 ====================

    private fun render(graphics: net.minecraft.client.gui.GuiGraphicsExtractor) {
        val cfg = MurderMysteryConfigHandler.instance
        if (!cfg.enabled || !cfg.hudEnabled) return
        val player = mc.player ?: return
        val level = mc.level ?: return

        val enemy = findClosestEnemy() ?: return

        val now = System.currentTimeMillis()
        val info = WeaponTracker.getInfo(enemy.uuid)
        val weapon = enemy.mainHandItem.takeIf { !it.isEmpty }
            ?: info?.lastWeapon

        val distance = player.distanceTo(enemy).toDouble()

        val windowWidth = calculateWindowWidth(enemy, weapon)
        val windowX = cfg.hudX.coerceIn(0, (mc.window.guiScaledWidth - windowWidth).coerceAtLeast(0))
        val windowY = cfg.hudY.coerceIn(0, (mc.window.guiScaledHeight - WINDOW_HEIGHT).coerceAtLeast(0))
        this.windowX = windowX
        this.windowY = windowY
        this.windowWidth = windowWidth

        val borderColor = calculateBorderColor(distance)
        val bgColor = (cfg.hudBgAlpha.coerceIn(0, 255) shl 24)

        graphics.fill(windowX, windowY, windowX + windowWidth, windowY + WINDOW_HEIGHT, bgColor)
        drawBorder(graphics, windowX, windowY, windowWidth, WINDOW_HEIGHT, 2, borderColor)

        val font = mc.font
        var contentY = windowY + 8
        val contentX = windowX + 8

        // 1. 玩家头像（24x24）
        PlayerFaceExtractor.extractRenderState(graphics, enemy.skin, contentX, contentY, 24)

        // 2. 玩家名字
        graphics.text(font, enemy.name, contentX + 30, contentY, 0xFFFFFFFF.toInt())

        // 3. 角色显示
        val role = MurderMysteryMod.getPlayerType(enemy)
        val (roleKey, roleColor) = when (role) {
            MurderMysteryMod.PlayerType.MURDERER -> "mh.nametag.murderer" to cfg.nameTagMurdererColor()
            MurderMysteryMod.PlayerType.DETECTIVE_LIKE -> "mh.nametag.bow" to cfg.nameTagBowColor()
            else -> "mh.nametag.civilian" to cfg.nameTagCivilianColor()
        }
        graphics.text(font, Component.translatable(roleKey), contentX + 30, contentY + 10, 0xFF shl 24 or roleColor)

        // 4. 武器信息（图标 + 名称/类型 + 状态）
        contentY += 28
        if (weapon != null) {
            graphics.item(weapon, contentX, contentY - 4)

            graphics.text(font, weaponNameAndType(weapon, role), contentX + 22, contentY, 0xFFFFFFFF.toInt())
            contentY += 10
            graphics.text(font, weaponStatus(enemy, weapon, role, info, now), contentX + 22, contentY, 0xFFFFFFFF.toInt())
        } else {
            graphics.text(font, Component.translatable("mh.hud.unknown"), contentX, contentY, 0xFFAAAAAA.toInt())
        }

        // 5. 距离 + 威胁状态
        contentY += 14
        graphics.text(
            font,
            Component.literal(String.format(Locale.ROOT, "%.1fm", distance)),
            contentX,
            contentY,
            calculateDistanceColor(distance)
        )
        graphics.text(font, threatText(distance), contentX + 42, contentY, 0xFFFFFFFF.toInt())

        // 6. 飞刀/箭矢警告
        val warning = warningText(enemy, role, info, now)
        if (warning != null) {
            contentY += 12
            graphics.text(font, warning, contentX, contentY, 0xFFFFFFFF.toInt())
        }

        // 7. 坐标
        contentY += 12
        val coordText = Component.empty()
            .append(literal("§7X:§f"))
            .append(literal("%.0f".format(enemy.x)))
            .append(literal(" §7Y:§f"))
            .append(literal("%.0f".format(enemy.y)))
            .append(literal(" §7Z:§f"))
            .append(literal("%.0f".format(enemy.z)))
        graphics.text(font, coordText, contentX, contentY, 0xFFFFFFFF.toInt())

        // 右侧透明度滑块
        drawOpacitySlider(graphics, windowX, windowY, windowWidth)
    }

    private fun literal(text: String): MutableComponent = Component.literal(text)

    // ==================== 文本构建 ====================

    private fun weaponNameAndType(weapon: net.minecraft.world.item.ItemStack, role: MurderMysteryMod.PlayerType): Component {
        val name = weapon.hoverName.copy()
        if (role == MurderMysteryMod.PlayerType.DETECTIVE_LIKE) {
            val category = WeaponTracker.bowCategoryOf(weapon)
            if (category != WeaponTracker.BowCategory.NONE) {
                name.append(" ").append(Component.translatable(category.displayKey))
            }
        }
        return name
    }

    private fun weaponStatus(
        enemy: AbstractClientPlayer,
        weapon: net.minecraft.world.item.ItemStack,
        role: MurderMysteryMod.PlayerType,
        info: WeaponTracker.WeaponInfo?,
        now: Long
    ): Component {
        // 手持状态
        val holdingNow = !enemy.mainHandItem.isEmpty && enemy.mainHandItem.item == weapon.item
        var status: MutableComponent = if (holdingNow) {
            Component.translatable("mh.hud.status.holding").withStyle { it.withColor(0x55FF55) }
        } else {
            Component.translatable("mh.hud.status.unarmed").withStyle { it.withColor(0xAAAAAA) }
        }

        when (role) {
            MurderMysteryMod.PlayerType.MURDERER -> {
                val knifeState = info?.knifeState(now) ?: WeaponTracker.KnifeState.NONE
                when (knifeState) {
                    WeaponTracker.KnifeState.IN_FLIGHT ->
                        status = status.append(colored(" ", 0xFFFFFFFF.toInt()))
                            .append(Component.translatable("mh.hud.status.flying").withStyle { it.withColor(0xFF5555) })

                    WeaponTracker.KnifeState.COOLDOWN -> {
                        val cd = info!!.knifeCooldownRemainingSeconds(now)
                        if (cd <= 0.0) {
                            status = status.append(readyTag())
                        } else {
                            status = status.append(
                                Component.translatable("mh.hud.status.cd", String.format(Locale.ROOT, "%.1f", cd))
                                    .withStyle { it.withColor(0xFFAA00) }
                            )
                        }
                    }

                    WeaponTracker.KnifeState.NONE ->
                        status = status.append(readyTag())
                }
            }

            MurderMysteryMod.PlayerType.DETECTIVE_LIKE -> {
                if (info != null) {
                    when (info.drawState(now)) {
                        WeaponTracker.DrawState.DRAWING ->
                            status = status.append(colored(" ", 0xFFFFFFFF.toInt()))
                                .append(Component.translatable("mh.hud.status.drawing").withStyle { it.withColor(0xFFFF55) })

                        WeaponTracker.DrawState.CHARGED ->
                            status = status.append(colored(" ", 0xFFFFFFFF.toInt()))
                                .append(Component.translatable("mh.hud.status.charged").withStyle { it.withColor(0xFF5555) })

                        WeaponTracker.DrawState.NONE -> {
                            val shotState = info.shotState(now)
                            when (shotState) {
                                WeaponTracker.ShotState.COOLDOWN -> {
                                    val cd = info.bowCooldownRemainingSeconds(now)
                                    if (cd <= 0.0) {
                                        status = status.append(readyTag())
                                    } else {
                                        status = status.append(
                                            Component.translatable("mh.hud.status.cd", String.format(Locale.ROOT, "%.1f", cd))
                                                .withStyle { it.withColor(0xFFAA00) }
                                        )
                                    }
                                }

                                WeaponTracker.ShotState.SHOT ->
                                    status = status.append(colored(" ", 0xFFFFFFFF.toInt()))
                                        .append(Component.translatable("mh.hud.status.shot").withStyle { it.withColor(0xAAAAAA) })

                                WeaponTracker.ShotState.READY ->
                                    status = status.append(readyTag())
                            }
                        }
                    }
                }
            }

            else -> {}
        }

        return status
    }

    private fun readyTag(): MutableComponent =
        Component.translatable("mh.hud.status.ready").withStyle { it.withColor(0x55FF55) }

    private fun colored(text: String, color: Int): MutableComponent =
        Component.literal(text).withStyle { it.withColor(color) }

    private fun threatText(distance: Double): Component {
        val key = when {
            distance <= DANGER_DISTANCE -> "mh.hud.threat.danger"
            distance <= ALERT_DISTANCE -> "mh.hud.threat.alert"
            distance <= SAFE_DISTANCE -> "mh.hud.threat.watch"
            else -> "mh.hud.threat.safe"
        }
        return Component.translatable(key)
    }

    private fun warningText(
        enemy: AbstractClientPlayer,
        role: MurderMysteryMod.PlayerType,
        info: WeaponTracker.WeaponInfo?,
        now: Long
    ): Component? {
        // 飞刀在空中
        if (role == MurderMysteryMod.PlayerType.MURDERER && info != null
            && info.knifeState(now) == WeaponTracker.KnifeState.IN_FLIGHT
        ) {
            val stand = WeaponTracker.getKnifeEntity(info)
            val local = mc.player
            if (stand != null && local != null) {
                val knifeDist = stand.position().distanceTo(local.position())
                val color = when {
                    knifeDist <= 5.0 -> 0xFF5555
                    knifeDist <= 15.0 -> 0xFFAA00
                    else -> 0xFFFF55
                }
                return Component.literal("").withStyle { it.withColor(color) }
                    .append(Component.translatable("mh.hud.knife_in_air", String.format(Locale.ROOT, "%.1f", knifeDist)))
            }
        }

        // 箭矢接近
        if (role == MurderMysteryMod.PlayerType.DETECTIVE_LIKE) {
            val arrowDist = WeaponTracker.getNearestArrowDistance(enemy.uuid)
            if (arrowDist != null) {
                val warningKey = when {
                    arrowDist <= 3.0 -> "mh.hud.arrow.danger"
                    arrowDist <= 8.0 -> "mh.hud.arrow.warning"
                    arrowDist <= 15.0 -> "mh.hud.arrow.incoming"
                    else -> "mh.hud.arrow.far"
                }
                return Component.translatable(
                    "mh.hud.arrow",
                    String.format(Locale.ROOT, "%.1f", arrowDist),
                    Component.translatable(warningKey)
                )
            }
        }

        return null
    }

    // ==================== 布局与绘制辅助 ====================

    private var windowX = 0
    private var windowY = 0
    private var windowWidth = MIN_WIDTH

    private fun calculateWindowWidth(enemy: AbstractClientPlayer, weapon: net.minecraft.world.item.ItemStack?): Int {
        val font = mc.font
        var maxWidth = MIN_WIDTH

        val nameWidth = font.width(enemy.name) + 38
        maxWidth = maxOf(maxWidth, nameWidth)

        if (weapon != null) {
            val weaponLine = weaponNameAndType(weapon, MurderMysteryMod.getPlayerType(enemy))
            maxWidth = maxOf(maxWidth, font.width(weaponLine) + 32)

            val status = "Holding [CD: 00.0s]"
            maxWidth = maxOf(maxWidth, font.width(status) + 32)
        }

        val coordText = "X:000 Y:000 Z:000"
        maxWidth = maxOf(maxWidth, font.width(coordText) + 16)

        return maxWidth + SLIDER_WIDTH
    }

    private fun drawOpacitySlider(graphics: net.minecraft.client.gui.GuiGraphicsExtractor, windowX: Int, windowY: Int, windowWidth: Int) {
        val sliderX = windowX + windowWidth - SLIDER_WIDTH

        graphics.fill(sliderX, windowY, sliderX + SLIDER_WIDTH, windowY + WINDOW_HEIGHT, 0x80000000.toInt())

        val trackX = sliderX + 4
        val trackY = windowY + 10
        val trackHeight = WINDOW_HEIGHT - 20
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0xFF555555.toInt())

        val alpha = MurderMysteryConfigHandler.instance.hudBgAlpha.coerceIn(0, 255)
        val ratio = alpha / 255.0f
        val knobY = trackY + (trackHeight * (1.0f - ratio)).toInt()
        graphics.fill(sliderX + 2, knobY - 3, sliderX + 8, knobY + 3, 0xFFFFFFFF.toInt())

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(sliderX + 5.0f, windowY + WINDOW_HEIGHT / 2.0f)
        pose.rotate(-Math.PI.toFloat() / 2.0f)
        val text = "${(ratio * 100).toInt()}%"
        val textWidth = mc.font.width(text)
        graphics.text(mc.font, text, -textWidth / 2, -4, 0xFFFFFFFF.toInt())
        pose.popMatrix()
    }

    private fun drawBorder(
        graphics: net.minecraft.client.gui.GuiGraphicsExtractor,
        x: Int, y: Int, width: Int, height: Int, thickness: Int, color: Int
    ) {
        graphics.fill(x, y, x + width, y + thickness, color)
        graphics.fill(x, y + height - thickness, x + width, y + height, color)
        graphics.fill(x, y, x + thickness, y + height, color)
        graphics.fill(x + width - thickness, y, x + width, y + height, color)
    }

    private fun calculateBorderColor(distance: Double): Int {
        val safeColor = 0x90EE90
        val dangerColor = 0x8B0000
        return when {
            distance >= SAFE_DISTANCE -> 0xFF shl 24 or safeColor
            distance <= DANGER_DISTANCE -> 0xFF shl 24 or dangerColor
            else -> {
                val ratio = ((distance - DANGER_DISTANCE) / (SAFE_DISTANCE - DANGER_DISTANCE)).toFloat()
                interpolateColor(dangerColor, safeColor, ratio)
            }
        }
    }

    private fun calculateDistanceColor(distance: Double): Int {
        return when {
            distance >= SAFE_DISTANCE -> 0xFF00FF00.toInt()
            distance <= DANGER_DISTANCE -> 0xFFFF0000.toInt()
            else -> {
                val ratio = ((distance - DANGER_DISTANCE) / (SAFE_DISTANCE - DANGER_DISTANCE)).toFloat()
                interpolateColor(0xFF0000, 0x00FF00, ratio)
            }
        }
    }

    private fun interpolateColor(color1: Int, color2: Int, ratioIn: Float): Int {
        val ratio = ratioIn.coerceIn(0f, 1f)
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        val r = (r1 + (r2 - r1) * ratio).toInt()
        val g = (g1 + (g2 - g1) * ratio).toInt()
        val b = (b1 + (b2 - b1) * ratio).toInt()

        return 0xFF shl 24 or (r shl 16) or (g shl 8) or b
    }

    // ==================== 鼠标交互（聊天界面拖动） ====================

    private fun handleMouse() {
        val cfg = MurderMysteryConfigHandler.instance
        if (!cfg.enabled || !cfg.hudEnabled) return
        if (mc.player == null || mc.level == null) return

        // 只允许在聊天界面拖动
        if (mc.gui.screen() !is ChatScreen) {
            dragging = false
            draggingSlider = false
            return
        }

        val window = mc.window
        val displayWidth = window.width
        val displayHeight = window.height
        if (displayWidth == 0 || displayHeight == 0) return

        val mouseX = (mc.mouseHandler.xpos() * window.guiScaledWidth / displayWidth).toInt()
        val mouseY = (window.guiScaledHeight - mc.mouseHandler.ypos() * window.guiScaledHeight / displayHeight - 1).toInt()
        val mousePressed = mc.mouseHandler.isLeftPressed

        var justFinishedDragging = false

        val sliderX = cfg.hudX + windowWidth - SLIDER_WIDTH

        if (mousePressed) {
            if (isOverSlider(mouseX, mouseY, sliderX) && !dragging) {
                draggingSlider = true
                updateSliderValue(mouseY)
            } else if (isOverWindow(mouseX, mouseY) && mouseX < sliderX && !draggingSlider) {
                if (!dragging) {
                    dragging = true
                    dragOffsetX = mouseX - cfg.hudX
                    dragOffsetY = mouseY - cfg.hudY
                }
            }
        } else {
            if (dragging) justFinishedDragging = true
            dragging = false
            if (draggingSlider) {
                draggingSlider = false
                justFinishedDragging = true
            }
        }

        if (dragging) {
            cfg.hudX = mouseX - dragOffsetX
            cfg.hudY = mouseY - dragOffsetY
        }
        if (draggingSlider) {
            updateSliderValue(mouseY)
        }

        if (justFinishedDragging) {
            MurderMysteryConfigHandler.save()
        }
    }

    private fun updateSliderValue(mouseY: Int) {
        val trackY = windowY + 10
        val trackHeight = WINDOW_HEIGHT - 20

        val relativeY = (mouseY - trackY).coerceIn(0, trackHeight)
        val ratio = 1.0f - relativeY.toFloat() / trackHeight
        MurderMysteryConfigHandler.instance.hudBgAlpha = (ratio * 255).toInt().coerceIn(0, 255)
    }

    private fun isOverSlider(mouseX: Int, mouseY: Int, sliderX: Int): Boolean {
        return mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH &&
                mouseY >= windowY && mouseY <= windowY + WINDOW_HEIGHT
    }

    private fun isOverWindow(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= windowX && mouseX <= windowX + windowWidth &&
                mouseY >= windowY && mouseY <= windowY + WINDOW_HEIGHT
    }
}

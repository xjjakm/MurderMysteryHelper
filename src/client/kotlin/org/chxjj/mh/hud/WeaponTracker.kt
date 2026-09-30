package org.chxjj.mh.hud

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.chxjj.mh.MurderMysterySwordDetection
import java.util.UUID

/**
 * 飞刀/弓箭武器状态追踪器
 *
 * 移植自 1.8.9 版本的 KnifeThrownDetector / BowShotDetector：
 * - 飞刀投掷物 = 隐形盔甲架主手持刀，按「物品相同 + 距离最近 < 20 格」匹配投掷者
 * - 飞刀销毁后进入 5 秒冷却
 * - 弓按组件区分类型：无限附魔=Kali 弓，Lore 1 行=侦探弓，否则普通弓
 * - 拉弓状态来自同步的使用物品标记（1 秒充满）
 * - 箭矢实体出现 = 射击（侦探弓 5 秒 CD）
 */
object WeaponTracker {
    const val KNIFE_COOLDOWN_MS = 5000L
    const val DETECTIVE_BOW_COOLDOWN_MS = 5000L
    const val NORMAL_BOW_SHOT_DISPLAY_MS = 3000L
    const val BOW_FULL_DRAW_TIME_MS = 1000L

    private const val PROJECTILE_TIMEOUT_MS = 15000L
    private const val ARROW_TIMEOUT_MS = 5000L
    private const val KNIFE_MATCH_MAX_DISTANCE = 20.0

    enum class BowCategory {
        DETECTIVE,
        KALI,
        NORMAL,
        NONE;

        val displayKey: String
            get() = "mh.hud.bowtype.${name.lowercase()}"
    }

    /** 飞刀状态 */
    enum class KnifeState { NONE, IN_FLIGHT, COOLDOWN }

    /** 拉弓状态 */
    enum class DrawState { NONE, DRAWING, CHARGED }

    /** 射击状态 */
    enum class ShotState { READY, SHOT, COOLDOWN }

    class WeaponInfo(val uuid: UUID) {
        // 手持状态
        var holdingKnife = false
        var holdingBow = false
        var knifeItem: Item? = null
        var lastWeapon: ItemStack? = null

        // 飞刀
        var knifeEntityId = -1
        var knifeThrowTime = 0L
        var lastDestroyTime = 0L

        // 弓
        var bowCategory = BowCategory.NONE
        var drawStart = 0L
        var lastShotTime = 0L

        fun knifeState(now: Long): KnifeState {
            if (knifeEntityId != -1) return KnifeState.IN_FLIGHT
            if (lastDestroyTime > 0 && now - lastDestroyTime < KNIFE_COOLDOWN_MS) return KnifeState.COOLDOWN
            return KnifeState.NONE
        }

        fun knifeCooldownRemainingSeconds(now: Long): Double {
            if (knifeState(now) != KnifeState.COOLDOWN) return 0.0
            return ((KNIFE_COOLDOWN_MS - (now - lastDestroyTime)).coerceAtLeast(0L)) / 1000.0
        }

        fun drawState(now: Long): DrawState {
            if (drawStart == 0L) return DrawState.NONE
            return if (now - drawStart >= BOW_FULL_DRAW_TIME_MS) DrawState.CHARGED else DrawState.DRAWING
        }

        fun shotState(now: Long): ShotState {
            if (lastShotTime == 0L) return ShotState.READY
            val elapsed = now - lastShotTime
            return when (bowCategory) {
                BowCategory.DETECTIVE ->
                    if (elapsed < DETECTIVE_BOW_COOLDOWN_MS) ShotState.COOLDOWN else ShotState.READY
                BowCategory.NORMAL ->
                    if (elapsed < NORMAL_BOW_SHOT_DISPLAY_MS) ShotState.SHOT else ShotState.READY
                else -> ShotState.READY
            }
        }

        fun bowCooldownRemainingSeconds(now: Long): Double {
            if (shotState(now) != ShotState.COOLDOWN) return 0.0
            return ((DETECTIVE_BOW_COOLDOWN_MS - (now - lastShotTime)).coerceAtLeast(0L)) / 1000.0
        }
    }

    class ArrowEntry(val ownerUuid: UUID, val spawnTime: Long)

    private val mc = Minecraft.getInstance()
    private val infos = HashMap<UUID, WeaponInfo>()
    private val knifeStands = ObjectOpenHashSet<Int>()
    private val arrows = HashMap<Int, ArrowEntry>()

    fun getInfo(uuid: UUID): WeaponInfo? = infos[uuid]

    /** 获取飞刀投掷物的当前位置（飞行中才有） */
    fun getKnifeEntity(info: WeaponInfo): ArmorStand? {
        if (info.knifeEntityId == -1) return null
        return mc.level?.getEntity(info.knifeEntityId) as? ArmorStand
    }

    /** 获取该玩家射出的、离本地玩家最近的箭矢距离 */
    fun getNearestArrowDistance(uuid: UUID): Double? {
        val local = mc.player ?: return null
        var nearest: Double? = null
        for ((arrowId, entry) in arrows) {
            if (entry.ownerUuid != uuid) continue
            val arrow = mc.level?.getEntity(arrowId) as? AbstractArrow ?: continue
            val dist = arrow.position().distanceTo(local.position())
            if (nearest == null || dist < nearest) nearest = dist
        }
        return nearest
    }

    fun tick() {
        val level = mc.level ?: return
        if (mc.player == null) return
        val now = System.currentTimeMillis()

        val seenPlayers = ObjectOpenHashSet<UUID>()

        for (entity in level.entitiesForRendering()) {
            when (entity) {
                is AbstractClientPlayer -> {
                    if (entity === mc.player) continue
                    seenPlayers.add(entity.uuid)
                    trackPlayer(entity, now)
                }

                is ArmorStand -> trackKnifeStand(entity, now)

                is AbstractArrow -> trackArrow(entity, now)
            }
        }

        // 清理消失玩家的信息
        infos.keys.retainAll(seenPlayers)

        // 飞刀盔甲架消失 -> 进入冷却
        for (info in infos.values) {
            if (info.knifeEntityId != -1 && level.getEntity(info.knifeEntityId) == null) {
                finishKnife(info, now)
            } else if (info.knifeEntityId != -1 && now - info.knifeThrowTime > PROJECTILE_TIMEOUT_MS) {
                finishKnife(info, now)
            }
        }
        knifeStands.retainAll { id -> level.getEntity(id) != null }

        // 清理过期箭矢
        arrows.entries.removeIf { (id, entry) ->
            now - entry.spawnTime > ARROW_TIMEOUT_MS || level.getEntity(id) == null
        }
    }

    private fun trackPlayer(player: AbstractClientPlayer, now: Long) {
        val info = infos.computeIfAbsent(player.uuid) { WeaponInfo(it) }

        val mainHand = player.getItemBySlot(EquipmentSlot.MAINHAND)
        val isSword = !mainHand.isEmpty && MurderMysterySwordDetection.isSword(mainHand)
        val isBow = !mainHand.isEmpty && mainHand.item is BowItem

        info.holdingKnife = isSword
        info.holdingBow = isBow
        if (isSword) info.knifeItem = mainHand.item

        if (isSword || isBow) {
            info.lastWeapon = mainHand.copy()
        }

        if (isBow) {
            info.bowCategory = bowCategoryOf(mainHand)
            val using = player.isUsingItem && player.useItem.item is BowItem
            info.drawStart = when {
                using && info.drawStart == 0L -> now
                using -> info.drawStart
                else -> 0L
            }
        } else {
            info.bowCategory = BowCategory.NONE
            info.drawStart = 0L
        }
    }

    /**
     * 盔甲架主手持刀 = 飞刀投掷物
     * 按「手持相同刀具 + 还没有飞行中的刀 + 距离最近」匹配投掷者
     */
    private fun trackKnifeStand(stand: ArmorStand, now: Long) {
        if (stand.id in knifeStands) return

        val mainHand = stand.getItemBySlot(EquipmentSlot.MAINHAND)
        if (mainHand.isEmpty) return
        if (!MurderMysterySwordDetection.isSword(mainHand)) return
        if (!stand.isInvisible) return

        val level = mc.level ?: return
        var bestInfo: WeaponInfo? = null
        var bestDistance = KNIFE_MATCH_MAX_DISTANCE

        for (info in infos.values) {
            if (!info.holdingKnife) continue
            if (info.knifeEntityId != -1) continue
            if (info.knifeItem != mainHand.item) continue

            val player = level.getPlayerByUUID(info.uuid) as? AbstractClientPlayer ?: continue
            val distance = player.distanceTo(stand).toDouble()
            if (distance < bestDistance) {
                bestDistance = distance
                bestInfo = info
            }
        }

        if (bestInfo != null) {
            bestInfo.knifeEntityId = stand.id
            bestInfo.knifeThrowTime = now
            knifeStands.add(stand.id)
        }
    }

    /** 箭矢实体出现 = 该玩家射出了一箭 */
    private fun trackArrow(arrow: AbstractArrow, now: Long) {
        if (arrow.id in arrows) return
        if (arrow.owner !is AbstractClientPlayer) return
        val owner = arrow.owner as AbstractClientPlayer
        if (owner === mc.player) return

        val info = infos[owner.uuid] ?: return
        arrows[arrow.id] = ArrowEntry(owner.uuid, now)
        info.lastShotTime = now
        info.drawStart = 0L
    }

    private fun finishKnife(info: WeaponInfo, now: Long) {
        info.knifeEntityId = -1
        info.lastDestroyTime = now
    }

    /** 按物品组件判断弓类型（移植自 1.8.9 ItemClassifier.getBowCategory） */
    fun bowCategoryOf(stack: ItemStack): BowCategory {
        val enchantments = stack.get(DataComponents.ENCHANTMENTS)
        if (enchantments != null) {
            for (holder in enchantments.keySet()) {
                val key = holder.unwrapKey().orElse(null) ?: continue
                if (key.identifier().path == "infinity") return BowCategory.KALI
            }
        }

        return when (stack.get(DataComponents.LORE)?.lines?.size ?: 0) {
            1 -> BowCategory.DETECTIVE
            2 -> BowCategory.NORMAL
            else -> BowCategory.NORMAL
        }
    }

    fun clear() {
        infos.clear()
        knifeStands.clear()
        arrows.clear()
    }
}

package org.chxjj.mh.murdermystery

import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Camera
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f
import kotlin.math.max

object RenderUtils {
    fun renderBoxAroundEntity(
        matrices: PoseStack,
        camera: Camera,
        entity: Entity,
        fillRed: Float,
        fillGreen: Float,
        fillBlue: Float,
        fillAlpha: Float,
        outlineRed: Float,
        outlineGreen: Float,
        outlineBlue: Float,
        outlineAlpha: Float
    ) {
        val cameraPos = camera.position
        val x = entity.x - cameraPos.x
        val y = entity.y - cameraPos.y
        val z = entity.z - cameraPos.z

        val bb = AABB(
            x - entity.bbWidth / 2.0,
            y,
            z - entity.bbWidth / 2.0,
            x + entity.bbWidth / 2.0,
            y + entity.bbHeight,
            z + entity.bbWidth / 2.0
        )

        drawBox(matrices, bb, fillRed, fillGreen, fillBlue, fillAlpha, outlineRed, outlineGreen, outlineBlue, outlineAlpha)
    }

    fun drawBox(
        matrices: PoseStack,
        bb: AABB,
        fillRed: Float,
        fillGreen: Float,
        fillBlue: Float,
        fillAlpha: Float,
        outlineRed: Float,
        outlineGreen: Float,
        outlineBlue: Float,
        outlineAlpha: Float
    ) {
        val buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR)
        val pose = matrices.last().pose()

        drawFilledBox(pose, buffer, bb, fillRed, fillGreen, fillBlue, fillAlpha)
        BufferUploader.drawWithShader(buffer.buildOrThrow())

        val lineBuffer = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR)
        drawOutlineBox(pose, lineBuffer, bb, outlineRed, outlineGreen, outlineBlue, outlineAlpha)
        BufferUploader.drawWithShader(lineBuffer.buildOrThrow())
    }

    private fun drawFilledBox(
        pose: Matrix4f,
        buffer: VertexConsumer,
        bb: AABB,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        val minX = bb.minX.toFloat()
        val minY = bb.minY.toFloat()
        val minZ = bb.minZ.toFloat()
        val maxX = bb.maxX.toFloat()
        val maxY = bb.maxY.toFloat()
        val maxZ = bb.maxZ.toFloat()

        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha)
    }

    private fun drawOutlineBox(
        pose: Matrix4f,
        buffer: VertexConsumer,
        bb: AABB,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        val minX = bb.minX.toFloat()
        val minY = bb.minY.toFloat()
        val minZ = bb.minZ.toFloat()
        val maxX = bb.maxX.toFloat()
        val maxY = bb.maxY.toFloat()
        val maxZ = bb.maxZ.toFloat()

        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, minY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, minY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(red, green, blue, alpha)
        buffer.addVertex(pose, minX, maxY, minZ).setColor(red, green, blue, alpha)
    }
}

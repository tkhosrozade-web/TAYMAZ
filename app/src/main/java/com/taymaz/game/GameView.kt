package com.taymaz.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World

class GameView(context: Context) : View(context) {

    private enum class Phase { AIMING, SETTLING, LEVEL_DONE, GAME_OVER, ALL_DONE }

    private class Placed(val body: Body, val def: ItemDef)

    companion object {
        const val WORLD_W = 10f          // world width in meters (screen is always 10 m wide)
        const val TABLE_W = 6f
        const val TABLE_H = 0.5f
        const val STEP = 1f / 60f
    }

    private var scale = 1f
    private var worldH = 20f
    private var tableTop = 17f

    private var world = World(Vec2(0f, 12f))
    private val placed = ArrayList<Placed>()

    private var levelIndex = 0
    private var itemIndex = 0
    private var total = 0
    private var phase = Phase.AIMING

    private var heldAngle = 0f
    private var hoverX = WORLD_W / 2
    private var dragging = false

    private var settleTimer = 0f
    private var waitTimer = 0f
    private var acc = 0f
    private var lastNs = 0L

    private val rotateBtn = RectF()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(160, 40, 40, 40)
        strokeWidth = 0.05f
    }
    private val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(70, 255, 255, 255)
        strokeWidth = 0.04f
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private val bgColor = Color.rgb(38, 50, 56)
    private val tableColor = Color.rgb(141, 110, 99)

    private fun currentLevel() = Items.levels[levelIndex]
    private fun currentItem(): ItemDef? = currentLevel().getOrNull(itemIndex)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        scale = w / WORLD_W
        worldH = h / scale
        tableTop = worldH - 3f
        rotateBtn.set(w - 320f, 50f, w - 40f, 180f)
        buildLevel()
    }

    private fun buildLevel() {
        world = World(Vec2(0f, 12f))
        placed.clear()
        itemIndex = 0
        heldAngle = 0f
        hoverX = WORLD_W / 2
        dragging = false
        settleTimer = 0f
        waitTimer = 0f
        phase = Phase.AIMING

        val bd = BodyDef()
        bd.type = BodyType.STATIC
        bd.position.set(WORLD_W / 2, tableTop + TABLE_H / 2)
        val table = world.createBody(bd)
        val shape = PolygonShape()
        shape.setAsBox(TABLE_W / 2, TABLE_H / 2)
        val fd = FixtureDef()
        fd.shape = shape
        fd.friction = 0.8f
        table.createFixture(fd)
    }

    private fun towerTop(): Float {
        var top = tableTop
        for (p in placed) {
            var f = p.body.fixtureList
            while (f != null) {
                val y = f.getAABB(0).lowerBound.y
                if (y < top) top = y
                f = f.next
            }
        }
        return top
    }

    private fun hoverY(): Float = maxOf(1.2f, towerTop() - 2.6f)

    private fun drop() {
        val def = currentItem() ?: return
        val bd = BodyDef()
        bd.type = BodyType.DYNAMIC
        bd.position.set(hoverX, hoverY())
        bd.angle = heldAngle
        val body = world.createBody(bd)
        for (p in def.parts) {
            val fd = FixtureDef()
            if (p.type == PartType.BOX) {
                val s = PolygonShape()
                s.setAsBox(p.a / 2, p.b / 2, Vec2(p.ox, p.oy), 0f)
                fd.shape = s
            } else {
                val s = CircleShape()
                s.m_radius = p.a
                s.m_p.set(p.ox, p.oy)
                fd.shape = s
            }
            fd.density = def.density
            fd.friction = def.friction
            fd.restitution = 0.05f
            body.createFixture(fd)
        }
        placed.add(Placed(body, def))
        itemIndex++
        heldAngle = 0f
        phase = Phase.SETTLING
        settleTimer = 0f
        waitTimer = 0f
    }

    private fun update(dt: Float) {
        acc += dt
        while (acc >= STEP) {
            world.step(STEP, 8, 3)
            acc -= STEP
        }
        if (phase != Phase.AIMING && phase != Phase.SETTLING) return

        // anything that fell below the table = game over
        for (p in placed) {
            if (p.body.position.y > tableTop + 0.3f) {
                phase = Phase.GAME_OVER
                return
            }
        }

        if (phase == Phase.SETTLING) {
            waitTimer += dt
            var still = true
            for (p in placed) {
                if (p.body.linearVelocity.lengthSquared() > 0.02f ||
                    Math.abs(p.body.angularVelocity) > 0.1f
                ) still = false
            }
            settleTimer = if (still) settleTimer + dt else 0f

            if (settleTimer > 1f || waitTimer > 6f) {
                if (itemIndex >= currentLevel().size) {
                    total += currentLevel().size
                    phase = if (levelIndex >= Items.levels.size - 1) Phase.ALL_DONE else Phase.LEVEL_DONE
                } else {
                    phase = Phase.AIMING
                }
            }
        }
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        var dt = if (lastNs == 0L) 0f else (now - lastNs) / 1e9f
        lastNs = now
        if (dt > 0.05f) dt = 0.05f
        update(dt)

        c.drawColor(bgColor)

        c.save()
        c.scale(scale, scale)

        // table + legs
        fill.color = tableColor
        fill.alpha = 255
        val left = WORLD_W / 2 - TABLE_W / 2
        val right = WORLD_W / 2 + TABLE_W / 2
        c.drawRect(left, tableTop, right, tableTop + TABLE_H, fill)
        c.drawRect(left + 0.4f, tableTop + TABLE_H, left + 0.8f, worldH, fill)
        c.drawRect(right - 0.8f, tableTop + TABLE_H, right - 0.4f, worldH, fill)

        for (p in placed) {
            drawDef(c, p.def, p.body.position.x, p.body.position.y, p.body.angle, 255)
        }

        if (phase == Phase.AIMING) {
            currentItem()?.let {
                c.drawLine(hoverX, hoverY(), hoverX, tableTop, guide)
                drawDef(c, it, hoverX, hoverY(), heldAngle, 210)
            }
        }
        c.restore()

        drawHud(c)
        postInvalidateOnAnimation()
    }

    private fun drawDef(c: Canvas, def: ItemDef, x: Float, y: Float, angle: Float, alpha: Int) {
        c.save()
        c.translate(x, y)
        c.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        for (p in def.parts) {
            fill.color = p.color
            fill.alpha = alpha
            if (p.type == PartType.BOX) {
                val l = p.ox - p.a / 2
                val t = p.oy - p.b / 2
                c.drawRoundRect(l, t, l + p.a, t + p.b, 0.06f, 0.06f, fill)
                c.drawRoundRect(l, t, l + p.a, t + p.b, 0.06f, 0.06f, line)
            } else {
                c.drawCircle(p.ox, p.oy, p.a, fill)
                c.drawCircle(p.ox, p.oy, p.a, line)
            }
        }
        c.restore()
    }

    private fun drawHud(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val ts = w * 0.055f

        text.textAlign = Paint.Align.LEFT
        text.textSize = ts
        c.drawText("مرحله ${levelIndex + 1}", 40f, 100f, text)
        c.drawText("امتیاز: $total", 40f, 100f + ts * 1.3f, text)
        currentItem()?.let {
            if (phase == Phase.AIMING) {
                c.drawText("${it.name}  (${itemIndex + 1}/${currentLevel().size})", 40f, 100f + ts * 2.6f, text)
            }
        }

        if (phase == Phase.AIMING) {
            fill.color = Color.rgb(0, 150, 136)
            fill.alpha = 255
            c.drawRoundRect(rotateBtn, 30f, 30f, fill)
            text.textAlign = Paint.Align.CENTER
            c.drawText("چرخش ⟳", rotateBtn.centerX(), rotateBtn.centerY() + ts * 0.35f, text)
            text.textSize = ts * 0.7f
            c.drawText("بکش و رها کن", w / 2, h - 60f, text)
        }

        val msg: String? = when (phase) {
            Phase.LEVEL_DONE -> "آفرین! مرحله تمام شد ✅"
            Phase.GAME_OVER -> "افتاد! دوباره تلاش کن"
            Phase.ALL_DONE -> "همه مرحله‌ها تمام شد 🎉"
            else -> null
        }
        if (msg != null) {
            fill.color = Color.argb(150, 0, 0, 0)
            fill.alpha = 150
            c.drawRect(0f, 0f, w, h, fill)
            text.textAlign = Paint.Align.CENTER
            text.textSize = ts * 1.2f
            c.drawText(msg, w / 2, h / 2, text)
            text.textSize = ts * 0.8f
            c.drawText("برای ادامه لمس کن", w / 2, h / 2 + ts * 1.6f, text)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (phase) {
            Phase.AIMING -> when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (rotateBtn.contains(e.x, e.y)) {
                        heldAngle += (Math.PI / 4).toFloat()
                    } else {
                        dragging = true
                        hoverX = (e.x / scale).coerceIn(0.6f, WORLD_W - 0.6f)
                    }
                }
                MotionEvent.ACTION_MOVE -> if (dragging) {
                    hoverX = (e.x / scale).coerceIn(0.6f, WORLD_W - 0.6f)
                }
                MotionEvent.ACTION_UP -> if (dragging) {
                    dragging = false
                    drop()
                }
                MotionEvent.ACTION_CANCEL -> dragging = false
            }
            Phase.LEVEL_DONE -> if (e.actionMasked == MotionEvent.ACTION_UP) {
                levelIndex++
                buildLevel()
            }
            Phase.GAME_OVER -> if (e.actionMasked == MotionEvent.ACTION_UP) {
                buildLevel()
            }
            Phase.ALL_DONE -> if (e.actionMasked == MotionEvent.ACTION_UP) {
                levelIndex = 0
                total = 0
                buildLevel()
            }
            Phase.SETTLING -> {}
        }
        return true
    }
}

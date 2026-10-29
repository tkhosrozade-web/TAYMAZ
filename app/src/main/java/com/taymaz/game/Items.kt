package com.taymaz.game

import android.graphics.Color

enum class PartType { BOX, CIRCLE }

/** BOX: a = width, b = height.  CIRCLE: a = radius. (ox, oy) = offset from the item's center (meters). */
class Part(
    val type: PartType,
    val ox: Float,
    val oy: Float,
    val a: Float,
    val b: Float,
    val color: Int
)

class ItemDef(
    val name: String,
    val parts: List<Part>,
    val density: Float = 1f,
    val friction: Float = 0.6f
)

object Items {

    private fun box(w: Float, h: Float, color: Int, ox: Float = 0f, oy: Float = 0f) =
        Part(PartType.BOX, ox, oy, w, h, color)

    private fun ball(r: Float, color: Int, ox: Float = 0f, oy: Float = 0f) =
        Part(PartType.CIRCLE, ox, oy, r, r, color)

    // ---- Level 1: simple, flat things ----
    val eraser = ItemDef("پاک‌کن", listOf(box(1.5f, 0.7f, Color.rgb(244, 143, 177))), friction = 0.9f)
    val sharpener = ItemDef("تراش", listOf(box(1.1f, 0.8f, Color.rgb(100, 181, 246))), friction = 0.8f)
    val pencil = ItemDef(
        "مداد",
        listOf(
            box(2.4f, 0.26f, Color.rgb(255, 213, 79)),
            box(0.3f, 0.26f, Color.rgb(240, 98, 146), ox = -1.35f),
            box(0.3f, 0.20f, Color.rgb(215, 180, 140), ox = 1.35f)
        ),
        friction = 0.5f
    )

    // ---- Level 2+: bigger / trickier ----
    val book = ItemDef("کتاب", listOf(box(2.6f, 0.5f, Color.rgb(77, 182, 172))), friction = 0.7f)
    val cup = ItemDef("لیوان", listOf(box(1.0f, 1.2f, Color.rgb(255, 138, 101))), friction = 0.6f)
    val phone = ItemDef("گوشی", listOf(box(0.8f, 1.5f, Color.rgb(96, 125, 139))), friction = 0.5f)
    val ruler = ItemDef("خط‌کش", listOf(box(3.2f, 0.3f, Color.rgb(255, 241, 118))), friction = 0.5f)
    val bottle = ItemDef(
        "بطری",
        listOf(
            box(0.7f, 1.7f, Color.rgb(129, 199, 132)),
            box(0.3f, 0.3f, Color.rgb(66, 66, 66), oy = -1.0f)
        ),
        friction = 0.5f
    )

    // ---- Round and awkward ----
    val apple = ItemDef(
        "سیب",
        listOf(
            ball(0.45f, Color.rgb(229, 57, 53)),
            box(0.15f, 0.15f, Color.rgb(67, 160, 71), oy = -0.5f)
        ),
        friction = 0.5f
    )
    val ball = ItemDef("توپ", listOf(ball(0.5f, Color.rgb(255, 167, 38))), friction = 0.5f)
    val kettle = ItemDef(
        "قوری",
        listOf(
            ball(0.7f, Color.rgb(171, 71, 188)),
            box(0.5f, 0.25f, Color.rgb(123, 31, 162), oy = -0.8f)
        ),
        friction = 0.6f
    )

    val levels: List<List<ItemDef>> = listOf(
        listOf(eraser, sharpener, pencil),
        listOf(book, cup, phone, ruler),
        listOf(book, bottle, ruler, cup, apple),
        listOf(book, ruler, cup, ball, phone, kettle),
        listOf(book, ruler, bottle, kettle, cup, apple, ball, pencil)
    )
}

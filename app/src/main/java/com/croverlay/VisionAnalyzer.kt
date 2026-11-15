package com.croverlay

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Screen region analysis for Clash Royale.
 * CR uses a fixed layout — regions are relative to screen dimensions.
 *
 * Layout (portrait):
 *   Top 15%     — enemy towers + king tower
 *   15-50%      — enemy arena half
 *   50-85%      — my arena half
 *   85-90%      — elixir bar
 *   90-100%     — hand cards (4 slots)
 */
object VisionAnalyzer {

    fun analyze(bitmap: Bitmap): ArenaState {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val elixir = estimateElixir(bitmap, w, h)
        val timeRemaining = estimateTime(bitmap, w, h)
        val phase = detectPhase(bitmap, w, h)
        val handCards = detectHandCards(bitmap, w, h)
        val myTowerLeft = estimateTowerHp(bitmap, w, h, TowerPos.MY_LEFT)
        val myTowerRight = estimateTowerHp(bitmap, w, h, TowerPos.MY_RIGHT)
        val enemyTowerLeft = estimateTowerHp(bitmap, w, h, TowerPos.ENEMY_LEFT)
        val enemyTowerRight = estimateTowerHp(bitmap, w, h, TowerPos.ENEMY_RIGHT)

        return ArenaState(
            elixir = elixir,
            timeRemaining = timeRemaining,
            myTowerHpLeft = myTowerLeft,
            myTowerHpRight = myTowerRight,
            enemyTowerHpLeft = enemyTowerLeft,
            enemyTowerHpRight = enemyTowerRight,
            handCards = handCards,
            isDoublElixir = phase != GamePhase.NORMAL,
            phase = phase
        )
    }

    /**
     * Elixir bar: bottom ~8% of screen, purple pixels density
     * Returns 0-10
     */
    private fun estimateElixir(bmp: Bitmap, w: Float, h: Float): Float {
        val barY = (h * 0.88f).toInt()
        val barXStart = (w * 0.05f).toInt()
        val barXEnd = (w * 0.95f).toInt()
        val barHeight = (h * 0.015f).toInt().coerceAtLeast(1)

        var purplePixels = 0
        var totalPixels = 0

        for (x in barXStart..barXEnd step 3) {
            for (y in barY until (barY + barHeight)) {
                if (y >= bmp.height) continue
                val pixel = bmp.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Purple elixir color: high R, low G, high B
                if (r > 100 && g < 80 && b > 100) purplePixels++
                totalPixels++
            }
        }

        if (totalPixels == 0) return 5f
        val ratio = purplePixels.toFloat() / totalPixels
        return (ratio * 10f / 0.9f).coerceIn(0f, 10f)
    }

    /**
     * Time remaining: read timer region at top center
     * FILL: integrate ML Kit Text Recognition for exact time
     */
    private fun estimateTime(bmp: Bitmap, w: Float, h: Float): Int {
        return 180
    }

    private fun detectPhase(bmp: Bitmap, w: Float, h: Float): GamePhase {
        // Double elixir: check if elixir bar has a "2x" indicator (orange/yellow region)
        val indicatorX = (w * 0.5f).toInt()
        val indicatorY = (h * 0.87f).toInt()
        if (indicatorX < bmp.width && indicatorY < bmp.height) {
            val pixel = bmp.getPixel(indicatorX, indicatorY)
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            if (r > 180 && g > 120) return GamePhase.DOUBLE_ELIXIR
        }
        return GamePhase.NORMAL
    }

    /**
     * Hand cards: bottom 10% of screen, 4 card slots
     * Cards have distinct colored backgrounds — detect by dominant color per slot
     */
    private fun detectHandCards(bmp: Bitmap, w: Float, h: Float): List<DetectedCard> {
        val cards = mutableListOf<DetectedCard>()
        val slotWidth = w / 5f
        val cardY = (h * 0.91f).toInt()
        val cardH = (h * 0.08f).toInt()

        for (slot in 0..3) {
            val slotX = (slotWidth * (slot + 0.5f)).toInt()
            if (slotX >= bmp.width || cardY >= bmp.height) continue

            val dominantColor = getDominantColor(bmp, slotX, cardY,
                (slotWidth * 0.4f).toInt(), cardH)

            // FILL: replace with ML Kit image classification trained on CR card images
            val cardType = classifyCardByColor(dominantColor)
            cards.add(DetectedCard(
                name = "Card_${slot}", // FILL: ML Kit label
                cost = estimateCostFromColor(dominantColor),
                type = cardType,
                slot = slot
            ))
        }
        return cards
    }

    private fun getDominantColor(bmp: Bitmap, cx: Int, cy: Int, hw: Int, hh: Int): Triple<Int, Int, Int> {
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0
        for (x in (cx - hw).coerceAtLeast(0)..(cx + hw).coerceAtMost(bmp.width - 1) step 2) {
            for (y in cy.coerceAtLeast(0)..(cy + hh).coerceAtMost(bmp.height - 1) step 2) {
                val p = bmp.getPixel(x, y)
                rSum += Color.red(p); gSum += Color.green(p); bSum += Color.blue(p); count++
            }
        }
        if (count == 0) return Triple(0, 0, 0)
        return Triple((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }

    private fun classifyCardByColor(color: Triple<Int, Int, Int>): CardType {
        val (r, g, b) = color
        return when {
            r > 150 && g < 100 -> CardType.TROOP    // reddish = melee troops
            g > 150 && r < 100 -> CardType.SPELL    // greenish = spells
            b > 150 && r < 100 -> CardType.BUILDING // bluish = buildings
            else -> CardType.CYCLE
        }
    }

    private fun estimateCostFromColor(color: Triple<Int, Int, Int>): Int {
        // FILL: ML Kit classification gives exact cost
        return 3
    }

    private fun estimateTowerHp(bmp: Bitmap, w: Float, h: Float, pos: TowerPos): Float {
        val (cx, cy) = when (pos) {
            TowerPos.MY_LEFT     -> Pair(w * 0.18f, h * 0.78f)
            TowerPos.MY_RIGHT    -> Pair(w * 0.82f, h * 0.78f)
            TowerPos.ENEMY_LEFT  -> Pair(w * 0.18f, h * 0.18f)
            TowerPos.ENEMY_RIGHT -> Pair(w * 0.82f, h * 0.18f)
        }
        val x = cx.toInt().coerceIn(0, bmp.width - 1)
        val y = cy.toInt().coerceIn(0, bmp.height - 1)
        val pixel = bmp.getPixel(x, y)
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        return when {
            g > 150 && r < 100 -> 1.0f
            g > 100 && r > 100 -> 0.5f
            r > 150 && g < 80  -> 0.2f
            else -> 0.75f
        }
    }

    enum class TowerPos { MY_LEFT, MY_RIGHT, ENEMY_LEFT, ENEMY_RIGHT }
}

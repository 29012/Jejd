package com.croverlay

/**
 * Pro-level placement strategy engine.
 * Rules derived from high-level CR meta:
 * - Elixir advantage management
 * - Lane pressure + split push detection
 * - Counter-push timing
 * - Bridge spam recognition
 */
class StrategyEngine {

    fun recommend(state: ArenaState): List<Recommendation> {
        val recs = mutableListOf<Recommendation>()

        if (state.handCards.isEmpty()) return recs

        val elixirAdvantage = estimateElixirAdvantage(state)
        val weakLane = findWeakLane(state)
        val strongLane = findStrongLane(state)

        for (card in state.handCards) {
            if (card.cost > state.elixir) continue // Can't play yet

            val rec = when {
                // URGENT: defend if tower at critical HP
                state.myTowerHpLeft < 0.3f && card.type == CardType.TROOP ->
                    defendLane(card, LaneSide.LEFT, "Башня слева критически повреждена!")

                state.myTowerHpRight < 0.3f && card.type == CardType.TROOP ->
                    defendLane(card, LaneSide.RIGHT, "Башня справа критически повреждена!")

                // Double elixir: be more aggressive
                state.phase == GamePhase.DOUBLE_ELIXIR && elixirAdvantage > 2f ->
                    pushLane(card, strongLane, "Двойной эликсир — атакуй! Преимущество +${elixirAdvantage.toInt()}")

                // Elixir lead: apply pressure
                elixirAdvantage > 3f && card.type == CardType.TROOP ->
                    pushLane(card, weakLane, "Преимущество по эликсиру — давим ${if (weakLane == LaneSide.LEFT) "влево" else "вправо"}")

                // Spell: use when enemy has clump
                card.type == CardType.SPELL && hasEnemyClump(state) ->
                    Recommendation(card, 0.5f, 0.35f, Priority.HIGH,
                        "Скопление противника — используй заклинание", LaneSide.CENTER)

                // Building: place defensively in center
                card.type == CardType.BUILDING ->
                    Recommendation(card, 0.5f, 0.65f, Priority.MEDIUM,
                        "Здание в центр — отвлечёт танк", LaneSide.CENTER)

                // Cycle card: maintain elixir flow
                card.type == CardType.CYCLE ->
                    cyclePlacement(card, state)

                // Default: defend or counter-push
                else -> counterPush(card, state)
            }
            recs.add(rec)
        }

        return recs.sortedByDescending { it.priority.ordinal }
    }

    private fun estimateElixirAdvantage(state: ArenaState): Float {
        val towerDamageDealt = (2f - state.enemyTowerHpLeft - state.enemyTowerHpRight)
        val towerDamageReceived = (2f - state.myTowerHpLeft - state.myTowerHpRight)
        return (towerDamageDealt - towerDamageReceived) * 3f + (state.elixir - 5f) * 0.5f
    }

    private fun findWeakLane(state: ArenaState): LaneSide {
        return if (state.myTowerHpLeft < state.myTowerHpRight) LaneSide.LEFT else LaneSide.RIGHT
    }

    private fun findStrongLane(state: ArenaState): LaneSide {
        return if (state.enemyTowerHpLeft < state.enemyTowerHpRight) LaneSide.LEFT else LaneSide.RIGHT
    }

    private fun hasEnemyClump(state: ArenaState): Boolean {
        val enemyUnits = state.fieldUnits.filter { !it.isMine }
        if (enemyUnits.size < 3) return false
        for (unit in enemyUnits) {
            val nearby = enemyUnits.count { other ->
                other != unit &&
                Math.abs(other.x - unit.x) < 0.1f &&
                Math.abs(other.y - unit.y) < 0.1f
            }
            if (nearby >= 2) return true
        }
        return false
    }

    private fun defendLane(card: DetectedCard, side: LaneSide, reason: String): Recommendation {
        val x = if (side == LaneSide.LEFT) 0.25f else 0.75f
        return Recommendation(card, x, 0.72f, Priority.URGENT, reason, side)
    }

    private fun pushLane(card: DetectedCard, side: LaneSide, reason: String): Recommendation {
        val x = when (side) {
            LaneSide.LEFT -> 0.2f
            LaneSide.RIGHT -> 0.8f
            else -> 0.5f
        }
        return Recommendation(card, x, 0.52f, Priority.HIGH, reason, side)
    }

    private fun cyclePlacement(card: DetectedCard, state: ArenaState): Recommendation {
        return Recommendation(card, 0.5f, 0.75f, Priority.LOW,
            "Цикл — ставь сзади для набора эликсира", LaneSide.CENTER)
    }

    private fun counterPush(card: DetectedCard, state: ArenaState): Recommendation {
        val myUnitsInFront = state.fieldUnits.filter { it.isMine && it.y < 0.5f }
        return if (myUnitsInFront.isNotEmpty()) {
            val frontUnit = myUnitsInFront.minByOrNull { it.y }!!
            Recommendation(card, frontUnit.x, frontUnit.y + 0.05f,
                Priority.MEDIUM, "Контратака — поддержи свой отряд", LaneSide.CENTER)
        } else {
            Recommendation(card, 0.5f, 0.65f, Priority.LOW, "Держи у моста", LaneSide.CENTER)
        }
    }
}

package com.croverlay

data class ArenaState(
    val elixir: Float = 0f,           // 0-10 estimated
    val timeRemaining: Int = 180,      // seconds
    val myTowerHpLeft: Float = 1f,     // 0-1
    val myTowerHpRight: Float = 1f,
    val enemyTowerHpLeft: Float = 1f,
    val enemyTowerHpRight: Float = 1f,
    val myKingHp: Float = 1f,
    val enemyKingHp: Float = 1f,
    val handCards: List<DetectedCard> = emptyList(),
    val fieldUnits: List<FieldUnit> = emptyList(),
    val isDoublElixir: Boolean = false,
    val phase: GamePhase = GamePhase.NORMAL
)

enum class GamePhase { NORMAL, DOUBLE_ELIXIR, TRIPLE_ELIXIR, SUDDEN_DEATH }

data class DetectedCard(
    val name: String,
    val cost: Int,
    val type: CardType,
    val slot: Int  // 0-3 hand position
)

data class FieldUnit(
    val name: String,
    val isMine: Boolean,
    val x: Float,  // 0-1 normalized screen position
    val y: Float,
    val hp: Float  // 0-1
)

enum class CardType { TROOP, SPELL, BUILDING, CYCLE }

data class Recommendation(
    val card: DetectedCard,
    val placementX: Float,     // 0-1 arena X
    val placementY: Float,     // 0-1 arena Y
    val priority: Priority,
    val reason: String,
    val laneSide: LaneSide
)

enum class Priority { URGENT, HIGH, MEDIUM, LOW }
enum class LaneSide { LEFT, RIGHT, CENTER, BRIDGE_LEFT, BRIDGE_RIGHT }
